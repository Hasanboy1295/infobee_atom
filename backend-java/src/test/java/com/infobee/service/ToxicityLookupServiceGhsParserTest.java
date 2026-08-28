package com.infobee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToxicityLookupServiceGhsParserTest {

    @Test
    void extractsStatementsPictogramsAndSignalWords() {
        Map<String, Object> body = ghsBody();

        ToxicityLookupService.GhsData data = ToxicityLookupService.parseGhs(body);

        assertEquals(2, data.statements().size());
        assertTrue(data.statements().contains("H302: Harmful if swallowed"));
        assertTrue(data.statements().contains("H315: Causes skin irritation"));
        assertEquals(List.of("Irritant (GHS07)"), data.pictograms());
        assertEquals(List.of("Warning"), data.signalWords());
    }

    @Test
    void toleratesMissingOrMalformedSections() {
        Map<String, Object> empty = new LinkedHashMap<>();
        ToxicityLookupService.GhsData data = ToxicityLookupService.parseGhs(empty);
        assertEquals(0, data.statements().size());

        Map<String, Object> noRecord = new LinkedHashMap<>();
        noRecord.put("Fault", Map.of("Code", "PUGVIEW-404"));
        assertTrue(ToxicityLookupService.parseGhs(noRecord).statements().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> info(String name, String text) {
        return Map.of(
            "Name", name,
            "Value", Map.of("StringWithMarkup", List.of(Map.of("String", text))));
    }

    private static Map<String, Object> section(String tocHeading, List<Object> children) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("TOCHeading", tocHeading);
        if (tocHeading.equals("GHS Classification")) {
            section.put("Section", children);
        } else {
            section.put("Information", children);
        }
        return section;
    }

    private static Map<String, Object> ghsBody() {
        List<Object> subsections = List.of(
            section("Hazard Statement(s)", List.of(
                info("Hazard statement", "H302: Harmful if swallowed"),
                info("Hazard statement", "H315: Causes skin irritation"))),
            section("Pictogram(s)", List.of(info("Pictogram", "Irritant (GHS07)"))),
            section("Signal Word", List.of(info("Signal word", "Warning"))),
            section("Precautionary Statement(s)", List.of(info("Precautionary", "P264: Wash thoroughly")))
        );
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("RecordType", "CID");
        record.put("RecordNumber", 7159);
        record.put("Section", List.of(section("GHS Classification", (List<Object>) (Object) subsections)));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Record", record);
        return body;
    }
}
