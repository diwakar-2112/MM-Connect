# TASK-0002: Admin portal backend scope assessment

- Status: analysis complete; feature implementation not authorized
- Requested by: project owner, 2026-11-19
- Scope: current backend inventory, gaps, proposed features/APIs, decisions needed
- Related: [admin context](../context/admin-portal.md), [ADR-0001](../decisions/ADR-0001-repository-and-agentkit.md)

## Goal and boundaries

Identify what the backend already supports for an admin portal before choosing
or building UI. This record is a proposal, not an approved product specification.
No application code, schema, data, infrastructure, or frontend is changed.

The source inventory found 20 controller routes, zero admin routes, and no
implemented admin authorization rules. Actuator health is additional to those
controller routes. This is static source evidence, not a live API test.

## Existing endpoints and reusable foundations

| Area | Existing routes | What can be reused / what is missing |
| --- | --- | --- |
| Session | `POST /v1/auth/session` | Firebase ID-token exchange and backend JWT issuance; no approved staff identity/provisioning or admin session policy. |
| Profile lifecycle | `POST /v1/onboarding/complete`, `GET/PUT /v1/profiles/me` | Member onboarding/update service and data; no cross-user admin management APIs. |
| Selected member profile | `GET /v1/profiles/{userId}` | Shareable DTO with viewer onboarding/photo policy; not an admin detail DTO or moderation view. |
| Discovery | `GET /v1/discover`, `GET /v1/discover/shortlist`, `POST /v1/discover/{targetUserId}/shortlist`, `POST /v1/discover/{targetUserId}/pass` | Member discovery/actions; not an all-user directory or admin search. |
| Photos | `POST /v1/media/upload-url`, `POST /v1/media/confirm`, `GET /v1/media/view` | Storage signing and saved-photo quota; no moderation queue/review API or authorized admin preview. |
| Connections | `POST /v1/requests/{targetUserId}`, `GET /v1/requests/sent`, `GET /v1/requests/received`, `POST /v1/requests/{requestId}/accept`, `POST /v1/requests/{requestId}/decline`, `POST /v1/requests/{requestId}/withdraw`, `GET /v1/connections/{requestId}/contact` | Member/participant workflows; no global admin list, investigation, or intervention policy. |
| Configuration | `GET /v1/config` | Public read of stored settings; no safe internal/public separation or admin write API. |

Source entry points: [auth](../../src/main/java/com/MM_CONNECT/api/auth/AuthController.java),
[profiles](../../src/main/java/com/MM_CONNECT/api/profile/ProfileController.java),
[onboarding](../../src/main/java/com/MM_CONNECT/api/profile/OnboardingController.java),
[discovery](../../src/main/java/com/MM_CONNECT/api/matching/DiscoveryController.java),
[media](../../src/main/java/com/MM_CONNECT/api/media/MediaController.java),
[connections](../../src/main/java/com/MM_CONNECT/api/connection/ConnectionController.java),
[configuration](../../src/main/java/com/MM_CONNECT/api/config/ConfigController.java).

## Authorization findings

- [User](../../src/main/java/com/MM_CONNECT/api/auth/User.java) stores a role,
  default ROLE_USER, plus active/verified/deleted state. These are data fields,
  not an admin feature or complete permissions system.
- [JwtService](../../src/main/java/com/MM_CONNECT/api/auth/JwtService.java) puts
  the role in the access token; [JwtAuthFilter](../../src/main/java/com/MM_CONNECT/api/auth/JwtAuthFilter.java)
  reads it into a granted authority. The filter does not load current account
  state/permissions on each request.
- [SecurityRuleConfig](../../src/main/java/com/MM_CONNECT/api/config/SecurityRuleConfig.java)
  permits public routes and requires authentication for the rest, without
  admin-specific role restrictions or method-security annotations in the source.
