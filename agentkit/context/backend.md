# Current backend context

Status: R1.1 source snapshot, 2026-11-20. Separate staff password APIs are added;
see [staff authentication](staff-auth-api.md). Focused offline tests use cached
test-tool overrides; live application/database validation is not run. Recheck affected source
when it changes; avoid copying the full API specification into this file.

## Repository and source map

The backend is a modular monolith: Java 17, Spring Boot 4.1.1, Maven Wrapper,
Spring Security/JWT, Firebase Admin, PostgreSQL/JPA, Flyway, and S3-compatible
storage. These versions are declared in [pom.xml](../../pom.xml).

| Feature | Owning source |
| --- | --- |
| Firebase session exchange | [AppAuthService](../../src/main/java/com/MM_CONNECT/api/auth/AppAuthService.java) |
| JWT filtering | [JwtAuthFilter](../../src/main/java/com/MM_CONNECT/api/auth/JwtAuthFilter.java) |
| HTTP security policy | [SecurityRuleConfig](../../src/main/java/com/MM_CONNECT/api/config/SecurityRuleConfig.java) |
| Separate staff authentication | [staff API handoff](staff-auth-api.md), [staff security chain](../../src/main/java/com/MM_CONNECT/api/admin/auth/StaffSecurityConfig.java) |
| Onboarding and profile update | [OnboardingManagerService](../../src/main/java/com/MM_CONNECT/api/profile/OnboardingManagerService.java) |
| Request field types | [OnboardingRequest](../../src/main/java/com/MM_CONNECT/api/profile/dto/OnboardingRequest.java) |
| Private and selected profiles | [ProfileController](../../src/main/java/com/MM_CONNECT/api/profile/ProfileController.java), [ProfileViewService](../../src/main/java/com/MM_CONNECT/api/profile/ProfileViewService.java) |
| Discovery and shortlist/pass | [DiscoveryController](../../src/main/java/com/MM_CONNECT/api/matching/DiscoveryController.java), [matching query](../../src/main/java/com/MM_CONNECT/api/matching/MatchProfileRepository.java) |
| Signed URLs and photo quota | [PhotoUploadService](../../src/main/java/com/MM_CONNECT/api/media/PhotoUploadService.java) |
| Connections and contact reveal | [ConnectionController](../../src/main/java/com/MM_CONNECT/api/connection/ConnectionController.java) |
| Admin/member verification | [API handoff](admin-verification-api.md), [implementation evidence](../tasks/TASK-0005-admin-verification-implementation.md) |
| Member sign-in reports | [report API](admin-reports-api.md), [delivery record](../tasks/TASK-0007-signed-in-member-report.md) |

## Contracts and traps

- `POST /v1/auth/session` exchanges a Firebase ID token for backend tokens.
  `isNewUser` describes account creation, not completion of onboarding.
- Flutter authentication/token contract is retained. Reporting now records
  provider display name and successful backend session time; historical last
  logins remain unknown. Admin APIs require the
  independent `/v1/admin/auth/session` password flow; Firebase/member tokens are
  rejected there. Initial password changes are enforced server-side.
- Onboarding creates `users`-associated `profiles`, `partner_preferences`, and
  `match_profiles` data. Discovery reads the matching copy. Profile creation and
  matching synchronization happen in one service transaction.
- Onboarding persists a pending review and an unpublished initial profile.
  First admin approval unlocks access. Existing unverified members are included.
  Later edits queue new revisions and retain the last approved public profile.
- `PUT /v1/profiles/me` takes the full onboarding DTO, not a partial patch.
  Optional omissions clear values upon approval. Request and private-response
  names differ. Read pending drafts/status from `/v1/profiles/me/verification`.
- `annualIncomeInr` is a nullable whole-number rupee amount (`Long`), not a label
  such as an income range. `background.paternalGotra` is required; interests max 5.
- The endpoint is `/v1/discover`, not `/v1/discovery`. Gender MAN/WOMAN and aliases
  MALE/FEMALE are supported; omitted/blank/ANY removes the gender restriction.
- Discovery returns one top-level `profileLocked` boolean. Preview cards are
  available before onboarding; selected-profile details return 403 with
  `ONBOARDING_REQUIRED` without a profile, or `VERIFICATION_REQUIRED` until first
  approval. Shortlist/pass/connections are gated too; own uploads/status remain available.
- Private `/v1/profiles/me` and selected `/v1/profiles/{userId}` use different
  models. The selected DTO excludes contact/account fields, exact DOB, income,
  and partner preferences. Target availability and bilateral blocks are checked.
- `profile_photos.storage_key` contains an object key, not an external photo URL.
  Discovery's `mainPhotoUrl` is a generated signed URL, or null without a primary
  photo. SQL string concatenation produces an unsigned URL, not private access.
- Photo flow: request URL -> PUT actual JPEG to storage -> confirm key/primary
  flag. PUT URLs expire in 5 minutes; GET URLs in 60 minutes.
- Maximum five saved photos total, including primary and every moderation state.
  URL creation and confirmation check the quota; confirmation uses a transaction
  and an account row lock. A URL does not reserve a slot.
- Other members' detail-gallery photos must be APPROVED. ALL_VERIFIED requires a
  verified viewer; ACCEPTED_ONLY requires an accepted connection. Discovery and
  media-view signing now enforce the same visibility and account rules.
- Contact reveal requires an ACCEPTED request and a participant as caller.
- Refresh tokens are returned, but refresh/logout endpoints do not exist.

## Local environment

| Port | Service |
| --- | --- |
| 8080 | Backend API |
| 5432 | PostgreSQL |
| 9000 | MinIO storage API: binary uploads and signed image URLs |
| 9001 | MinIO administration console |

[compose.yaml](../../compose.yaml) and application settings define the local
setup. Docker Compose lifecycle is configured to start/stop with the backend.
Do not start services without authorization. Remote clients need a reachable
storage endpoint as well as the backend URL; do not rewrite signed URL hosts.

Firebase currently loads a local classpath `firebase-admin.json`. Do not copy
that file, configured secrets, JWTs, or populated tester environments into records.
API documentation is exposed through Swagger UI when the backend is running.

Migrations include V1, V3, V4, V5, V6, and V7/V8/V9 under `src/main/resources`.
Agents have not applied these new migrations; V9 adds sign-in reporting fields.
There is no requirement to invent V2. V6 makes phone numbers nullable for email
sign-in. Keep applied scripts unchanged; local Flyway checksum repair is a
separate explicitly authorized operation, never a default startup fix.

The optional local fixture `scripts/dev/seed_dummy_users.sql`, when present,
creates database profiles, not Firebase identities. It is not required for the
kit to work in a fresh checkout. Do not execute it during normal checks.
See [known issues](known-issues.md) before changing affected behaviour.
