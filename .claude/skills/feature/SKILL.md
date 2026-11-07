---
name: feature
description: Automate complete feature development from context loading through design to implementation and testing. Use when user says "새 기능 추가", "기능 개발", "/feature", or provides a feature specification.
---

# Feature Development Skill

You are a Backend Developer for the gudogging-BE project.

## Purpose

Automate the complete feature development workflow from context loading through design to implementation and testing.

## Trigger Scenarios

- User requests a new feature
- User says "새 기능 추가", "기능 개발", "기능 구현"
- User invokes `/feature [FeatureName]`
- User provides a feature specification file
- User describes business requirements

## Execution Flow

### Phase 1: Context Loading (Strict Lazy Loading)

**Goal:** Load only what's needed, avoid unnecessary context

**Step 1: Initial Load **

- Read `CLAUDE.md` - Project overview
- Read `core/essential-rules.yaml` - Non-negotiable rules
- Read `memory.md` - Current status and known issues

**Step 2: Identify Target Domain(s)**

Analyze user request to identify:

- **Primary Domain:** The main domain for this feature
- **Secondary Domains:** Related domains (if needed)

**Step 3: Load Required Context (On Demand)**

**MUST READ:**

- `core/system-design.yaml` - Architecture, API, Entity rules
- `references/data-model/domains/{PrimaryDomain}/*.yaml` - Use Glob to find all domain files

**CONDITIONAL READ (only if needed):**

- `references/data-model/domains/{SecondaryDomain}/*.yaml` - ONLY IF cross-domain validation or foreign key reference
  required
- `references/conventions/testing-guide.yaml` - When writing tests
- `references/conventions/coding-style.yaml` - When unsure about style

**FORBIDDEN:**

- Do NOT load unrelated domain YAMLs
- Do NOT read entire codebase

### Phase 2: Feature Design

**Check Input:**

**IF** spec file provided:

1. Read spec file
2. Validate completeness (API contract, business logic, validations)
3. **SKIP** design phase
4. **PROCEED** to implementation

**ELSE** (no spec provided):

1. Use template from `.claude/docs/templates/feature-spec.yaml`
2. Create spec based on user requirements
3. Ask user for approval before implementation

**Constraints (CRITICAL):**

1. **Immutable Spec:**
    - Do NOT modify provided spec file without user permission
    - If spec is unclear/incomplete, **ASK** user first
    - If spec conflicts with existing architecture, **ASK** user for guidance

2. **No Over-engineering:**
    - Implement ONLY what's in the spec
    - Do NOT add "nice-to-have" features
    - Do NOT create extra validations not specified
    - Do NOT add fields/methods not in the spec

3. **Follow System Design:**
    - Adhere to layer architecture (Controller → UseCase → Domain Service → Repository)
    - Follow API naming conventions from `core/system-design.yaml`
    - Use Static Factory Method for DTOs (see `references/conventions/coding-style.yaml`)

### Phase 3: Implementation

**Step 1: Implement Layers (Bottom-Up)**

**Order:** Entity → Repository → Domain Service → UseCase → Controller → DTO

**Entity (if new entity needed):**

- Create entity class in `src/main/java/com/subscribers/gudogging/domain/{domain}/domain/entity/`
- **MUST** apply `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)` — **NEVER** use `@Setter`
- State changes via static factory method `create()` or domain methods only
- **NEVER** declare `id` — UUID id is inherited from `BaseEntity`. Follow column definitions in `docs/ERD.md`

```java
@Entity
@Table(name = "{table_name}")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class {Name} extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String fieldName;

    public static {Name} create(String fieldName) {
        return new {Name}(fieldName);
    }
}
```

**Repository (if new repository needed):**

- Create repository interface in `src/main/java/com/subscribers/gudogging/domain/{domain}/domain/repository/`
- Extend JpaRepository or custom interface

```java
public interface {Name}Repository extends JpaRepository<{Name}, UUID> {
}
```

**Domain Service:**

- Create in `src/main/java/com/subscribers/gudogging/domain/{domain}/domain/service/`
- **MUST** apply `@Service`, `@RequiredArgsConstructor`
- Depends ONLY on Repository — **FORBIDDEN:** Domain Service → Domain Service

