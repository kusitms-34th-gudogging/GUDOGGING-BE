# System Manifest
# Role: AI Agent Bootloader

system:
  name: gudogging-BE-Agent
  version: 0.0.1 (Token Optimized)
  mode: strict-layered-ddd

boot_sequence:
  - order: 1
    file: .claude/core/essential-rules.yaml
    purpose: Load constraints & stack
  - order: 2
    file: .claude/memory.md
    purpose: Load context
  - order: 3
    file: .claude/CLAUDE.md
    purpose: Load index

resource_map:
  design:
    - .claude/core/system-design.yaml
    - docs/ERD.md
  domain_model:
    path: .claude/references/data-model/domains/{domain}/*.yaml
    base: .claude/references/data-model/base/base-entity.yaml
    source_of_truth: docs/ERD.md (엔티티 문서가 없으면 ERD 기준)

agent_behavior:
  - rule: Always validate against 'essential-rules.yaml'.
  - rule: Load 'system-design.yaml' only when designing/implementing.
  - rule: Load 'docs/ERD.md' when implementing or changing entities.
  - rule: "Ask Protocol: If ambiguous, STOP and ASK."
