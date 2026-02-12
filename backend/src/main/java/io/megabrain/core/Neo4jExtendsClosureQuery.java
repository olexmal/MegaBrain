/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

/**
 * Neo4j-backed implementation of {@link ExtendsClosureQuery} (US-02-06, T4).
 * Finds all subclasses of a given class directly or transitively via EXTENDS
 * relationships, with a depth limit to avoid performance issues and unbounded
 * traversal. Uses Cypher variable-length path; results are deduplicated.
 * <p>
 * When Neo4j is not configured ({@code megabrain.neo4j.uri} absent), returns empty,
 * so this bean also serves as the default "stub" when no graph is available.
 * Handles multiple inheritance paths by returning each entity at most once (DISTINCT).
 */
@ApplicationScoped
public class Neo4jExtendsClosureQuery implements ExtendsClosureQuery {

    private static final Logger LOG = Logger.getLogger(Neo4jExtendsClosureQuery.class);

    /** Min/max depth for Cypher variable-length path; depth is validated and not parameterized. */
    private static final int MIN_DEPTH = 1;
    private static final int MAX_DEPTH = 10;

    /** Cypher template: depth is interpolated (validated int 1–10) to avoid unbounded traversal. */
    private static final String CYPHER_TEMPLATE =
            "MATCH (c:Class {name: $className})<-[:EXTENDS*%d..%d]-(sub) RETURN DISTINCT sub";

    @ConfigProperty(name = "megabrain.neo4j.uri")
    Optional<String> uri;

    @ConfigProperty(name = "megabrain.neo4j.username", defaultValue = "neo4j")
    Optional<String> username;

    @ConfigProperty(name = "megabrain.neo4j.password", defaultValue = "")
    Optional<String> password;

    private volatile Driver driver;

    @Override
    public Uni<List<GraphRelatedEntity>> findSubclassesOf(String className, int depth) {
        Driver neo4jDriver = getDriver();
        if (neo4jDriver == null) {
            return Uni.createFrom().item(List.<GraphRelatedEntity>of());
        }
        int boundedDepth = Math.clamp(depth, MIN_DEPTH, MAX_DEPTH);
        String cypher = String.format(CYPHER_TEMPLATE, MIN_DEPTH, boundedDepth);
        return Uni.createFrom().emitter(em -> {
            try (Session session = neo4jDriver.session()) {
                Result result = session.run(cypher, Values.parameters("className", className));
                List<GraphRelatedEntity> entities = mapResultToEntities(result);
                em.complete(entities);
            } catch (Exception e) {
                LOG.warnf(e, "Neo4j extends closure failed for class=%s depth=%d", className, depth);
                em.fail(new CompletionException(e));
            }
        });
    }

    private static List<GraphRelatedEntity> mapResultToEntities(Result result) {
        List<GraphRelatedEntity> entities = new ArrayList<>();
        result.forEachRemaining(record -> {
            if (record.get("sub").isNull()) {
                return;
            }
            org.neo4j.driver.types.Node subNode = record.get("sub").asNode();
            String name = subNode.containsKey("name") ? subNode.get("name").asString() : null;
            if (name != null && !name.isBlank()) {
                String type = subNode.labels().iterator().hasNext() ? subNode.labels().iterator().next() : null;
                entities.add(new GraphRelatedEntity(name, type, null));
            }
        });
        return entities;
    }

    private Driver getDriver() {
        if (uri.isEmpty() || uri.get().isBlank()) {
            return null;
        }
        if (driver == null) {
            synchronized (this) {
                if (driver == null) {
                    try {
                        org.neo4j.driver.AuthToken auth = (username.isPresent() && password.isPresent()
                                && !username.get().isBlank())
                                ? org.neo4j.driver.AuthTokens.basic(username.get(), password.get())
                                : org.neo4j.driver.AuthTokens.none();
                        driver = GraphDatabase.driver(uri.get(), auth);
                        LOG.infof("Neo4j driver created for extends closure (uri=%s)", uri.get());
                    } catch (Exception e) {
                        LOG.warnf(e, "Neo4j driver creation failed, extends closure will return empty");
                        return null;
                    }
                }
            }
        }
        return driver;
    }

    @PreDestroy
    void close() {
        if (driver != null) {
            try {
                driver.close();
            } catch (Exception e) {
                LOG.warnf(e, "Error closing Neo4j driver");
            }
            driver = null;
        }
    }
}
