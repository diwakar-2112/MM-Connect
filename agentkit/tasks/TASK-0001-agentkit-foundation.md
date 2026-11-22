# TASK-0001: Agent Kit foundation

- Status: verified for foundation structure; application behaviour not evaluated
- Requested by: project owner, 2026-11-19
- Scope: root instructions and tracked Agent Kit foundation
- Decision: [ADR-0001](../decisions/ADR-0001-repository-and-agentkit.md)

## Goal and scope

Persist the agreed project context, working boundaries, backend invariants,
agent roles, workflow, decisions, and verification expectations. Include a small
read-only structural checker that distinguishes kit integrity from backend tests.

Excluded: application changes, database writes, backend relocation, portal
implementation, executable agent registration, CI, Git actions, and deployment.
Preserve the pre-existing untracked root README and development seed scripts.

## Acceptance criteria

- Root AGENTS.md links to task-relevant guidance without duplicating all context.
- Current backend facts, future portal scope, and existing gaps are distinguished.
- Shared/backend rules and three role guides define ownership and boundaries.
- Guard records distinguish existing coverage from planned enforcement.
- Task, decision, and review templates capture evidence and unresolved work.
- The kit checker passes on this repository and rejects isolated invalid examples.
- No application source, migration, credentials, database, or infrastructure changes.

## Verification

| Command or check | Environment / snapshot | Outcome | Limitation |
| --- | --- | --- | --- |
| `powershell -NoProfile -File agentkit/checks/verify-kit.ps1` | Windows PowerShell; foundation working tree | Passed: 19 Markdown files and 10 guard records | Checks structure/file links, not Markdown anchors or correctness of source claims |
| `python agentkit/checks/test_verify_kit.py` | Existing Python and Windows PowerShell; disposable temporary fixture | Passed: repository kit, valid isolated kit, and seven rejected invalid fixtures | Tooling self-check, not backend coverage; no application data copied |
| Git diff / ignored-file review | Current repository | Reviewed for foundation-only additions and no ignored kit files | Existing untracked root README and seed scripts remain separate work |
| Java tests / database / storage / application startup | Not used | Not run | No application changes in this task; these results are not claimed |

Negative fixtures: missing root entry, broken file link, missing role boundary
heading, invalid guard status, duplicate guard ID, unregistered backend rule, and
unbalanced code fence. The harness restores each fixture and deletes only its
own verified temporary directory. Real application source was not modified.

## Handoff

Root instructions and the kit are ready for human review. The root README and
existing development scripts were not changed. No files were staged or committed.
Review was performed by the implementing agent; an independent agent review was
not run. Executable application guards/CI, backend relocation, portal work, and
optional runtime agent orchestration remain separate future tasks.
