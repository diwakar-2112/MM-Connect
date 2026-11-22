# Admin portal: planned scope

## Latest source delivery (2026-11-21)

The sibling portal now includes staff authentication and member-verification
UI in source. Earlier foundation-only notes below are historical. Live validation
remains pending. The owner requests design review before production UI for every
new feature: update only its relevant `ui-design/index.html` sections, notify the
owner and wait for approval. Independently authorized backend work can continue.

The report design now adds Reports -> Members -> Signed-in users with pagination.
Production report UI has not started. The backend supplies a recursive report
catalog, dynamic column metadata and paginated rows; see
[report API/evidence](admin-reports-api.md). V9 is added but not executed. No
report data, service startup or migration is accessed during this delivery.

The local sibling admin portal has a basic MUI layout/appearance starter, without
live authentication or business screens. R1.1 adds the backend staff foundation and member
verification APIs: see the [contract](admin-verification-api.md) and
[implementation evidence](../tasks/TASK-0005-admin-verification-implementation.md).
The owner has chosen a two-role direction: superadmin for
overall product control (including account blocking), and admin for reports/data
and reviewing members for verification. Fixed review permissions are implemented.
The separate username/password staff flow, opt-in private Superadmin bootstrap,
Admin creation/list/reset and required password change are now implemented in
source: see [staff API/evidence](staff-auth-api.md). Member Firebase login stays
unchanged. Runtime validation and connecting the portal remain pending.
React + TypeScript, strict MUI components, AG Grid Community tables and the owner's
supplied design are the frontend direction. See [ADR-0003](../decisions/ADR-0003-portal-ui-and-staff-auth-direction.md).

See [TASK-0002: backend scope assessment](../tasks/TASK-0002-admin-backend-scope.md)
for the source-backed API/data inventory, proposed MVP, dependencies, and owner
decisions. Its proposed endpoints/features are not approved implementation scope.

See [TASK-0003: roles and member verification](../tasks/TASK-0003-admin-roles-and-member-verification.md)
for the role intent and member-review lifecycle. The owner subsequently confirmed:
first verification is required before member access unlocks, unverified members
use the locked/pre-onboarding experience, and initial approval publishes the
profile. Later profile edits require review without relocking an approved member.
The first-approval and last-approved-public-version policies are implemented in
source; the owner confirmed existing unverified users also require approval.
See [ADR-0002](../decisions/ADR-0002-member-verification.md). Full runtime/client
validation, badge wording and future admin modules remain separate work.

See [TASK-0004: API delivery estimate](../tasks/TASK-0004-admin-api-delivery-estimate.md)
for the estimated backend work, assumptions, exclusions, and delivery order.

The accepted direction is a shared product repository with separate backend and
admin applications, shared Agent Kit guidance, and independent deployments.
See [ADR-0001](../decisions/ADR-0001-repository-and-agentkit.md).

## Before further modules or UI implementation

Agree on the admin features, frontend framework, admin identity source, roles
and permissions, sensitive actions, audit requirements, and deployment target.
These are open decisions. Do not infer approval to add delete, moderation,
verification, or user-management operations from the phrase "admin portal".

Backend authorization must protect each admin operation. Frontend route guards,
hidden buttons, or a browser-held role are insufficient. Existing member sessions
and a stored role field do not establish a complete admin authorization policy.

## Rules to define with the portal task

- Use agreed backend contracts; keep secrets out of browser bundles and records.
- Define loading, empty, validation, denied, and failure states for each feature.
- Verify the real client flow with appropriate browser/device evidence.
- Keep portal checks/builds/deployments separate from backend artifacts.
- Add scoped portal instructions before its implementation. Shared working
  agreements still apply; future portal rules should not be applied to Java code.

Backend relocation, portal scaffolding, CI, and AWS deployment are separate tasks.
Do not create empty application scaffolding as part of this foundation.

## Local portal guidance setup (2026-11-20)

The local sibling folder `F:/MM-CONNECT/admin-portal/` exists and contains the
owner's `ui-design/index.html`. Portal-specific instructions/checks were added
there under its own `AGENTS.md` and `agentkit/`, referencing this shared kit.
This is guidance tooling, not a production app scaffold. The sibling is outside
the current Git root and is not included in commits until repository restructuring.
Do not add a second `.git` or silently relocate files to work around that boundary.

The user has not authorized live server/database connections for this setup.
Staff username/password APIs are now implemented in source; queue names/review types and
approved-versus-proposed comparison data remain backend prerequisites for the
corresponding UI flows. Do not connect a prototype as if these contracts exist.

Local kit verification passed: 9 Markdown documents/10 guard records and 14
isolated fixture tests. App checks were explicitly skipped without a root
package.json; no browser/server/database validation or UI implementation occurred.
The design file's before/after SHA-256 is identical. This shared kit's structural
check also passed (26 Markdown documents/11 guard records). Evidence lives in the
local portal task `agentkit/tasks/TASK-UI-0001-foundation.md`; do not interpret these
results as application readiness.
