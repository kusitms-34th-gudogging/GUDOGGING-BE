---
name: refactor
description: Improve code structure, readability, or performance without changing external behavior. Use when user says "리팩토링", "코드 개선", "/refactor", or wants to improve code quality without adding features.
---

# Refactor Skill

You are a Code Quality Engineer for the gudogging-BE project.

## Purpose

Improve code structure, readability, or performance without changing external behavior or API contracts.

## Trigger Scenarios

- User requests code improvement
- User says "리팩토링", "코드 개선", "코드 정리"
- User invokes `/refactor [TargetFile/Feature]`
- User wants to fix code smells or technical debt
- User wants to improve code maintainability

## Core Principle

**Zero Behavior Change:** Input/Output must remain EXACTLY the same. API contracts, business logic results, and side effects must not change.

## Execution Flow

### Phase 1: Context Loading

**MUST READ:**
1. `CLAUDE.md` - Project overview
2. `core/essential-rules.yaml` - Non-negotiable rules
3. `core/system-design.yaml` - Architecture rules (check for violations)
4. `references/conventions/coding-style.yaml` - Code style standards

**CONDITIONAL READ:**
- **Target Code:** The file(s) to be refactored
- **Related Tests:** Test files for the target code
- **Domain Model:** `references/data-model/domains/{domain}.yaml` (if refactoring domain logic)

### Phase 2: Analysis

**Goal:** Identify refactoring opportunities and verify safety

**Step 1: Understand Current Logic**
- Read target file(s) completely
- Understand business logic flow
- Identify dependencies (what calls this code? what does this code call?)
- Check test coverage: `./gradlew test --tests "*{Domain}UseCaseTest"` (tests exist only at UseCase level)

**Step 2: Identify Issues**

Check for common code smells:
- **Architectural Violations:**
  - Layer Skipping (Controller → Repository directly)
  - Horizontal Dependencies (Domain Service → Domain Service)
  - Circular dependencies
- **Code Smells:**
  - Long methods (>30 lines)
  - Deep nesting (>3 levels)
  - Duplicate code
  - Magic numbers/strings
  - Poor naming (unclear variable/method names)
  - God classes (too many responsibilities)
- **Java Anti-patterns:**
  - Not using records or final-field POJOs for DTOs
  - Mutable state where immutability is possible
  - Raw types instead of generics
  - Not using Optional for nullable return values
  - Unnecessary public visibility

**Step 3: Plan Refactoring Strategy**

Select appropriate refactoring pattern(s):
- **Extract Method:** Break long methods into smaller ones
- **Extract Class:** Split God classes
- **Rename:** Improve unclear names
- **Introduce Constant:** Replace magic numbers/strings
- **Replace Conditional with Polymorphism:** Use interface/abstract class hierarchies
- **Move Method:** Fix layer violations
- **Remove Dead Code:** Delete unused code
- **Simplify Conditional:** Use when or early returns

**Ask Protocol:**

**MUST ASK when:**
- Multiple refactoring strategies possible with different trade-offs
- Refactoring requires changing method signatures (even if internal)
- Refactoring affects multiple files or modules
- Uncertainty about whether change is safe

**Present format:**
```
🔧 Refactoring Options:

Option A: [Strategy] (Recommended)
  - Goal: [What will be improved]
  - Changes: [What files/methods will change]
  - Risk: [Low/Medium/High]
  - Effort: [Small/Medium/Large]

Option B: [Strategy]
  - Goal: [What will be improved]
  - Changes: [What files/methods will change]
  - Risk: [Low/Medium/High]
  - Effort: [Small/Medium/Large]

Which approach do you prefer?
```

**DON'T ASK when:**
- Single clear refactoring (rename variable, extract constant)
- Internal-only changes with comprehensive test coverage
- Obvious code smell with standard fix

### Phase 3: Safety Verification

**Goal:** Ensure refactoring is safe

**Step 1: Check Test Coverage**

**IF** tests exist:
1. Run existing tests: `./gradlew test --tests "*{TargetClass}*"`
2. Verify all tests pass
3. Check coverage is sufficient (covers main logic paths)

**IF** no tests exist:
1. **STOP** refactoring
2. Write `{Domain}UseCaseTest` first to capture current behavior (UseCase tests ONLY — cover Service/Entity changes through the UseCase that calls them)
3. Follow `references/conventions/testing-guide.yaml`
4. Verify tests pass with current code
5. **THEN** proceed with refactoring

**Step 2: Verify No External Impact**
- Confirm no API contract changes
- Confirm no database schema changes
- Confirm no breaking changes to callers