```java
@Service
@RequiredArgsConstructor
public class {Name}CreateService {

    private final {Name}Repository {name}Repository;

    public {Name} create{Name}({Name} {name}) {
        return {name}Repository.save({name});
    }
}
```

**UseCase:**

- Create in `src/main/java/com/subscribers/gudogging/domain/{domain}/application/usecase/`
- **Naming: `{Domain}UseCase` — Single Facade per domain (e.g., `PostUseCase`)**
- **MUST** apply `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)` on class
- **MUST** apply `@Transactional` on each write method
- **FORBIDDEN:** Do NOT call other UseCases
- **FORBIDDEN:** Do NOT call Repositories directly (ALWAYS go through Domain Service)

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class {Name}UseCase {

    private final {Name}CreateService {name}CreateService;
    private final {Name}GetService {name}GetService;

    @Transactional
    public {Name}Response create{Name}(Create{Name}Request request) {
        {Name} {name} = {Name}.create(request.field());
        {Name} saved = {name}CreateService.create{Name}({name});
        return {Name}Response.from(saved);
    }

    public List<{Name}Response> getAll{Name}s() {
        return {name}GetService.getAll{Name}s().stream()
                .map({Name}Response::from)
                .toList();
    }
}
```

**Controller:**

- Create in `src/main/java/com/subscribers/gudogging/domain/{domain}/presentation/`
- **MUST** apply `@RestController`, `@RequiredArgsConstructor`, `@Tag`
- **MUST** apply `@Operation` on every handler — `description` MUST contain **기능** / **요청값** / **응답값** (see `references/conventions/coding-style.yaml` → swagger)
- Delegate to UseCase (1:1 mapping) — no business logic

```java
@Tag(name = "{Name}", description = "{도메인} 관련 API")
@RestController
@RequestMapping("/api/v1/{resources}")
@RequiredArgsConstructor
public class {Name}Controller {

    private final {Name}UseCase {name}UseCase;

    @Operation(
        summary = "{기능 요약}",
        description = """
            **기능**
            {무엇을 하는 API인지, 동작 조건·제약}

            **요청값**
            - {field} ({Type}, 필수|선택): {설명}

            **응답값**
            - {field} ({Type}): {설명}
            """
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<{Name}Response> create{Name}(@RequestBody @Valid Create{Name}Request request) {
        {Name}Response response = {name}UseCase.create{Name}(request);
        return CommonResponse.success({Name}ResponseCode.{NAME}_CREATED, response);
    }
}
```

**DTO (Request/Response):**

- Request DTO: `src/main/java/com/subscribers/gudogging/domain/{domain}/application/dto/request/`
- Response DTO: `src/main/java/com/subscribers/gudogging/domain/{domain}/application/dto/response/`
- ErrorCode: `src/main/java/com/subscribers/gudogging/domain/{domain}/application/exception/`
- ResponseCode: `src/main/java/com/subscribers/gudogging/domain/{domain}/presentation/response/`

```java
// Request — record with @Valid annotations
public record Create{Name}Request(
        @NotBlank @Size(max = 100) String field
) {}

// Response — record with static factory method
public record {Name}Response(UUID id, String field, LocalDateTime createdAt) {
    public static {Name}Response from({Name} {name}) {
        return new {Name}Response({name}.getId(), {name}.getField(), {name}.getCreatedAt());
    }
}

// ResponseCode — @Getter + @AllArgsConstructor MUST apply
@Getter
@AllArgsConstructor
public enum {Name}ResponseCode implements ResponseCodeInterface {
    {NAME}_CREATED(201, HttpStatus.CREATED, "..."),
    {NAME}_FOUND(200, HttpStatus.OK, "...");

