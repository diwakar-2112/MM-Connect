# Guard register

Status describes available enforcement, not a passing test result. Existing
tests must actually run to provide execution evidence. No CI gate is configured
by this kit. Read [known issues](../context/known-issues.md) before interpreting
partial coverage as a system-wide guarantee.

| ID | Rule | Evidence / enforcement | Status |
| --- | --- | --- | --- |
| KIT-001 | Kit entry points, required files, links, roles, and guard records are consistent. | [verify-kit.ps1](verify-kit.ps1); task records the positive/negative runs. This is a structural check only. | Executable |
| DB-001 | Applied migrations stay unchanged; dev fixtures stay outside migrations. | Manual diff review today; a base-branch migration-change guard is planned. | Manual convention |
| API-001 | Preserve DTO types, validation, and approved contracts. | [OnboardingRequestValidationTests](../../src/test/java/com/MM_CONNECT/api/profile/dto/OnboardingRequestValidationTests.java); broader JSON deserialization/compatibility checks are needed. | Partially covered |
| AUTH-001 | Enforce onboarding/profile access on the server. | [DiscoveryControllerTests](../../src/test/java/com/MM_CONNECT/api/matching/DiscoveryControllerTests.java), [ProfileControllerTests](../../src/test/java/com/MM_CONNECT/api/profile/ProfileControllerTests.java), [ProfileViewServiceTests](../../src/test/java/com/MM_CONNECT/api/profile/ProfileViewServiceTests.java). | Existing tests |
| PRIV-001 | Exclude private fields and enforce photo visibility. | Profile controller/service tests and [PhotoAccessServiceTests](../../src/test/java/com/MM_CONNECT/api/media/PhotoAccessServiceTests.java). Live storage checks remain unrun. | Partially covered |
| MEDIA-001 | Five saved photos max, enforced at signing and atomic confirmation. | [PhotoUploadServiceTests](../../src/test/java/com/MM_CONNECT/api/media/PhotoUploadServiceTests.java), [MediaControllerTests](../../src/test/java/com/MM_CONNECT/api/media/MediaControllerTests.java). Real PostgreSQL concurrency coverage is missing. | Partially covered |
| MEDIA-002 | Store keys and generate valid, authorized signed URLs. | Photo access and own-key checks have focused tests; storage existence/live storage verification remain incomplete. | Partially covered |
| CONN-001 | Participant/state checks protect requests and contact reveal. | Checks exist in the controller; dedicated authorization/state regression tests are planned. | Planned |
| SEC-001 | Keep credentials and sensitive artifacts out of committed material. | Partial ignore rules/manual review; a dedicated secret scan and runtime-config hardening are planned. | Partially covered |
| VERIFY-001 | Claims are supported by actual positive/negative verification. | Task/review records and human review; runtime/infrastructure coverage must be reported explicitly. | Manual convention |
| VERIF-001 | First approval gates access/publication; later edit reviews do not relock an approved member. | [Member tests](../../src/test/java/com/MM_CONNECT/api/verification/MemberVerificationServiceTests.java), [staff tests](../../src/test/java/com/MM_CONNECT/api/admin/AdminVerificationServiceTests.java), [security tests](../../src/test/java/com/MM_CONNECT/api/auth/AdminSecurityTests.java), [evidence](../tasks/TASK-0005-admin-verification-implementation.md). PostgreSQL races/client checks remain unrun. | Partially covered |

Planned additions: approved CI, migration-history guard, secret scanning, real
PostgreSQL race tests, storage permission/integration tests, and a disposable
fresh-machine setup check. Browser/device checks belong to tasks with an actual
client. Do not install tools or start infrastructure to satisfy these plans
without authorization.
