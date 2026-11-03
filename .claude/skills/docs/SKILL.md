---
name: docs
description: Automate creation and maintenance of project documentation (entities, specs, README). Writes new feature specs from user requirements before implementation, and syncs docs with existing code. Use when user says "문서 생성", "문서 업데이트", "스펙 문서 작성", "/docs entity", "/docs spec", or wants to sync code with docs.
---

# Documentation Skill

You are a Technical Writer + System Analyst for the gudogging-BE project.

## Purpose

Automate the creation and maintenance of project documentation:
- **Before implementation:** Write a new feature spec from the user's requirements (input for `/feature`)
- **After implementation:** Create or update docs based on the current codebase, ensuring synchronization between code and docs

## Trigger Scenarios

- User asks to create or update documentation
- User says "문서 생성", "문서 업데이트", "문서 동기화", "스펙 문서 작성"
- User describes a feature not yet implemented and asks for a spec (e.g., `/docs 소셜 로그인 기능 스펙 문서 작성해줘`)
- User invokes `/docs entity [EntityName]`
- User invokes `/docs spec [FeatureName]`
- User invokes `/docs update [FilePath]`

## Execution Flow

### Phase 1: Context Loading

**MUST READ:**
1. `CLAUDE.md` - Project overview
2. `core/essential-rules.yaml` - Core rules
3. `core/system-design.yaml` - Architecture, API, Entity Rules

**CONDITIONAL READ:**
- **Target Code:** The source code files to be documented (Controllers, Entities, UseCases, etc.)
- **Target Doc:** The documentation file to be created or updated
- **New Spec only:** `docs/spec-writing-guide.yaml`, `docs/templates/feature-spec.yaml`, `docs/ERD.md` (related tables only), `references/data-model/domains/{domain}/*.yaml` (if exists), `references/conventions/testing-guide.yaml`

### Phase 1.5: Select Spec Scenario

When the request is about a feature spec, decide which scenario applies:

| Condition | Scenario |
|---|---|
| Feature is described in words and related code (Controller/UseCase) does NOT exist | **Scenario B-1: New Spec from Requirements** |
| Related code already exists (implemented feature) | **Scenario B-2: Sync Spec with Code** |
| Unclear (e.g., partially implemented, or ambiguous whether to follow code or request) | **STOP and ASK** the user which one to follow |

Check existence with Grep/Glob (e.g., `**/{Domain}Controller.java`, `**/{Domain}UseCase.java`).

### Phase 2: Analysis & Extraction

**Goal:** Extract information from code (Scenario A, B-2) or from user requirements (Scenario B-1)

**Actions:**
- **Requirement Analysis (B-1):** Identify domain, actors, API operations, inputs/outputs, business rules, error cases from the user's description
- **Entity Analysis:** Extract fields, types, relationships, constraints from Entity classes
- **Feature Analysis:** Reverse-engineer business logic, API endpoints, and data flow from Controller/UseCase/Service

### Phase 3: Documentation

**Goal:** Create or update documentation

**Scenario A: Update Domain Model (`/docs entity`)**

**When:** User wants to update entity documentation

**Input:** Entity class files (e.g., `Subscription.java`, `SubscriptionCheck.java`)

**Output:** Update `references/data-model/domains/{domain}/{Entity}.yaml`

**Process:**
1. Read entity class files using Glob: `src/**/entity/{EntityName}.java`
2. Extract structure:
   - Fields and types
   - Relationships (@ManyToOne, @OneToMany, etc.)
   - Constraints (@NotNull, @Size, etc.)
   - Embedded types
3. Update or create YAML file following existing structure in `references/data-model/`
4. Validate YAML syntax

**Example:**
```yaml
# references/data-model/domains/subscription/subscription.yaml
entity: SUBSCRIPTION
description: 사용자가 등록한 구독
table: subscriptions
domain: Subscription
extends: BaseEntity

columns:
  - user_id:
      type: BINARY(16) (UUID)
      nullable: false
      description: "FK → users"
  - name:
      type: VARCHAR(30)
      nullable: false
      description: "서비스명 (1~30자)"
  - amount:
      type: INT
      nullable: false
      description: "1회 결제 금액 (원 단위, 양의 정수)"

relationships:
  - type: ManyToOne
    target: User
  - type: OneToMany
    target: SubscriptionCheck

business_logic:
  - "create(...): 정적 팩토리 메서드"
  - "cancel(): status = CANCELED, canceledOn = 오늘"
```

