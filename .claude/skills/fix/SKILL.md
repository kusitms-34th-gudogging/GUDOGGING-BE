---
name: fix
description: Automate bug fix workflow from root cause analysis to fix and regression testing. Use when user says "버그 수정", "에러 해결", "/fix", reports a bug, or describes unexpected behavior.
---

# Bug Fix Skill

You are a Debugger + Fixer for the gudogging-BE project.

## Purpose

Automate the bug fix workflow from context loading through root cause analysis to fix implementation and regression testing.

## Trigger Scenarios

- User reports a bug or unexpected behavior
- User says "버그 수정", "에러 해결", "오류 고쳐줘"
- User invokes `/fix [BugDescription]`
- User provides error messages or stack traces
- User describes functionality that doesn't work as expected

## Execution Flow

### Phase 1: Context Loading

**MUST READ (in order):**
1. `memory.md` - Check Known Issues (prevent duplicate work)
2. `CLAUDE.md` - Project overview
3. `core/essential-rules.yaml` - Core rules and tech stack
4. `references/conventions/testing-guide.yaml` - Testing conventions

**CONDITIONAL READ:**
- **Domain Entities:** `references/data-model/domains/{domain}.yaml` (if bug is domain-related)
- **Enums:** `references/data-model/enums.yaml` (if bug involves status/state transitions)
- **Code Style:** `references/conventions/coding-style.yaml` (to maintain consistency in fix)
- **Related Tests:** Existing test files for the buggy component

### Phase 2: Root Cause Analysis

**Goal:** Identify the root cause, not just symptoms

**Step 1: Symptom Analysis**
- Extract error message/stack trace
- Identify reproduction steps
- Determine impact scope (which data/users affected?)
- Check if issue is in Known Issues (memory.md)

**Step 2: Code Investigation**
- Follow call chain: Controller → UseCase → Domain Service → Repository
- Read related entity definitions (use `references/data-model/`)
- Check logs (if available)
- Look for similar code patterns that might have the same bug

**Step 3: Hypothesis Formation**
- List possible root causes:
  - Transaction boundary issues
  - Missing validation
  - State transition errors
  - Null handling
  - Type mismatch
  - Concurrency issues
  - Incorrect business logic
- Assess impact scope
- Identify reproduction method

**Step 4: Hypothesis Validation**
- Write a failing UseCase test that reproduces the bug
- Run the test to confirm: `./gradlew test --tests "*{TestClass}*"`
- Verify 100% reproducibility

**Output Format:**
```
🐛 Bug: [Brief description]
📍 Location: [FilePath:LineNumber]
🔍 Root Cause: [Specific cause, not symptoms]
💡 Hypothesis: [Why this bug occurred]
🎯 Fix Strategy: [Proposed solution]
⚠️ Impact: [Affected features/data/APIs]
🧪 Reproduction: [How to reproduce - test case or manual steps]
```

**Ask Protocol (Strategy Decision):**

**MUST ASK when:**
- Multiple fix strategies exist with trade-offs:
  - "Quick null check" vs "Full validation refactor"
  - "Temporary hotfix" vs "Full fix with data migration"
  - "DB constraint" vs "Application-level validation only"
- API contract or behavior change needed
- Data cleanup/migration required
- Breaking change unavoidable

**Present format:**
```
🔧 Fix Strategy Options:

Option A: [Strategy Name] (Recommended)
  - Pros: [Benefits]
  - Cons: [Drawbacks]
  - Impact: [Scope of change]

Option B: [Strategy Name]
  - Pros: [Benefits]
  - Cons: [Drawbacks]
  - Impact: [Scope of change]

Which strategy do you prefer?
```

**DON'T ASK when:**
- Single clear fix (typo, missing null check, etc.)
- Internal logic only (no API/behavior change)
- Follows `essential-rules.yaml` standards

### Phase 3: Fix & Test

**Goal:** Safe fix with regression prevention

**Step 1: Minimal Change Principle**

