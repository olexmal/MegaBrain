# EPIC-07: Documentation Intelligence

## Epic Overview

| Attribute | Value |
|:----------|:------|
| **Epic ID** | EPIC-07 |
| **Priority** | Medium |
| **Estimated Scope** | M |
| **Dependencies** | EPIC-01 (Code Ingestion & Indexing) |
| **Spec Reference** | Section 4.6 (FR-DOC) |
| **Status** | Planned |

## Business Value

This epic extracts and indexes documentation embedded in source code:

- **Enhanced search:** Documentation matches rank higher, improving result quality
- **API discovery:** Find documented APIs easily with "docs only" filter
- **Quality metrics:** Track documentation coverage across the codebase
- **Example extraction:** Find usage examples from doc comments

Better documentation intelligence means better answers from RAG and more useful search results.

---

## User Stories

### US-07-01: Javadoc Extraction

**As a** Java developer, **I want** Javadoc comments extracted and indexed, **so that** I can search for APIs by their documentation.

**Acceptance Criteria:**
- [ ] Parse `/** ... */` comment blocks
- [ ] Extract: summary, `@param`, `@return`, `@throws`, `@see`, `@example`
- [ ] Link documentation to corresponding class/method via AST
- [ ] Handle nested classes and inner documentation
- [ ] Preserve formatting for display

**Spec Reference:** FR-DOC-01

---

### US-07-02: JSDoc Extraction

**As a** JavaScript/TypeScript developer, **I want** JSDoc comments extracted, **so that** my JS/TS APIs are searchable.

**Acceptance Criteria:**
- [ ] Parse `/** ... */` JSDoc blocks
- [ ] Extract: description, `@param`, `@returns`, `@example`, `@typedef`
- [ ] Link to corresponding function/class
- [ ] Handle TypeScript-specific annotations
- [ ] Support markdown in descriptions

**Spec Reference:** FR-DOC-01

---

### US-07-03: Python Docstring Extraction

**As a** Python developer, **I want** docstrings extracted, **so that** Python APIs are searchable.

**Acceptance Criteria:**
- [ ] Parse triple-quoted strings (`"""..."""` and `'''...'''`)
- [ ] Support Google, NumPy, and Sphinx docstring formats
- [ ] Extract: summary, Args, Returns, Raises, Examples
- [ ] Link to function/class/module
- [ ] Handle multi-line docstrings

**Spec Reference:** FR-DOC-01

---

### US-07-04: Rust/Go Doc Comment Extraction

**As a** Rust or Go developer, **I want** doc comments extracted, **so that** my code is searchable.

**Acceptance Criteria:**
- [ ] Rust: Parse `///` and `//!` comments with markdown
- [ ] Go: Parse `//` comments preceding declarations
- [ ] Extract structured sections (Examples, Panics, Safety)
- [ ] Link to corresponding items
- [ ] Preserve code blocks in examples

**Spec Reference:** FR-DOC-01

---

### US-07-05: Doxygen Extraction (C/C++)

**As a** C/C++ developer, **I want** Doxygen comments extracted, **so that** my code is searchable.

**Acceptance Criteria:**
- [ ] Parse `/** ... */`, `///`, `//!` with Doxygen commands
- [ ] Extract: `@brief`, `@param`, `@return`, `@code`/`@endcode`
- [ ] Handle Doxygen-specific syntax
- [ ] Link to functions, classes, macros

**Spec Reference:** FR-DOC-01

---

### US-07-06: Documentation Indexing

**As a** developer, **I want** documentation content indexed with boosted relevance, **so that** API descriptions rank high in search.

**Acceptance Criteria:**
- [ ] Separate index fields: `doc_summary`, `doc_params`, `doc_returns`, `doc_examples`
- [ ] Higher boost weights for doc fields (2.0x default)
- [ ] "Docs only" filter in search API
- [ ] Markdown rendering in search results
- [ ] Documentation snippets in result cards

**Spec Reference:** FR-DOC-02

---

### US-07-07: Documentation Coverage Metrics

**As a** tech lead, **I want** to track documentation coverage, **so that** I can prioritize documentation efforts.

