package com.infobee.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SedMosCalculationRequest(
    @NotNull @DecimalMin(value = "0", inclusive = false) Double dailyAmountGrams,
    @NotNull @DecimalMin("0") @DecimalMax("100") Double concentrationPercent,
    @DecimalMin("0") @DecimalMax("1") Double retentionFactor,
    @DecimalMin("0") @DecimalMax("100") Double dermalAbsorptionPercent,
    @NotNull @DecimalMin(value = "0", inclusive = false) Double noaelMgKgDay,
    @DecimalMin(value = "0", inclusive = false) Double bodyWeightKg
) {}
