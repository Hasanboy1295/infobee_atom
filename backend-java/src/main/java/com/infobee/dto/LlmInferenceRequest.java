package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;

public record LlmInferenceRequest(
    String modelName,
    @NotBlank String inferenceType,
    @NotBlank String prompt,
    String referenceSources
) {}
