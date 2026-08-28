package com.infobee.dto;

import com.infobee.model.CpsrEvaluation;

public record CpsrEvaluationResponse(
    Long id, Long cpsrRequestId,
    Long evaluatorId, String evaluatorName,
    String status,
    Double sedValue, String sedUnit,
    Double mosValue,
    Double noaelValue, String noaelUnit,
    String riskAssessment, String conclusion,
    String evaluatorOpinion, String remarks
) {
    public static CpsrEvaluationResponse from(CpsrEvaluation e) {
        return new CpsrEvaluationResponse(
            e.getId(), e.getCpsrRequest().getId(),
            e.getEvaluator() != null ? e.getEvaluator().getId() : null,
            e.getEvaluator() != null ? e.getEvaluator().getFullName() : null,
            e.getStatus().name(),
            e.getSedValue(), e.getSedUnit(),
            e.getMosValue(),
            e.getNoaelValue(), e.getNoaelUnit(),
            e.getRiskAssessment(), e.getConclusion(),
            e.getEvaluatorOpinion(), e.getRemarks());
    }
}
