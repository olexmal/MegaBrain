# Implement Task

A reusable command that guides through the complete task implementation process for any development task in MegaBrain.

## Description

This command provides a comprehensive, reusable process for implementing any task, following the MegaBrain development methodology. It ensures consistent quality, testing, and documentation standards across all implementations.

## Usage

```
/implement-task [task-id] [task-description]
```

## Parameters

- `task-id`: Task identifier (e.g., "T1", "T2", "US-01-01")
- `task-description`: Brief description of what needs to be implemented

## Process Overview

## Implementation Checklist

Use this checklist to track progress through the implementation process:

### Task Analysis Phase
- Read and understand task requirements from user story
- Identify all acceptance criteria and success metrics
- Review technical constraints and dependencies
- Estimate implementation effort (2-6 hours typical)
- Identify required files, interfaces, and tests to create/modify
- Document scope and deliverables clearly

### Implementation Planning Phase
- Design solution architecture and component interactions
- Plan code changes and identify new files needed
- Design interfaces and data structures
- Plan comprehensive testing strategy (>80% coverage target)
- Identify performance considerations and requirements
- Plan error handling and edge cases coverage

### Code Implementation Phase
- Create/modify source files as planned
- Implement business logic with proper error handling
- Add comprehensive logging and monitoring
- Follow reactive patterns (Mutiny Uni/Multi where applicable)
- Use CDI dependency injection (no manual instantiation)
- Handle all identified edge cases and error scenarios
- Add performance monitoring and resource management

### Unit Testing Phase
- Create comprehensive unit test class
- Implement tests for success scenarios
- Implement tests for error and failure scenarios
- Test all edge cases and boundary conditions
- Mock external dependencies properly
- Use descriptive assertions and test naming
- Achieve >80% test coverage

### Compilation & Test Verification Phase
- During development: Run specific tests as needed: `mvn test -Dtest=TestClass`
- Fix any compilation errors (imports, syntax, AssertJ issues)
- Fix any test failures (assertions, mocking, logic errors)
- **MANDATORY (Final Validation)**: Compile whole backend: `mvn clean install`
- Verify test coverage meets requirements

### Documentation & Completion Phase
- Update task status in user story files (mark as completed)
- Mark all acceptance criteria as completed
- Add implementation notes and technical details
- Update API documentation if applicable
- Verify all requirements and success criteria are met
- Commit changes following Git workflow standards

## Detailed Process Phases

### 1. Task Analysis (15-30 min)
**Input:** Task requirements, acceptance criteria, dependencies

**Activities:**
- Read and understand task requirements
- Identify acceptance criteria and success metrics
- Review technical constraints and dependencies
- Estimate implementation effort
- Identify required changes (files, interfaces, tests)

**Output:** Clear understanding of scope and deliverables

### 2. Implementation Planning (30-45 min)
**Input:** Task analysis results

**Activities:**
- Design solution architecture
- Plan code changes and new files
- Design interfaces and data structures
- Plan testing strategy and coverage targets
- Identify performance considerations
- Plan error handling and edge cases

**Output:** Detailed implementation plan

### 3. Code Implementation (2-6 hours)
**Input:** Implementation plan

**Activities:**
- Create/modify source files
- Implement business logic with proper error handling
- Add logging and monitoring
- Follow reactive patterns (Mutiny Uni/Multi)
- Use CDI dependency injection
- Handle all identified edge cases

**Patterns to Follow:**
```java
@ApplicationScoped
public class ServiceImpl implements Service {

    @Inject
    DependencyService dependency;

    public Uni<Result> process(Request request) {
        long startTime = System.nanoTime();
        try {
            return validateRequest(request)
                .flatMap(valid -> performOperation(valid))
                .invoke(result -> {
                    long duration = (System.nanoTime() - startTime) / 1_000_000;
                    LOG.infof("Operation completed in %d ms", duration);
                });
        } catch (Exception e) {
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            LOG.errorf(e, "Operation failed after %d ms", duration);
            throw e;
        }
    }
}
```

**Output:** Working implementation meeting requirements

### 4. Unit Testing (1-3 hours)
**Input:** Implemented code

**Activities:**
- Create comprehensive unit tests (>80% coverage)
- Test success paths and error scenarios
- Mock external dependencies
- Test edge cases and boundary conditions
- Use proper assertions and test naming

