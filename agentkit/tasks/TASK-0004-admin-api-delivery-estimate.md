# TASK-0004: Admin API delivery estimate

- Status: estimate/implementation plan; no API changes made in this task
- Date: 2026-11-19
- Requested by: project owner, before starting admin API implementation
- Related: [backend scope](TASK-0002-admin-backend-scope.md), [confirmed lifecycle](TASK-0003-admin-roles-and-member-verification.md)

## Scope and sizing basis

Backend-only estimate for one experienced developer familiar with the project,
with an available JDK/dependency cache and an approved disposable test environment.
A developer-day means roughly one focused working day; these are engineering
estimates, not measured throughput, a delivery commitment, or agent runtime.
Calendar duration depends on reviews, decisions, dependencies, and test access.

The source still has no admin endpoints. Reuse the existing Firebase/JWT, JPA,
profile/preference/matching, storage-signing, and connection foundations. The
work includes both a new admin module and changes to member-facing behaviour.

## Confirmed policy

- Superadmin has all defined admin capabilities; admin capabilities are restricted.
- Initial profile submission is stored for review but does not unlock access or
  publish the member. Pending users keep the pre-onboarding locked preview flow.
- First approval unlocks access. Subsequent edits require review without relocking
  an approved member; account suspension is independent.
- No UI implementation is part of this estimate.

## Assumptions for the base estimate

- A fixed, explicit superadmin/admin permission matrix; no per-admin permission
  editor or configurable IAM engine in the first release.
- Firebase identity can be reused for trusted staff provisioning; first-superadmin
  bootstrap is private/explicit, never public self-registration or promotion.
- Reports initially mean agreed dashboard/operational counts, not a full abuse
  complaint/support workflow or report designer.
- Reviewed edits are stored separately and the last approved profile remains
  public until approval. This publication approach still needs owner confirmation.
- First-release operations: staff access/provisioning, user directory/details,
  verification and edit review, suspension/reactivation, photo moderation, basic
  counts, and audit history. Superadmin's full control means all defined operations;
  it does not automatically add arbitrary deletion/export/impersonation features.
- No external identity-document/KYC integration, push/email notification delivery,
  immediate revocation of issued storage URLs, or new hosting topology.

## Engineering effort estimate

| Work package | Main work | Developer-days |
| --- | --- | --- |
| Contracts and lifecycle design | Final permission/action matrix, review states, DTO compatibility, existing-member transition | 1-2 |
| Staff access and authorization | Trusted provisioning, role hierarchy, live account/permission enforcement, protected admin namespace | 2-3 |
| Persistence and audit foundation | New review/revision/audit storage, versions/history, forward migrations, transaction boundaries | 1-2 |
| Initial verification workflow | Submission, queue, approval/corrections/resubmit, publication/access checks, status responses | 2-3 |
| Approved-member edit review | Pending revision storage, safe public/private reads, approve/reject with stale-version handling, no relock | 3-5 |
| Directory, account controls, dashboard | Paged queries, minimal admin DTOs, suspension/reactivation, synchronized matching state, counts | 2-3 |
| Photo moderation and access consistency | Queue/preview/review, audit, ownership/state validation, close older signing/access gaps | 2-3 |
| Integration/regression verification and handoff | Role-denial/state/race/API checks, member compatibility, OpenAPI/examples, rollout notes | 2-4 |
| TOTAL | Backend first release with verification, edit-review handling, and test evidence | 15-25 |

The estimate includes feature-level tests inside each package; the final package
covers cross-feature integration/regression and handoff rather than postponing
all testing to the end. Roughly 3-5 working weeks for one full-time developer,
assuming decisions and test infrastructure are available; not a calendar promise.

Approximately 18-24 new admin operations are likely across staff, directory,
verification/edit queues and decisions, photos, counts, and audit. This is a
planning range, not an approved route count. Additional member API changes are
required; adding only `/v1/admin/*` cannot implement the agreed access policy.

## Affected backend areas

- `auth` / `config`: staff provisioning/roles, current account/permission checks,
  admin endpoint protection, and configured credential handling.
