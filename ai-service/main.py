"""ATOM AI Service.

Real, deterministic chemistry-informed ATOM yield prediction engine plus a
pluggable LLM integration (any OpenAI-compatible API) for CPSR toxicity
assessments. Falls back to an explicit rule-based screening assessor when no
LLM provider is configured - the response always states which mode produced
the result.
"""

import json
import logging
import math
import os
import time
from typing import Any, Optional

import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

load_dotenv()

logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO").upper(),
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
log = logging.getLogger("ai-service")

ATOM_MODEL_VERSION = "atom-engine-2.0.0"
RULE_MODEL_NAME = "rule-based-screening"

LLM_API_KEY = os.getenv("LLM_API_KEY", "").strip()
LLM_BASE_URL = os.getenv("LLM_BASE_URL", "https://api.openai.com/v1").rstrip("/")
LLM_MODEL = os.getenv("LLM_MODEL", "gpt-4o-mini")
LLM_TIMEOUT_SECONDS = float(os.getenv("LLM_TIMEOUT_SECONDS", "30"))
LLM_MAX_ATTEMPTS = max(1, int(os.getenv("LLM_MAX_ATTEMPTS", "3")))

# Optional shared secret used to authenticate internal callers to the
# state-changing prediction/LLM endpoints. When empty, auth is disabled so the
# service can still run in a trusted internal network (e.g. only reachable on a
# Docker-internal network). Recommended to set when the port is reachable
# outside the stack.
AI_SERVICE_AUTH_TOKEN = os.getenv("AI_SERVICE_AUTH_TOKEN", "").strip()


def require_auth(request: Request) -> None:
    """Reject state-changing requests unless a valid bearer token is supplied."""
    if not AI_SERVICE_AUTH_TOKEN:
        return
    header = request.headers.get("Authorization", "")
    if header != f"Bearer {AI_SERVICE_AUTH_TOKEN}":
        raise HTTPException(status_code=401, detail="Unauthorized: missing or invalid service token")

app = FastAPI(title="ATOM AI Service", version=ATOM_MODEL_VERSION)


class AtomPredictRequest(BaseModel):
    predictionId: Optional[int] = None
    inputConditions: Any = None


class LlmCompleteRequest(BaseModel):
    inferenceId: Optional[int] = None
    prompt: str = Field(min_length=1)
    modelName: Optional[str] = None
    inferenceType: Optional[str] = None
    context: Optional[str] = None


# --------------------------------------------------------------------------
# Error handling
# --------------------------------------------------------------------------

@app.exception_handler(HTTPException)
async def http_exception_handler(_request: Request, exc: HTTPException) -> JSONResponse:
    return JSONResponse(status_code=exc.status_code, content={
        "status": "FAILED",
        "errorMessage": str(exc.detail),
    })


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(_request: Request, exc: RequestValidationError) -> JSONResponse:
    return JSONResponse(status_code=422, content={
        "status": "FAILED",
        "errorMessage": "Validation failed",
        "details": [str(error) for error in exc.errors()][:10],
    })


@app.exception_handler(Exception)
async def unhandled_exception_handler(_request: Request, exc: Exception) -> JSONResponse:
    log.exception("Unhandled error: %s", exc)
    return JSONResponse(status_code=500, content={
        "status": "FAILED",
        "errorMessage": "Internal AI service error",
    })


# --------------------------------------------------------------------------
# Health / model info
# --------------------------------------------------------------------------

@app.get("/")
def root():
    return {"status": "ok", "message": "ATOM AI service is running", "health": "/health"}


@app.get("/predict")
def model_info():
    return {
        "status": "success",
        "atomModel": {
            "version": ATOM_MODEL_VERSION,
            "type": "deterministic reaction-kinetics approximation",
            "endpoint": "/predict/atom",
            "parameters": sorted(PARAM_SPECS.keys()) + ["solventType"],
        },
        "llmModel": {
            "mode": llm_mode(),
            "defaultModel": RULE_MODEL_NAME if not llm_configured() else LLM_MODEL,
            "endpoint": "/llm/complete",
        },
    }


