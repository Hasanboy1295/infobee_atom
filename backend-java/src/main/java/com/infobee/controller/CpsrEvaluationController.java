package com.infobee.controller;

import com.infobee.dto.CpsrEvaluationRequest;
import com.infobee.dto.CpsrEvaluationResponse;
import com.infobee.dto.SubstanceInfoRequest;
import com.infobee.dto.SubstanceInfoResponse;
import com.infobee.model.CpsrEvaluation;
import com.infobee.model.CpsrRequest;
import com.infobee.model.SubstanceInfo;
import com.infobee.model.User;
import com.infobee.repository.CpsrEvaluationRepository;
import com.infobee.repository.CpsrRequestRepository;
import com.infobee.repository.SubstanceInfoRepository;
import com.infobee.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cpsr-requests/{cpsrRequestId}")
@Tag(name = "CPSR evaluation workflow", description = "Substance info, toxicity evaluation, approval")
@SecurityRequirement(name = "bearerAuth")
public class CpsrEvaluationController {
    private final CpsrRequestRepository cpsrRepo;
    private final SubstanceInfoRepository substanceRepo;
    private final CpsrEvaluationRepository evalRepo;
    private final UserRepository userRepo;

    public CpsrEvaluationController(CpsrRequestRepository cpsrRepo,
                                     SubstanceInfoRepository substanceRepo,
                                     CpsrEvaluationRepository evalRepo,
                                     UserRepository userRepo) {
        this.cpsrRepo = cpsrRepo;
        this.substanceRepo = substanceRepo;
        this.evalRepo = evalRepo;
        this.userRepo = userRepo;
    }

    private CpsrRequest findCpsr(Long id) {
        return cpsrRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CPSR request not found"));
    }

    @PostMapping("/evaluations/calculate")
    @Operation(summary = "Calculate SED and MoS per SCCS Notes of Guidance (no persistence)")
    public com.infobee.dto.SedMosCalculationResponse calculateSedMos(
            @PathVariable Long cpsrRequestId,
            @Valid @RequestBody com.infobee.dto.SedMosCalculationRequest req) {
        findCpsr(cpsrRequestId);
        var result = com.infobee.service.SedMosCalculator.calculate(
            req.dailyAmountGrams(), req.concentrationPercent(), req.retentionFactor(),
            req.dermalAbsorptionPercent(), req.noaelMgKgDay(), req.bodyWeightKg());
        return new com.infobee.dto.SedMosCalculationResponse(
            result.sedMgKgDay(), "mg/kg bw/day", result.mosValue(), result.safe(),
            result.conclusion(), result.formulaBreakdown());
    }

    // ── Substance Info CRUD ──────────────────────────────────────────

    @GetMapping("/substances")
    @Operation(summary = "List substance infos for CPSR request")
    public List<SubstanceInfoResponse> listSubstances(@PathVariable Long cpsrRequestId) {
        return substanceRepo.findByCpsrRequestId(cpsrRequestId).stream()
            .map(SubstanceInfoResponse::from).toList();
    }

    @PostMapping("/substances")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add substance info to CPSR request")
    public SubstanceInfoResponse addSubstance(@PathVariable Long cpsrRequestId,
                                               @Valid @RequestBody SubstanceInfoRequest req,
                                               Authentication auth) {
        CpsrRequest cpsr = findCpsr(cpsrRequestId);
        SubstanceInfo info = new SubstanceInfo();
        info.setCpsrRequest(cpsr);
        info.setSubstanceName(req.substanceName());
        info.setCasNumber(req.casNumber());
        info.setEcNumber(req.ecNumber());
        info.setMolecularFormula(req.molecularFormula());
        info.setMolecularWeight(req.molecularWeight());
        info.setPurity(req.purity());
        info.setIntendedUse(req.intendedUse());
        info.setIntendedConcentration(req.intendedConcentration());
        info.setProductType(req.productType());
        info.setTargetPopulation(req.targetPopulation());
        info.setRemarks(req.remarks());
        return SubstanceInfoResponse.from(substanceRepo.save(info));
    }

