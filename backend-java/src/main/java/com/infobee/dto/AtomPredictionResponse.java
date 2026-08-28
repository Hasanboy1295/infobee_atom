package com.infobee.dto;

import com.infobee.model.AtomPrediction;

public record AtomPredictionResponse(
    Long id, Long atomRequestId, Long createdByUserId,
    String status, String inputConditions,
    String inputFilename, String inputContentType,
    String resultData, String resultFilename, String resultContentType,
    String modelVersion, Long executionTimeMs, String errorMessage
) {
    public static AtomPredictionResponse from(AtomPrediction p) {
        return new AtomPredictionResponse(
            p.getId(), p.getAtomRequest().getId(), p.getCreatedBy().getId(),
            p.getStatus().name(), p.getInputConditions(),
            p.getInputFilename(), p.getInputContentType(),
            p.getResultData(), p.getResultFilename(), p.getResultContentType(),
            p.getModelVersion(), p.getExecutionTimeMs(), p.getErrorMessage());
    }
}
