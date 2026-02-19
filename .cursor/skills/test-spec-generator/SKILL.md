---
name: test-spec-generator
description: Generates a test specification in Markdown from a feature solution description. Uses a structured template (reviewers, introduction, references, test cases) and emphasizes testing input limits per panel (grouped inputs) rather than per field. Use when the user asks for a test spec, test specification, QA test cases from a feature description, or invokes /test-spec.
---

# Test Spec Generator

Act as a QA engineer. Produce a test specification in Markdown from the user's feature solution description and **write it to a file** in the project. Follow the template and guidelines exactly so output works well with free or less capable models (explicit structure, clear steps).

## Output Location

- **Folder:** `docs/test-specs/` (create the folder if it does not exist).
- **Filename:** Derive from the feature name: lowercase, hyphens for spaces, suffix `-test-spec.md`. Examples: `user-profile-update-test-spec.md`, `payment-details-panel-test-spec.md`.
- **Action:** Write the full generated Markdown to that file. After writing, confirm the path to the user.

If the user specifies a different path or filename, use that instead.

## When to Apply

- User asks for a test spec, test specification, or QA test cases from a feature description.
- User invokes `/test-spec` or provides a feature solution description and wants test cases.
- User mentions testing a feature, validation scenarios, or test design for a panel/form.

## Output Template

Use this structure for the generated document:

```markdown
# Test Specification: [Feature Name]

## Reviewers
- Reviewer 1: [Name/Role – e.g., Product Owner]
- Reviewer 2: [Name/Role – e.g., Lead Developer]

## Introduction
[Brief description of the feature and scope of testing. Include objectives and assumptions.]

## References
- Epic: [Link to Epic – placeholder or actual URL]
- Business Case: [Link to Business Case – placeholder or actual URL]

## Test case count

| Execution type | Count | Description |
| -------------- | ----- | ----------- |
| Automatic      | [N]   | Runnable by automation (API, CLI, headless UI tests). |
| Manual         | [N]   | Requires human execution (visual checks, exploratory, UX). |
| **Total**      | [N]   | |

*Requirements: **at least 30 Automatic**, **more than 15 Manual** (i.e. ≥16). Fill this table after writing all test cases.*

## Test Cases

### Test Case 1: [Descriptive Title]
| Field | Value |
| ----- | ----- |
| **ID**         | TC001                                                        |
| **Description**| [What is being tested?]                                      |
| **Preconditions**| [What must be true before test execution?]                 |
| **Test Data**  | [Specific values used in the test]                           |
| **Steps**      | 1. [Step one]<br>2. [Step two]<br>3. ...                     |
| **Expected Results** | [What should happen after steps are executed?]          |
| **Category**   | e.g. Positive, Negative, Edge, Boundary, Performance, Security, Concurrency |
| **Execution**  | Automatic or Manual (automatable by CI vs human-only)        |

### Test Case 2: [Title]
...
```

## Quantity and Coverage

- **Minimum by execution type:**
  - **Automatic:** at least **30** test cases (runnable by automation: API, CLI, headless).
  - **Manual:** more than **15** (i.e. at least **16**) test cases (require human: visual, exploratory, UX).
  - So **total minimum is 46** test cases. Classify every test case with **Execution** = Automatic or Manual in its table.
- **Coverage categories:** Ensure test cases span:
  - **Positive** – valid inputs and happy paths
  - **Negative** – invalid inputs, missing required fields, wrong types
  - **Edge** – unusual but valid (e.g. empty optional fields, single-item lists)
  - **Boundary** – min/max lengths, limits, just inside/outside allowed range
  - **Performance** – latency, throughput, large payloads (where applicable)
  - **Security** – auth failures, path traversal, injection, no sensitive data in responses
  - **Concurrency** – simultaneous requests or users (where applicable)
- Add **Category** and **Execution** (Automatic/Manual) to each test case table so reviewers can verify coverage and automation balance.

## Guidelines

1. **Identify inputs** – List all new or changed input fields from the feature description. Group them by the panel/section they belong to (e.g. "User Profile Panel", "Payment Details Panel").

