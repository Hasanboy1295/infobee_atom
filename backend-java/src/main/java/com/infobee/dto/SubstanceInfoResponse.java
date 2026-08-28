package com.infobee.dto;

import com.infobee.model.SubstanceInfo;

public record SubstanceInfoResponse(
    Long id, Long cpsrRequestId, String substanceName, String casNumber, String ecNumber,
    String molecularFormula, Double molecularWeight, String purity, String intendedUse,
    Double intendedConcentration, String productType, String targetPopulation, String remarks
) {
    public static SubstanceInfoResponse from(SubstanceInfo s) {
        return new SubstanceInfoResponse(
            s.getId(), s.getCpsrRequest().getId(), s.getSubstanceName(), s.getCasNumber(),
            s.getEcNumber(), s.getMolecularFormula(), s.getMolecularWeight(), s.getPurity(),
            s.getIntendedUse(), s.getIntendedConcentration(), s.getProductType(),
            s.getTargetPopulation(), s.getRemarks());
    }
}
