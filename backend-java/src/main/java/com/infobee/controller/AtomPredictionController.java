package com.infobee.controller;

import com.infobee.dto.AtomPredictionResponse;
import com.infobee.model.AtomPrediction;
import com.infobee.model.AtomRequest;
import com.infobee.model.User;
import com.infobee.repository.AtomPredictionRepository;
import com.infobee.repository.AtomRequestRepository;
import com.infobee.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/atom-requests/{atomRequestId}/predictions")
@Tag(name = "ATOM AI predictions", description = "Experiment prediction: input conditions, file upload, results, download")
@SecurityRequirement(name = "bearerAuth")
public class AtomPredictionController {
    private final AtomPredictionRepository predRepo;
    private final AtomRequestRepository atomRepo;
    private final UserRepository userRepo;
    private final com.infobee.service.AiProcessingService aiProcessingService;
    private final String aiInternalSecret;

    public AtomPredictionController(AtomPredictionRepository predRepo,
                                     AtomRequestRepository atomRepo,
                                     UserRepository userRepo,
                                     com.infobee.service.AiProcessingService aiProcessingService,
                                     @org.springframework.beans.factory.annotation.Value("${app.internal.ai-secret:}") String aiInternalSecret) {
        this.predRepo = predRepo;
        this.atomRepo = atomRepo;
        this.userRepo = userRepo;
        this.aiProcessingService = aiProcessingService;
        this.aiInternalSecret = aiInternalSecret;
    }

    private AtomRequest findAtom(Long id) {
        return atomRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ATOM request not found"));
    }

    @GetMapping
    @Operation(summary = "List predictions for ATOM request")
    public List<AtomPredictionResponse> list(@PathVariable Long atomRequestId) {
        findAtom(atomRequestId);
        return predRepo.findByAtomRequestIdOrderByCreatedAtDesc(atomRequestId).stream()
            .map(AtomPredictionResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create prediction with experiment conditions (JSON body)")
    public AtomPredictionResponse create(@PathVariable Long atomRequestId,
                                          @RequestBody com.fasterxml.jackson.databind.JsonNode body,
                                          Authentication auth) {
        AtomRequest atom = findAtom(atomRequestId);
        User user = userRepo.findByUsername(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        AtomPrediction pred = new AtomPrediction();
        pred.setAtomRequest(atom);
        pred.setCreatedBy(user);
        String conditions = body.has("inputConditions") ? body.get("inputConditions").toString() : body.toString();
        pred.setInputConditions(conditions);
        pred.setStatus(AtomPrediction.PredictionStatus.INPUT_READY);
        return AtomPredictionResponse.from(predRepo.save(pred));
    }

    @PostMapping("/{predictionId}/submit")
    @Operation(summary = "Submit prediction for AI processing (status -> QUEUED)")
    public AtomPredictionResponse submit(@PathVariable Long atomRequestId,
                                          @PathVariable Long predictionId) {
        AtomPrediction pred = predRepo.findById(predictionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prediction not found"));
        if (pred.getStatus() == AtomPrediction.PredictionStatus.CANCELLED
                || pred.getStatus() == AtomPrediction.PredictionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Prediction in status " + pred.getStatus() + " cannot be submitted");
        }
        pred.setStatus(AtomPrediction.PredictionStatus.QUEUED);
        pred.setErrorMessage(null);
        AtomPredictionResponse response = AtomPredictionResponse.from(predRepo.save(pred));
        aiProcessingService.processAtomPrediction(predictionId);
        return response;
    }

    @PatchMapping("/{predictionId}/result")
    @Operation(summary = "Update prediction result (called by AI service after inference)")
    public AtomPredictionResponse updateResult(@PathVariable Long predictionId,
                                                @RequestBody Map<String, Object> body,
                                                @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        if (aiInternalSecret != null && !aiInternalSecret.isBlank()
                && (secret == null || !secret.equals(aiInternalSecret))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal secret");
        }
        AtomPrediction pred = predRepo.findById(predictionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prediction not found"));
        if (body.containsKey("resultData")) {
            pred.setResultData((String) body.get("resultData"));
        }
        if (body.containsKey("status")) {
            pred.setStatus(AtomPrediction.PredictionStatus.valueOf((String) body.get("status")));
        }
        if (body.containsKey("modelVersion")) {
            pred.setModelVersion((String) body.get("modelVersion"));
        }
        if (body.containsKey("executionTimeMs")) {
            pred.setExecutionTimeMs(((Number) body.get("executionTimeMs")).longValue());
        }
        if (body.containsKey("errorMessage")) {
            pred.setErrorMessage((String) body.get("errorMessage"));
        }
        if (body.containsKey("resultFilename")) {
            pred.setResultFilename((String) body.get("resultFilename"));
        }
        if (body.containsKey("resultContentType")) {
            pred.setResultContentType((String) body.get("resultContentType"));
        }
        return AtomPredictionResponse.from(predRepo.save(pred));
    }

    @GetMapping("/{predictionId}")
    @Operation(summary = "Get prediction detail (includes result data)")
    public AtomPredictionResponse get(@PathVariable Long atomRequestId,
                                       @PathVariable Long predictionId) {
        findAtom(atomRequestId);
        return AtomPredictionResponse.from(predRepo.findById(predictionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prediction not found")));
    }

    @PostMapping("/{predictionId}/cancel")
    @Operation(summary = "Cancel prediction")
    public AtomPredictionResponse cancel(@PathVariable Long predictionId) {
        AtomPrediction pred = predRepo.findById(predictionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prediction not found"));
        pred.setStatus(AtomPrediction.PredictionStatus.CANCELLED);
        return AtomPredictionResponse.from(predRepo.save(pred));
    }

    @GetMapping("/{predictionId}/download")
    @Operation(summary = "Download result file (returns result data as JSON attachment)")
    public Map<String, Object> downloadResult(@PathVariable Long predictionId) {
        AtomPrediction pred = predRepo.findById(predictionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prediction not found"));
        if (pred.getResultData() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No result available yet");
        }
        return Map.of(
            "filename", pred.getResultFilename() != null ? pred.getResultFilename() : "result.json",
            "contentType", pred.getResultContentType() != null ? pred.getResultContentType() : "application/json",
            "data", pred.getResultData()
        );
    }
}
