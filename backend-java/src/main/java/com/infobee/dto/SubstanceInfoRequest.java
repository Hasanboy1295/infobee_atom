package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubstanceInfoRequest(
    @NotBlank @Size(max = 300) String substanceName,
    @Size(max = 30) String casNumber,
    @Size(max = 30) String ecNumber,
    @Size(max = 100) String molecularFormula,
    Double molecularWeight,
    @Size(max = 100) String purity,
    String intendedUse,
    Double intendedConcentration,
    @Size(max = 100) String productType,
    @Size(max = 100) String targetPopulation,
    @Size(max = 2000) String remarks
) {}
