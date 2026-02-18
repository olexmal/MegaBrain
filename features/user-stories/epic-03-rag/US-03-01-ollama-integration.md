# US-03-01: Ollama Local LLM Integration

## Story
**As a** security-conscious organization  
**I want** to use a local LLM via Ollama  
**So that** no code is sent to external services

## Story Points: 5
## Priority: Critical
## Sprint Target: Sprint 4

---

## Acceptance Criteria

- [x] **AC1:** Ollama client integration via LangChain4j
- [ ] **AC2:** Configurable model selection (Codellama, Mistral, Llama2)
- [ ] **AC3:** Local LLM works without internet connectivity
- [x] **AC4:** Connection to configurable Ollama endpoint
- [ ] **AC5:** Health check verifies Ollama availability
- [ ] **AC6:** Clear error messages when Ollama unavailable

---

## Demo Script

### Setup
1. Ensure Ollama running locally with Codellama model
2. MegaBrain configured to use Ollama

### Demo Steps
1. **Show Config:** Display Ollama configuration
   ```yaml
   megabrain:
     llm:
       provider: ollama
       endpoint: http://localhost:11434
       model: codellama
   ```
2. **Health Check:** Verify Ollama connection
3. **Ask Question:** Submit question via API
   ```bash
   curl -X POST "http://localhost:8080/api/v1/rag" \
     -d '{"question": "How does the auth service work?"}'
   ```
4. **Show Response:** Display generated answer
5. **Offline Demo:** Disconnect network, show still works

### Expected Outcome
- Question answered using local Codellama
- No external API calls made
- Works offline

---

## Technical Tasks

- [x] **T1:** Add LangChain4j Ollama dependency (backend)
- [x] **T2:** Create `OllamaLLMClient` configuration (backend)
- [ ] **T3:** Implement model selection configuration (backend)
- [ ] **T4:** Add Ollama endpoint configuration (backend)
- [ ] **T5:** Create health check for Ollama (backend)
- [ ] **T6:** Write integration tests with Ollama (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| Successful generation | Ollama running | Send prompt | Response generated |
| Model selection | codellama configured | Send prompt | Uses codellama |
| Ollama offline | Ollama not running | Send prompt | Clear error message |
| Custom endpoint | Non-default URL | Configure + send | Connects to custom URL |

---

## Dependencies

- **Blocked by:** US-02-03 (needs search for context)
- **Enables:** US-03-03 (prompt construction), US-03-04 (streaming)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Ollama performance | Slow responses | Medium | GPU recommendation; model selection guide |
| Model not installed | Feature unavailable | Medium | Clear setup documentation |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