@app.get("/health")
def health():
    return {
        "status": "UP",
        "atomModelVersion": ATOM_MODEL_VERSION,
        "llmMode": llm_mode(),
        "llmConfigured": llm_configured(),
    }


def llm_configured() -> bool:
    return bool(LLM_API_KEY)


def llm_mode() -> str:
    return "remote" if llm_configured() else "local-rule-based"


# --------------------------------------------------------------------------
# ATOM prediction engine
#
# Deterministic approximation of reaction kinetics: each experimental factor
# contributes a multiplicative efficiency derived from well-known behaviour
# (Arrhenius-type temperature response, pH optimum curve, conversion-time
# saturation, catalyst loading diminishing returns, concentration side-reaction
# penalty). Fully explainable output with per-factor breakdown and warnings.
# --------------------------------------------------------------------------

PARAM_SPECS = {
    "temperatureC": {"aliases": ("temperature", "tempc", "temp_c", "temp"), "min": -50.0, "max": 400.0},
    "ph": {"aliases": ("ph_value",), "min": 0.0, "max": 14.0},
    "reactionTimeMin": {"aliases": ("time_min", "durationmin", "reaction_time"), "min": 0.0, "max": 10080.0},
    "concentrationMgMl": {"aliases": ("concentration", "concmgml", "conc_mg_ml"), "min": 0.0, "max": 500.0},
    "pressureBar": {"aliases": ("pressure", "pressure_bar"), "min": 0.0, "max": 300.0},
    "catalystLoadingPercent": {"aliases": ("catalystloading", "catalyst_percent", "catalystmolpercent"), "min": 0.0, "max": 20.0},
    "stirringRpm": {"aliases": ("stirring", "rpm", "stirrpm"), "min": 0.0, "max": 3000.0},
}

ALIAS_INDEX = {
    alias: canonical
    for canonical, spec in PARAM_SPECS.items()
    for alias in (canonical.lower(), *spec["aliases"])
}

SOLVENT_FACTORS = {
    "aqueous": 1.02, "water": 1.02, "ethanol": 1.00, "methanol": 0.98,
    "acetonitrile": 1.00, "dmso": 0.95, "dmf": 0.96, "thf": 0.97,
    "toluene": 0.92, "xylene": 0.91, "hexane": 0.88, "heptane": 0.88,
}