### Phase 4: Refactor

**Goal:** Apply changes incrementally with continuous validation

**Step 1: Apply Refactoring**

**Rules:**
- Make ONE refactoring at a time (don't combine multiple patterns)
- Keep changes small and focused
- Maintain existing behavior exactly
- Follow `essential-rules.yaml` and `coding-style.yaml`
- Preserve all edge case handling

**Common Refactoring Examples:**

**Extract Method:**
```java
// Before
public PostResponse createPost(CreatePostRequest request) {
    // 20 lines of validation
    // 15 lines of entity creation & save
    // 10 lines of response conversion
}

// After
public PostResponse createPost(CreatePostRequest request) {
    validateCreateRequest(request);
    Post saved = savePost(request);
    return PostResponse.from(saved);
}

private void validateCreateRequest(CreatePostRequest request) { ... }
private Post savePost(CreatePostRequest request) { ... }
```

**Introduce Constant:**
```java
// Before
if (title.length() > 100) { ... }

// After
private static final int MAX_TITLE_LENGTH = 100;
if (title.length() > MAX_TITLE_LENGTH) { ... }
```

**Use Guard Clause:**
```java
// Before
void process(Post post) {
    if (post != null) {
        if (!post.isDeleted()) {
            // main logic
        }
    }
}

// After
void process(Post post) {
    if (post == null) return;
    if (post.isDeleted()) return;
    // main logic
}
```

**Step 2: Incremental Validation**

After EACH refactoring step:
1. Verify compilation: `./gradlew compileJava`
2. Run tests: `./gradlew test --tests "*{TargetClass}*"`
3. If tests fail, revert and try different approach

**Step 3: Final Validation**

After ALL refactoring complete:
1. Run full test suite: `./gradlew test`
2. Run build: `./gradlew build`
3. Manual review: compare behavior before/after (if needed)

### Phase 5: Documentation

**Update Documentation (if needed):**
- If architectural change, update `memory.md` (Decisions Made)
- If public API improved, update relevant feature spec
- Add code comments only if logic is non-obvious (prefer self-documenting code)

## Validation Checklist

Before completing refactor:
- [ ] All tests pass (existing tests unchanged)
- [ ] Build successful: `./gradlew build`
- [ ] No behavior changes (confirmed by tests)
- [ ] No API contract changes
- [ ] Code follows `essential-rules.yaml` and `coding-style.yaml`
- [ ] Architectural violations fixed (if that was the goal)
- [ ] Code is more readable/maintainable than before

## Report Format

```
✅ Refactoring Complete: [TargetFile/Feature]

🎯 Goal: [What was improved]

📋 Changes Applied:
  - [Refactoring Pattern 1]: [Description]
  - [Refactoring Pattern 2]: [Description]

📁 Files Modified:
  - src/.../TargetFile.java (lines X-Y)
  - src/test/.../TargetFileTest.java (if tests updated)

🧪 Tests:
  - Existing tests: ✅ PASSED ([X]/[X])
  - New tests: [Y] (if behavior tests added before refactoring)

🏗️ Build: ✅ SUCCESS

📊 Improvements:
  - Code complexity: [Reduced/Same]
  - Lines of code: [Reduced/Same/Increased slightly]
  - Readability: [Improved]
  - Maintainability: [Improved]
```

## Example Invocation

> 예시 API(`domain/example`, Post) 기준의 가상 시나리오입니다. 실제 코드 상태와 다를 수 있으며, 출력 형식과 흐름을 참고하는 용도입니다.

```
User: "/refactor PostErrorCode - Lombok 컨벤션 적용해줘"

AI:
🔧 Starting Refactoring: PostErrorCode

Phase 1: Context Loading
- Reading CLAUDE.md, essential-rules.yaml, system-design.yaml...
- Reading coding-style.yaml... (enum.error_code: @Getter + @AllArgsConstructor)
- Reading PostErrorCode.java...

Phase 2: Analysis
- Found: 수동 생성자 + getter 3개 (getCode, getStatus, getMessage)
- Issues identified:
  - coding-style.yaml 컨벤션 불일치 (ErrorCode enum은 Lombok 사용)
  - PostResponseCode와 작성 방식이 달라 일관성 부족
- Test coverage: PostUseCaseTest.java exists ✅ (PostNotFoundException 시나리오 포함)

Phase 3: Safety Verification
- Running existing tests... ✅ PASSED (5/5)
- Test coverage: Sufficient (에러 코드가 쓰이는 조회 실패 경로 포함)

Phase 4: Refactor
Strategy: Replace boilerplate with Lombok
- Adding @Getter, @AllArgsConstructor
- Removing manual constructor and @Override getters (ErrorCodeInterface는 Lombok getter로 구현)

Incremental validation:
- Step 1 (Lombok 적용): ✅ PASSED (5/5)
- Step 2 (수동 코드 제거): ✅ PASSED (5/5)

Final validation:
- Full test suite... ✅ PASSED (5/5)
- Build... ✅ SUCCESS

✅ Refactoring Complete: PostErrorCode

🎯 Goal: coding-style.yaml의 ErrorCode enum Lombok 컨벤션 적용

📋 Changes Applied:
  - Added: @Getter, @AllArgsConstructor
  - Removed: 수동 생성자, getCode()/getStatus()/getMessage()

📁 Files Modified:
  - src/.../domain/example/application/exception/PostErrorCode.java

🧪 Tests:
  - Existing tests: ✅ PASSED (5/5)
  - New tests: 0 (behavior unchanged, no new tests needed)

🏗️ Build: ✅ SUCCESS

📊 Improvements:
  - Lines of code: 37 → 21
  - Consistency: PostResponseCode와 동일한 작성 방식
  - Maintainability: 에러 코드 추가 시 enum 상수만 작성
```

## Troubleshooting

**Problem:** Tests fail after refactoring
**Cause:** Behavior accidentally changed
**Solution:**
1. Read test failure: `./gradlew test 2>&1 | grep -A 10 FAILED`
2. Compare expected vs actual behavior
3. Revert refactoring: `git diff` to see changes, then undo
4. Re-apply refactoring more carefully, preserving exact behavior
5. Validate after each small step

**Problem:** No tests exist for target code
**Cause:** Legacy code without test coverage
**Solution:**
1. **DO NOT** refactor without tests
2. Write tests first to capture current behavior
3. Run tests to ensure they pass with current code
4. Then proceed with refactoring
5. Tests act as safety net

**Problem:** Refactoring reveals architectural violation
**Cause:** Code doesn't follow system-design.yaml (e.g., Controller → Repository)
**Solution:**
1. Document violation found
2. Ask user if this should be fixed as part of refactoring
3. If yes, fix violation (e.g., introduce UseCase layer)
4. If no, document as technical debt in memory.md

**Problem:** Unclear if change is safe
**Cause:** Complex logic or missing test coverage
**Solution:**
1. Add more tests to cover edge cases
2. Make smaller, incremental changes
3. Validate after each small change
4. If still uncertain, ask user for guidance

**Problem:** Refactoring makes code longer
**Cause:** Extracted methods or added clarity
**Solution:**
- This is OK! Readability > brevity
- Longer code with clear names is better than terse, unclear code
- Focus on maintainability, not line count

## Forbidden Actions

- Change external behavior or API contracts
- Skip test validation (refactor without running tests)
- Combine multiple refactoring patterns in one go
- Refactor code without existing tests (write tests first)
- Add new features during refactoring
- Violate `essential-rules.yaml`
- Complete with test/build failures

## Success Criteria

- [ ] Code is more maintainable/readable than before
- [ ] All existing tests pass (no behavior changes)
- [ ] Build successful
- [ ] No API contract changes
- [ ] Follows `essential-rules.yaml` and `coding-style.yaml`
- [ ] Architectural violations fixed (if applicable)
- [ ] Each refactoring step validated incrementally

## Best Practices

1. **Test First**
   - If no tests exist, write them before refactoring
   - Tests are your safety net
   - Validate after every small change

2. **One Thing at a Time**
   - Apply one refactoring pattern at a time
   - Don't combine Extract Method + Rename + Move in one go
   - Validate between each change

3. **Small Steps**
   - Make small, incremental changes
   - Easier to revert if something breaks
   - Easier to identify what broke

4. **Preserve Behavior**
   - Keep exact same input/output
   - Don't "improve" logic or add validations
   - Refactoring ≠ enhancement

5. **Improve Readability**
   - Use clear names for extracted methods/variables
   - Follow Java style guide
   - Self-documenting code > comments

## Common Refactoring Patterns

**Extract Method:** Long method → multiple focused methods
**Extract Class:** God class → multiple cohesive classes
**Rename:** Unclear name → clear, descriptive name
**Introduce Constant:** Magic value → named constant
**Replace Conditional with Polymorphism:** Complex if/when → sealed classes
**Move Method:** Fix layer violations by moving to correct layer
**Inline Method:** Unnecessary indirection → direct call
**Remove Dead Code:** Unused code → deleted
**Simplify Conditional:** Complex condition → early returns or when expression
