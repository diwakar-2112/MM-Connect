# MM-CONNECT repository instructions

MM-CONNECT is a Spring Boot backend for a Flutter matrimonial application.
The backend currently lives at the repository root (`pom.xml`, `src/`).
Admin foundation/member verification APIs are implemented in R1.1 source. The
separate staff password APIs are documented in
[staff authentication](agentkit/context/staff-auth-api.md). Runtime verification is
pending; read the linked backend context for the delivered scope and evidence.

## Start here

1. Read [working agreements](agentkit/rules/working-agreements.md) and the compact
   [backend context](agentkit/context/backend.md) when starting work in this repo.
2. Check Git changes and inspect only the files relevant to the task. Reuse known
   context; do not repeatedly reread unchanged files or scan unrelated projects.
3. For Java, SQL, API, security, storage, or configuration changes, read
   [backend rules](agentkit/rules/backend.md) and relevant
   [known issues](agentkit/context/known-issues.md).
4. For admin-portal planning, read its
   [scope and open decisions](agentkit/context/admin-portal.md). Do not treat
   proposed features, roles, or folders as existing functionality.
5. Use the [workflow](agentkit/workflows/development.md),
   [verification guide](agentkit/checks/README.md), and
   [guard register](agentkit/checks/guards.md) as appropriate to the change.

## Essential boundaries

- Follow the current user's authorized scope and higher-priority instructions.
  These files provide guidance; they do not grant tool or system permissions.
- Do not install tools/dependencies, start the application/services, write to a
  database, run migrations/seeds/repair, or change infrastructure unless the user
  explicitly authorizes that action. Keep already-authorized work moving.
- Humans own commits, pushes, merges, and deployments. Do not perform these
  actions without an explicit user request for the action.
- Never edit an applied Flyway migration or silently change a public API contract.
- Do not expose credentials, tokens, database dumps, or populated environments in
  output, documentation, or commits. Inspect configuration with values redacted.
- Report checks as passed, failed, or not run, with evidence. Added tests and
  static inspection are not proof that runtime behaviour passed.

## Roles and durable context

Use [role guides](agentkit/README.md#roles) only when relevant. Delegate to other
agents only when the user or applicable instructions explicitly request it.
Role Markdown does not create agents or enforce read-only permissions.

Record significant new decisions using [the ADR template](agentkit/templates/decision.md).
For substantive work, maintain a focused task record using
[the task template](agentkit/templates/task.md). Keep prose short, link to source
and tests, and distinguish intended behaviour from observed behaviour.

Do not move backend files, scaffold the portal, or add deployment pipelines just
because their future structure is documented. Those are separate tasks.