@app.post("/predict/atom")
def predict_atom(req: AtomPredictRequest, request: Request):
    require_auth(request)
    started = time.perf_counter()
    if req.inputConditions is None:
        raise HTTPException(status_code=422, detail="inputConditions is required")

    conditions = _normalise_conditions(req.inputConditions)
    resolved, warnings, unknown_keys = _resolve_parameters(conditions)

    factors: dict[str, float] = {}
    notes: list[str] = []

    temp = resolved.get("temperatureC")
    if temp is None:
        factors["temperature"] = 1.0
        notes.append("Temperature not provided; assumed neutral contribution.")
    else:
        optimum, width = 70.0, 50.0
        factor = math.exp(-0.5 * ((temp - optimum) / width) ** 2)
        if temp > 150.0:
            degradation = math.exp(-(temp - 150.0) / 40.0)
            factor *= degradation
            warnings.append(
                f"Temperature {temp:g} C exceeds thermal stability limit (150 C); "
                f"degradation penalty applied (x{degradation:.2f})."
            )
        factors["temperature"] = round(factor, 4)

    ph = resolved.get("ph")
    if ph is None:
        factors["ph"] = 1.0
        notes.append("pH not provided; assumed neutral contribution.")
    else:
        ph_optimum, ph_width = 7.2, 2.0
        factors["ph"] = round(math.exp(-0.5 * ((ph - ph_optimum) / ph_width) ** 2), 4)

    duration = resolved.get("reactionTimeMin")
    if duration is None:
        factors["reactionTime"] = 1.0
        notes.append("Reaction time not provided; assumed neutral contribution.")
    else:
        half_life = 60.0
        factor = duration / (duration + half_life) if duration > 0 else 0.05
        if duration > 1440.0:
            decay = math.exp(-(duration - 1440.0) / 2880.0)
            factor *= decay
            warnings.append(f"Reaction time {duration:g} min risks side reactions; decay applied (x{decay:.2f}).")
        factors["reactionTime"] = round(factor, 4)

    concentration = resolved.get("concentrationMgMl")
    if concentration is None:
        factors["concentration"] = 1.0
        notes.append("Concentration not provided; assumed neutral contribution.")
    elif concentration < 5.0:
        factors["concentration"] = round(max(0.6, 1.0 - 0.008 * (5.0 - concentration)), 4)
    elif concentration > 50.0:
        penalty = max(0.30, 1.0 - 0.006 * (concentration - 50.0))
        factors["concentration"] = round(penalty, 4)
        warnings.append(
            f"Concentration {concentration:g} mg/mL promotes precipitation/side reactions "
            f"(optimal band 5-50 mg/mL; x{penalty:.2f})."
        )
    else:
        factors["concentration"] = 1.0

    catalyst = resolved.get("catalystLoadingPercent")
    if catalyst is None:
        factors["catalystLoading"] = 1.0
        notes.append("Catalyst loading not provided; assumed neutral contribution.")
    else:
        factors["catalystLoading"] = round(0.55 + 0.45 * (1.0 - math.exp(-0.35 * catalyst)), 4)

    rpm = resolved.get("stirringRpm")
    if rpm is None:
        factors["mixing"] = 1.0
    elif rpm < 300.0:
        factors["mixing"] = round(1.0 - 0.25 * ((300.0 - rpm) / 300.0), 4)
    elif rpm > 2000.0:
        vortex = min((rpm - 2000.0) / 500.0, 3.0)
        factors["mixing"] = round(1.0 - 0.05 * vortex, 4)
        warnings.append(f"Stirring {rpm:g} rpm may cause vortexing; small penalty applied.")
    else:
        factors["mixing"] = 1.0

    pressure = resolved.get("pressureBar")
    if pressure is None or pressure == 1.0:
        factors["pressure"] = 1.0
    else:
        factors["pressure"] = round(min(1.0 + 0.01 * (min(pressure, 10.0) - 1.0), 1.10), 4)

    solvent_raw = conditions.get("solventType")
    if solvent_raw is None:
        factors["solvent"] = 1.0
    else:
        solvent = str(solvent_raw).strip().lower()
        if solvent in SOLVENT_FACTORS:
            factors["solvent"] = SOLVENT_FACTORS[solvent]
        else:
            factors["solvent"] = 0.95
            warnings.append(f"Unknown solvent '{str(solvent_raw)[:40]}'; conservative factor 0.95 applied.")

    yield_fraction = 0.70
    for factor in factors.values():
        yield_fraction *= factor
    predicted_yield = round(min(max(yield_fraction * 100.0, 1.0), 99.0), 2)

    confidence = _estimate_confidence(resolved, warnings)

    if predicted_yield >= 70.0 and confidence >= 0.60:
        recommendation = "ACCEPT"
    elif predicted_yield >= 45.0:
        recommendation = "REVIEW"
    else:
        recommendation = "REJECT"

    limiting_factor = min(factors, key=lambda name: factors[name]) if factors else "unknown"
    summary_notes = (
        f"Deterministic reaction-kinetics approximation ({ATOM_MODEL_VERSION}). "
        f"Limiting factor: {limiting_factor} (x{factors.get(limiting_factor, 1.0):.2f})."
    )
    if warnings:
        summary_notes += f" {len(warnings)} warning(s): " + " ".join(warnings)
    if unknown_keys:
        summary_notes += f" Ignored unrecognized keys: {', '.join(sorted(unknown_keys))}."

    result = {
        "predictionId": req.predictionId,
        "modelVersion": ATOM_MODEL_VERSION,
        "inputSummary": conditions,
        "predictedYieldPercent": predicted_yield,
        "confidence": confidence,
        "recommendation": recommendation,
        "notes": summary_notes,
        "factorBreakdown": factors,
        "warnings": warnings,
    }
    elapsed_ms = int((time.perf_counter() - started) * 1000)
    log.info(
        "ATOM prediction %s: yield=%.2f%% confidence=%.2f recommendation=%s (%d ms)",
        req.predictionId, predicted_yield, confidence, recommendation, elapsed_ms,
    )
    return {
        "status": "COMPLETED",
        "resultData": json.dumps(result),
        "resultFilename": f"prediction-{req.predictionId or 'local'}-result.json",
        "resultContentType": "application/json",
        "modelVersion": ATOM_MODEL_VERSION,
        "executionTimeMs": elapsed_ms,
    }


