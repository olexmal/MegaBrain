# EPIC-03: RAG Answer Generation

## Epic Overview

| Attribute | Value |
|:----------|:------|
| **Epic ID** | EPIC-03 |
| **Priority** | Critical |
| **Estimated Scope** | M |
| **Dependencies** | EPIC-01 (Ingestion), EPIC-02 (Search) |
| **Spec Reference** | Section 4.3 (FR-RAG) |
| **Status** | Planned |

## Business Value

This epic enables the core "ask questions about your code" capability. By combining retrieved code chunks with LLM generation:

- Developers can ask natural language questions and receive grounded answers
- Answers are based exclusively on the organization's actual code (not general training data)
- Source attribution ensures traceability and trust
- Streaming responses provide responsive UX

This transforms MegaBrain from a search tool into an intelligent code assistant.

---

## User Stories

### US-03-01: Contextual Answer Generation

**As a** developer, **I want** to ask natural language questions about my codebase, **so that** I can quickly understand how things work without manually reading code.

**Acceptance Criteria:**
- [ ] User submits question via API/UI
- [ ] System performs hybrid search to find top-k relevant chunks
- [ ] Chunks formatted into a prompt with clear instructions
- [ ] LLM generates answer based only on retrieved context
- [ ] Answer returned to user with source references
- [ ] Questions like "How does authentication work?" answered correctly

**Spec Reference:** FR-RAG-01

---

### US-03-02: Streaming Token Response

**As a** developer, **I want** to see the LLM's answer appear token-by-token, **so that** I get immediate feedback and don't wait for the full response.

**Acceptance Criteria:**
- [ ] LLM response streamed via SSE
- [ ] First token appears within 2 seconds of request
- [ ] Tokens displayed incrementally in UI
- [ ] Stream can be cancelled mid-generation
- [ ] Error handling for stream interruptions
- [ ] Graceful degradation if streaming unavailable

**Spec Reference:** FR-RAG-02

---

### US-03-03: Source Attribution

**As a** developer, **I want** answers to cite specific source files and code entities, **so that** I can verify the answer and explore further.

**Acceptance Criteria:**
- [ ] Each answer includes list of source chunks used
- [ ] Sources include: file path, entity name, line numbers
- [ ] Sources clickable/linkable in UI
- [ ] LLM instructed to reference sources in answer text
- [ ] Inline citations format: `[Source: path/to/file.java:42]`
- [ ] Source relevance scores included

**Spec Reference:** FR-RAG-03

---

### US-03-04: Local LLM Support (Ollama)

**As a** security-conscious organization, **I want** to use a local LLM via Ollama, **so that** no code is sent to external services.

**Acceptance Criteria:**
- [ ] Ollama integration via LangChain4j
- [ ] Configurable model selection (Codellama, Mistral, etc.)
- [ ] Local LLM works without internet connectivity
- [ ] Performance acceptable for interactive use
- [ ] Clear documentation on Ollama setup

**Spec Reference:** Deployment & Configuration (7.0)

---

### US-03-05: Cloud LLM Support

**As a** user who prioritizes quality over privacy, **I want** to use cloud LLMs (OpenAI, Anthropic), **so that** I get the best possible answer quality.

**Acceptance Criteria:**
- [ ] OpenAI API integration (GPT-4, GPT-3.5)
- [ ] Anthropic API integration (Claude)
- [ ] API keys managed securely via environment variables
- [ ] Model selection configurable per request or globally
- [ ] Rate limiting and error handling for API failures
- [ ] Cost tracking/logging for API usage

**Spec Reference:** Technical Stack (3.2) - LLM Integration

---

### US-03-06: Prompt Engineering

**As a** system, **I want** well-designed prompts that maximize answer quality, **so that** users get accurate, helpful responses.

**Acceptance Criteria:**
- [ ] System prompt establishes role and constraints
- [ ] Context window efficiently utilized
- [ ] Code chunks clearly delineated in prompt
- [ ] Instructions to cite sources included
- [ ] Instructions to admit uncertainty when context insufficient
- [ ] Prompt templates configurable/customizable

**Spec Reference:** FR-RAG-01 (Process)

---

## Technical Notes

### Key Components
- **`RagService`:** Manages LLM interaction, prompt construction, token streaming
- **`MegaBrainOrchestrator`:** Coordinates search → retrieval → generation pipeline

### Technology Stack
| Component | Technology |
|:----------|:-----------|
| LLM Client | Quarkus LangChain4j Extension |
| Local LLM | Ollama (Codellama, Mistral, Llama2) |
| Cloud LLMs | OpenAI API, Anthropic API |
| Streaming | Mutiny Multi, SSE |

### Prompt Template Structure

```
System: You are a code assistant analyzing a specific codebase. 
Answer questions based ONLY on the provided code context. 
If the context doesn't contain relevant information, say so.
Always cite sources using [Source: filename:line] format.

Context:
---
[Chunk 1: path/to/file.java - ClassName.methodName()]
{code content}
---
[Chunk 2: path/to/other.py - function_name()]
{code content}
---

User Question: {question}

Answer:
```

### Context Window Management
- Estimate tokens per chunk (avg ~200 tokens)
- Reserve tokens for system prompt (~500) and response (~1000)
- Fill remaining context with highest-ranked chunks
- Truncate chunks if needed, preserving structure

### Streaming Architecture
```
Request → RagService → LangChain4j → LLM
                                   ← Token Stream
         SSE Encoder ←
Client ←
```

---

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| LLM hallucination | Incorrect answers | Medium | Strong prompt constraints; source attribution; confidence indicators |
| Context window overflow | Truncated context | Medium | Smart chunking; summarization; chunk selection |
| Local LLM too slow | Poor UX | Medium | GPU acceleration; model selection guidance; streaming |
| Cloud API costs | Budget overrun | Medium | Usage tracking; caching; rate limiting |
| API key exposure | Security breach | Low | Vault integration; env vars; never log keys |

---

## Non-Functional Requirements

| NFR | Target | Validation |
|:----|:-------|:-----------|
| First token latency | <2s | End-to-end timing tests |
| Full response time | <30s for typical questions | Benchmark with varied queries |
| Concurrent RAG requests | 10+ simultaneous | Load testing |
| Answer accuracy | >80% relevant to question | Human evaluation on test set |

---

## Definition of Done

- [ ] All user stories complete and accepted
- [ ] Ollama integration working with Codellama
- [ ] OpenAI integration working with GPT-4
- [ ] SSE streaming implemented end-to-end
- [ ] Source attribution in all answers
- [ ] Prompt templates finalized and documented
- [ ] First token latency NFR met
- [ ] Unit tests for RagService (>80% coverage)
- [ ] Integration tests with mock LLM
- [ ] Documentation updated

---

## Open Questions

1. Should we support conversation history (multi-turn RAG)?
2. How do we handle "I don't know" responses gracefully?
3. Should users be able to customize prompt templates?
4. Do we need a feedback mechanism for answer quality?

---

**Epic Owner:** TBD  
**Created:** December 2024  
**Last Updated:** December 2024