**Rules:**
- Modify ONLY code directly related to the bug
- DO NOT touch unrelated code in the same file (comments, whitespace, imports)
- DO NOT create new helper functions/classes (reuse existing)
- DO NOT add extra validations/features (bug fix only)
- Separate refactoring into different task

**Allowed changes:**
- The exact line(s) where the bug occurs
- Directly related code essential for the fix
- New test case (reproduction + verification)

**Validation:**
- For each change, ask: "Without this change, would the bug remain?"
- If "No", remove the change

**Step 2: Fix Implementation**
1. Apply the fix:
   - Fix root cause, not symptoms
   - Follow `essential-rules.yaml`
   - Maintain existing code style
2. Verify compilation: `./gradlew compileJava`
3. Run affected tests: `./gradlew test --tests "*{AffectedClass}*"`

**Step 3: Regression Test**
1. Add new test case to `{Domain}UseCaseTest` (UseCase tests ONLY — even if the bug is in a Domain Service or Entity, reproduce it through the UseCase):
   - Use JUnit 5 with @Test and @DisplayName
   - Use Korean @DisplayName (e.g., "게시글을 생성하면 입력한 작성자명이 반환된다")
   - Follow `references/conventions/testing-guide.yaml`
2. Verify test fails before fix (confirms bug reproduction)
3. Verify test passes after fix
4. Run all tests: `./gradlew test`

**Step 4: Style Consistency**
- Match existing code style (indentation, spacing, naming)
- Follow Java style guide
- Maintain import order

**Step 5: Verification**
```bash
# Full verification
./gradlew clean build test
```

**Step 6: Update Documentation**
- Update `memory.md`:
  - Remove from Known Issues (if listed)
  - Add to Recent Changes
  - Record fix strategy in Decisions Made (if significant)

## Validation Checklist

Before completing fix:
- [ ] Root cause identified and documented
- [ ] Minimal fix applied (no extra changes)
- [ ] New test added (reproduces bug + verifies fix)
- [ ] Test failed before fix, passes after fix
- [ ] All existing tests still pass
- [ ] Build successful: `./gradlew build`
- [ ] Code style matches existing code
- [ ] `memory.md` updated

## Report Format

```
✅ Bug Fixed: [Brief description]

🐛 Bug: [What was wrong]
📍 Location: [FilePath:LineNumber]
🔍 Root Cause: [Why it happened]
🔧 Fix Applied: [What was changed]

📝 Files Modified:
  - src/.../UseCase.java (lines 45-47)
  - src/test/.../application/usecase/{Domain}UseCaseTest.java (added test case)

🧪 Tests:
  - Added: "[Korean test scenario]"
  - All tests passed: ✅

📚 Documentation:
  - Updated: memory.md (removed from Known Issues)

✅ Build: SUCCESS
```

## Example Invocation

> 예시 API(`domain/example`, Post) 기준의 가상 시나리오입니다. 실제 코드 상태와 다를 수 있으며, 출력 형식과 흐름을 참고하는 용도입니다.

