package io.megabrain.api;

import io.megabrain.ingestion.IngestionService;
import io.megabrain.ingestion.ProgressEvent;
import io.megabrain.ingestion.StreamEvent;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * REST API resource for repository ingestion operations.
 * Provides endpoints for both full and incremental repository indexing.
 */
@Path("/ingestion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IngestionResource {

    private static final Logger LOG = LoggerFactory.getLogger(IngestionResource.class);
    private static final String REPOSITORY_URL_KEY = "repositoryUrl";

    private final IngestionService ingestionService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Inject
    public IngestionResource(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Starts ingestion of a repository.
     * Supports both full and incremental indexing modes.
     *
     * @param request the ingestion request containing repository URL and options
     * @return Server-Sent Events stream of progress updates
     */
    @POST
    @Path("/repositories")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> ingestRepository(IngestionRequest request) {
        LOG.info("Received ingestion request: {}", request);

        Multi<ProgressEvent> progressStream;

        if (request.isIncremental()) {
            LOG.info("Starting incremental ingestion for repository: {}", request.getRepositoryUrl());
            progressStream = ingestionService.ingestRepositoryIncrementally(request.getRepositoryUrl());
        } else {
            LOG.info("Starting full ingestion for repository: {}", request.getRepositoryUrl());
            progressStream = ingestionService.ingestRepository(request.getRepositoryUrl());
        }

        // Convert ProgressEvent to SSE format
        return progressStream.map(event ->
            "data: " + event.toJson() + "\n\n"
        ).onFailure().recoverWithItem(throwable -> {
            LOG.error("Ingestion failed", throwable);
            return "data: {\"message\": \"Ingestion failed: " + throwable.getMessage() + "\", \"progress\": 0.0}\n\n";
        });
    }

    /**
     * Starts ingestion of a repository with StreamEvent streaming.
     * Uses the new StreamEvent format for progress reporting.
     *
     * @param request the ingestion request containing repository URL and options
     * @return Server-Sent Events stream of StreamEvent progress updates
     */
    @POST
    @Path("/repositories/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> ingestRepositoryStream(@Valid IngestionRequest request) {
        LOG.info("Received streaming ingestion request: {}", request);

        Multi<StreamEvent> progressStream = createStreamEventProgressStream(request);

        // Convert StreamEvent to SSE format
        return progressStream.map(event -> {
            try {
                // Use Jackson ObjectMapper for proper JSON serialization
                String jsonData = objectMapper.writeValueAsString(event);
                return "data: " + jsonData + "\n\n";
            } catch (Exception e) {
                LOG.error("Failed to serialize StreamEvent to JSON", e);
                return "data: {\"stage\":\"FAILED\",\"message\":\"Serialization error\",\"percentage\":0,\"timestamp\":\"" +
                       Instant.now() + "\",\"metadata\":{}}\n\n";
            }
        }).onFailure().recoverWithItem(throwable -> {
            LOG.error("Streaming ingestion failed", throwable);
            return "data: {\"stage\":\"FAILED\",\"message\":\"" + throwable.getMessage().replace("\"", "\\\"") +
                   "\",\"percentage\":0,\"timestamp\":\"" + Instant.now() + "\",\"metadata\":{}}\n\n";
        });
    }

    /**
     * Creates a progress stream using StreamEvent for the ingestion request.
     * This demonstrates the new StreamEvent-based streaming approach.
     */
    private Multi<StreamEvent> createStreamEventProgressStream(IngestionRequest request) {
        return Multi.createFrom().emitter(emitter -> {
            try {
                // Statistics tracking
                final int[] filesProcessed = {0};
                final int[] chunksCreated = {0};
                final StreamEvent.Stage[] lastStage = {StreamEvent.Stage.CLONING};
                final long[] startTime = {System.currentTimeMillis()};

                // Emit start event
                emitter.emit(StreamEvent.of(StreamEvent.Stage.CLONING, "Starting ingestion process", 0));

                // Get the existing progress stream and convert to StreamEvent
                Multi<ProgressEvent> existingStream;
                if (request.isIncremental()) {
                    existingStream = ingestionService.ingestRepositoryIncrementally(request.getRepositoryUrl());
                } else {
                    existingStream = ingestionService.ingestRepository(request.getRepositoryUrl());
                }

                // Subscribe to the existing stream and convert events
                existingStream.subscribe().with(
                    progressEvent -> {
                        // Convert ProgressEvent to StreamEvent
                        StreamEvent.Stage stage = mapProgressToStage(progressEvent);
                        String message = progressEvent.message();
                        int percentage = (int) Math.round(progressEvent.progress());

                        // Track statistics from progress messages
                        extractStatisticsFromMessage(message, filesProcessed, chunksCreated);
                        lastStage[0] = stage;

                        StreamEvent streamEvent = StreamEvent.of(stage, message, percentage);
                        emitter.emit(streamEvent);
                    },
                    error -> {
                        LOG.error("Ingestion error", error);
                        long duration = System.currentTimeMillis() - startTime[0];
                        Map<String, Object> errorMetadata = Map.of(
                            "errorType", error.getClass().getSimpleName(),
                            "stage", lastStage[0].toString(),
                            "durationMs", duration,
                            REPOSITORY_URL_KEY, request.getRepositoryUrl()
                        );
                        emitter.emit(StreamEvent.of(StreamEvent.Stage.FAILED,
                                                  "Ingestion failed: " + error.getMessage(), 0, errorMetadata));
                        emitter.fail(error);
                    },
                    () -> {
                        // Emit completion event with statistics
                        long duration = System.currentTimeMillis() - startTime[0];
                        Map<String, Object> completionMetadata = Map.of(
                            "filesProcessed", filesProcessed[0],
                            "chunksCreated", chunksCreated[0],
                            "durationMs", duration,
                            REPOSITORY_URL_KEY, request.getRepositoryUrl(),
                            "ingestionType", request.isIncremental() ? "incremental" : "full"
                        );
                        emitter.emit(StreamEvent.of(StreamEvent.Stage.COMPLETE,
                                                  String.format("Ingestion completed successfully - processed %d files, created %d chunks in %d ms",
                                                      filesProcessed[0], chunksCreated[0], duration),
                                                  100, completionMetadata));
                        emitter.complete();
                    }
                );

            } catch (Exception e) {
                LOG.error("Failed to create stream event progress stream", e);
                Map<String, Object> errorMetadata = Map.of(
                    "errorType", e.getClass().getSimpleName(),
                    "stage", "INITIALIZATION",
                    REPOSITORY_URL_KEY, request.getRepositoryUrl()
                );
                emitter.emit(StreamEvent.of(StreamEvent.Stage.FAILED,
                                          "Failed to start ingestion: " + e.getMessage(), 0, errorMetadata));
                emitter.fail(e);
            }
        });
    }

    /**
     * Maps ProgressEvent progress values to StreamEvent stages.
     * This mapping provides more granular stage detection for cloning operations.
     */
    private StreamEvent.Stage mapProgressToStage(ProgressEvent progressEvent) {
        double progress = progressEvent.progress();
        String message = progressEvent.message().toLowerCase();

        // Specific cloning-related messages
        if (message.contains("starting repository clone") ||
            message.contains("preparing clone destination") ||
            message.contains("clone started") ||
            message.contains("clone completed") ||
            message.contains("cloning repository")) {
            return StreamEvent.Stage.CLONING;
        }

        // Parsing-related messages
        if (message.contains("extract") ||
            message.contains("parsing") ||
            message.contains("found") && message.contains("files")) {
            return StreamEvent.Stage.PARSING;
        }

        // Indexing-related messages
        if (message.contains("index") ||
            message.contains("chunk") ||
            message.contains("starting full indexing") ||
            message.contains("incremental indexing")) {
            return StreamEvent.Stage.INDEXING;
        }

        // Terminal states
        if (progress >= 100.0 ||
            message.contains("complete") ||
            message.contains("ingestion completed")) {
            return StreamEvent.Stage.COMPLETE;
        }

        if (message.contains("fail") ||
            message.contains("error") ||
            message.contains("failed")) {
            return StreamEvent.Stage.FAILED;
        }

        // Progress-based fallback logic
        // CLONING: 0-25%, PARSING: 25-75%, INDEXING: 75-100%
        if (progress < 25.0) {
            return StreamEvent.Stage.CLONING;
        } else if (progress < 75.0) {
            return StreamEvent.Stage.PARSING;
        } else {
            return StreamEvent.Stage.INDEXING;
        }
    }

    /**
     * Extracts statistics from progress messages to track files processed and chunks created.
     */
    void extractStatisticsFromMessage(String message, int[] filesProcessed, int[] chunksCreated) {
        String lowerMessage = message.toLowerCase();

        // Extract file counts from parsing messages like "Parsed file 3/10: Calculator.java"
        Pattern filePattern = Pattern.compile("parsed file (\\d+)/(\\d+)");
        Matcher fileMatcher = filePattern.matcher(lowerMessage);
        if (fileMatcher.find()) {
            filesProcessed[0] = Math.max(filesProcessed[0], Integer.parseInt(fileMatcher.group(2)));
        }

        // Extract chunk counts from parsing messages like "Parsed file 3/10: Calculator.java (25 chunks)"
        Pattern chunkPattern = Pattern.compile("\\((\\d+) chunks?\\)");
        Matcher chunkMatcher = chunkPattern.matcher(lowerMessage);
        if (chunkMatcher.find()) {
            int chunksInMessage = Integer.parseInt(chunkMatcher.group(1));
            chunksCreated[0] += chunksInMessage;
        }

        // Extract chunk counts from indexing messages like "Indexed batch 2: 40/80 chunks"
        Pattern indexPattern = Pattern.compile("indexed.*?(\\d+)/(\\d+) chunks");
        Matcher indexMatcher = indexPattern.matcher(lowerMessage);
        if (indexMatcher.find()) {
            chunksCreated[0] = Math.max(chunksCreated[0], Integer.parseInt(indexMatcher.group(2)));
        }

        // Extract file counts from completion messages
        if (lowerMessage.contains("processed") && lowerMessage.contains("files")) {
            Pattern processedPattern = Pattern.compile("processed (\\d+) files");
            Matcher processedMatcher = processedPattern.matcher(lowerMessage);
            if (processedMatcher.find()) {
                filesProcessed[0] = Math.max(filesProcessed[0], Integer.parseInt(processedMatcher.group(1)));
            }
        }
    }

    /**
     * Gets the ingestion status or capabilities.
     * This is a placeholder for future status endpoint.
     *
     * @return API capabilities information
     */
    @GET
    @Path("/status")
    public Response getIngestionStatus() {
        return Response.ok()
            .entity(new IngestionStatus("READY", "Ingestion service is available"))
            .build();
    }

    /**
     * Gets performance metrics for recent ingestion operations.
     * Useful for verifying incremental vs full indexing performance (AC7).
     *
     * @return Performance metrics information
     */
    @GET
    @Path("/performance")
    public Response getPerformanceMetrics() {
        // This is a placeholder - in a real implementation, you'd track and return
        // actual performance metrics from recent operations
        return Response.ok()
            .entity(new PerformanceMetrics(
                "Performance metrics not yet implemented. Check application logs for timing information.",
                "To verify AC7: Compare 'Full ingestion completed' vs 'Incremental ingestion completed' log entries"
            ))
            .build();
    }

    /**
     * Ingestion status response model.
     */
    public record IngestionStatus(String status, String message) {
    }

    /**
     * Performance metrics response model.
     */
    public record PerformanceMetrics(String note, String verification) {
    }
}
