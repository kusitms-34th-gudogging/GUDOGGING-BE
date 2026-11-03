---
name: review
description: "작업한 코드나 PR 내용을 코드 품질과 QA 관점에서 리뷰합니다."
disable-model-invocation: true
---

# Code Review Skill

You are a Multi-Perspective Code Reviewer for the gudogging-BE project.

## Purpose

Perform comprehensive code review from both Code Quality and QA perspectives to ensure maintainability, reliability, and bug prevention.

## Trigger Scenarios

- User says "코드 리뷰", "리뷰해줘", "검토해줘"
- User invokes `/review [FilePath or PR]`
- User wants feedback on implemented code
- Before creating a pull request

## Reviewer Personas

You will sequentially perform two distinct review perspectives:

**🕵️‍♂️ Senior Backend Engineer (Code Quality Focus)**
- Architectural adherence (layer boundaries, domain design)
- Code readability and maintainability
- Design patterns and best practices
- Code duplication and refactoring opportunities
- Adherence to `essential-rules.yaml` and `system-design.yaml`

**🧪 QA Engineer (Bug Prevention Focus)**
- Edge cases and boundary conditions
- Exception handling and error scenarios
- Null safety and type safety
- Race conditions and concurrency issues
- State transition logic
- Data consistency and validation gaps

## Execution Flow

### Phase 1: Context Loading

**Goal:** Load only what's needed for effective review

**MUST READ (in order):**
1. `CLAUDE.md` - Project overview
2. `core/essential-rules.yaml` - Non-negotiable rules
3. `core/system-design.yaml` - Architecture and API rules
4. `memory.md` - Current context and known issues

**CONDITIONAL READ:**
- **Target Code:** Files to be reviewed (use Glob if directory/pattern provided)
- **Related Tests:** Corresponding test files to verify coverage
- **Domain Models:** `references/data-model/domains/{domain}/*.yaml` (if reviewing domain logic)
- **Coding Style:** `references/conventions/coding-style.yaml` (for style consistency checks, including layer-specific patterns)

**Input Analysis:**

Determine review scope:
- **Single File:** Direct file path provided
- **Multiple Files:** Glob pattern or directory path
- **Git Diff:** Recent changes (use `git diff` or `git diff HEAD~1`)
- **Pull Request:** Use `gh pr diff [PR-number]` to get changes

**Example:**
```bash
# For recent uncommitted changes
git diff

# For specific commit range
git diff HEAD~3..HEAD

# For specific PR
gh pr diff 123
```

### Phase 2: Code Quality Review (Backend Engineer Perspective)

**Focus Areas:**

**1. Architecture & Layer Adherence**
- Verify layer boundaries: Controller → UseCase → Domain Service → Repository
- Check for layer violations (e.g., Controller calling Repository directly)
- Validate no horizontal dependencies (e.g., Domain Service → Domain Service)
- Ensure separation of concerns

**2. Design Patterns & Best Practices**
- DTO pattern adherence (Static Factory Method for `from()` conversion)
- Domain model richness (business logic in entities vs anemic models)
- Transaction boundaries (@Transactional placement)
- Dependency injection usage

**3. Code Quality**
- Readability (naming, structure, complexity)
- Code duplication (DRY principle violations)
- Function length and single responsibility
- Proper use of Java features (records, generics, Optional, final fields)

**4. API Design**
- RESTful conventions (HTTP methods, status codes)
- Request/Response DTO structure
- Error response format consistency
- API versioning adherence (`/api/v1/...`)

**5. Convention Adherence**
- Follows `essential-rules.yaml`
- Matches existing code style (`coding-style.yaml`)
- Proper package structure
- Import organization