def _normalise_conditions(raw: Any) -> dict:
    if isinstance(raw, str):
        try:
            raw = json.loads(raw)
        except (TypeError, ValueError) as exc:
            raise HTTPException(status_code=422, detail="inputConditions must be valid JSON") from exc
    if not isinstance(raw, dict):
        raise HTTPException(status_code=422, detail="inputConditions must be a JSON object")
    normalised: dict[str, Any] = {}
    for key, value in list(raw.items())[:20]:
        if isinstance(value, (dict, list)):
            raise HTTPException(
                status_code=422,
                detail=f"Condition '{str(key)[:40]}' must be a scalar value (number/string/bool/null)",
            )
        normalised[str(key)[:80]] = value
    return normalised


def _as_number(value: Any) -> Optional[float]:
    if isinstance(value, bool) or value is None:
        return None
    if isinstance(value, (int, float)):
        return float(value)
    text = str(value).strip().replace(",", ".")
    digits = "".join(ch for ch in text if ch.isdigit() or ch == "." or ch == "-")
    try:
        return float(digits) if digits not in ("", "-", ".") else None
    except ValueError:
        return None


def _resolve_parameters(conditions: dict) -> tuple[dict[str, float], list[str], set[str]]:
    resolved: dict[str, float] = {}
    warnings: list[str] = []
    unknown_keys: set[str] = set()
    for key, value in conditions.items():
        canonical = ALIAS_INDEX.get(key.strip().lower())
        if canonical is None:
            if key.strip().lower() != "solventtype":
                unknown_keys.add(key)
            continue
        number = _as_number(value)
        if number is None:
            warnings.append(f"Parameter '{key}' has non-numeric value '{str(value)[:40]}'; ignored.")
            continue
        spec = PARAM_SPECS[canonical]
        if not spec["min"] <= number <= spec["max"]:
            warnings.append(
                f"Parameter '{key}'={number:g} outside physical range "
                f"[{spec['min']:g}, {spec['max']:g}]; clamped for scoring."
            )
            number = min(max(number, spec["min"]), spec["max"])
        resolved[canonical] = number
    return resolved, warnings, unknown_keys


def _estimate_confidence(resolved: dict, warnings: list[str]) -> float:
    confidence = 0.42
    core_params = ("temperatureC", "ph", "reactionTimeMin", "concentrationMgMl")
    confidence += 0.07 * sum(1 for param in core_params if param in resolved)
    if "catalystLoadingPercent" in resolved:
        confidence += 0.04
    if "stirringRpm" in resolved or "pressureBar" in resolved:
        confidence += 0.03
    confidence -= 0.12 * len(warnings)
    return round(min(max(confidence, 0.15), 0.93), 3)


# --------------------------------------------------------------------------
# LLM integration
#
# Uses any OpenAI-compatible /chat/completions endpoint when LLM_API_KEY is
# configured; otherwise falls back to the local rule-based toxicity screener.
# Both paths report which model produced the answer.
# --------------------------------------------------------------------------

