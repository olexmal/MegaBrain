/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;

/**
 * Service that checks if a given model is available on an Ollama instance.
 * Abstracts the Ollama /api/tags API for testability and caching.
 */
public interface OllamaModelAvailabilityService {

    /**
     * Checks if the given model is available on the Ollama instance at baseUrl.
     *
     * @param baseUrl Ollama API base URL (e.g. http://localhost:11434)
     * @param model   model name (e.g. codellama, mistral)
     * @return Uni that emits true if available, false otherwise
     */
    Uni<Boolean> isModelAvailable(String baseUrl, String model);
}