    @PutMapping("/substances/{substanceId}")
    @Operation(summary = "Update substance info")
    public SubstanceInfoResponse updateSubstance(@PathVariable Long cpsrRequestId,
                                                  @PathVariable Long substanceId,
                                                  @Valid @RequestBody SubstanceInfoRequest req) {
        findCpsr(cpsrRequestId);
        SubstanceInfo info = substanceRepo.findById(substanceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Substance not found"));
        info.setSubstanceName(req.substanceName());
        info.setCasNumber(req.casNumber());
        info.setEcNumber(req.ecNumber());
        info.setMolecularFormula(req.molecularFormula());
        info.setMolecularWeight(req.molecularWeight());
        info.setPurity(req.purity());
        info.setIntendedUse(req.intendedUse());
        info.setIntendedConcentration(req.intendedConcentration());
        info.setProductType(req.productType());
        info.setTargetPopulation(req.targetPopulation());
        info.setRemarks(req.remarks());
        return SubstanceInfoResponse.from(substanceRepo.save(info));
    }

    @DeleteMapping("/substances/{substanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete substance info")
    public void deleteSubstance(@PathVariable Long substanceId) {
        substanceRepo.deleteById(substanceId);
    }

    // ── Evaluation Workflow ──────────────────────────────────────────

    @GetMapping("/evaluations")
    @Operation(summary = "List evaluations for CPSR request")
    public List<CpsrEvaluationResponse> listEvaluations(@PathVariable Long cpsrRequestId) {
        return evalRepo.findByCpsrRequestIdOrderByCreatedAtDesc(cpsrRequestId).stream()
            .map(CpsrEvaluationResponse::from).toList();
    }

    @PostMapping("/evaluations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create evaluation (assign evaluator)")
    public CpsrEvaluationResponse createEvaluation(@PathVariable Long cpsrRequestId,
                                                    @Valid @RequestBody CpsrEvaluationRequest req) {
        CpsrRequest cpsr = findCpsr(cpsrRequestId);
        CpsrEvaluation eval = new CpsrEvaluation();
        eval.setCpsrRequest(cpsr);
        if (req.evaluatorId() != null) {
            eval.setEvaluator(userRepo.findById(req.evaluatorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluator not found")));
        }
        eval.setStatus(CpsrEvaluation.EvaluationStatus.PENDING);
        eval.setSedValue(req.sedValue());
        eval.setSedUnit(req.sedUnit());
        eval.setMosValue(req.mosValue());
        eval.setNoaelValue(req.noaelValue());
        eval.setNoaelUnit(req.noaelUnit());
        eval.setRiskAssessment(req.riskAssessment());
        eval.setConclusion(req.conclusion());
        eval.setEvaluatorOpinion(req.evaluatorOpinion());
        eval.setRemarks(req.remarks());
        return CpsrEvaluationResponse.from(evalRepo.save(eval));
    }

    @PutMapping("/evaluations/{evalId}")
    @Operation(summary = "Update evaluation")
    public CpsrEvaluationResponse updateEvaluation(@PathVariable Long evalId,
                                                    @Valid @RequestBody CpsrEvaluationRequest req) {
        CpsrEvaluation eval = evalRepo.findById(evalId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation not found"));
        if (req.evaluatorId() != null) {
            eval.setEvaluator(userRepo.findById(req.evaluatorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluator not found")));
        }
        eval.setSedValue(req.sedValue());
        eval.setSedUnit(req.sedUnit());
        eval.setMosValue(req.mosValue());
        eval.setNoaelValue(req.noaelValue());
        eval.setNoaelUnit(req.noaelUnit());
        eval.setRiskAssessment(req.riskAssessment());
        eval.setConclusion(req.conclusion());
        eval.setEvaluatorOpinion(req.evaluatorOpinion());
        eval.setRemarks(req.remarks());
        return CpsrEvaluationResponse.from(evalRepo.save(eval));
    }

    @PostMapping("/evaluations/{evalId}/approve")
    @Operation(summary = "Approve evaluation")
    public CpsrEvaluationResponse approveEvaluation(@PathVariable Long evalId, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can approve evaluations");
        }
        CpsrEvaluation eval = evalRepo.findById(evalId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation not found"));
        eval.setStatus(CpsrEvaluation.EvaluationStatus.APPROVED);
        return CpsrEvaluationResponse.from(evalRepo.save(eval));
    }

    @PostMapping("/evaluations/{evalId}/reject")
    @Operation(summary = "Reject evaluation")
    public CpsrEvaluationResponse rejectEvaluation(@PathVariable Long evalId, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can reject evaluations");
        }
        CpsrEvaluation eval = evalRepo.findById(evalId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation not found"));
        eval.setStatus(CpsrEvaluation.EvaluationStatus.REJECTED);
        return CpsrEvaluationResponse.from(evalRepo.save(eval));
    }

    @PostMapping("/evaluations/{evalId}/start")
    @Operation(summary = "Start evaluation (set status IN_PROGRESS)")
    public CpsrEvaluationResponse startEvaluation(@PathVariable Long evalId) {
        CpsrEvaluation eval = evalRepo.findById(evalId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation not found"));
        eval.setStatus(CpsrEvaluation.EvaluationStatus.IN_PROGRESS);
        return CpsrEvaluationResponse.from(evalRepo.save(eval));
    }

    @PostMapping("/evaluations/{evalId}/complete")
    @Operation(summary = "Complete evaluation (set status COMPLETED)")
    public CpsrEvaluationResponse completeEvaluation(@PathVariable Long evalId) {
        CpsrEvaluation eval = evalRepo.findById(evalId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation not found"));
        eval.setStatus(CpsrEvaluation.EvaluationStatus.COMPLETED);
        return CpsrEvaluationResponse.from(evalRepo.save(eval));
    }
}