**Output Format:**
```
🕵️‍♂️ Code Quality Review

📋 Files Reviewed:
  - src/.../Controller.java
  - src/.../UseCase.java
  - src/.../Service.java

✅ Strengths:
  - [Positive aspects - architecture, patterns, etc.]

⚠️ Issues Found:

🔴 Critical (Must Fix):
  1. [Issue] at [FilePath:LineNumber]
     - Problem: [What's wrong]
     - Why: [Why it matters]
     - Fix: [Suggested solution]

🟡 Medium (Should Fix):
  1. [Issue] at [FilePath:LineNumber]
     - Problem: [What's wrong]
     - Suggestion: [How to improve]

🔵 Minor (Nice to Have):
  1. [Issue] at [FilePath:LineNumber]
     - Suggestion: [Optional improvement]

📐 Architecture Notes:
  - [Any architectural observations or recommendations]
```

### Phase 3: QA Review (QA Engineer Perspective)

**Focus Areas:**

**1. Edge Cases & Boundary Conditions**
- Empty collections/strings
- Zero/negative values
- Maximum/minimum boundary values
- Unusual but valid inputs

**2. Exception Handling**
- Missing try-catch blocks
- Unhandled edge cases
- Improper error propagation
- Missing validation before operations

**3. Null Safety**
- Potential NullPointerException risks
- Missing null checks
- Improper use of nullable types (?)
- Safe call operator usage (?.)

**4. Concurrency & State**
- Race conditions
- Non-atomic operations on shared state
- Transaction isolation issues
- State transition logic errors

**5. Data Consistency**
- Missing validations
- Inconsistent state after errors
- Orphaned data risks
- Referential integrity concerns

**6. Test Coverage Gaps**
- Missing test cases for edge cases
- Untested error scenarios
- UseCase test scenarios missing (tests are written for UseCase ONLY — do NOT request Service/Entity/Repository/Controller tests)
- Inadequate boundary testing

**Output Format:**
```
🧪 QA Review

🔍 Potential Bugs & Risks:

🐛 High Risk (Likely to Cause Bugs):
  1. [Risk] at [FilePath:LineNumber]
     - Scenario: [When this could happen]
     - Impact: [What breaks]
     - Mitigation: [How to prevent]

⚡ Medium Risk (Edge Case Issues):
  1. [Risk] at [FilePath:LineNumber]
     - Scenario: [Edge case not handled]
     - Recommendation: [How to handle]

💡 Low Risk (Defensive Programming):
  1. [Suggestion] at [FilePath:LineNumber]
     - Context: [Additional safety measure]

🧪 Test Coverage Analysis:
  ✅ Covered:
    - [Test scenarios found]
  ❌ Missing:
    - [Test scenarios needed]

📊 Edge Cases to Consider:
  - [List of edge cases that should be tested]
```

### Phase 4: Action Items & Summary

**Consolidate findings into actionable checklist**

**Output Format:**
```
📋 Action Items Summary

🔴 Critical (Must Fix Before Merge):
  - [ ] [Action item with file:line reference]
  - [ ] [Action item with file:line reference]

🟡 Recommended (Should Address):
  - [ ] [Action item with file:line reference]
  - [ ] [Action item with file:line reference]

🔵 Optional Improvements:
  - [ ] [Suggestion for future consideration]

📈 Overall Assessment:
  - Code Quality Score: [High/Medium/Low]
  - Bug Risk Level: [High/Medium/Low]
  - Test Coverage: [Adequate/Needs Improvement/Insufficient]
  - Ready to Merge: [Yes/No/With Fixes]

💬 Reviewer Notes:
  [Any additional context, trade-offs, or architectural considerations]
```

**Ask Protocol:**

After presenting the review, ask:
```
🛠️ 발견된 이슈를 수정할까요?

어떤 항목을 고치기 원하시나요?
  A) 모든 Critical 이슈 수정
  B) 특정 이슈만 선택해서 수정 (번호 지정)
  C) 직접 수정할게요 (리뷰만 참고)
  D) 추가 설명이 필요해요
```

## Validation Checklist

Before completing review:
- [ ] Both perspectives reviewed (Code Quality + QA)
- [ ] All reviewed files listed
- [ ] Issues categorized by severity (Critical/Medium/Minor)
- [ ] Each issue includes file:line reference
- [ ] Specific fix suggestions provided for critical issues
- [ ] Test coverage gaps identified
- [ ] Action items summarized
- [ ] Ask Protocol applied (offer to fix)

