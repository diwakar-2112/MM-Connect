# Working agreements

## Authority and scope

- Work within the user's request and applicable higher-priority instructions.
  Requirements and accepted decisions describe intended behaviour; source and
  runtime evidence describe actual behaviour. Report conflicts rather than
  silently treating an existing bug as the intended contract.
- Ask for unresolved product/security/data-policy decisions that change the
  outcome. Make routine implementation choices within an accepted brief without
  requesting repeated confirmation.
- Inspect current Git state and preserve unrelated changes, untracked work,
  credentials, and local configuration. Do not stage, discard, or reformat them.
- Installing/downloading build tools or dependencies, starting applications or
  services, database writes/repair/seeding, and infrastructure changes need
  explicit authorization. This project is normally run by the user in IntelliJ.
- Humans own commits, pushes, merges, and deployments; agents perform them only
  on an explicit user request. A repository rule does not grant system permission.

## Collaboration and permissions

- Use a single agent for small/dependent changes. Delegate only when explicitly
  requested by the user or applicable instructions, with clear file ownership.
- Analyst/reviewer roles do not edit application files. Return briefs/findings
  for the lead to record. Markdown role definitions do not impose a sandbox;
  actual tool restrictions must be configured separately.
- Parallel editors need disjoint ownership or isolated worktrees. Review the
  final combined change; a review of an earlier snapshot is not final approval.

## Evidence and records

- Establish a relevant baseline before substantive changes. Reuse a valid result
  when code and environment are unchanged; do not rerun every test for every edit.
- Match checks to the change. A documentation correction does not require app
  startup; a database race cannot be proven with mocked repositories alone.
- Record the exact command, environment scope, outcome, and material limitation.
  "Not run" is distinct from pass. Do not claim tests passed because they exist.
- Validate a new guard with both valid and intentionally invalid isolated
  fixtures. Do not plant violations in application source or a shared database.
- Update context/ADRs only for material changes. Link source/tests instead of
  duplicating implementations, transcripts, full logs, or speculative features.
- Never record real credentials, bearer tokens, service-account JSON, populated
  environments, personal tester data, or database dumps. Redact before output.

## Communication

Lead with the outcome and keep progress updates useful. At handoff state what
changed, why, what was checked, what was not checked, and any unresolved decision.
Do not represent application startup, database changes, deployment, or external
service access as completed unless they actually occurred.
