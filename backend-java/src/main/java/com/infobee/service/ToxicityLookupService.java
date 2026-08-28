package com.infobee.service;

import com.infobee.dto.ToxicityLookupResponse;
import com.infobee.dto.ToxicityRecordResponse;
import com.infobee.model.ToxicityRecord;
import com.infobee.repository.ToxicityRecordRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Resolves toxicity data for a substance: local cache first, then PubChem
 * GHS classification. External results are persisted as PUBCHEM-sourced
 * records so subsequent lookups are served from the local DB.
 */
@Service
public class ToxicityLookupService {
    private static final Logger log = LoggerFactory.getLogger(ToxicityLookupService.class);
    private static final int MAX_STATEMENTS = 40;

    private final ToxicityRecordRepository repo;
    private final PubChemClient pubChem;

    public ToxicityLookupService(ToxicityRecordRepository repo, PubChemClient pubChem) {
        this.repo = repo;
        this.pubChem = pubChem;
    }

    public ToxicityLookupResponse lookup(String casNumber, String substanceName) {
        String query = casNumber != null && !casNumber.isBlank() ? casNumber.trim() : null;
        String name = substanceName != null && !substanceName.isBlank() ? substanceName.trim() : null;

        List<ToxicityRecord> cached = findCached(query, name);
        if (!cached.isEmpty()) {
            return ToxicityLookupResponse.of(
                query != null ? query : name, "LOCAL_CACHE", null, null, null, null,
                List.of(), List.of(), cached.stream().map(ToxicityRecordResponse::from).toList());
        }

        if (!pubChem.isEnabled() || (query == null && name == null)) {
            return ToxicityLookupResponse.notFound(query != null ? query : name);
        }

        Long cid = resolveCid(query, name);
        if (cid == null) {
            return ToxicityLookupResponse.notFound(query != null ? query : name);
        }

        Map<?, ?> properties = safeProperties(cid);
        String compoundTitle = stringValue(properties, "Title");
        String formula = stringValue(properties, "MolecularFormula");
        Double molecularWeight = properties != null && properties.get("MolecularWeight") instanceof Number weight
            ? weight.doubleValue()
            : null;

        Map<String, Object> ghs = safeGhs(cid);
        GhsData ghsData = ghs != null ? parseGhs(ghs) : new GhsData(List.of(), List.of(), List.of());

        List<ToxicityRecord> saved = persistRecords(query, compoundTitle, cid, ghsData.statements());
        log.info("PubChem lookup for '{}': cid={} statements={} pictograms={}",
            query != null ? query : name, cid, ghsData.statements().size(), ghsData.pictograms().size());

        return ToxicityLookupResponse.of(
            query != null ? query : name, "PUBCHEM", cid, compoundTitle, formula, molecularWeight,
            ghsData.signalWords(), ghsData.pictograms(),
            saved.stream().map(ToxicityRecordResponse::from).toList());
    }

    private List<ToxicityRecord> findCached(String casNumber, String name) {
        if (casNumber != null && !repo.findByCasNumber(casNumber).isEmpty()) {
            return repo.findByCasNumber(casNumber);
        }
        if (name != null) {
            return repo.findBySubstanceNameContainingIgnoreCase(name);
        }
        return List.of();
    }

    private Long resolveCid(String casNumber, String name) {
        try {
            if (casNumber != null) {
                Long byCas = pubChem.resolveCid(casNumber);
                if (byCas != null) {
                    return byCas;
                }
            }
            return name != null ? pubChem.resolveCid(name) : null;
        } catch (Exception e) {
            log.warn("PubChem CID resolution failed: {}", e.getMessage());
            return null;
        }
    }

