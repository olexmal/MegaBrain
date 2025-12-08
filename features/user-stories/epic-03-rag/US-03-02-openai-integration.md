# US-03-02: OpenAI Cloud LLM Integration

## Story
**As a** user who prioritizes answer quality  
**I want** to use cloud LLMs like GPT-4  
**So that** I get the best possible answer quality

## Story Points: 3
## Priority: High
## Sprint Target: Sprint 4

---

## Acceptance Criteria

- [ ] **AC1:** OpenAI API integration (GPT-4, GPT-3.5-turbo)
- [ ] **AC2:** Anthropic API integration (Claude)
- [ ] **AC3:** API keys managed via environment variables
- [ ] **AC4:** Model selection configurable per request or globally
- [ ] **AC5:** Rate limiting and retry logic for API failures
- [ ] **AC6:** Usage logging for cost tracking

---

## Demo Script

### Setup
1. Configure OpenAI API key in environment
2. MegaBrain configured to use OpenAI

### Demo Steps
1. **Show Config:** Display cloud LLM configuration
2. **Ask Question:** Submit question
   ```bash
   curl -X POST "http://localhost:8080/api/v1/rag" \
     -d '{"question": "Explain the caching strategy", "model": "gpt-4"}'
   ```
3. **Show Response:** Display high-quality answer
4. **Model Selection:** Switch to GPT-3.5 for comparison
5. **Usage Log:** Show API usage tracking

### Expected Outcome
- GPT-4 provides detailed answer
- Model selection works
- Usage tracked for billing

---

## Technical Tasks

- [ ] **T1:** Add LangChain4j OpenAI dependency (backend)
- [ ] **T2:** Create `OpenAILLMClient` configuration (backend)
- [ ] **T3:** Add Anthropic Claude support (backend)
- [ ] **T4:** Implement API key management (backend)
- [ ] **T5:** Add rate limiting and retry logic (backend)
- [ ] **T6:** Implement usage logging (backend)
- [ ] **T7:** Write integration tests with mock API (test)

---

## Test Scenarios

| Scenario | Given | When | Then |
|:---------|:------|:-----|:-----|
| GPT-4 response | Valid API key | Send prompt | Response from GPT-4 |
| Model selection | GPT-3.5 specified | Send prompt | Uses GPT-3.5 |
| Invalid API key | Bad key | Send prompt | Auth error, clear message |
| Rate limited | Too many requests | Send prompts | Retry with backoff |

---

## Dependencies

- **Blocked by:** US-02-03 (needs search for context)
- **Enables:** US-03-03 (prompt construction), US-03-04 (streaming)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| API costs | Budget overrun | Medium | Usage tracking; alerts; model selection |
| API key exposure | Security breach | Low | Env vars only; never log |
| API outages | Feature unavailable | Low | Fallback to local; cache |

---

## Definition of Ready

- [x] Acceptance criteria clear
- [x] Dependencies identified
- [x] Tech tasks estimated
- [x] Test scenarios defined
- [x] Demo script approved
- [x] No blockers

