# Test Spec Generator – Template

## Example: User Profile Update

### Input (feature description)

```
Feature: User Profile Update
Users can now edit their profile information in a new "Edit Profile" panel. The panel contains:
- Full name (text, max 50 chars, required)
- Email (email format, required)
- Age (integer, 18-120, optional)
- Country (dropdown with 10 options, required)
- Newsletter subscription (checkbox, default unchecked)

All fields are on the same panel. The "Save" button updates the profile and shows a success message.
```

### Output (generated test specification)

```markdown
# Test Specification: User Profile Update

## Reviewers
- Reviewer 1: Product Owner
- Reviewer 2: Lead Developer

## Introduction
This specification covers testing of the new "Edit Profile" panel, which allows users to update their profile information. All five input fields (Full name, Email, Age, Country, Newsletter) are tested together as a group to ensure proper validation, error handling, and successful update.

## References
- Epic: [Link to Epic]
- Business Case: [Link to Business Case]

## Test Cases

### Test Case 1: Valid update with all fields filled
|                |                                                              |
| -------------- | ------------------------------------------------------------ |
| **ID**         | TC001                                                        |
| **Description**| Verify that a user can successfully update their profile with all fields correctly filled. |
| **Preconditions**| User is logged in and on the Edit Profile panel.             |
| **Test Data**  | Full name: "John Doe"<br>Email: "john.doe@example.com"<br>Age: 30<br>Country: "USA"<br>Newsletter: checked |
| **Steps**      | 1. Enter the test data into all fields.<br>2. Click "Save".  |
| **Expected Results** | A success message appears. The profile is updated in the database. |

### Test Case 2: Boundary values for Age and Name length
|                |                                                              |
| -------------- | ------------------------------------------------------------ |
| **ID**         | TC002                                                        |
| **Description**| Test the combination of maximum allowed name length and minimum allowed age. |
| **Preconditions**| User is logged in and on the Edit Profile panel.             |
| **Test Data**  | Full name: "A" × 50<br>Email: "test@example.com"<br>Age: 18<br>Country: "Canada"<br>Newsletter: unchecked |
| **Steps**      | 1. Enter the test data.<br>2. Click "Save".                  |
| **Expected Results** | Update succeeds. Name is stored with 50 characters, age as 18. |

### Test Case 3: Invalid email format + age out of range
|                |                                                              |
| -------------- | ------------------------------------------------------------ |
| **ID**         | TC003                                                        |
| **Description**| Verify that the panel rejects an invalid email together with an age outside the allowed range (121). |
| **Preconditions**| User is logged in and on the Edit Profile panel.             |
| **Test Data**  | Full name: "Jane Smith"<br>Email: "notanemail"<br>Age: 121<br>Country: "UK"<br>Newsletter: checked |
| **Steps**      | 1. Enter the test data.<br>2. Click "Save".                  |
| **Expected Results** | Error messages appear for both Email and Age fields. Profile is not updated. |

### Test Case 4: Missing required fields (Name, Email, Country)
|                |                                                              |
| -------------- | ------------------------------------------------------------ |
| **ID**         | TC004                                                        |
| **Description**| Test that all three required fields (Name, Email, Country) trigger validation errors when left empty. |
| **Preconditions**| User is logged in and on the Edit Profile panel.             |
| **Test Data**  | Full name: ""<br>Email: ""<br>Age: 25<br>Country: ""<br>Newsletter: unchecked |
| **Steps**      | 1. Leave Name, Email, and Country empty.<br>2. Click "Save". |
| **Expected Results** | Three separate error messages indicating that Name, Email, and Country are required. No update occurs. |
```

### Notes

- All inputs are on one panel, so test cases exercise **combinations** (e.g. TC002: name length + age boundary; TC003: invalid email + invalid age).
- No separate test case per single field; each case targets the panel as a whole.
- Positive (TC001, TC002) and negative (TC003, TC004) cases are included.