```
User: "/fix 게시글 생성 응답의 authorName에 본문 내용이 들어가는 버그 수정"

AI:
🔍 Starting Bug Fix: 게시글 응답 작성자명 매핑 오류

Phase 1: Context Loading
- Reading memory.md... (checking Known Issues)
- Reading CLAUDE.md, essential-rules.yaml...
- Reading testing-guide.yaml...
- Loading references/data-model/domains/example/post.yaml for entity structure...

Phase 2: Root Cause Analysis
- Analyzing PostController.java → PostUseCase.java → PostCreateService.java → PostResponse.java

🐛 Bug: POST /api/v1/posts 응답의 authorName에 content 값이 들어감
📍 Location: PostResponse.java:19
🔍 Root Cause: PostResponse.from()에서 authorName 자리에 post.getContent()를 매핑
💡 Hypothesis: DTO 변환 시 필드 순서 실수 (저장된 데이터는 정상, 응답 변환만 오류)
🎯 Fix Strategy: PostResponse.from()의 매핑을 post.getAuthorName()으로 수정
⚠️ Impact: PostResponse를 쓰는 모든 게시글 API 응답 (생성·목록·단건 조회)
🧪 Reproduction: PostUseCaseTest에 "게시글을 생성하면 입력한 작성자명이 반환된다" 추가

Phase 3: Fix & Test
- Adding test case to PostUseCaseTest (CreatePost @Nested)... ❌ FAILED (bug reproduced)
- Fixing mapping in PostResponse.java:19...
- Running tests... ✅ PASSED (5 existing + 1 new)
- Running build... ✅ SUCCESS
- Updating memory.md...

✅ Bug Fixed: 게시글 응답 작성자명 매핑 오류

🐛 Bug: 게시글 응답의 authorName에 content 값이 들어감
📍 Location: PostResponse.java:19
🔍 Root Cause: PostResponse.from()의 필드 매핑 오류
🔧 Fix Applied: post.getContent() → post.getAuthorName()

📝 Files Modified:
  - src/main/java/.../domain/example/application/dto/response/PostResponse.java (line 19)
  - src/test/java/.../domain/example/application/usecase/PostUseCaseTest.java (added test)

🧪 Tests:
  - Added: "게시글을 생성하면 입력한 작성자명이 반환된다"
  - All tests passed: ✅ (6/6)

📚 Documentation:
  - Updated: memory.md (removed from Known Issues)

✅ Build: SUCCESS
```

## Troubleshooting

**Problem:** Cannot reproduce the bug
**Cause:** Missing context or environment-specific issue
**Solution:**
1. Ask user for exact reproduction steps
2. Check environment variables (application.yml)
3. Review recent changes in git history: `git log --oneline -20`
4. Check if bug is in specific environment only

**Problem:** Test passes but bug still occurs
**Cause:** Test doesn't accurately reproduce the bug
**Solution:**
1. Review reproduction steps with user
2. Check if bug is related to state/timing
3. Add more realistic test data
4. Test manually with actual API call

**Problem:** Fix breaks existing tests
**Cause:** Unexpected side effects or missing edge cases
**Solution:**
1. Analyze which tests failed: `./gradlew test 2>&1 | grep FAILED`
2. Understand why tests failed (behavior changed?)
3. If behavior change is correct, update tests
4. If behavior change is wrong, revise fix strategy

**Problem:** Multiple similar bugs in codebase
**Cause:** Same pattern repeated across files
**Solution:**
1. Use Grep to find similar patterns: `references/conventions/coding-style.yaml`
2. Fix all instances in one PR
3. Document the pattern in memory.md to prevent future occurrences

## Forbidden Actions

- Skip root cause analysis (don't just fix symptoms)
- Include unrelated refactoring
- Fix without adding test
- Violate `essential-rules.yaml`
- Use English test names (must use Korean)
- Complete with build/test failures

## Success Criteria

- [ ] Root cause identified and documented
- [ ] Minimal fix applied (only the bug, no extra changes)
- [ ] New test added (reproduces bug + verifies fix)
- [ ] All existing tests still pass
- [ ] Build successful
- [ ] `memory.md` updated (Known Issues cleared if applicable)
- [ ] No side effects on other features

## Best Practices

1. **Understand Before Fixing**
   - Don't guess - analyze thoroughly
   - Reproduce the bug first with a test
   - Identify root cause, not just symptoms

2. **Test-Driven Fix**
   - Write failing test that reproduces bug
   - Apply fix
   - Verify test passes
   - Verify no regressions

3. **Minimal Change**
   - Change only what's necessary
   - Avoid "while I'm here" improvements
   - Keep refactoring separate

4. **Document the Fix**
   - Update `memory.md`
   - Add code comments if logic is non-obvious
   - Explain "why" in commit message (when creating commit)

5. **Check Side Effects**
   - Look for similar code patterns
   - Test related features manually if needed
   - Consider data consistency implications
