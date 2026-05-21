# Context Loader

Loads relevant context files based on the current task.

## Loading Order
1. `system.md` — Always loaded first
2. `context.md` — Repository understanding
3. `router.md` — Determine active mode
4. `modes/{active-mode}.md` — Mode-specific instructions
5. `rules/*.md` — Applicable rules
6. `memory/project-state.md` — Current project state
7. `skills/{relevant}.md` — Task-relevant skills

## Context Selection
- Load only what's relevant to the current task
- Memory files are always included for continuity
- Rules are always enforced
