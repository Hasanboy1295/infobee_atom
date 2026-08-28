package com.infobee.service;

import com.infobee.model.AtomPrediction;
import com.infobee.model.LlmInference;
import com.infobee.repository.AtomPredictionRepository;
import com.infobee.repository.LlmInferenceRepository;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AiProcessingService {
    private static final Logger log = LoggerFactory.getLogger(AiProcessingService.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final AtomPredictionRepository predictionRepo;
    private final LlmInferenceRepository inferenceRepo;
    private final AiServiceClient aiClient;
    private final int maxAttempts;
    private final long retryBackoffMs;

    public AiProcessingService(AtomPredictionRepository predictionRepo,
                               LlmInferenceRepository inferenceRepo,
                               AiServiceClient aiClient,
                               @Value("${app.ai.max-attempts:2}") int maxAttempts,
                               @Value("${app.ai.retry-backoff-ms:1500}") long retryBackoffMs) {
        this.predictionRepo = predictionRepo;
        this.inferenceRepo = inferenceRepo;
        this.aiClient = aiClient;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
    }

    @Async("aiExecutor")
    public void processAtomPrediction(Long predictionId) {
        AtomPrediction pred = predictionRepo.findById(predictionId).orElse(null);
        if (pred == null || pred.getStatus() != AtomPrediction.PredictionStatus.QUEUED) {
            return;
        }
        if (!aiClient.isEnabled()) {
            pred.setStatus(AtomPrediction.PredictionStatus.CANCELLED);
            pred.setErrorMessage(truncate("AI processing is disabled (AI_ENABLED=false)"));
            predictionRepo.save(pred);
            log.info("ATOM prediction {} cancelled: AI processing disabled", predictionId);
            return;
        }
        pred.setStatus(AtomPrediction.PredictionStatus.RUNNING);
        final AtomPrediction running = predictionRepo.save(pred);
        long started = System.currentTimeMillis();
        try {
            Map<String, Object> response = callWithRetry(
                "ATOM prediction " + predictionId,
                () -> aiClient.predictAtom(Map.of(
                    "predictionId", predictionId,
                    "inputConditions", running.getInputConditions() == null ? "" : running.getInputConditions()
                )));
            applyAtomResult(running, response, System.currentTimeMillis() - started);
        } catch (Exception e) {
            log.warn("ATOM prediction {} processing failed after {} attempt(s)", predictionId, maxAttempts, e);
            fail(running, e.getMessage());
        }
        predictionRepo.save(running);
    }

    @Async("aiExecutor")
    public void processLlmInference(Long inferenceId, String referenceContext) {
        LlmInference inf = inferenceRepo.findById(inferenceId).orElse(null);
        if (inf == null || inf.getStatus() != LlmInference.InferenceStatus.PENDING) {
            return;
        }
        if (!aiClient.isEnabled()) {
            inf.setStatus(LlmInference.InferenceStatus.FAILED);
            inf.setErrorMessage(truncate("AI processing is disabled (AI_ENABLED=false)"));
            inferenceRepo.save(inf);
            log.info("LLM inference {} failed: AI processing disabled", inferenceId);
            return;
        }
        inf.setStatus(LlmInference.InferenceStatus.RUNNING);
        final LlmInference running = inferenceRepo.save(inf);
        long started = System.currentTimeMillis();
        try {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("inferenceId", inferenceId);
            payload.put("prompt", running.getPrompt() == null ? "" : running.getPrompt());
            payload.put("modelName", running.getModelName() == null ? "auto" : running.getModelName());
            payload.put("inferenceType", running.getInferenceType() == null
                ? "GENERAL" : running.getInferenceType().name());
            if (referenceContext != null && !referenceContext.isBlank()) {
                payload.put("context", referenceContext);
            }
            Map<String, Object> response = callWithRetry(
                "LLM inference " + inferenceId,
                () -> aiClient.completeLlm(payload));
            applyLlmResult(running, response, started);
        } catch (Exception e) {
            log.warn("LLM inference {} processing failed after {} attempt(s)", inferenceId, maxAttempts, e);
            running.setStatus(LlmInference.InferenceStatus.FAILED);
            running.setErrorMessage(truncate(e.getMessage()));
        }
        inferenceRepo.save(running);
    }

    private <T> T callWithRetry(String description, Supplier<T> call) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                lastFailure = e;
                log.warn("{} attempt {}/{} failed: {}", description, attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts && retryBackoffMs > 0) {
                    sleep(retryBackoffMs * attempt);
                }
            }
        }
        throw lastFailure != null ? lastFailure : new IllegalStateException(description + ": AI call failed");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI call interrupted while waiting for retry", e);
        }
    }

    private void applyAtomResult(AtomPrediction pred, Map<String, Object> response, long elapsedMs) {
        Object data = response == null ? null : response.get("resultData");
        String status = response == null ? "" : String.valueOf(response.getOrDefault("status", ""));
        if (!(data instanceof String resultData) || resultData.isBlank()) {
            fail(pred, "AI service returned a malformed response (missing or empty resultData)");
            return;
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            fail(pred, String.valueOf(response.getOrDefault("errorMessage", "AI service reported failure")));
            return;
        }
        if (!"COMPLETED".equalsIgnoreCase(status)) {
            fail(pred, "AI service returned unexpected status '" + status + "'");
            return;
        }
        pred.setResultData(resultData);
        pred.setResultFilename(String.valueOf(
            response.getOrDefault("resultFilename", "prediction-" + pred.getId() + "-result.json")));
        Object contentType = response.get("resultContentType");
        pred.setResultContentType(contentType instanceof String ct && !ct.isBlank()
            ? ct
            : "application/json");
        if (response.get("modelVersion") instanceof String version && !version.isBlank()) {
            pred.setModelVersion(version);
        }
        pred.setExecutionTimeMs(response.get("executionTimeMs") instanceof Number reported
            ? reported.longValue()
            : elapsedMs);
        pred.setStatus(AtomPrediction.PredictionStatus.COMPLETED);
        pred.setErrorMessage(null);
    }

    private void applyLlmResult(LlmInference inf, Map<String, Object> response, long started) {
        Object text = response == null ? null : response.get("responseText");
        String status = response == null ? "" : String.valueOf(response.getOrDefault("status", ""));
        if (!(text instanceof String responseText) || responseText.isBlank()) {
            inf.setStatus(LlmInference.InferenceStatus.FAILED);
            inf.setErrorMessage(truncate(response == null
                ? "AI service returned an empty response"
                : String.valueOf(response.getOrDefault("errorMessage", "AI service returned no response text"))));
            return;
        }
        inf.setResponseText(responseText);
        inf.setTokensUsed(response.get("tokensUsed") instanceof Number tokens ? tokens.intValue() : null);
        inf.setLatencyMs(response.get("latencyMs") instanceof Number latency ? latency.longValue() : null);
        if (response.get("modelName") instanceof String modelName && !modelName.isBlank()) {
            inf.setModelName(modelName);
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            inf.setStatus(LlmInference.InferenceStatus.FAILED);
            inf.setErrorMessage(truncate(String.valueOf(
                response.getOrDefault("errorMessage", "AI service reported failure"))));
            return;
        }
        if (!"COMPLETED".equalsIgnoreCase(status)) {
            inf.setStatus(LlmInference.InferenceStatus.FAILED);
            inf.setErrorMessage(truncate("AI service returned unexpected status '" + status + "'"));
            return;
        }
        inf.setStatus(LlmInference.InferenceStatus.COMPLETED);
        inf.setErrorMessage(null);
        log.info("LLM inference {} completed in {} ms", inf.getId(), System.currentTimeMillis() - started);
    }

    private void fail(AtomPrediction pred, String message) {
        pred.setStatus(AtomPrediction.PredictionStatus.FAILED);
        pred.setErrorMessage(truncate(message));
    }

    private String truncate(String message) {
        if (message == null) {
            return "Unknown error";
        }
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
