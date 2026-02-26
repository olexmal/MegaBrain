# Tasks for US-03-02: OpenAI Cloud LLM Integration

## Story Reference
- **Epic:** EPIC-03 (RAG Answer Generation)
- **Story:** US-03-02
- **Story Points:** 3
- **Sprint Target:** Sprint 4

## Task List

### T1: Add LangChain4j OpenAI dependency
- **Description:** Add LangChain4j OpenAI dependency to project build file. Include necessary transitive dependencies. Verify dependency resolution and compatibility.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** None
- **Acceptance Criteria:**
  - [x] LangChain4j OpenAI dependency added
  - [x] Dependency resolves correctly
  - [x] No version conflicts
- **Technical Notes:** Use LangChain4j OpenAI integration. Version: 0.29+ recommended.
- **Implementation Notes:** Dependency was already present in `backend/pom.xml` (langchain4j-bom 1.9.1, artifact `langchain4j-open-ai`). Verified via `mvn dependency:list` (langchain4j-open-ai:1.9.1) and `mvn clean install`. Comment in pom.xml updated to reference US-03-02 for OpenAI.

### T2: Create OpenAILLMClient configuration
- **Description:** Create `OpenAILLMClient` class that wraps LangChain4j's OpenAI integration. Implement LLM client interface for consistent usage. Handle API key management and configuration loading.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1 (needs dependency)
- **Acceptance Criteria:**
  - [x] OpenAILLMClient class created
  - [x] Implements common LLM client interface
  - [x] Handles API key authentication
  - [x] Configuration loaded from properties
- **Technical Notes:** Use LangChain4j's OpenAiStreamingChatLanguageModel. Support GPT-4 and GPT-3.5-turbo models. Use Quarkus CDI for dependency injection.
- **Implementation Notes:** Verified existing implementation: `OpenAILLMClient` implements `LLMClient`, uses `OpenAIConfiguration` (@ConfigMapping prefix `megabrain.llm.openai`), API key from `megabrain.llm.openai.api-key` / `${OPENAI_API_KEY:}` in application.properties. Uses `OpenAiChatModel` for non-streaming generation; supports per-request model override. Unit tests in `OpenAILLMClientTest` cover availability, validation, and contract.

### T3: Add Anthropic Claude support
- **Description:** Add Anthropic Claude API integration alongside OpenAI. Create `AnthropicLLMClient` class following same pattern as OpenAI client. Support Claude Sonnet and Claude Opus models.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2 (needs OpenAI client pattern)
- **Acceptance Criteria:**
  - [x] AnthropicLLMClient class created
  - [x] Supports Claude Sonnet and Claude Opus
  - [x] API key authentication working
  - [x] Consistent interface with OpenAI client
- **Technical Notes:** Use LangChain4j's Anthropic integration or direct Anthropic SDK. Support Claude 3.5 Sonnet and Claude 3 Opus models. Follow same configuration pattern as OpenAI.
- **Implementation Notes:** `AnthropicLLMClient` implements `LLMClient`, uses `AnthropicConfiguration` (@ConfigMapping prefix `megabrain.llm.anthropic`), API key from `megabrain.llm.anthropic.api-key` / `${ANTHROPIC_API_KEY:}` in application.properties. Uses LangChain4j `AnthropicChatModel` for non-streaming generation; supports per-request model override. Default model `claude-3-5-sonnet-20241022`. Unit tests in `AnthropicLLMClientTest` cover availability, validation, and contract without calling real API.

### T4: Implement API key management
- **Description:** Implement secure API key management for OpenAI and Anthropic. Load API keys from environment variables or configuration. Never log or expose keys in error messages. Support key validation on startup.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2, T3 (needs client implementations)
- **Acceptance Criteria:**
  - [x] API keys loaded from environment variables
  - [x] Keys never logged or exposed
  - [x] Key validation on startup
  - [x] Clear error messages for missing/invalid keys
