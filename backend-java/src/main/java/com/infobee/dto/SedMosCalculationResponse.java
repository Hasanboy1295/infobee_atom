package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;

public record SedMosCalculationResponse(
    Double sedValue,
    @NotBlank String sedUnit,
    Double mosValue,
    boolean safe,
    String conclusion,
    String formulaBreakdown
) {}