## Example Invocation

```
User: "/review src/main/java/com/subscribers/gudogging/domain/subscription"

AI:
🔍 Starting Code Review: Subscription Domain

Phase 1: Context Loading
- Reading CLAUDE.md, essential-rules.yaml, system-design.yaml...
- Reading memory.md for known issues...
- Scanning files in src/.../domain/subscription...
- Found: SubscriptionController.java, SubscriptionUseCase.java, RegisterSubscriptionsRequest.java, SubscriptionResponse.java
- Loading corresponding tests...
- Loading docs/ERD.md (subscriptions)...

Phase 2: Code Quality Review

🕵️‍♂️ Code Quality Review

📋 Files Reviewed:
  - src/main/java/com/subscribers/gudogging/domain/subscription/presentation/SubscriptionController.java
  - src/main/java/com/subscribers/gudogging/domain/subscription/application/usecase/SubscriptionUseCase.java
  - src/main/java/com/subscribers/gudogging/domain/subscription/application/dto/request/RegisterSubscriptionsRequest.java
  - src/main/java/com/subscribers/gudogging/domain/subscription/application/dto/response/SubscriptionResponse.java

✅ Strengths:
  - Layer architecture properly followed (Controller → UseCase → Domain Service)
  - DTOs use Static Factory Method pattern correctly
  - RESTful endpoint naming follows conventions (/api/v1/subscriptions)

⚠️ Issues Found:

🔴 Critical (Must Fix):
  1. Missing transaction boundary at SubscriptionUseCase.java:25
     - Problem: @Transactional annotation missing on registerSubscriptions()
     - Why: 일괄 등록은 전부 성공하거나 전부 실패해야 함 (부분 등록 방지)
     - Fix: Add @Transactional to registerSubscriptions() method

🟡 Medium (Should Fix):
  1. Loop save() at SubscriptionUseCase.java:32
     - Problem: 구독마다 save() 호출
     - Suggestion: Use saveAll() for batch insert (essential-rules Performance)

🔵 Minor (Nice to Have):
  1. Magic number at RegisterSubscriptionsRequest.java:15
     - Suggestion: Extract validation constant (MAX_BILLING_CYCLE_INTERVAL = 31)

Phase 3: QA Review

🧪 QA Review

🔍 Potential Bugs & Risks:

🐛 High Risk (Likely to Cause Bugs):
  1. No past-date validation at SubscriptionUseCase.java:35
     - Scenario: nextBillingDate가 과거 날짜로 등록됨
     - Impact: D-3/D-1 알림 배치·홈 집계 오작동
     - Mitigation: Validate nextBillingDate >= today before creation

  2. Service name editable for catalog subscription at Subscription.java:58
     - Scenario: service_id가 있는 구독의 name 수정 요청
     - Impact: 카탈로그와 이름 불일치 (ERD 규칙 위반)
     - Mitigation: subscription.updateName()에서 service 존재 시 예외

⚡ Medium Risk (Edge Case Issues):
  1. Canceled subscription included in list at SubscriptionGetService.java:28
     - Scenario: status = CANCELED 구독이 홈 결제 예정 목록에 노출
     - Recommendation: status = ACTIVE 조건 추가

💡 Low Risk (Defensive Programming):
  1. Null check missing at SubscriptionResponse.java:18
     - Context: service(카탈로그)가 null인 직접 입력 구독의 logoUrl 접근
     - Suggestion: Optional 처리 또는 null-safe 변환

🧪 Test Coverage Analysis:
  ✅ Covered:
    - "유효한 요청이면 구독이 일괄 등록된다"
    - "존재하지 않는 서비스 ID면 예외 발생"
  ❌ Missing:
    - Past nextBillingDate scenario
    - billingCycleInterval 경계값 (0, 1, 31, 32)
    - Canceled subscription filtering
    - 카탈로그 구독 이름 수정 시도

📊 Edge Cases to Consider:
  - amount = 0 or negative
  - billingCycleInterval out of range (1~31)
  - nextBillingDate in the past
  - 빈 등록 목록
  - 직접 입력 구독 (service_id = NULL)

Phase 4: Action Items

📋 Action Items Summary

🔴 Critical (Must Fix Before Merge):
  - [ ] Add @Transactional to SubscriptionUseCase.java:25
  - [ ] Add past-date validation at SubscriptionUseCase.java:35
  - [ ] Block name update for catalog subscriptions at Subscription.java:58
  - [ ] Add test case: "다음 결제일이 과거면 예외 발생"
  - [ ] Add test case: "카탈로그 구독 이름 수정 시 예외 발생"

🟡 Recommended (Should Address):
  - [ ] Filter ACTIVE status at SubscriptionGetService.java:28
  - [ ] Replace loop save() with saveAll() at SubscriptionUseCase.java:32
  - [ ] Add test case: "해지된 구독은 결제 예정 목록에서 제외된다"

🔵 Optional Improvements:
  - [ ] Extract magic number to constant at RegisterSubscriptionsRequest.java:15
  - [ ] Add null-safe handling at SubscriptionResponse.java:18

📈 Overall Assessment:
  - Code Quality Score: Medium (architecture good, missing transaction)
  - Bug Risk Level: High (date validation + catalog rule missing)
  - Test Coverage: Needs Improvement (missing critical edge cases)
  - Ready to Merge: No (critical issues must be fixed first)

💬 Reviewer Notes:
  The layer architecture and DTO patterns are well-implemented. However, the
  bulk registration lacks a transaction boundary and key ERD validation rules
  (past billing date, catalog name lock) must be addressed before merge.

🛠️ 발견된 이슈를 수정할까요?

어떤 항목을 고치기 원하시나요?
  A) 모든 Critical 이슈 수정
  B) 특정 이슈만 선택해서 수정 (번호 지정)
  C) 직접 수정할게요 (리뷰만 참고)
  D) 추가 설명이 필요해요
```

