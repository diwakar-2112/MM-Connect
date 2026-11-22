# Known issues and verification gaps

Source-inspected on 2026-11-19. These are open limitations, not permission to
expand a task, acceptable production guarantees, or proof of a successful test.
Recheck the relevant source and update an entry when its behaviour changes.

| ID | Gap | Source / next verification |
| --- | --- | --- |
| KI-001 | R1.1 adds shared discovery/media photo authorization; live storage/client enforcement is not verified. Previously signed URLs remain usable until expiry. | [PhotoAccessService](../../src/main/java/com/MM_CONNECT/api/media/PhotoAccessService.java), [PhotoAccessServiceTests](../../src/test/java/com/MM_CONNECT/api/media/PhotoAccessServiceTests.java); [delivery evidence](../tasks/TASK-0005-admin-verification-implementation.md). |
| KI-002 | Photo confirmation now checks the caller's key prefix/format, but not storage-object existence. Unconfirmed object cleanup, delete/reorder/replace, and single-primary enforcement remain absent. | [PhotoUploadService](../../src/main/java/com/MM_CONNECT/api/media/PhotoUploadService.java), quota/foreign-key tests. |
| KI-003 | Saved partner preferences, including gotra exclusions, are not applied to discovery. | [MatchProfileRepository](../../src/main/java/com/MM_CONNECT/api/matching/MatchProfileRepository.java). Request age/city/gender filters are separate. |
| KI-004 | Decline does not require SENT state. Duplicate requests stay blocked after decline/withdraw. | [ConnectionController](../../src/main/java/com/MM_CONNECT/api/connection/ConnectionController.java). Changes need an explicit state/retry decision. |
| KI-005 | Staff now require independent password sessions, with account/role/version and first-password-change checks. Flutter Firebase source is preserved. V8, live transactions/bootstrap and external Firebase remain unrun. Account-based lockout is not an edge/IP limiter. | [Staff handoff/evidence](staff-auth-api.md), [AdminSecurityTests](../../src/test/java/com/MM_CONNECT/api/auth/AdminSecurityTests.java), [AuthIdentityTests](../../src/test/java/com/MM_CONNECT/api/auth/AuthIdentityTests.java). |
| KI-006 | Local settings include development credentials, permissive CORS, and browser sign-in helpers. Deployment hardening is not complete. | Security and application configuration; inspect values redacted. Moving values out of a file does not remove Git history exposure. |
| KI-007 | Existing quota tests mock repositories; they do not prove concurrent confirmation safety in PostgreSQL. | [PhotoUploadServiceTests](../../src/test/java/com/MM_CONNECT/api/media/PhotoUploadServiceTests.java). Add a real database concurrency test in an approved environment. |
| KI-008 | There is no committed CI pipeline or recorded fresh-machine verification in this kit. | [Guard register](../checks/guards.md). Existing test files are not evidence of execution. |
| KI-009 | R1.1 persists review revisions, approval/audit and access/publication gates. V7 migration, PostgreSQL concurrency, and client integration remain untested. Legacy unverified profiles without a revision need one full PUT to enter the queue. | [API handoff](admin-verification-api.md), [delivery evidence](../tasks/TASK-0005-admin-verification-implementation.md). |
| KI-010 | The public configuration endpoint returns every stored platform setting; internal admin configuration has no separate read boundary. | [ConfigController](../../src/main/java/com/MM_CONNECT/api/config/ConfigController.java). Define public/internal separation before adding administrative settings. |

Not implemented: integrated admin UI, suspension API, photo moderation,
member refresh/logout, chat, dashboard/reports, lookup endpoints, block/report
endpoints and account deletion. Admin APIs include verification and basic staff
credential management/logout; general role changes and account control are absent.
Exposed platform settings are not all enforced by business logic.

For each fix, link a task, acceptance tests, the reviewed change, and observed
verification. Do not silently remove an entry after only changing documentation.
