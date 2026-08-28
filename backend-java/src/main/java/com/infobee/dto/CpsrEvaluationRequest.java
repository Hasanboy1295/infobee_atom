package com.infobee.dto;

public record CpsrEvaluationRequest(
    Long evaluatorId,
    Double sedValue,
    String sedUnit,
    Double mosValue,
    Double noaelValue,
    String noaelUnit,
    String riskAssessment,
    String conclusion,
    String evaluatorOpinion,
    String remarks
) {}