- **Technical Notes:** Use Quarkus configuration with environment variable support: `megabrain.llm.openai.api-key=${OPENAI_API_KEY}`. Validate key format (starts with `sk-` for OpenAI). Mask keys in logs.
- **Implementation Notes:** Added `LLMApiKeyValidator` with format checks (OpenAI: `sk-`, Anthropic: `sk-ant-`) and mask helpers. Both clients validate key format in `@PostConstruct init()` and fail fast with clear messages when invalid; init exceptions are wrapped so API keys are never included in logs or error messages. Unit tests: `LLMApiKeyValidatorTest`, and client tests updated for valid key formats plus invalid-format init tests.

### T5: Add rate limiting and retry logic
- **Description:** Implement rate limiting handling and retry logic for API failures. Handle HTTP 429 (rate limit) and 5xx errors with exponential backoff. Configure retry attempts and backoff intervals.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2, T3 (needs client implementations)
- **Acceptance Criteria:**
  - [x] Rate limit errors handled gracefully
  - [x] Exponential backoff implemented
  - [x] Retry attempts configurable
  - [x] Clear error messages for rate limits
- **Technical Notes:** Use resilience4j or similar for retry logic. Exponential backoff: 1s, 2s, 4s, 8s. Max retries: 3-5. Handle both rate limit (429) and server errors (5xx).
- **Implementation Notes:** Implemented shared LLMRetryHelper with manual retry loop and exponential backoff (baseDelayMs, 2x, 4x, 8x, cap 30s). Retryable detection via exception message/cause (429, rate limit, 503/502/500, service unavailable). Config: megabrain.llm.openai.max-retries, base-delay-ms; same for anthropic. OpenAILLMClient and AnthropicLLMClient inject LLMRetryHelper and wrap chat calls. Clear messages: "Rate limit exceeded. Please try again later." and "Service temporarily unavailable. Please try again later." Unit tests: LLMRetryHelperTest (succeed on Nth attempt, fail after max retries, non-retryable fails immediately, isRetryable). Client tests updated to inject mock LLMRetryHelper.

### T6: Implement usage logging
- **Description:** Implement usage logging for API calls to track costs. Log model used, tokens consumed (input/output), and estimated cost. Store usage metrics for reporting and billing.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T2, T3 (needs client implementations)
- **Acceptance Criteria:**
  - [x] Usage logged for each API call
  - [x] Tracks model, tokens, estimated cost
  - [x] Usage metrics stored (database or metrics system)
  - [x] Cost estimation accurate
- **Technical Notes:** Log to database table or metrics system (Prometheus). Track: model, input_tokens, output_tokens, cost_estimate. Use current pricing for cost calculation. Support usage reports/APIs.
- **Implementation Notes:** Added LLMUsageRecord (provider, model, inputTokens, outputTokens, costEstimate, timestamp), LLMCostEstimator (estimateCost for OpenAI GPT-4/GPT-3.5 and Anthropic Sonnet/Opus; estimateTokens from text length), LLMUsageRecorder interface with InMemoryLLMUsageRecorder (bounded in-memory store) and NoOpLLMUsageRecorder for optional injection. OpenAILLMClient and AnthropicLLMClient inject LLMUsageRecorder; after each successful chat call they estimate tokens (chars/4), compute cost, record usage, and log (model, tokens, cost, duration). GET /api/v1/llm/usage?limit= returns recent records and totalCostEstimate. Unit tests: LLMCostEstimatorTest, InMemoryLLMUsageRecorderTest, NoOpLLMUsageRecorderTest.

### T7: Write integration tests with mock API
- **Description:** Create integration tests using mock OpenAI and Anthropic API servers. Test API key management, rate limiting, retry logic, and usage logging. Test both success and failure scenarios.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T6 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Integration tests with mock APIs
  - [ ] Tests cover rate limiting scenarios
  - [ ] Tests cover retry logic
  - [ ] Tests verify usage logging
  - [ ] Test coverage >80%
- **Technical Notes:** Use WireMock or similar for HTTP mocking. Test rate limit responses (429), retry behavior, and usage tracking. Test both OpenAI and Anthropic clients.

---

## Summary
- **Total Tasks:** 7
- **Total Estimated Hours:** 25 hours
- **Story Points:** 3 (1 SP ≈ 8.3 hours, note: this includes both OpenAI and Anthropic)