**Scenario B-1: New Spec from Requirements (before implementation)**

**When:** User describes a feature that is not implemented yet and asks for a spec

**Input:** User's feature description (natural language)

**Output:** Create `.claude/features/{domain}/{number}_{feature_name}.yaml` (Status: Draft)

**Process:**
1. Analyze the request: domain, API operations, inputs/outputs, business rules
2. Check related tables in `docs/ERD.md` and existing domain docs — reuse exact entity/column names
3. If key behavior is ambiguous (e.g., API flow, auth requirement, duplicate handling), **ASK** before writing — do not guess
4. Decide numbering: find the highest `FEAT-{Number}` in `.claude/features/**` and use the next number for both `meta.id` and file name prefix
5. Write the spec using `.claude/docs/templates/feature-spec.yaml`, following `docs/spec-writing-guide.yaml`:
   - API: `/api/v1`, kebab-case plural URLs, `{Action}{Resource}Request` / `{Resource}Response`, `CommonResponse<T>`
   - Data flow: Controller → `{Domain}UseCase` → Domain Service → Repository
   - Error codes: use the domain range in `core/system-design.yaml`; if the domain has no range yet, propose the next free range (403xx~) and mark it as "제안"
   - Testing strategy: `{Domain}UseCaseTest` scenarios ONLY
6. Show a short summary of the API behavior and ask the user to confirm it matches the intended feature
7. After confirmation, guide the next step: `/feature .claude/features/{domain}/{number}_{feature_name}.yaml`

**Rules:**
- **NO implementation:** Do NOT create or modify any source code — spec document only
- **No Over-engineering:** Include ONLY what the user requested — no extra APIs, fields, or "nice-to-have" features
- Mark assumptions explicitly in `notes` so the user can review them

**Scenario B-2: Sync Spec with Code (`/docs spec`, after implementation)**

**When:** User wants to create or update a spec for an already implemented feature

**Input:** Controller, UseCase, Domain Service files

**Output:** Update or create `.claude/features/{domain}/{number}_{feature_name}.yaml`

**Process:**
1. Read feature implementation files using Grep: `class.*{FeatureName}.*Controller`
2. Extract API contract:
   - Endpoint paths and methods
   - Request/Response DTOs
   - Status codes
3. Extract business logic from UseCase
4. Compare with existing spec (if exists)
5. Update spec file using template from `.claude/docs/templates/feature-spec.yaml`

**Template Reference:** See `.claude/docs/templates/feature-spec.yaml` for detailed structure

**Scenario C: General Update (`/docs update`)**

**When:** User requests update to specific documentation

**Input:** User instruction + target file path

**Output:** Update specified document (e.g., README.md, memory.md)

**Process:**
1. Read target document
2. Apply user-requested changes
3. Maintain existing format and structure
4. Validate markdown syntax if applicable

## Validation Checklist

Before completing documentation:

**New Spec (B-1):**
- [ ] No source code created or modified
- [ ] Only requested features included (no extras)
- [ ] Entity/column names match `docs/ERD.md`
- [ ] Testing strategy lists UseCase test scenarios only
- [ ] User confirmed the API behavior

**Code-based Docs (A, B-2, C):**
- [ ] All fields/properties documented
- [ ] Relationships clearly defined
- [ ] Constraints explicitly stated
- [ ] YAML/Markdown syntax valid
- [ ] Follows existing structure and naming conventions

## Report Format

```
📚 Documentation Update: [Type]

📄 Source Files Analyzed:
  - src/.../Controller.java
  - src/.../Entity.java

📝 Documents Created/Modified:
  - .claude/references/.../{File}.yaml

✅ Status: [SUCCESS/WARNINGS]
⚠️ Warnings: [If any inconsistencies found]
```