- [AppAuthService](../../src/main/java/com/MM_CONNECT/api/auth/AppAuthService.java)
  creates normal member accounts and issues tokens without a central active/
  deleted-account rejection. Profile/discovery checks do not protect every route.
- A future admin route must deny ordinary members even with a valid JWT. Existing
  onboarding/photo requirements should not force staff to create a matrimonial
  profile or obtain normal member gallery access to do an authorized admin task.
- Decide trusted admin identity/provisioning, permission levels, session handling,
  and the effect of suspension/role removal on already-issued tokens. Never let
  public registration or client-supplied role values grant staff access.

## Data that exists, and state that does not

The [schema](../../src/main/resources/db/migration/V1__initial_schema.sql) and
existing entities provide:

- `users`: identity/contact fields, role, active/verified flags, creation/update
  timestamps, deleted timestamp. No suspension reason, admin action history, or
  last-seen/login event history is currently implemented.
- `profiles` and `partner_preferences`: member details and saved preferences.
- `match_profiles`: copied discovery fields, including active/verified flags.
  Account/verification changes need consistent transactional updates and reads;
  changing only one copy would leave discovery or access inconsistent.
- `profile_photos`: storage key, visibility, moderation status, primary/sort
  fields. No review actor/time/reason or moderation history.
- `connections`: status, participants, note, timestamps, version. No admin
  intervention history.
- `platform_settings`: existing public app settings; the config controller returns
  all stored keys. Do not store private admin settings here without changing the
  read contract/separation.
- `lookup_options`: category/code/display/parent/order/active data exists in SQL;
  no lookup-management entity/API is implemented in Java.

There is no admin audit table/entity, report/ticket workflow, persisted profile
review state, or admin dashboard API.

Important: [onboarding](../../src/main/java/com/MM_CONNECT/api/profile/OnboardingManagerService.java)
returns the literal `UNDER_REVIEW`, but does not persist a profile approval state.
It activates the matching profile immediately. Profile approval, account enabled
state, identity verification, and photo moderation must not be conflated.

## Proposed first release (not yet approved)

| Priority | Capability | Backend work |
| --- | --- | --- |
| P0 | Admin access boundary | Trusted staff provisioning, server-enforced permissions, current account/access checks, and denial tests. Reuse Firebase/JWT only after choosing staff identity/session policy. |
| P0 | Audit foundation | Persist attributable admin actions with actor, action, target, timestamp, reason, outcome, and bounded/redacted changes. Define sensitive read auditing as policy requires. |
| P1 | User directory and detail | Paged search/filter/sort by agreed fields; dedicated minimal admin DTOs, onboarding/account indicators, scoped profile/preferences/photo access. |
| P1 | Suspend/reactivate accounts | Reasoned, audited actions; consistent discovery state and enforcement with existing tokens. Do not equate suspension with deletion. |
| P1 | Photo moderation | Paged pending queue, authorized signed preview, approve/reject with reason, concurrency protection, and consistent member-facing visibility. |
| P1 | Basic dashboard | Counts derivable from existing data: total/enabled accounts, onboarded profiles, verified accounts, pending photos, and connection statuses. Define each metric; enabled accounts are not daily active users. |
| Decision-dependent | Profile approval and identity verification | Define whether these workflows are required, what they mean, their evidence/statuses, and whether they gate discovery/contact/photos. Add persisted state/history only for agreed workflows. |

The first release should expose operations through admin APIs rather than direct
browser database access, member impersonation, or a generic table-editing screen.
MinIO Console is a storage administration tool, not the application's admin portal.

## Proposed endpoint contract outline

All routes below are NEW proposals under an admin namespace, not live endpoints.
Names and payloads must be agreed before implementation.