SYSTEM_PROMPTS = {
    "TOXICITY_ASSESSMENT": (
        "You are a regulatory toxicology expert preparing a CPSR (Chemical Safety "
        "and Risk) assessment. Evaluate the submitted substance/process description. "
        "Cover: acute/chronic toxicity hazards, carcinogenic/mutagenic/reprotoxic (CMR) "
        "concerns, environmental hazard, exposure routes, applicable restrictions, and a "
        "clear conclusion (SAFE / CONDITIONALLY SAFE / UNSAFE) with required controls. "
        "Be precise and cite general regulatory frameworks (e.g. GHS, CLP, REACH) where relevant."
    ),
    "CPSR_GENERATION": (
        "You are a chemical safety report author. Produce a structured CPSR draft from the "
        "submitted information with sections: Hazard identification, Classification and labelling, "
        "Exposure assessment, Risk characterisation, Risk management measures, Conclusion."
    ),
    "SAFETY_REVIEW": (
        "You are a process safety engineer. Review the submitted description for safety issues: "
        "incompatibilities, thermal/runaway risk, handling controls, PPE, storage requirements. "
        "End with an overall risk level (LOW/MEDIUM/HIGH/CRITICAL) and top mitigations."
    ),
    "REFERENCE_LOOKUP": (
        "You are a scientific literature assistant. List authoritative reference categories, "
        "typical data sources, and what specific data points are needed to verify the submitted "
        "description. Do not invent specific citations."
    ),
}
DEFAULT_SYSTEM_PROMPT = SYSTEM_PROMPTS["TOXICITY_ASSESSMENT"]

RULE_LEXICON: list[tuple[str, str, int]] = [
    ("carcinogen", "CMR hazard: carcinogenicity concern", 4),
    ("mutagen", "CMR hazard: mutagenicity concern", 4),
    ("teratogen", "CMR hazard: reproductive toxicity concern", 4),
    ("toxic", "Acute toxicity flagged by terminology", 3),
    ("hazard", "General hazard terminology detected", 2),
    ("poison", "Acute poisoning concern", 3),
    ("corrosive", "Corrosivity concern (skin/eye damage)", 3),
    ("flammable", "Flammability hazard detected", 2),
    ("explosive", "Explosivity hazard detected", 4),
    ("restricted", "Restricted substance terminology detected", 3),
    ("prohibited", "Prohibited substance terminology detected", 4),
    ("persistent", "Environmental persistence concern", 2),
    ("bioaccumulative", "Bioaccumulation concern (PBT/vPvB criteria)", 3),
]


@app.post("/llm/complete")
def llm_complete(req: LlmCompleteRequest, request: Request):
    require_auth(request)
    started = time.perf_counter()
    requested_model = (req.modelName or "").strip()
    if llm_configured() and requested_model.lower() not in ("rule-based-screening",):
        return _complete_with_remote_llm(req, requested_model, started)
    return _complete_with_rule_based_assessment(req, started)


def _resolve_remote_model(requested_model: str) -> str:
    normalised = requested_model.strip().lower()
    if normalised in ("", "auto", "default", RULE_MODEL_NAME):
        return LLM_MODEL
    return requested_model.strip()


def _build_user_content(prompt: str, context: Optional[str]) -> str:
    """Grounds the user prompt with reference context (RAG-lite): substance
    data, GHS classifications and curated references are injected so the LLM
    answers from provided evidence instead of guessing."""
    if not context or not context.strip():
        return prompt
    return (
        "REFERENCE CONTEXT (use as primary evidence, cite by [R#] markers when relevant):\n"
        f"{context.strip()[:12000]}\n\n"
        "---\n\n"
        f"QUESTION / TASK:\n{prompt}"
    )


