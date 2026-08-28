package com.infobee.dto;

import com.infobee.model.ToxicityRecord;

public record ToxicityRecordResponse(
    Long id, String substanceName, String casNumber, String ecNumber,
    String sourceDb, String sourceId, String endpointName, String endpointValue,
    String endpointUnit, String testGuideline, String testMethod, String remarks
) {
    public static ToxicityRecordResponse from(ToxicityRecord t) {
        return new ToxicityRecordResponse(
            t.getId(), t.getSubstanceName(), t.getCasNumber(), t.getEcNumber(),
            t.getSourceDb(), t.getSourceId(), t.getEndpointName(), t.getEndpointValue(),
            t.getEndpointUnit(), t.getTestGuideline(), t.getTestMethod(), t.getRemarks());
    }
}