| Proposed route | Purpose |
| --- | --- |
| `GET /v1/admin/me` | Current authorized staff identity and permissions |
| `GET /v1/admin/dashboard` | Agreed summary counts |
| `GET /v1/admin/users` | Paged member/account directory |
| `GET /v1/admin/users/{userId}` | Scoped account detail |
| `GET /v1/admin/users/{userId}/profile` | Scoped profile/preferences data |
| `PATCH /v1/admin/users/{userId}/status` | Suspend/reactivate with reason |
| `GET /v1/admin/photos` | Paged moderation queue/filter |
| `GET /v1/admin/photos/{photoId}/view-url` | Authorized preview using photo ID, not an arbitrary storage key |
| `POST /v1/admin/photos/{photoId}/review` | Approve/reject with reason and expected version |
| `GET /v1/admin/audit-events` | Permission-scoped audit history |

Optional later contracts, if approved: profile review, verification decisions,
read-only connection investigation, lookup management, validated settings writes,
report/support cases, and staff permission management. No public self-promotion
or forced member accept/decline endpoint is proposed.

## Dependencies and regression cases

- Suspension must work across sign-in, existing-token requests, and discovery.
  Policy may use current account checks and/or revocation/versioning; choose it
  explicitly. Updating `users.is_active` alone is insufficient today.
- Photo rejection must prevent new unauthorized links across discovery,
  selected-profile, and media endpoints. Older routes bypass the detail policy.
  Already-issued signed URLs have a validity window; immediate revocation is a
  separate requirement that needs a storage/access strategy.
- Photo moderation must check ownership, valid state transitions, concurrent
  decisions, reasons, and audit writes; preserve the existing five-photo quota.
- Public config currently exposes every stored key. Internal settings require
  separation or an approved public DTO/filter, validated values, audit, and actual
  business-rule enforcement before presenting them as functional switches.
- Metrics need definitions. Last-active/DAU/MAU, revenue, conversion funnels, and
  identity-verification statistics require data/events not currently collected.
- Existing public DTO boundaries, contact acceptance/participation checks,
  onboarding, and Flutter contracts must continue to work.

## Decisions needed from the owner

Follow-up: the owner has clarified superadmin/admin role intent and details-based
verification. See [TASK-0003](TASK-0003-admin-roles-and-member-verification.md).
The questions below still require detailed policy and contract decisions.

1. Who may administer the product, how are they provisioned, and which actions
   need distinct permissions or stronger authentication?
2. Can a newly onboarded member appear immediately, or must an admin approve the
   profile? What constitutes identity verification, and is that part of v1?
3. Which personal fields may staff view/edit, and what is the suspension,
   reactivation, deletion, and signed-link revocation policy?

## Suggested delivery order

Agree the first-release actions and permissions -> admin authorization/account
enforcement plus audit foundation -> directory/read APIs and basic counts ->
moderation and account actions with member-facing enforcement -> API verification
-> separate UI design and implementation task.

Defer payment/monetization management, chat, advanced analytics, bulk exports,
arbitrary database editing, automatic approvals, and production test-data reset
tools unless they become explicit product requirements.

## Acceptance criteria for this assessment

- Current API/data support is separated from proposed features and endpoints.
- Security dependencies and inconsistent state/visibility paths are identified.
- The proposed MVP, exclusions, owner decisions, and verification needs are clear.
- Findings are traceable to source without claiming runtime success.
- No application code, schema, data, credentials, UI, or deployment changes.

## Verification and handoff

Evidence: controller mapping inventory, relevant service/entity/schema review,
and security annotation/role searches. No admin routes or admin authorization
rules were found. Source review only; no Java tests, server startup, database
queries, Firebase/MinIO calls, or UI tests were run. Official security documentation
was consulted for the authorization recommendations.

`powershell -NoProfile -File agentkit/checks/verify-kit.ps1` passed with 20 Markdown
files and 10 guard records. The application-source Git diff remained empty.
The kit checker validates this record's links/structure, not admin functionality.
Detailed permissions, remaining product policy, final contracts, migrations, and implementation are
still unapproved. No code or environment changes are authorized by this brief.

Security references: [OWASP authorization guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)
and [Spring request authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html).
