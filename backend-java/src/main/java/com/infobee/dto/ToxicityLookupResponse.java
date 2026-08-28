package com.infobee.dto;

import java.util.List;

public record ToxicityLookupResponse(
    String query,
    String sourceDb,
    Long cid,
    String compoundTitle,
    String resolvedName,
    String casNumber,
    String molecularFormula,
    Double molecularWeight,
    List<String> signalWords,
    String ghsSignalWord,
    List<String> pictograms,
    List<String> ghsHazardCodes,
    List<String> ghsHazardStatements,
    List<ToxicityRecordResponse> records
) {
    public static ToxicityLookupResponse notFound(String query) {
        return new ToxicityLookupResponse(query, "NOT_FOUND", null, null, null, null, null, null,
            List.of(), null, List.of(), List.of(), List.of(), List.of());
    }

    public static ToxicityLookupResponse of(String query, String sourceDb, Long cid,
            String compoundTitle, String molecularFormula, Double molecularWeight,
            List<String> signalWords, List<String> pictograms,
            List<ToxicityRecordResponse> records) {
        String resolvedName = compoundTitle;
        String ghsSignalWord = (signalWords != null && !signalWords.isEmpty()) ? signalWords.get(0) : null;
        List<String> ghsHazardCodes = pictograms;
        List<String> ghsHazardStatements = (records != null)
            ? records.stream().map(ToxicityRecordResponse::remarks).filter(h -> h != null && !h.isBlank()).distinct().toList()
            : List.of();
        return new ToxicityLookupResponse(query, sourceDb, cid, compoundTitle, resolvedName, null,
            molecularFormula, molecularWeight, signalWords, ghsSignalWord, pictograms, ghsHazardCodes,
            ghsHazardStatements, records != null ? records : List.of());
    }
}