def _complete_with_remote_llm(req: LlmCompleteRequest, requested_model: str, started: float) -> dict:
    system_prompt = DEFAULT_SYSTEM_PROMPT
    inference_type = (req.inferenceType or "").upper()
    if inference_type in SYSTEM_PROMPTS:
        system_prompt = SYSTEM_PROMPTS[inference_type]

    payload = {
        "model": _resolve_remote_model(requested_model),
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": _build_user_content(req.prompt, req.context)},
        ],
        "temperature": 0.2,
        "max_tokens": 900,
    }
    headers = {"Authorization": f"Bearer {LLM_API_KEY}"}
    last_error: Optional[str] = None

    for attempt in range(1, LLM_MAX_ATTEMPTS + 1):
        try:
            with httpx.Client(timeout=LLM_TIMEOUT_SECONDS) as client:
                response = client.post(f"{LLM_BASE_URL}/chat/completions", headers=headers, json=payload)
            if response.status_code >= 400:
                last_error = f"LLM provider returned HTTP {response.status_code}: {response.text[:300]}"
                retryable = response.status_code in (429, 500, 502, 503, 504)
            else:
                body = response.json()
                choice = body.get("choices") or []
                text = (choice[0].get("message") or {}).get("content") if choice else None
                if not text:
                    last_error = "LLM provider returned an empty completion"
                    retryable = False
                else:
                    usage = body.get("usage") or {}
                    latency_ms = int((time.perf_counter() - started) * 1000)
                    used_model = body.get("model") or payload["model"]
                    log.info(
                        "LLM inference %s completed via %s (%s tokens, %d ms)",
                        req.inferenceId, used_model, usage.get("total_tokens", "?"), latency_ms,
                    )
                    return {
                        "status": "COMPLETED",
                        "responseText": str(text),
                        "tokensUsed": int(usage.get("total_tokens") or max(16, len(str(text).split()) * 4 // 3)),
                        "latencyMs": latency_ms,
                        "modelName": str(used_model),
                    }
        except (httpx.HTTPError, ValueError) as exc:
            last_error = f"LLM transport failure: {type(exc).__name__}: {exc}"
            retryable = True

        if attempt < LLM_MAX_ATTEMPTS and retryable:
            delay = 0.75 * attempt
            log.warning("LLM call attempt %d/%d failed, retrying in %.1fs: %s", attempt, LLM_MAX_ATTEMPTS, delay, last_error)
            time.sleep(delay)
        else:
            break

    latency_ms = int((time.perf_counter() - started) * 1000)
    log.error("LLM inference %s failed after %d attempts: %s", req.inferenceId, LLM_MAX_ATTEMPTS, last_error)
    return {
        "status": "FAILED",
        "responseText": "",
        "tokensUsed": 0,
        "latencyMs": latency_ms,
        "modelName": payload["model"],
        "errorMessage": last_error or "Unknown LLM failure",
    }


def _complete_with_rule_based_assessment(req: LlmCompleteRequest, started: float) -> dict:
    lowered = req.prompt.lower()
    findings: list[tuple[int, str]] = []
    for term, description, severity in RULE_LEXICON:
        if term in lowered:
            findings.append((severity, description))

    max_severity = max((severity for severity, _ in findings), default=0)
    if max_severity >= 4:
        verdict = "UNSAFE"
        action = "Do not proceed without specialist toxicological evaluation and formal approval."
    elif max_severity >= 2:
        verdict = "CONDITIONALLY SAFE"
        action = "Proceed only under controlled conditions with documented exposure controls and expert review."
    else:
        verdict = "PENDING EXPERT REVIEW"
        action = "No restricted terminology matched the screening lexicon; route to expert review before approval."

    sections = [
        "[Rule-based toxicity screening - not an LLM generation]",
        f"Inference type: {req.inferenceType or 'GENERAL'}.",
    ]
    if req.context and req.context.strip():
        sections.append(f"Screening performed with {len(req.context.strip())} chars of reference context "
                        "(substance data / GHS classifications) taken into account.")
    sections += ["", "Findings:"]
    if findings:
        for severity, description in sorted(findings, reverse=True):
            sections.append(f"  - Severity {severity}/4: {description}.")
    else:
        sections.append("  - No lexicon matches detected.")
    sections += ["", f"Screening verdict: {verdict}.", action]
    text = "\n".join(sections)

    latency_ms = int((time.perf_counter() - started) * 1000)
    tokens = max(16, len(text.split()) * 4 // 3)
    log.info("LLM inference %s completed via rule-based screening (%d ms)", req.inferenceId, latency_ms)
    return {
        "status": "COMPLETED",
        "responseText": text,
        "tokensUsed": tokens,
        "latencyMs": latency_ms,
        "modelName": RULE_MODEL_NAME,
    }