## Example Invocation

**New Spec from Requirements (B-1):**

```
User: "/docs 사용자가 결제 전 알림 설정을 켜고 끌 수 있는 기능 스펙 문서 작성해줘"

AI:
📚 Starting Documentation: New Feature Spec

Phase 1: Context Loading
- Reading CLAUDE.md, essential-rules.yaml, system-design.yaml...
- Reading spec-writing-guide.yaml, feature-spec.yaml template...
- Reading docs/ERD.md (users)...

Phase 1.5: Select Scenario
- No related Controller/UseCase found → Scenario B-1 (New Spec)

Phase 2: Requirement Analysis
- Domain: User
- API: PATCH /api/v1/users/me/notification-settings
- Entity: User (check notification column)

Phase 3: Documentation
- Creating: .claude/features/user/004_update_notification_setting.yaml (Status: Draft)

📋 API 동작 요약:
  - PATCH /api/v1/users/me/notification-settings
  - 로그인한 사용자의 결제 전 점검 알림 설정을 켜거나 끕니다.
  - 응답: 변경된 알림 설정 값

생각하신 기능과 API 동작 방식이 맞나요?
맞다면 `/feature .claude/features/user/004_update_notification_setting.yaml`로 구현을 진행하세요.
```

**Entity Docs from Code (A):**

```
User: "/docs entity Subscription"

AI:
📚 Starting Documentation Update: Entity Model

Phase 1: Context Loading
- Reading CLAUDE.md, essential-rules.yaml, system-design.yaml...

Phase 2: Analysis
- Found: src/main/java/com/subscribers/gudogging/domain/subscription/domain/entity/Subscription.java
- Extracting fields: name, category, amount, billingCycleInterval, nextBillingDate, status...
- Extracting relationships: ManyToOne -> User, ManyToOne -> Service, OneToMany -> SubscriptionCheck...
- Extracting constraints: nullable, length, @Enumerated(EnumType.STRING)...

Phase 3: Documentation
- Updating: references/data-model/domains/subscription/subscription.yaml
- Format: YAML (following existing structure)

📚 Documentation Update: Entity Model

📄 Source Files Analyzed:
  - src/main/java/com/subscribers/gudogging/domain/subscription/domain/entity/Subscription.java

📝 Documents Created/Modified:
  - .claude/references/data-model/domains/subscription/subscription.yaml

✅ Status: SUCCESS
```

## Troubleshooting

**Problem:** Cannot find entity file
**Cause:** Entity name mismatch or file moved
**Solution:** Use Glob with pattern `**/*{EntityName}*.java` to search recursively

**Problem:** YAML syntax error after update
**Cause:** Indentation or special characters
**Solution:** Validate using `python -c "import yaml; yaml.safe_load(open('file.yaml'))"`

**Problem:** Not sure whether to write a new spec (B-1) or sync with code (B-2)
**Cause:** Feature is partially implemented, or the request differs from existing code
**Solution:** STOP and ASK the user which source to follow (requirements or code)

**Problem:** Spec file doesn't match implementation
**Cause:** Implementation changed after spec was written
**Solution:** Ask user if spec should be updated or if implementation should be rolled back

**Problem:** Missing relationships in entity documentation
**Cause:** Relationships defined in other entity (inverse side)
**Solution:** Read both sides of relationship and document bidirectional mapping

## Success Criteria

- [ ] All requested documentation created/updated
- [ ] Format matches existing documentation structure
- [ ] All code elements documented (no missing fields)
- [ ] YAML/Markdown syntax valid
- [ ] Relationships and constraints clearly stated
- [ ] Report generated with file paths

## Best Practices

1. **Always Read Existing Docs First**: Maintain consistency with existing structure
2. **Extract, Don't Invent (A, B-2)**: Document what exists in code, don't add missing features
3. **Request Only (B-1)**: Write only what the user asked for; ask instead of guessing; never write code
4. **Be Precise**: Use exact types, constraint values, and relationship cardinality
5. **Validate Syntax**: Check YAML/Markdown validity before completing
6. **Report Discrepancies**: If code and existing doc differ, report to user
