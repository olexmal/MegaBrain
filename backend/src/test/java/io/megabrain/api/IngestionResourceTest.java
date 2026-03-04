/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.megabrain.ingestion.IngestionService;
import org.junit.jupiter.api.Test;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class IngestionResourceTest {

    @InjectMock
    IngestionService ingestionService;

    @Inject
    IngestionResource ingestionResource;

    @Test
    void testResourceInjected() {
        assertNotNull(ingestionResource, "IngestionResource should be injected");
        assertNotNull(ingestionService, "IngestionService mock should be injected");
    }
}