    private Map<?, ?> safeProperties(Long cid) {
        try {
            Map<String, Object> body = pubChem.fetchCompoundProperties(cid);
            if (body == null || !(body.get("PropertyTable") instanceof Map<?, ?> table)) {
                return null;
            }
            if (!(table.get("Properties") instanceof List<?> list)
                    || list.isEmpty()
                    || !(list.get(0) instanceof Map<?, ?> first)) {
                return null;
            }
            return first;
        } catch (Exception e) {
            log.warn("PubChem property fetch failed for cid {}: {}", cid, e.getMessage());
            return null;
        }
    }

    private static String stringValue(Map<?, ?> source, String key) {
        return source != null && source.get(key) instanceof String text ? text : null;
    }

    private Map<String, Object> safeGhs(Long cid) {
        try {
            return pubChem.fetchGhsClassification(cid);
        } catch (Exception e) {
            log.warn("PubChem GHS fetch failed for cid {}: {}", cid, e.getMessage());
            return null;
        }
    }

    private List<ToxicityRecord> persistRecords(String casNumber, String substanceName, Long cid,
                                                List<String> statements) {
        List<ToxicityRecord> saved = new ArrayList<>();
        for (String statement : statements.subList(0, Math.min(statements.size(), MAX_STATEMENTS))) {
            ToxicityRecord rec = new ToxicityRecord();
            rec.setSubstanceName(substanceName != null ? substanceName : casNumber);
            rec.setCasNumber(casNumber);
            rec.setSourceDb("PUBCHEM");
            rec.setSourceId("CID:" + cid);
            rec.setEndpointName("GHS Classification");
            rec.setEndpointValue(statement);
            rec.setRemarks("Imported from PubChem PUG View");
            saved.add(repo.save(rec));
        }
        return saved;
    }

    /**
     * Pure recursive parser over the PUG View JSON tree. Extracts hazard
     * statements, pictograms and signal words from the GHS Classification
     * section. Package-private and side-effect free so it can be unit tested.
     */
    static GhsData parseGhs(Map<String, Object> body) {
        List<String> statements = new ArrayList<>();
        List<String> pictograms = new ArrayList<>();
        List<String> signalWords = new ArrayList<>();
        if (body != null && body.get("Record") instanceof Map<?, ?> record) {
            walkSection(record.get("Section"), "", statements, pictograms, signalWords);
        }
        return new GhsData(statements, pictograms, signalWords);
    }

    private static void walkSection(Object sectionObj, String headingPath,
                                    List<String> statements, List<String> pictograms,
                                    List<String> signalWords) {
        if (!(sectionObj instanceof List<?> sections)) {
            return;
        }
        for (Object item : sections) {
            if (!(item instanceof Map<?, ?> section)) {
                continue;
            }
            String tocHeading = string(section.get("TOCHeading"));
            String path = (headingPath + ">" + tocHeading).toLowerCase();
            collectStrings(section.get("Information"), path, statements, pictograms, signalWords);
            walkSection(section.get("Section"), path, statements, pictograms, signalWords);
        }
    }

    private static void collectStrings(Object infoObj, String path,
                                       List<String> statements, List<String> pictograms,
                                       List<String> signalWords) {
        if (!(infoObj instanceof List<?> infos)) {
            return;
        }
        for (Object item : infos) {
            if (!(item instanceof Map<?, ?> info)
                    || !(info.get("Value") instanceof Map<?, ?> value)
                    || !(value.get("StringWithMarkup") instanceof List<?> stringList)) {
                continue;
            }
            for (Object entry : stringList) {
                if (!(entry instanceof Map<?, ?> swm)) {
                    continue;
                }
                String text = string(swm.get("String"));
                if (text == null || text.isBlank()) {
                    continue;
                }
                if (path.contains("hazard statement")) {
                    statements.add(text.strip());
                } else if (path.contains("pictogram")) {
                    pictograms.add(text.strip());
                } else if (path.contains("signal word")) {
                    signalWords.add(text.strip());
                }
            }
        }
    }

    private static String string(Object value) {
        return value instanceof String text ? text : null;
    }

    record GhsData(List<String> statements, List<String> pictograms, List<String> signalWords) {}
}
