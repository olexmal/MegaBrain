/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import io.megabrain.core.LLMUsageRecord;
import io.megabrain.core.LLMUsageRecorder;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for LLM usage reporting (US-03-02 T6).
 * Exposes recent usage records and total cost estimate for billing and monitoring.
 */
@Path("/llm/usage")
@Produces(MediaType.APPLICATION_JSON)
public class LLMUsageResource {

    private final LLMUsageRecorder usageRecorder;

    @Inject
    public LLMUsageResource(LLMUsageRecorder usageRecorder) {
        this.usageRecorder = usageRecorder;
    }

    /**
     * Returns recent LLM usage records and total cost estimate.
     *
     * @param limit maximum number of records to return (default 100, max 1000)
     * @return JSON with recent usage list and totalCostEstimate
     */
    @GET
    public Response getUsage(
            @QueryParam("limit") @DefaultValue("100") int limit) {
        int capped = Math.min(1000, Math.max(1, limit));
        List<LLMUsageRecord> recent = usageRecorder.getRecent(capped);
        double totalCost = usageRecorder.getTotalCostEstimate();
        List<UsageRecordDto> dtos = recent.stream()
                .map(r -> new UsageRecordDto(
                        r.provider(),
                        r.model(),
                        r.inputTokens(),
                        r.outputTokens(),
                        r.costEstimate(),
                        r.timestamp().toString()))
                .collect(Collectors.toList());
        UsageReportResponse body = new UsageReportResponse(dtos, totalCost);
        return Response.ok(body).build();
    }

    /** DTO for a single usage record in API response. */
    public record UsageRecordDto(
            String provider,
            String model,
            int inputTokens,
            int outputTokens,
            double costEstimate,
            String timestamp
    ) {}

    /** Response body for usage report. */
    public record UsageReportResponse(List<UsageRecordDto> recent, double totalCostEstimate) {}
}