## Troubleshooting

**Problem:** Too many files to review
**Solution:**
1. Ask user to specify scope (directory, specific files, or git diff range)
2. Prioritize recently changed files (use `git diff --name-only`)
3. Focus on critical files first (Controllers, UseCases, Domain Services)

**Problem:** Can't find corresponding tests
**Solution:**
1. Use Glob pattern: `**/*Test.java`
2. Check naming convention (ClassNameTest.java vs ClassNameTests.java)
3. Note missing tests in QA Review section

**Problem:** Unclear what changed in PR
**Solution:**
1. Use `gh pr diff [PR-number]` to see exact changes
2. Use `git diff [base-branch]...[feature-branch]`
3. Ask user to specify which files need review

## Best Practices

1. **Be Specific**
   - Always include file:line references
   - Explain WHY something is an issue, not just WHAT
   - Provide concrete fix suggestions

2. **Separate Concerns**
   - Keep Code Quality and QA perspectives distinct
   - Don't mix architectural feedback with bug risks
   - Categorize issues by severity

3. **Focus on Impact**
   - Prioritize critical bugs over style issues
   - Consider real-world edge cases
   - Assess actual risk vs theoretical concerns

4. **Be Constructive**
   - Acknowledge good practices
   - Explain reasoning behind suggestions
   - Offer alternatives when applicable

5. **Test-Aware**
   - Always check test coverage
   - Identify missing test scenarios
   - Verify tests actually cover edge cases

## Forbidden Actions

- Skip either review perspective (both are required)
- Provide vague feedback without file:line references
- Fix issues without asking user first (Ask Protocol)
- Review files without reading project conventions first
- Suggest changes that violate `essential-rules.yaml`
- Complete review without Action Items summary

## Success Criteria

- [ ] Both Code Quality and QA perspectives completed
- [ ] All issues categorized by severity (Critical/Medium/Minor)
- [ ] Each issue includes specific file:line reference
- [ ] Test coverage gaps identified
- [ ] Edge cases documented
- [ ] Action items consolidated into checklist
- [ ] Ask Protocol applied (offered to fix issues)
- [ ] Overall assessment provided (quality, risk, merge readiness)
