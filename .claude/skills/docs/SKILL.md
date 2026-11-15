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
- User describes a feature not yet implemented and asks for a spec (e.g., `/docs 게시글 수정 기능 스펙 문서 작성해줘`)
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
| The requested feature (its endpoint / UseCase method) does NOT exist yet — the domain's other classes may exist | **Scenario B-1: New Spec from Requirements** |
| The requested feature is already implemented (endpoint / UseCase method exists) | **Scenario B-2: Sync Spec with Code** |
| Unclear (e.g., partially implemented, or ambiguous whether to follow code or request) | **STOP and ASK** the user which one to follow |

Check existence with Grep (e.g., the HTTP method + path in `{Domain}Controller`, the method name in `{Domain}UseCase`).

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

**Input:** Entity class files (e.g., `Post.java`)

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
# references/data-model/domains/example/post.yaml
entity: POST
description: 게시글. 제목·본문·작성자명을 보유하는 기본 게시글 엔티티.
table: posts
domain: Post
extends: BaseEntity

columns:
  - title:
      type: VARCHAR(100)
      nullable: false
      description: "게시글 제목 (최대 100자)"
  - content:
      type: TEXT
      nullable: false
      description: "게시글 본문"
  - author_name:
      type: VARCHAR(50)
      nullable: false
      description: "작성자명 (최대 50자)"

business_logic:
  - "create(title, content, authorName): 정적 팩토리 메서드"
  - "softDelete(): → BaseEntity 위임"
```

**Scenario B-1: New Spec from Requirements (before implementation)**

**When:** User describes a feature that is not implemented yet and asks for a spec

**Input:** User's feature description (natural language)

**Output:** Create `.claude/features/{domain}/{number}_{feature_name}.yaml` (Status: Draft)

**Process:**
1. Analyze the request: domain, API operations, inputs/outputs, business rules
2. Check related tables in `docs/ERD.md` and existing domain docs — reuse exact entity/column names
3. If key behavior is ambiguous (e.g., API flow, auth requirement, duplicate handling), **ASK** before writing — do not guess
4. Split & number: **one API = one spec file**. If the request has several APIs, write one spec per API (link with `meta.depends_on`). Number per domain: find the highest number in `.claude/features/{domain}/` and use the next one for both the file name prefix and `meta.id` (`{DOMAIN}-{Number}`, e.g., `AUTH-001`). A new domain starts at 001
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

> 예시 API(`domain/example`, Post) 기준의 가상 시나리오입니다. 실제 코드 상태와 다를 수 있으며, 출력 형식과 흐름을 참고하는 용도입니다.

**New Spec from Requirements (B-1):**

```
User: "/docs 게시글 제목과 본문을 수정하는 기능 스펙 문서 작성해줘"

AI:
📚 Starting Documentation: New Feature Spec

Phase 1: Context Loading
- Reading CLAUDE.md, essential-rules.yaml, system-design.yaml...
- Reading spec-writing-guide.yaml, feature-spec.yaml template...
- Reading references/data-model/domains/example/post.yaml...

Phase 1.5: Select Scenario
- PostController exists, but no update endpoint / PostUseCase.updatePost not found → Scenario B-1 (New Spec)

Phase 2: Requirement Analysis
- Domain: Post (domain/example)
- API: PATCH /api/v1/posts/{postId}
- Entity: Post (title, content)

Phase 3: Documentation
- Highest existing spec in features/example/: EXAMPLE-003 → next: EXAMPLE-004
- Creating: .claude/features/example/004_update_post.yaml (Status: Draft)

📋 API 동작 요약:
  - PATCH /api/v1/posts/{postId}
  - 게시글의 제목과 본문을 수정합니다. (작성자명은 수정 불가)
  - 존재하지 않거나 삭제된 게시글이면 POST_NOT_FOUND
  - 응답: 수정된 게시글 정보 (PostResponse)

생각하신 기능과 API 동작 방식이 맞나요?
맞다면 `/feature .claude/features/example/004_update_post.yaml`로 구현을 진행하세요.
```

**Entity Docs from Code (A):**

```
User: "/docs entity Post"

AI:
📚 Starting Documentation Update: Entity Model

Phase 1: Context Loading
- Reading CLAUDE.md, essential-rules.yaml, system-design.yaml...

Phase 2: Analysis
- Found: src/main/java/com/subscribers/gudogging/domain/example/domain/entity/Post.java
- Extracting fields: title, content, authorName
- Extracting relationships: none
- Extracting constraints: length = 100, columnDefinition = "TEXT", length = 50, nullable = false

Phase 3: Documentation
- Updating: references/data-model/domains/example/post.yaml
- Format: YAML (following existing structure)

📚 Documentation Update: Entity Model

📄 Source Files Analyzed:
  - src/main/java/com/subscribers/gudogging/domain/example/domain/entity/Post.java

📝 Documents Created/Modified:
  - .claude/references/data-model/domains/example/post.yaml

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