- `profile`: persisted initial approval, member status, submitted corrections,
  pending edit revisions, and member/admin response models.
- `matching`: unpublished initial submissions excluded from every feed variant;
  approved public matching data stays consistent with profile revisions/account state.
- `connection` and member actions: server-enforced eligibility for the agreed
  first-approval action matrix; preserve participation/acceptance constraints.
- `media`: authorized admin preview, per-photo moderation/history, object-key
  validation, and consistent member-facing photo access without changing quota.
- New `admin` module, migrations, and focused service/controller/integration tests.
  Exact class/table/file counts depend on the accepted data model.

Current [profile updates](../../src/main/java/com/MM_CONNECT/api/profile/OnboardingManagerService.java)
write immediately to profile/preferences/matching data. Staging reviewed edits
is the largest functional change; do not implement it by toggling `is_verified`
false, which would wrongly remove established access or reveal unreviewed changes.

## Delivery sequence

1. Freeze the first-release permission matrix, lifecycle/DTOs, and transition plan.
2. Implement staff authorization, trusted bootstrap, and audit foundation; verify
   normal members cannot call staff APIs and privilege removal is effective.
3. Deliver directory/read/count APIs as the first independently demonstrable
   slice. Allocate roughly 6-10 developer-days cumulatively for this foundation;
   initial review and later edit workflows are not completed at this milestone.
4. Add initial verification plus approved-member edit staging as a coherent slice,
   including public visibility and first-approval gates on affected member APIs.
5. Add account/photo actions with consistent enforcement and complete the final
   integration/regression checks and API handoff.

Do not claim a complete verification release while profile editing still updates
the public approved record immediately. No relocation or deployment is needed to
start adding the admin module to the current Spring Boot application.

## Items that change the estimate

| Additional scope / decision | Impact |
| --- | --- |
| Superadmin can configure different permissions for each admin | Roughly +2-4 developer-days for policy storage, APIs, revocation behaviour, and tests |
| Full user-submitted abuse reports/support cases | Roughly +3-5 developer-days for a basic intake/review workflow; depends on evidence, assignment, and resolution scope |
| Review notifications | Separate provider/client scope; not included in the base estimate |
| Delete/restore, bulk exports, admin impersonation, or arbitrary settings management | Separate policy and sizing; not implied by the superadmin role |
| Admin UI, backend folder relocation, GitHub Actions, AWS infrastructure/deployment | Separate deliverables and estimates |
| Missing toolchain, inaccessible Docker/services, or failed baseline | Schedule risk; no dependency installation/service startup is implicitly authorized |

Existing accounts currently have no established initial-review history and may
have `is_verified=false`. Enforcing the gate can lock them and hide their profiles.
Choose an explicit review/transition policy; do not auto-verify all existing users
or modify tester data silently. New schema changes use new Flyway versions.

## Verification required for implementation

- Normal members denied admin APIs; controlled admins denied superadmin-only
  actions; first/last-superadmin and role-removal cases covered.
- Initial pending/corrections stay locked and unpublished; approval unlocks.
- Later edit review/corrections do not relock; unreviewed data is not represented
  as approved; stale/concurrent decisions are rejected safely.
- Suspension and public discovery state remain consistent, including existing
  sessions and all gender/filter variants.
- Moderation/authorized signing prevents new unauthorized links; existing URL
  lifetime limits are explicitly documented, not called immediate revocation.
- Existing quota, contact, privacy, onboarding, and member DTO behaviour are
  tested against the agreed contract changes. Audit records accompany actions.

## Verification for this estimate and handoff

Sizing used the existing source inventory, unchanged Git application diff, and a
targeted review of the immediate profile-update path. No Java tests, application
startup, database writes/queries, infrastructure, UI, or deployment work occurred.
`powershell -NoProfile -File agentkit/checks/verify-kit.ps1` passed with 22 Markdown
files and 11 guard records. That checks links/structure only; it does not validate
the estimate or demonstrate a functioning admin feature.

Next implementation starts with the contracts/permissions/access foundation.
Resolve the assumptions above before treating this range as a fixed delivery plan.