2. **Test limits per panel** – For each panel, design test cases that exercise **combinations** of its inputs. Focus on:
   - Boundary values (min, max, just inside/outside)
   - Invalid combinations (e.g. mutually exclusive choices, out-of-range pairs)
   - Stress scenarios (many inputs at their limits at once)
   - Do **not** create separate test cases for individual inputs unless they are fully independent (rare).

3. **Use standard techniques** – Apply equivalence partitioning and boundary value analysis in the context of the panel’s combined inputs.

4. **Coverage** – Include both positive (valid) and negative (invalid) test cases. Cover critical user journeys. Explicitly add performance, security, and concurrency scenarios where the feature touches them.

5. **Assumptions** – If the feature description omits details, make reasonable assumptions and document them in the Introduction or as notes in test cases.

6. **Clarity** – Be explicit; avoid ambiguous phrasing. Structure output exactly as in the template. Use bullet points and tables for readability.

## Step-by-step reasoning (do this before writing)

1. **Extract requirements** – List all functional requirements, user stories, acceptance criteria, and API/UI details from the feature description.
2. **Per requirement** – For each, brainstorm: valid inputs, invalid inputs, boundary values, edge cases.
3. **Non-functional** – Identify performance (latency, load), security (auth, injection, path traversal), and concurrency (simultaneous users/requests) scenarios.
4. **Group** – Map scenarios to panels/sections; ensure each panel has multiple cases (positive, negative, boundary, and at least one performance or security or concurrency if applicable).
5. **Count** – Before writing, confirm you have **at least 30 Automatic** and **more than 15 Manual** (≥16) test cases. If not, add more automatable cases (API/CLI/headless) or more manual cases (visual/exploratory/UX) until both minimums are met.

## Workflow

1. Take the feature solution description from the user (or from `[FEATURE_DESCRIPTION]` if they used that placeholder).
2. **Perform step-by-step reasoning** above (extract requirements, brainstorm, NFR, group, count ≥30 automatic and >15 manual).
3. Identify panels and their inputs; list them.
4. For each panel, design test cases that cover combinations (boundaries, invalid combos, stress) and ensure coverage categories are represented. Plan so that **at least 30** are Automatic and **more than 15** (≥16) are Manual.
5. Fill the template: reviewers, introduction (with assumptions if any), references, **test case count** table (fill counts after writing), then **all** test cases in the table format above. Each test case must include **Category** and **Execution** (Automatic or Manual). Meet minimums: **≥30 Automatic**, **>15 Manual**.
6. Number test cases TC001, TC002, … and keep steps and expected results concrete.
7. **Write the result to a file** in `docs/test-specs/` using the filename derived from the feature name (e.g. `docs/test-specs/feature-name-test-spec.md`). Create `docs/test-specs/` if needed. Tell the user the path where the file was written.
8. **Fix table structure** – Before finishing, ensure every test case table uses valid GFM structure: a header row with column names, then a separator row with at least three dashes per column. Use `| Field | Value |` as the header and `| ----- | ----- |` as the separator for each test case table. Do not use blank or placeholder header cells; proper headers improve parsing and rendering.
9. **Print statistics to the user** – When the skill terminates, output in chat a short summary so the user sees it immediately:
   - **Output path:** The path to the written file (e.g. `docs/test-specs/feature-name-test-spec.md`).
   - **Statistics table:** Show counts for **Automatic**, **Manual**, and **Total**. Confirm whether the spec meets the minimums: **≥30 Automatic**, **>15 Manual**. Example:
     | Execution | Count |
     | --------- | ----- |
     | Automatic | 22    |
     | Manual    | 13    |
     | **Total** | **35** |
   - Optionally: counts by **Category** (Positive, Negative, Edge, etc.) if that helps the user. Keep the message concise.

## Additional Resources

- For a full worked example (User Profile Update panel), see [template.md](template.md).

**Invocation:** Use the command palette or type `/` and select the skill; paste the feature description in chat. The agent replaces `[FEATURE_DESCRIPTION]` when the user supplies it.