**Acceptance Criteria:**
- [ ] Coverage score: percentage of public APIs with docs (0.0-1.0)
- [ ] Completeness score: param/return/example presence
- [ ] Per-repository reports
- [ ] Per-module/package reports
- [ ] Trend tracking over time
- [ ] Identify undocumented public APIs

**Spec Reference:** FR-DOC-03

---

### US-07-08: Documentation Quality Alerts

**As a** tech lead, **I want** alerts for documentation issues, **so that** problems are surfaced proactively.

**Acceptance Criteria:**
- [ ] Alert: undocumented public APIs
- [ ] Alert: missing `@param` for function parameters
- [ ] Alert: stale docs (references non-existent entities)
- [ ] Configurable thresholds for alerts
- [ ] Alerts in ingestion report
- [ ] Export alerts as actionable list

**Spec Reference:** FR-DOC-03

---

### US-07-09: Code Example Extraction

**As a** developer, **I want** to find code examples from documentation, **so that** I can learn from usage patterns.

**Acceptance Criteria:**
- [ ] Extract `@example` blocks (JSDoc/Javadoc)
- [ ] Extract fenced code blocks from markdown docs
- [ ] Extract doctest blocks (Python)
- [ ] Link examples to related function/class
- [ ] Query: "show me examples of using X"
- [ ] Syntax highlighting for examples

**Spec Reference:** FR-DOC-04

---

## Technical Notes

### Key Components
- **`DocCommentParser`:** Multi-format documentation extraction
- **`DocumentationQualityAnalyzer`:** Computes coverage metrics

### Technology Stack
| Component | Technology |
|:----------|:-----------|
| Javadoc Parsing | Custom parser or JavaParser integration |
| JSDoc Parsing | Custom parser on AST |
| Python Docstrings | Tree-sitter + custom extraction |
| Markdown Parsing | Flexmark or CommonMark |

### Documentation Data Model

```java
record DocumentationBlock(
    String entityId,           // Link to code entity
    String summary,            // Brief description
    List<ParamDoc> params,     // @param entries
    String returns,            // @return/@returns
    List<String> examples,     // @example blocks
    List<String> seeAlso,      // @see references
    DocFormat format,          // JAVADOC, JSDOC, DOCSTRING, etc.
    double coverageScore       // 0.0 - 1.0
)

record ParamDoc(
    String name,
    String type,
    String description
)
```

### Index Fields for Documentation

```
Field: doc_summary (TextField, indexed, boost=2.0)
Field: doc_params (TextField, indexed)
Field: doc_returns (TextField, indexed)
Field: doc_examples (TextField, indexed, boost=1.5)
Field: doc_see_also (TextField, indexed)
Field: doc_coverage_score (DoubleField, stored)
```

---

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|:-----|:-------|:-----------|:-----------|
| Non-standard doc formats | Extraction failures | Medium | Graceful degradation; fallback to raw text |
| Large doc blocks slow parsing | Performance impact | Low | Async extraction; streaming |
| Stale doc detection false positives | Noisy alerts | Medium | Confidence scoring; configurable sensitivity |
| Multi-language projects | Inconsistent coverage | Medium | Per-language normalization; clear reporting |

---

## Non-Functional Requirements

| NFR | Target | Validation |
|:----|:-------|:-----------|
| Extraction overhead | <10% of parsing time | Benchmark |
| Coverage report generation | <30s for <1M LOC | Timing tests |
| Doc search boost effectiveness | Measurable relevance improvement | A/B testing |

---

## Definition of Done

- [ ] All user stories complete and accepted
- [ ] Extraction working for all 6 doc formats
- [ ] Documentation indexed with boost weights
- [ ] Coverage metrics calculated correctly
- [ ] Quality alerts functional
- [ ] Example extraction working
- [ ] Unit tests (>80% coverage)
- [ ] Integration tests with real codebases
- [ ] Documentation updated

---

## Open Questions

1. Should we support custom doc formats via configuration?
2. How do we handle generated documentation (autodoc)?
3. Should coverage metrics exclude test files?
4. Do we need historical coverage tracking?

---

**Epic Owner:** TBD  
**Created:** December 2025  
**Last Updated:** December 2025

