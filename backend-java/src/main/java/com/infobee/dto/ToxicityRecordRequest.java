package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ToxicityRecordRequest(
    @NotBlank @Size(max = 300) String substanceName,
    @Size(max = 30) String casNumber,
    @Size(max = 30) String ecNumber,
    @NotBlank @Size(max = 50) String sourceDb,
    @Size(max = 100) String sourceId,
    @NotBlank @Size(max = 200) String endpointName,
    String endpointValue,
    @Size(max = 50) String endpointUnit,
    @Size(max = 200) String testGuideline,
    @Size(max = 200) String testMethod,
    @Size(max = 2000) String remarks
) {}
