package com.infobee.service;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Client for the free PubChem PUG REST / PUG View APIs (no API key required).
 * Used to enrich CPSR substances with compound identity data and GHS hazard
 * classifications. See https://pubchem.ncbi.nlm.nih.gov/docs/pug-rest
 */
@Service
public class PubChemClient {
    private static final String PUG_REST = "/rest/pug/compound";
    private static final String PUG_VIEW = "/rest/pug_view/data/compound";

    private final RestClient restClient;

    public PubChemClient(
        @Value("${app.pubchem.base-url:https://pubchem.ncbi.nlm.nih.gov}") String baseUrl,
        @Value("${app.pubchem.enabled:true}") boolean enabled,
        @Value("${app.pubchem.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${app.pubchem.read-timeout-ms:15000}") int readTimeoutMs
    ) {
        this.enabled = enabled;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .build();
    }

    private final boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Resolves the first PubChem CID for a substance name or CAS number.
     * Returns null when the compound cannot be resolved.
     */
    public Long resolveCid(String query) {
        Map<String, Object> body = get(PUG_REST + "/name/" + urlEncode(query) + "/cids/JSON");
        if (body == null || !(body.get("IdentifierList") instanceof Map<?, ?> identifiers)) {
            return null;
        }
        if (!(identifiers.get("CID") instanceof List<?> cids) || cids.isEmpty()) {
            return null;
        }
        Object first = cids.get(0);
        return first instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(first));
    }

    /**
     * Fetches compound properties (title, molecular formula, molecular weight).
     */
    public Map<String, Object> fetchCompoundProperties(Long cid) {
        return get(PUG_REST + "/cid/" + cid + "/property/MolecularFormula,MolecularWeight/JSON");
    }

    /**
     * Fetches the GHS Classification section of the PubChem safety record.
     */
    public Map<String, Object> fetchGhsClassification(Long cid) {
        return get(PUG_VIEW + "/" + cid + "/JSON?heading_type=compound&heading=GHS+Classification");
    }

    private Map<String, Object> get(String path) {
        return restClient.get()
            .uri(path)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