**Testing Template:**
```java
@ExtendWith(SystemStubsExtension.class)
class ServiceTest {

    @TempDir Path tempDir;
    @Mock DependencyService dependency;
    @InjectMocks ServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSuccessScenario() {
        // Given
        when(dependency.call()).thenReturn(Uni.createFrom().item(expectedResult));

        // When
        Uni<Result> result = service.process(validRequest);

        // Then
        assertThat(result.await().indefinitely()).satisfies(actual -> {
            assertThat(actual.getValue()).isEqualTo(expectedValue);
        });
    }

    @Test
    void testErrorScenario() {
        // Given
        when(dependency.call()).thenReturn(Uni.createFrom().failure(new RuntimeException()));

        // When & Then
        assertThatThrownBy(() -> service.process(request).await().indefinitely())
            .isInstanceOf(RuntimeException.class);
    }
}
```

**Output:** Comprehensive test suite

### 5. Compilation & Test Verification (30-60 min)
**Input:** Implementation and tests

**Activities:**
- During development: Run specific tests as needed: `mvn test -Dtest=TestClass`
- Fix compilation errors (imports, syntax, AssertJ issues)
- Fix test failures (assertions, mocking, logic)
- **MANDATORY (Final Validation)**: Compile whole backend: `mvn clean install`
- Verify coverage meets requirements

**Common Fixes:**
- **AssertJ**: `assertThat(value).isCloseTo(expected, Offset.offset(0.001f))`
- **Imports**: Add missing `org.assertj.core.data.Offset`
- **Lambda**: Ensure variables are effectively final
- **Mocking**: Correct Mockito usage patterns

**Output:** Compiling, tested code

### 6. Documentation & Completion (15-30 min)
**Input:** Verified implementation

**Activities:**
- Update task status in user story files
- Mark acceptance criteria as completed
- Add implementation notes if needed
- Update any API documentation
- Verify all requirements met

**Output:** Complete, documented implementation

## Quality Standards

### Code Quality
- **Reactive**: Use Mutiny Uni/Multi for async operations
- **Error Handling**: Comprehensive try-catch with logging
- **Dependencies**: CDI injection, avoid manual instantiation
- **Performance**: Efficient algorithms, proper resource management

### Testing Quality
- **Coverage**: >80% unit test coverage
- **Scenarios**: Success, failure, edge cases, boundary conditions
- **Assertions**: Clear, descriptive assertions
- **Isolation**: Proper mocking of dependencies

### Documentation
- **JavaDoc**: Complete method documentation
- **Comments**: Clear implementation comments
- **Acceptance**: All criteria marked as completed

## Success Criteria

- [ ] **Code Compiles**: `mvn clean install` succeeds
- [ ] **Tests Pass**: `mvn test` passes all tests
- [ ] **Coverage**: >80% test coverage achieved
- [ ] **Requirements**: All acceptance criteria met
- [ ] **Documentation**: Task marked as completed
- [ ] **Quality**: No critical issues or TODOs

## Git Workflow

### Branch Creation
```bash
git checkout -b feature/task-description
# or
git checkout -b fix/issue-description
# or
git checkout -b refactor/improvement-description
```

### Commit Message
```bash
git commit -m "feat: implement [task description]

- [Key changes made]
- [Technical details]
- [Testing approach]
- [Acceptance criteria met]

Resolves [task-id]: [task description]"
```

### Pull Request
- **Title**: `feat: [task description]`
- **Description**: Include test plan, acceptance criteria, any breaking changes
- **Labels**: `enhancement`, `testing`, appropriate epic labels

## Metrics Tracking

- **Time Spent**: Track against estimates
- **Coverage**: Verify JaCoCo reports
- **Complexity**: Monitor cognitive complexity
- **Performance**: Basic performance validation

## Error Handling

### Compilation Issues
- Check imports and dependencies
- Verify syntax correctness
- Review AssertJ usage patterns

### Test Failures
- Verify mocking setup
- Check assertion logic
- Validate test data and expectations

### Environment Issues
- Handle Maven timeouts gracefully
- Use appropriate timeouts for long-running tasks
- Check for Quarkus/CDI configuration issues

## Reusability

This command can be applied to any task implementation by:
1. Adapting the task analysis to the specific requirements
2. Following the same implementation and testing patterns
3. Using the same verification and documentation steps
4. Maintaining consistent quality standards

The process ensures consistent, high-quality implementations across all MegaBrain development tasks.