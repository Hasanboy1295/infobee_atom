package com.infobee.controller;

import com.infobee.dto.LlmInferenceRequest;
import com.infobee.dto.LlmInferenceResponse;
import com.infobee.model.CpsrRequest;
import com.infobee.model.LlmInference;
import com.infobee.repository.CpsrRequestRepository;
import com.infobee.repository.LlmInferenceRepository;
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
@RequestMapping("/api/cpsr-requests/{cpsrRequestId}/llm")
@Tag(name = "LLM inference", description = "LLM prompt submission and inference results for CPSR")
@SecurityRequirement(name = "bearerAuth")
public class LlmInferenceController {
    private final LlmInferenceRepository llmRepo;
    private final CpsrRequestRepository cpsrRepo;
    private final com.infobee.service.AiProcessingService aiProcessingService;
    private final com.infobee.service.CpsrContextService contextService;
    private final String aiInternalSecret;

    public LlmInferenceController(LlmInferenceRepository llmRepo,
                                  CpsrRequestRepository cpsrRepo,
                                  com.infobee.service.AiProcessingService aiProcessingService,
                                  com.infobee.service.CpsrContextService contextService,
                                  @org.springframework.beans.factory.annotation.Value("${app.internal.ai-secret:}") String aiInternalSecret) {
        this.llmRepo = llmRepo;
        this.cpsrRepo = cpsrRepo;
        this.aiProcessingService = aiProcessingService;
        this.contextService = contextService;
        this.aiInternalSecret = aiInternalSecret;
    }

    private CpsrRequest findCpsr(Long id) {
        return cpsrRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CPSR request not found"));
    }

    @GetMapping
    @Operation(summary = "List LLM inferences for CPSR request")
    public List<LlmInferenceResponse> list(@PathVariable Long cpsrRequestId) {
        findCpsr(cpsrRequestId);
        return llmRepo.findByCpsrRequestIdOrderByCreatedAtDesc(cpsrRequestId).stream()
            .map(LlmInferenceResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit LLM prompt (creates inference record, status=PENDING)")
    public LlmInferenceResponse submit(@PathVariable Long cpsrRequestId,
                                        @Valid @RequestBody LlmInferenceRequest req) {
        CpsrRequest cpsr = findCpsr(cpsrRequestId);
        LlmInference inf = new LlmInference();
        inf.setCpsrRequest(cpsr);
        inf.setModelName(req.modelName() == null || req.modelName().isBlank()
            ? "auto"
            : req.modelName().trim());
        inf.setInferenceType(LlmInference.InferenceType.valueOf(req.inferenceType()));
        inf.setPrompt(req.prompt());
        inf.setReferenceSources(req.referenceSources());
        inf.setStatus(LlmInference.InferenceStatus.PENDING);
        LlmInferenceResponse response = LlmInferenceResponse.from(llmRepo.save(inf));
        aiProcessingService.processLlmInference(response.id(), contextService.buildContext(cpsr));
        return response;
    }

    @PatchMapping("/{inferenceId}/result")
    @Operation(summary = "Update LLM inference result (called by AI service after inference)")
    public LlmInferenceResponse updateResult(@PathVariable Long cpsrRequestId,
                                              @PathVariable Long inferenceId,
                                              @RequestBody java.util.Map<String, Object> body,
                                              @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        if (aiInternalSecret != null && !aiInternalSecret.isBlank()
                && (secret == null || !secret.equals(aiInternalSecret))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal secret");
        }
        LlmInference inf = llmRepo.findById(inferenceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inference not found"));
        if (body.containsKey("responseText")) {
            inf.setResponseText((String) body.get("responseText"));
        }
        if (body.containsKey("status")) {
            inf.setStatus(LlmInference.InferenceStatus.valueOf((String) body.get("status")));
        }
        if (body.containsKey("tokensUsed")) {
            inf.setTokensUsed((Integer) body.get("tokensUsed"));
        }
        if (body.containsKey("latencyMs")) {
            inf.setLatencyMs(((Number) body.get("latencyMs")).longValue());
        }
        if (body.containsKey("errorMessage")) {
            inf.setErrorMessage((String) body.get("errorMessage"));
        }
        return LlmInferenceResponse.from(llmRepo.save(inf));
    }

    @GetMapping("/{inferenceId}")
    @Operation(summary = "Get LLM inference detail")
    public LlmInferenceResponse get(@PathVariable Long cpsrRequestId, @PathVariable Long inferenceId) {
        findCpsr(cpsrRequestId);
        return LlmInferenceResponse.from(llmRepo.findById(inferenceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inference not found")));
    }
}
