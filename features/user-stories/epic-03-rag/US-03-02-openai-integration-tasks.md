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
- **Status:** Not Started
- **Dependencies:** None
- **Acceptance Criteria:**
  - [ ] LangChain4j OpenAI dependency added
  - [ ] Dependency resolves correctly
  - [ ] No version conflicts
- **Technical Notes:** Use LangChain4j OpenAI integration. Version: 0.29+ recommended.

### T2: Create OpenAILLMClient configuration
- **Description:** Create `OpenAILLMClient` class that wraps LangChain4j's OpenAI integration. Implement LLM client interface for consistent usage. Handle API key management and configuration loading.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs dependency)
- **Acceptance Criteria:**
  - [ ] OpenAILLMClient class created
  - [ ] Implements common LLM client interface
  - [ ] Handles API key authentication
  - [ ] Configuration loaded from properties
- **Technical Notes:** Use LangChain4j's OpenAiStreamingChatLanguageModel. Support GPT-4 and GPT-3.5-turbo models. Use Quarkus CDI for dependency injection.

### T3: Add Anthropic Claude support
- **Description:** Add Anthropic Claude API integration alongside OpenAI. Create `AnthropicLLMClient` class following same pattern as OpenAI client. Support Claude Sonnet and Claude Opus models.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs OpenAI client pattern)
- **Acceptance Criteria:**
  - [ ] AnthropicLLMClient class created
  - [ ] Supports Claude Sonnet and Claude Opus
  - [ ] API key authentication working
  - [ ] Consistent interface with OpenAI client
- **Technical Notes:** Use LangChain4j's Anthropic integration or direct Anthropic SDK. Support Claude 3.5 Sonnet and Claude 3 Opus models. Follow same configuration pattern as OpenAI.

### T4: Implement API key management
- **Description:** Implement secure API key management for OpenAI and Anthropic. Load API keys from environment variables or configuration. Never log or expose keys in error messages. Support key validation on startup.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3 (needs client implementations)
- **Acceptance Criteria:**
  - [ ] API keys loaded from environment variables
  - [ ] Keys never logged or exposed
  - [ ] Key validation on startup
  - [ ] Clear error messages for missing/invalid keys
- **Technical Notes:** Use Quarkus configuration with environment variable support: `megabrain.llm.openai.api-key=${OPENAI_API_KEY}`. Validate key format (starts with `sk-` for OpenAI). Mask keys in logs.

### T5: Add rate limiting and retry logic
- **Description:** Implement rate limiting handling and retry logic for API failures. Handle HTTP 429 (rate limit) and 5xx errors with exponential backoff. Configure retry attempts and backoff intervals.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3 (needs client implementations)
- **Acceptance Criteria:**
  - [ ] Rate limit errors handled gracefully
  - [ ] Exponential backoff implemented
  - [ ] Retry attempts configurable
  - [ ] Clear error messages for rate limits
- **Technical Notes:** Use resilience4j or similar for retry logic. Exponential backoff: 1s, 2s, 4s, 8s. Max retries: 3-5. Handle both rate limit (429) and server errors (5xx).

### T6: Implement usage logging
- **Description:** Implement usage logging for API calls to track costs. Log model used, tokens consumed (input/output), and estimated cost. Store usage metrics for reporting and billing.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3 (needs client implementations)
- **Acceptance Criteria:**
  - [ ] Usage logged for each API call
  - [ ] Tracks model, tokens, estimated cost
  - [ ] Usage metrics stored (database or metrics system)
  - [ ] Cost estimation accurate
- **Technical Notes:** Log to database table or metrics system (Prometheus). Track: model, input_tokens, output_tokens, cost_estimate. Use current pricing for cost calculation. Support usage reports/APIs.

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

