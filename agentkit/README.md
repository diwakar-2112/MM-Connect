# MM-CONNECT Agent Kit

Repository memory and working instructions for development of MM-CONNECT.
This is a project guidance kit, not an application dependency or an installed
agent framework. It contains no credentials and changes no runtime behaviour.

Start at [AGENTS.md](../AGENTS.md). Load only task-relevant guidance after the
initial working agreements and compact context. The root entry file links to
the rest of this kit; do not assume an AI client automatically reads every file.

## Map

| Area | Entry point | Purpose |
| --- | --- | --- |
| Current backend | [Backend context](context/backend.md) | Source map, contracts, environment traps |
| Existing gaps | [Known issues](context/known-issues.md) | Limitations, not guarantees or approved fixes |
| Future portal | [Admin portal context](context/admin-portal.md) | Planned scope and decisions still needed |
| Working rules | [Working agreements](rules/working-agreements.md) | Authority, permissions, communication, records |
| Backend rules | [Backend rules](rules/backend.md) | API, data, authorization, and media invariants |
| Workflow | [Development workflow](workflows/development.md) | Feature, bug-fix, review, and handoff steps |
| Guards | [Guard register](checks/guards.md) | Existing evidence versus missing enforcement |
| Verification | [Check guide](checks/README.md) | Commands, prerequisites, honest outcomes |
| Tasks | [Task template](templates/task.md) | Scope, acceptance criteria, and results |
| Decisions | [Decision index](decisions/README.md) | Reasons behind accepted choices |

## Roles

- [Analyst](agents/analyst.md): inspect relevant code and return a build brief.
- [Backend builder](agents/backend-builder.md): implement the approved scope.
- [Reviewer](agents/reviewer.md): independently assess correctness and evidence.

One assistant can perform these stages in sequence. Parallel agents are optional,
must be authorized, and need separate ownership of files or isolated worktrees.
The lead records findings returned by read-only analysts/reviewers.

These role guides are prompts, not executable agent registrations. Runtime
permissions must be configured in the AI client's tools/sandbox. Actual Codex
agent configuration can be added later without duplicating the canonical rules.

## Foundation scope

This version adds instructions, records, and a kit consistency checker. It does
not introduce CI, deploy anything, move source folders, or implement admin APIs.
Application tests remain under `src/test/java`; fixture SQL remains under
`scripts/dev`. The supporting `docs/` folder is currently ignored by Git, so this
kit does not depend on those files being present in a fresh checkout.

Run the read-only kit check from the repository root:

```powershell
powershell -NoProfile -File agentkit/checks/verify-kit.ps1
```

Its [positive/negative fixture check](checks/test_verify_kit.py) can be rerun with
`python agentkit/checks/test_verify_kit.py` when Python is already available.

See [the foundation task](tasks/TASK-0001-agentkit-foundation.md) for the actual
scope and verification record. Commit this kit with the repository when ready.
