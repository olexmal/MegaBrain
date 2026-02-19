# Development Guide

This guide covers project structure, development workflow, coding standards, testing, and contributing.

---

## Project Structure

```
MegaBrain/
├── backend/                    # Java/Quarkus backend
│   ├── pom.xml                # Maven configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/          # Java source code
│   │   │   └── resources/     # Configuration files
│   │   └── test/              # Test code
│   ├── src/it/                # Integration tests
│   ├── src/benchmark/         # JMH benchmarks
│   └── target/                # Build output
├── frontend/                   # Angular 20 frontend
│   ├── package.json           # npm dependencies
│   ├── angular.json           # Angular configuration
│   ├── src/
│   │   ├── app/               # Angular components
│   │   ├── assets/            # Static assets
│   │   └── environments/      # Environment configs
│   └── dist/                  # Build output
├── docs/                       # Documentation
├── features/                   # Feature specifications
│   ├── epics/                 # Epic definitions
│   ├── user-stories/          # User stories and tasks
│   └── feature_specification.md
└── README.md                   # Project overview
```

---

## Backend Development

### Start Quarkus Dev Mode

```bash
cd backend
mvn quarkus:dev
```

- Hot reload enabled -- changes automatically compiled
- Application restarts on code changes
- Dev UI available at `http://localhost:8080/q/dev`

### Run Tests

```bash
mvn test                    # Unit tests
mvn verify                  # Unit + integration tests
mvn test -pl backend        # Backend tests only
```

### Build for Production

```bash
mvn clean package           # Build JAR
mvn clean package -Pnative  # Build native executable (requires GraalVM)
```

### Run Benchmarks

```bash
mvn clean package -Pbenchmark
java -jar target/benchmarks.jar
```

---

## Frontend Development

### Start Development Server

```bash
cd frontend
npm install   # First time only
npm start     # or: ng serve
```

- Hot reload enabled
- API proxy configured to `http://localhost:8080`
- Available at `http://localhost:4200`

### Run Tests

```bash
npm test          # Run Jest tests
npm run lint      # Run ESLint
```

### Build for Production

```bash
npm run build
```

Output goes to `backend/src/main/resources/META-INF/resources` and is served by Quarkus in production.

### Environment Configuration

Edit `frontend/src/environments/environment.ts` (dev) or `environment.prod.ts` (prod) for API base URL and feature flags.

---

## Coding Standards

### Java Backend

**Naming:**
- Classes: `PascalCase` (e.g., `GitHubSourceControlClient`)
- Methods: `camelCase` (e.g., `extractRelationships()`)
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase.with.dots`

**Reactive Programming:**
- Use `Uni<T>` for single async values
- Use `Multi<T>` for streams
- Prefer reactive chains over blocking operations

**Dependency Injection:**
- Use constructor injection: `@Inject public MyService(OtherService other)`
- Annotate services with `@ApplicationScoped` or `@Singleton`
- Use `@ConfigProperty` for configuration

### TypeScript Frontend

**Architecture:**
- Standalone components (no NgModules)
- Use Angular Signals for state management
- Prefer RxJS for async operations

**Naming:**
- Components: `PascalCase` (e.g., `DashboardComponent`)
- Files: `kebab-case.component.ts`
- Services: `PascalCase` with `Service` suffix

**Code Style:**
- Use TypeScript strict mode
- Prefer interfaces over types for public APIs
- Use async/await over promises

---

## Testing

### Coverage Requirements

- **Minimum: >80%** for all new code
- Unit tests for all public methods
- Integration tests for critical paths
- Test both success and failure scenarios

### Backend Testing (JUnit 5 + Mockito)

```java
@QuarkusTest
class SearchServiceTest {
    @InjectMock
    LuceneIndexService indexService;

    @Inject
    SearchService searchService;

    @Test
    void testSearch() {
        when(indexService.search(anyString()))
            .thenReturn(List.of(result));

        Uni<SearchResult> result = searchService.search("query");
        assertThat(result.await().indefinitely()).isNotNull();
    }
}
```

**Conventions:**
- Unit tests: `*Test.java` in `src/test/java/`
- Integration tests: `*IT.java` or `*TestIT.java` in `src/it/java/`
- Use `@QuarkusTest` for Quarkus-specific integration tests
- Mock external dependencies (GitHub API, Ollama, database) with Mockito
- Use Testcontainers for database and service testing

### Frontend Testing (Jest)

```typescript
describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let service: IngestionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: IngestionService, useValue: mockService }
      ]
    });
    component = TestBed.createComponent(DashboardComponent).componentInstance;
  });

  it('should display jobs', () => {
    // Test implementation
  });
});
```

---

## Git Workflow

### Branch Naming

```bash
git checkout -b feature/US-01-08-grammar-management
git checkout -b fix/sonar-frontend-config
git checkout -b refactor/grammar-registry-optimization
git checkout -b ci/add-frontend-sonarqube-analysis
```

- `feature/` -- New features or user stories
- `fix/` -- Bug fixes
- `refactor/` -- Code refactoring
- `ci/` -- CI/CD pipeline changes
- `docs/` -- Documentation updates
- `test/` -- Test-related changes

### Commit Messages

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Test additions/changes
- `chore`: Build/tooling changes

**Example:**
```
feat(ingestion): add GitHub repository ingestion

Implement GitHubSourceControlClient with authentication
support and repository cloning.

Closes #123
```

### Workflow

1. Create feature branch from `main`
2. Make changes and commit with descriptive messages
3. Push branch: `git push origin feature/US-01-08-grammar-management`
4. Create pull request
5. Code review and merge (squash preferred)

---

## Contributing

### Development Setup

1. Fork the repository
2. Clone your fork
3. Create a feature branch
4. Make your changes
5. Write tests (>80% coverage required)
6. Ensure all tests pass
7. Submit a pull request

### Code Review Process

1. All code must be reviewed before merging
2. Minimum 80% test coverage required
3. All CI checks must pass
4. Documentation must be updated for user-facing changes

### Definition of Done

A task is considered done when:
- All acceptance criteria met
- Code reviewed and merged
- Unit tests passing (>80% coverage)
- Integration tests passing (if applicable)
- Documentation updated
- No critical bugs
- Performance requirements met (if specified)
