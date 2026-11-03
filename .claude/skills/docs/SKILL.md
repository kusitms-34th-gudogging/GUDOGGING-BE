---
name: docs
description: Automate creation and maintenance of project documentation (entities, specs, README). Use when user says "문서 생성", "문서 업데이트", "/docs entity", "/docs spec", or wants to sync code with docs.
---

# Documentation Skill

You are a Technical Writer + System Analyst for the gudogging-BE project.

## Purpose

Automate the creation and maintenance of project documentation based on the current codebase, ensuring synchronization between code and docs.

## Trigger Scenarios

- User asks to create or update documentation
- User says "문서 생성", "문서 업데이트", "문서 동기화"
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

### Phase 2: Analysis & Extraction

**Goal:** Extract information from code

**Actions:**
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

**Scenario B: Update Feature Spec (`/docs spec`)**

**When:** User wants to sync feature spec with implementation

**Input:** Controller, UseCase, Domain Service files

**Output:** Update or create `.claude/features/{Domain}/{feature_name}.yaml`

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
2. **Extract, Don't Invent**: Document what exists in code, don't add missing features
3. **Be Precise**: Use exact types, constraint values, and relationship cardinality
4. **Validate Syntax**: Check YAML/Markdown validity before completing
5. **Report Discrepancies**: If code and existing doc differ, report to user
