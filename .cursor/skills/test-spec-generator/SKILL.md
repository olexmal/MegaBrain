---
name: test-spec-generator
description: Generates a test specification in Markdown from a feature solution description. Uses a structured template (reviewers, introduction, references, test cases) and emphasizes testing input limits per panel (grouped inputs) rather than per field. Use when the user asks for a test spec, test specification, QA test cases from a feature description, or invokes /test-spec.
---

# Test Spec Generator

Act as a QA engineer. Produce a test specification in Markdown from the user's feature solution description. Follow the template and guidelines exactly so output works well with free or less capable models (explicit structure, clear steps).

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

## Test Cases

### Test Case 1: [Descriptive Title]
|                |                                                              |
| -------------- | ------------------------------------------------------------ |
| **ID**         | TC001                                                        |
| **Description**| [What is being tested?]                                      |
| **Preconditions**| [What must be true before test execution?]                 |
| **Test Data**  | [Specific values used in the test]                           |
| **Steps**      | 1. [Step one]<br>2. [Step two]<br>3. ...                     |
| **Expected Results** | [What should happen after steps are executed?]          |

### Test Case 2: [Title]
...
```

## Guidelines

1. **Identify inputs** – List all new or changed input fields from the feature description. Group them by the panel/section they belong to (e.g. "User Profile Panel", "Payment Details Panel").

2. **Test limits per panel** – For each panel, design test cases that exercise **combinations** of its inputs. Focus on:
   - Boundary values (min, max, just inside/outside)
   - Invalid combinations (e.g. mutually exclusive choices, out-of-range pairs)
   - Stress scenarios (many inputs at their limits at once)
   - Do **not** create separate test cases for individual inputs unless they are fully independent (rare).

3. **Use standard techniques** – Apply equivalence partitioning and boundary value analysis in the context of the panel’s combined inputs.

4. **Coverage** – Include both positive (valid) and negative (invalid) test cases. Cover critical user journeys.

5. **Assumptions** – If the feature description omits details, make reasonable assumptions and document them in the Introduction or as notes in test cases.

6. **Clarity** – Be explicit; avoid ambiguous phrasing. Structure output exactly as in the template. Use bullet points and tables for readability.

## Workflow

1. Take the feature solution description from the user (or from `[FEATURE_DESCRIPTION]` if they used that placeholder).
2. Identify panels and their inputs; list them.
3. For each panel, design test cases that cover combinations (boundaries, invalid combos, stress), not one test per input.
4. Fill the template: reviewers, introduction (with assumptions if any), references, then all test cases in the table format above.
5. Number test cases TC001, TC002, … and keep steps and expected results concrete.

## Additional Resources

- For a full worked example (User Profile Update panel), see [template.md](template.md).

**Invocation:** Use the command palette or type `/` and select the skill; paste the feature description in chat. The agent replaces `[FEATURE_DESCRIPTION]` when the user supplies it.
