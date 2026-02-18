# Tasks for US-03-01: Ollama Local LLM Integration

## Story Reference
- **Epic:** EPIC-03 (RAG Answer Generation)
- **Story:** US-03-01
- **Story Points:** 5
- **Sprint Target:** Sprint 4

## Task List

### T1: Add LangChain4j Ollama dependency
- **Description:** Add LangChain4j Ollama dependency to project build file (pom.xml or build.gradle). Include necessary transitive dependencies. Verify dependency resolution and compatibility with existing dependencies.
- **Estimated Hours:** 1 hour
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** None
- **Acceptance Criteria:**
  - [x] LangChain4j Ollama dependency added
  - [x] Dependency resolves correctly
  - [x] No version conflicts
  - [x] Build succeeds
- **Technical Notes:** Use LangChain4j Ollama integration. Version: 0.29+ recommended. Add to Quarkus project dependencies.
- **Implementation Notes:** Dependency was already present in `backend/pom.xml` (langchain4j-bom 1.9.1, langchain4j-ollama). Verified via `mvn dependency:list` (langchain4j:1.9.1, langchain4j-ollama:1.9.1) and `mvn compile`. Comment added in pom.xml documenting BOM version (0.29+).

### T2: Create OllamaLLMClient configuration
- **Description:** Create `OllamaLLMClient` class that wraps LangChain4j's Ollama integration. Implement LLM client interface for consistent usage across different providers. Handle connection management and configuration loading.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Completed
- **Dependencies:** T1 (needs dependency)
- **Acceptance Criteria:**
  - [x] OllamaLLMClient class created
  - [x] Implements common LLM client interface
  - [x] Handles connection to Ollama endpoint
  - [x] Configuration loaded from properties
- **Technical Notes:** Use LangChain4j's OllamaStreamingChatLanguageModel. Create unified interface for LLM clients (Ollama, OpenAI, Anthropic). Use Quarkus CDI for dependency injection.
- **Implementation Notes:** Added `LLMClient` interface (generate, isAvailable), `OllamaConfiguration` ConfigMapping (baseUrl, model, timeoutSeconds), and `OllamaLLMClient` wrapping LangChain4j `OllamaChatModel`. Config via `megabrain.llm.ollama.base-url`, `megabrain.llm.ollama.model`, `megabrain.llm.ollama.timeout-seconds`. Unit tests: OllamaLLMClientTest, LLMClientTest, OllamaConfigurationTest.

### T3: Implement model selection configuration
- **Description:** Implement configuration support for Ollama model selection. Allow users to specify model name (codellama, mistral, llama2, etc.) via application.properties. Support per-request model override via API parameters.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs OllamaLLMClient)
- **Acceptance Criteria:**
  - [ ] Model selection via configuration
  - [ ] Default model configurable
  - [ ] Per-request model override supported
  - [ ] Model validation (check if model available)
- **Technical Notes:** Use Quarkus configuration: `megabrain.llm.ollama.model=codellama`. Support model list query to verify availability. Cache model availability check.

### T4: Add Ollama endpoint configuration
- **Description:** Add configuration support for Ollama endpoint URL. Default to `http://localhost:11434`. Allow custom endpoint for remote Ollama instances. Support connection timeout and retry configuration.
- **Estimated Hours:** 2 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2 (needs OllamaLLMClient)
- **Acceptance Criteria:**
  - [ ] Endpoint URL configurable
  - [ ] Default endpoint: http://localhost:11434
  - [ ] Custom endpoint supported
  - [ ] Connection timeout configurable
- **Technical Notes:** Use Quarkus configuration: `megabrain.llm.ollama.endpoint=http://localhost:11434`. Support HTTP and HTTPS endpoints. Validate URL format on startup.

### T5: Create health check for Ollama
- **Description:** Implement health check that verifies Ollama service availability. Check if Ollama endpoint is reachable and if configured model is available. Integrate with Quarkus health checks.
- **Estimated Hours:** 3 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3, T4 (needs client and configuration)
- **Acceptance Criteria:**
  - [ ] Health check verifies Ollama endpoint
  - [ ] Health check verifies model availability
  - [ ] Integrates with `/q/health/ready` endpoint
  - [ ] Clear status messages
- **Technical Notes:** Use Quarkus SmallRye Health. Create custom health check that pings Ollama API. Check model availability via Ollama's `/api/tags` endpoint. Report status: UP, DOWN, UNKNOWN.

### T6: Write integration tests with Ollama
- **Description:** Create integration tests that interact with real Ollama instance (or mock). Test model selection, endpoint configuration, health checks, and actual LLM generation. Use Testcontainers or mock Ollama server if needed.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Integration tests for Ollama client
  - [ ] Tests verify model selection
  - [ ] Tests verify endpoint configuration
  - [ ] Tests verify health checks
  - [ ] Test coverage >80%
- **Technical Notes:** Use Testcontainers with Ollama container, or mock Ollama HTTP server. Test both success and failure scenarios. Test offline scenario (Ollama unavailable).

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 17 hours
- **Story Points:** 5 (1 SP ≈ 3.4 hours, note: this is simpler than estimate suggests)

