package com.infobee.dto;

import com.infobee.model.LlmInference;

public record LlmInferenceResponse(
    Long id, Long cpsrRequestId,
    String modelName, String inferenceType, String status,
    String prompt, String responseText, String referenceSources,
    Integer tokensUsed, Long latencyMs, String errorMessage
) {
    public static LlmInferenceResponse from(LlmInference i) {
        return new LlmInferenceResponse(
            i.getId(), i.getCpsrRequest().getId(),
            i.getModelName(), i.getInferenceType().name(), i.getStatus().name(),
            i.getPrompt(), i.getResponseText(), i.getReferenceSources(),
            i.getTokensUsed(), i.getLatencyMs(), i.getErrorMessage());
    }
}