    private final int code;
    private final HttpStatus status;
    private final String message;
}

// ErrorCode — @Getter + @AllArgsConstructor MUST apply
@Getter
@AllArgsConstructor
public enum {Name}ErrorCode implements ErrorCodeInterface {
    {NAME}_NOT_FOUND(40{XX}1, HttpStatus.NOT_FOUND, "...");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
```

**Step 3: Write Tests**

**Test Scope:**

- **ONLY** write `{Domain}UseCaseTest` — NO Domain Service / Entity / Repository / Controller / DTO tests
- Mock Domain Services the UseCase depends on (`@Mock`), inject the UseCase (`@InjectMocks`)
- Entity logic is verified through UseCase test scenarios (do not mock Entities)
- Location: `src/test/java/com/subscribers/gudogging/domain/{domain}/application/usecase/`

**Test Structure:**

- Use JUnit 5 with @ExtendWith(MockitoExtension.class) and @Nested
- Use Korean @DisplayName (e.g., "존재하지 않는 게시글을 수정하면 예외를 던진다")
- Follow AAA pattern (given / when / then)

**Test Coverage:**

- Happy path (successful case)
- Edge cases (boundary values)
- Error cases (validation failures, business rule violations)
- See `references/conventions/testing-guide.yaml` for details

**Run Tests:**

```bash
# Run specific tests
./gradlew test --tests "*{Domain}UseCaseTest"

# Full test suite
./gradlew test
```

**Step 2: Verification**

1. Compile: `./gradlew compileJava`
2. Run tests: `./gradlew test --tests "*{Domain}UseCaseTest"`
3. Build: `./gradlew build`
4. Check code style (manual review against `coding-style.yaml`)

## Validation Checklist

Before completing feature:

- [ ] All layers implemented (Controller, UseCase, Domain Service if needed, Repository if needed)
- [ ] DTOs use Static Factory Method pattern
- [ ] No layer skipping (Controller must call UseCase, not Repository)
- [ ] No horizontal dependencies (Domain Service → Domain Service)
- [ ] UseCase tests written (happy path + edge cases + error cases) — no Service/Entity/other tests
- [ ] All tests pass: `./gradlew test`
- [ ] Build successful: `./gradlew build`
- [ ] Code follows `essential-rules.yaml`
- [ ] Korean test names used

## Report Format

```
✅ Feature Implemented: [FeatureName]

📋 Specification:
  - Primary Domain: [Domain]
  - API Endpoint: [Method] [Path]
  - Business Logic: [Brief description]

📁 Files Created/Modified:
  - src/.../domain/{domain}/presentation/{Name}Controller.java
  - src/.../domain/{domain}/application/{Name}UseCase.java
  - src/.../domain/{domain}/domain/{Name}Service.java (if needed)
  - src/.../domain/{domain}/application/dto/{Name}Request.java, {Name}Response.java
  - src/test/.../domain/{domain}/application/usecase/{Name}UseCaseTest.java

🧪 Test Results:
  - Test cases: [Number] (happy path + edge cases + error cases)
  - Status: ✅ PASSED ([X]/[X])

🏗️ Build Status: ✅ SUCCESS

📝 Notes:
  - [Any important decisions or trade-offs made]
```

## Example Invocation

> 예시 API(`domain/example`, Post) 기준의 가상 시나리오입니다. 실제 코드 상태와 다를 수 있으며, 출력 형식과 흐름을 참고하는 용도입니다.

```
User: "/feature .claude/features/example/004_update_post.yaml 스펙대로 구현해줘"

AI:
🚀 Starting Feature Development: 게시글 수정

Phase 1: Context Loading
- Reading CLAUDE.md, essential-rules.yaml, memory.md...
- Identified Primary Domain: Post (domain/example)
- Loading core/system-design.yaml...
- Loading references/data-model/domains/example/post.yaml...
- Loading references/conventions/coding-style.yaml...

Phase 2: Feature Design
- Spec file provided: .claude/features/example/004_update_post.yaml
- Validating completeness (API contract, business logic, validations)... ✅
- Skipping design phase

Phase 3: Implementation
- Adding domain method: Post.update(title, content)
- Adding PostUseCase.updatePost(postId, request) (@Transactional, PostGetService.getPost 재사용)
- Adding PATCH /api/v1/posts/{postId} to PostController.java
- Creating UpdatePostRequest.java, adding PostResponseCode.POST_UPDATED
- Writing tests: PostUseCaseTest.java (UpdatePost @Nested)...
  - "유효한 수정 요청이면 수정된 게시글 정보를 반환한다"
  - "존재하지 않는 게시글을 수정하면 예외를 던진다"
  - "삭제된 게시글을 수정하면 예외를 던진다"
- Running tests... ✅ PASSED (3/3)
- Running build... ✅ SUCCESS

✅ Feature Implemented: 게시글 수정

📋 Specification:
  - Primary Domain: Post (domain/example)
  - API Endpoint: PATCH /api/v1/posts/{postId}
  - Business Logic: 게시글 조회(삭제 제외) → post.update() → PostResponse 반환

📁 Files Created/Modified:
  - src/.../domain/example/domain/entity/Post.java (update 메서드 추가)
  - src/.../domain/example/application/usecase/PostUseCase.java (updatePost 추가)
  - src/.../domain/example/application/dto/request/UpdatePostRequest.java (신규)
  - src/.../domain/example/presentation/PostController.java (PATCH 엔드포인트 추가)
  - src/.../domain/example/presentation/response/PostResponseCode.java (POST_UPDATED 추가)
  - src/test/.../domain/example/application/usecase/PostUseCaseTest.java

🧪 Test Results:
  - Test cases: 3 (1 happy + 2 error cases)
  - Status: ✅ PASSED (3/3)

🏗️ Build Status: ✅ SUCCESS

📝 Notes:
  - Static Factory Method used for PostResponse.from(post)
  - Transaction boundary set in UseCase (@Transactional), 변경 감지(dirty checking)로 저장
```

## Troubleshooting

**Problem:** Spec file is incomplete or unclear
**Cause:** User provided minimal requirements
**Solution:**

1. List missing information (API contract, validation rules, error cases)
2. Ask user to clarify or provide defaults based on `system-design.yaml`
3. Document assumptions in spec

**Problem:** Cross-domain dependency needed
**Cause:** Feature requires data from multiple domains
**Solution:**

1. Check if relationship already exists in entity YAML
2. If yes, use it
3. If no, ask user if new relationship should be created
4. Prefer UseCase orchestration over Domain Service coupling

**Problem:** Tests fail after implementation
**Cause:** Logic error or missing setup
**Solution:**

1. Read test failure message: `./gradlew test 2>&1 | grep -A 10 FAILED`
2. Check test data setup (are entities properly initialized?)
3. Verify mock behavior (if using mocks)
4. Run single test for debugging: `./gradlew test --tests "*{TestClass}*{TestMethod}*"`

**Problem:** Build fails with compilation error
**Cause:** Syntax error, missing imports, or type mismatch
**Solution:**

1. Read error message: `./gradlew compileJava`
2. Fix syntax errors
3. Add missing imports
4. Check type compatibility (especially with DTOs and entities)

## Forbidden Actions

- Modify immutable spec without user permission
- Skip test writing
- Violate layer architecture (Controller → Repository directly)
- Create horizontal dependencies (Domain Service → Domain Service)
- Add features not in spec ("nice-to-have" additions)
- Use English test names (must use Korean)
- Complete with build/test failures

## Success Criteria

- [ ] All layers implemented correctly (following architecture)
- [ ] No layer violations (Controller → UseCase → Domain Service/Repository)
- [ ] Tests written and passing (happy path + edge cases + error cases)
- [ ] Build successful
- [ ] DTOs use Static Factory Method pattern
- [ ] Code follows `essential-rules.yaml` and `coding-style.yaml`
- [ ] Feature matches spec exactly (no over-engineering)

## Best Practices

1. **Lazy Load Context**
    - Load only domains involved in the feature
    - Don't read entire codebase upfront

2. **Follow Architecture**
    - Respect layer boundaries
    - No shortcuts (Controller → Repository)
    - Keep Domain Services pure (no horizontal dependencies)

3. **Test First (or Alongside)**
    - Write tests as you implement
    - Use quick feedback loop: `./gradlew test --tests "*{Feature}*"`
    - Cover happy path and error cases

4. **Respect the Spec**
    - Implement exactly what's specified
    - Don't add extra features
    - Ask if spec is unclear

5. **Follow Existing Patterns**
    - Use Grep to find similar implementations
    - Follow existing code patterns
    - Maintain consistency with codebase
