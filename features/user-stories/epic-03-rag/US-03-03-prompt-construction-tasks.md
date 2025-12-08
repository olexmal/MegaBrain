# Tasks for US-03-03: Context-Aware Prompt Construction

## Story Reference
- **Epic:** EPIC-03 (RAG Answer Generation)
- **Story:** US-03-03
- **Story Points:** 5
- **Sprint Target:** Sprint 4

## Task List

### T1: Design prompt template structure
- **Description:** Design the prompt template structure that includes system prompt, user question, and code context. Define template sections: role definition, constraints, instructions, context formatting, and citation requirements. Create template as configurable resource.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** US-02-03 (needs search results)
- **Acceptance Criteria:**
  - [ ] Prompt template structure designed
  - [ ] Template includes all required sections
  - [ ] Template is configurable
  - [ ] Template supports citation instructions
- **Technical Notes:** Use template engine (e.g., Mustache, FreeMarker) or simple string replacement. Define sections: system_prompt, user_question, code_context, instructions. Store template in resources or database.

### T2: Implement context formatter for code chunks
- **Description:** Implement code chunk formatter that formats search results into readable context for LLM. Include source file path, entity name, line numbers, and code content. Format consistently across languages.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs template structure)
- **Acceptance Criteria:**
  - [ ] Code chunks formatted consistently
  - [ ] Includes source file, entity, line numbers
  - [ ] Format is clear and readable
  - [ ] Works for all supported languages
- **Technical Notes:** Format: `[Source: path/to/file.java - EntityName.method() (lines 25-45)]\n<code content>`. Use consistent formatting across languages. Include language identifier if helpful.

### T3: Implement token counting for context window
- **Description:** Implement token counting functionality to estimate token usage for prompt construction. Count tokens in system prompt, user question, and code chunks. Use approximate token counting (e.g., 1 token ≈ 4 characters) or integrate with model's tokenizer if available.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1, T2 (needs template and formatter)
- **Acceptance Criteria:**
  - [ ] Token counting implemented
  - [ ] Accurate enough for context window management
  - [ ] Handles different models (different context windows)
  - [ ] Efficient (doesn't slow down prompt construction)
- **Technical Notes:** Use approximate counting: tokens ≈ characters / 4. Or use tiktoken library if available for Java. Support different context windows per model (GPT-4: 8K/32K, Claude: 100K, Ollama: varies).

### T4: Add chunk selection based on relevance and token budget
- **Description:** Implement intelligent chunk selection that selects top-k most relevant chunks while staying within token budget. Prioritize high-relevance chunks, ensure diversity, and respect context window limits. Handle cases where all chunks don't fit.
- **Estimated Hours:** 5 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T2, T3 (needs formatter and token counting)
- **Acceptance Criteria:**
  - [ ] Chunks selected based on relevance
  - [ ] Token budget respected
  - [ ] Selection is efficient
  - [ ] Handles edge cases (too many chunks, no chunks)
- **Technical Notes:** Sort chunks by relevance score. Select chunks until token budget exhausted. Reserve tokens for system prompt and user question (e.g., 20% of context window). Consider diversity (avoid duplicate files).

### T5: Create configurable prompt template system
- **Description:** Implement configurable prompt template system that allows users to customize prompts. Load templates from files or database. Support template variables and conditional sections. Validate template syntax.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1 (needs template structure)
- **Acceptance Criteria:**
  - [ ] Templates loadable from files/database
  - [ ] Template variables supported
  - [ ] Templates configurable per model if needed
  - [ ] Template validation on load
- **Technical Notes:** Use template engine (Mustache, FreeMarker) or custom parser. Support variables: {{question}}, {{context}}, {{sources}}. Store templates in resources or configurable path. Validate on startup.

### T6: Write tests for prompt construction
- **Description:** Create comprehensive tests for prompt construction. Test template rendering, context formatting, token counting, chunk selection, and edge cases. Verify prompts are well-formed and within token limits.
- **Estimated Hours:** 4 hours
- **Assignee:** TBD
- **Status:** Not Started
- **Dependencies:** T1-T5 (needs complete implementation)
- **Acceptance Criteria:**
  - [ ] Tests for template rendering
  - [ ] Tests for context formatting
  - [ ] Tests for token counting
  - [ ] Tests for chunk selection
  - [ ] Test coverage >80%
- **Technical Notes:** Use JUnit 5. Test with various chunk counts, token budgets, and template configurations. Verify token counts are accurate. Test edge cases (empty context, too many chunks).

---

## Summary
- **Total Tasks:** 6
- **Total Estimated Hours:** 25 hours
- **Story Points:** 5 (1 SP ≈ 5 hours, aligns with estimate)

