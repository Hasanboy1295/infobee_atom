package com.infobee.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiServiceClient {
    private final RestClient restClient;
    private final boolean enabled;
    private final String authToken;

    public AiServiceClient(
        @Value("${app.ai.base-url:http://localhost:8000}") String baseUrl,
        @Value("${app.ai.enabled:true}") boolean enabled,
        @Value("${app.ai.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${app.ai.read-timeout-ms:30000}") int readTimeoutMs,
        @Value("${app.ai.auth-token:}") String authToken
    ) {
        this.enabled = enabled;
        this.authToken = authToken == null ? "" : authToken;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> predictAtom(Map<String, Object> payload) {
        return post("/predict/atom", payload);
    }

    public Map<String, Object> completeLlm(Map<String, Object> payload) {
        return post("/llm/complete", payload);
    }

    private RestClient.RequestHeadersSpec<?> authorized() {
        if (authToken.isEmpty()) {
            return restClient.post();
        }
        return restClient.post().header("Authorization", "Bearer " + authToken);
    }

    private Map<String, Object> post(String path, Map<String, Object> payload) {
        return authorized()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
