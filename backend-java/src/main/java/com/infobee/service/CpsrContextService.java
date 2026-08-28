package com.infobee.service;

import com.infobee.model.CpsrRequest;
import com.infobee.model.SubstanceInfo;
import com.infobee.model.ToxicityRecord;
import com.infobee.repository.SubstanceInfoRepository;
import com.infobee.repository.ToxicityRecordRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds a grounded reference context (RAG-lite) for LLM inference: substance
 * identity data, locally cached toxicity records and request metadata are
 * serialised into a compact evidence block the AI service injects into the
 * prompt. This is the first step towards full vector-DB RAG.
 */
@Service
public class CpsrContextService {
    private static final int MAX_CHARS_PER_RECORD = 300;

    private final SubstanceInfoRepository substanceRepo;
    private final ToxicityRecordRepository toxicityRepo;

    public CpsrContextService(SubstanceInfoRepository substanceRepo,
                              ToxicityRecordRepository toxicityRepo) {
        this.substanceRepo = substanceRepo;
        this.toxicityRepo = toxicityRepo;
    }

    public String buildContext(CpsrRequest cpsr) {
        StringBuilder sb = new StringBuilder();

        List<SubstanceInfo> substances = substanceRepo.findByCpsrRequestId(cpsr.getId());
        int refIndex = 1;
        for (SubstanceInfo substance : substances) {
            sb.append("[R").append(refIndex++).append("] SUBSTANCE: ")
                .append(safe(substance.getSubstanceName()));
            append(sb, "CAS", substance.getCasNumber());
            append(sb, "EC", substance.getEcNumber());
            append(sb, "formula", substance.getMolecularFormula());
            if (substance.getMolecularWeight() != null) {
                sb.append("; MW=").append(substance.getMolecularWeight());
            }
            if (substance.getIntendedConcentration() != null) {
                sb.append("; intended concentration=").append(substance.getIntendedConcentration()).append("%");
            }
            append(sb, "product type", substance.getProductType());
            append(sb, "intended use", substance.getIntendedUse());
            append(sb, "purity", substance.getPurity());
            sb.append('\n');

            List<ToxicityRecord> records = findToxicityRecords(substance);
            for (ToxicityRecord record : records.subList(0, Math.min(records.size(), 10))) {
                sb.append("    TOX DATA [").append(record.getSourceDb())
                    .append(record.getSourceId() == null ? "" : "/" + record.getSourceId()).append("]: ");
                sb.append(truncate(safe(record.getEndpointName())));
                if (record.getEndpointValue() != null && !record.getEndpointValue().isBlank()) {
                    sb.append(" = ").append(truncate(record.getEndpointValue()));
                    if (record.getEndpointUnit() != null && !record.getEndpointUnit().isBlank()) {
                        sb.append(' ').append(record.getEndpointUnit());
                    }
                }
                if (record.getTestGuideline() != null && !record.getTestGuideline().isBlank()) {
                    sb.append(" (").append(truncate(record.getTestGuideline())).append(')');
                }
                sb.append('\n');
            }
        }

        if (sb.isEmpty()) {
            return null;
        }
        return sb.toString().strip();
    }

    private List<ToxicityRecord> findToxicityRecords(SubstanceInfo substance) {
        if (substance.getCasNumber() != null && !substance.getCasNumber().isBlank()
                && !toxicityRepo.findByCasNumber(substance.getCasNumber()).isEmpty()) {
            return toxicityRepo.findByCasNumber(substance.getCasNumber());
        }
        return toxicityRepo.findBySubstanceNameContainingIgnoreCase(
            safe(substance.getSubstanceName()));
    }

    private static void append(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("; ").append(label).append('=').append(truncate(value));
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.strip();
    }

    private static String truncate(String value) {
        String stripped = value.strip();
        return stripped.length() > MAX_CHARS_PER_RECORD
            ? stripped.substring(0, MAX_CHARS_PER_RECORD)
            : stripped;
    }
}
