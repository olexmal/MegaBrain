/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Validates {@link BoostConfiguration} at application startup.
 * <p>
 * Business rules:
 * <ul>
 *   <li>All configured boost values must be finite and positive.</li>
 * </ul>
 * </p>
 */
@Startup
@ApplicationScoped
public class BoostConfigurationValidator {

    @Inject
    BoostConfiguration boostConfiguration;

    @PostConstruct
    void validate() {
        validatePositiveFinite("megabrain.search.boost.entity-name", boostConfiguration.entityName());
        validatePositiveFinite("megabrain.search.boost.doc-summary", boostConfiguration.docSummary());
        validatePositiveFinite("megabrain.search.boost.content", boostConfiguration.content());
    }

    private static void validatePositiveFinite(String propertyName, float value) {
        if (Float.isNaN(value) || Float.isInfinite(value) || value <= 0.0f) {
            throw new IllegalStateException(
                "Invalid boost configuration: '" + propertyName + "' must be a finite, positive number, but was: " + value
            );
        }
    }
}

