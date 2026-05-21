# Prompt Builder

Assembles the final prompt from loaded context.

## Template
```
[System Instructions]
{system.md}

[Repository Context]
{context.md}

[Active Mode: {mode}]
{modes/{mode}.md}

[Rules]
{rules/code-style.md}
{rules/security.md}

[Project State]
{memory/project-state.md}

[Relevant Skills]
{skills/{skill}.md}

[Task]
{user request}
```

## Guidelines
- Keep total context under token limits
- Prioritize recent memory over old
- Include decisions.md only when architectural context is needed
