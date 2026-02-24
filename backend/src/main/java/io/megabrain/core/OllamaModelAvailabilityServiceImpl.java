/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation that checks Ollama /api/tags and caches results.
 */
@ApplicationScoped
public class OllamaModelAvailabilityServiceImpl implements OllamaModelAvailabilityService {

    private static final Logger LOG = Logger.getLogger(OllamaModelAvailabilityServiceImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final OllamaConfiguration config;
    private final HttpClient httpClient;
    private final ConcurrentHashMap<String, CachedModelAvailability> cache = new ConcurrentHashMap<>();

    private record CachedModelAvailability(boolean available, long expiresAtMillis) {}

    public OllamaModelAvailabilityServiceImpl(OllamaConfiguration config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public Uni<Boolean> isModelAvailable(String baseUrl, String model) {
        if (baseUrl == null || baseUrl.isBlank() || model == null || model.isBlank()) {
            return Uni.createFrom().item(false);
        }
        String modelKey = model.trim();
        long now = System.currentTimeMillis();
        int cacheSeconds = config.modelAvailabilityCacheSeconds();
        long expiresAt = now + cacheSeconds * 1000L;

        CachedModelAvailability cached = cache.get(modelKey);
        if (cached != null && now < cached.expiresAtMillis()) {
            return Uni.createFrom().item(cached.available());
        }

        return fetchModelAvailability(baseUrl, modelKey)
                .invoke(available -> cache.put(modelKey, new CachedModelAvailability(available, expiresAt)));
    }

    private Uni<Boolean> fetchModelAvailability(String baseUrl, String model) {
        String url = baseUrl.endsWith("/") ? baseUrl + "api/tags" : baseUrl + "/api/tags";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return Uni.createFrom().completionStage(httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .onItem().transform(response -> {
                    if (response.statusCode() != 200) {
                        LOG.warnf("Ollama /api/tags returned status %d", response.statusCode());
                        return false;
                    }
                    return parseModelsAndCheck(model, response.body());
                })
                .onFailure().recoverWithItem(e -> {
                    LOG.debugf(e, "Failed to fetch Ollama model list from %s", url);
                    return false;
                });
    }

    private boolean parseModelsAndCheck(String requestedModel, String jsonBody) {
        try {
            JsonNode root = JSON.readTree(jsonBody);
            JsonNode models = root.get("models");
            if (models == null || !models.isArray()) {
                return false;
            }
            for (JsonNode m : models) {
                String name = m.has("name") ? m.get("name").asText("") : "";
                if (name.equals(requestedModel) || name.startsWith(requestedModel + ":")) {
                    return true;
                }
                String modelField = m.has("model") ? m.get("model").asText("") : "";
                if (modelField.equals(requestedModel) || modelField.startsWith(requestedModel + ":")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            LOG.debugf(e, "Failed to parse Ollama tags response");
            return false;
        }
    }
}
