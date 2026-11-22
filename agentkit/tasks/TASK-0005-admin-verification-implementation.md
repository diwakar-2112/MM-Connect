# TASK-0005: Admin foundation and member verification

- Status: implementation and focused offline checks complete; runtime validation not run
- Date: 2026-11-19
- Branch: `feat/admin-portal_R1.1`
- Policy: [ADR-0002](../decisions/ADR-0002-member-verification.md)
- Handoff: [API contract](../context/admin-verification-api.md)

## Scope

Fixed ADMIN/SUPERADMIN verification permissions, current-account authorization,
trusted staff identity binding, review queue/details/decisions, immutable submitted
payloads, stale-revision protection, first approval and later edit handling,
member status and access/publication gates, audit events, and consistent photo
view authorization. Additive V7 migration only; applied migrations unchanged.
Private staff provisioning is documented, not executed. No UI, services, database
operations, role-management, suspension or moderation endpoint is included.

The owner confirmed existing unverified members also require approval and pending
edits keep the approved public version. The owner plans their own DB reset; agents
must not perform it. They explicitly requested no local server connection now.

## Acceptance and evidence

- Initial submission saves a private pending revision and keeps access locked.
- Approval publishes the exact inspected payload, sets approval metadata, and
  writes an audit event within the transaction. Corrections do not revoke prior
  approval. New edits preserve canonical data and supersede previous pending work.
- Account row locking serializes submissions/decisions. A partial unique index
  permits only one pending review. Real PostgreSQL race verification is not run.
- Member APIs gate protected actions and unpublished targets; previews/status and
  own uploads stay available. Photo signing observes review-independent moderation.
- Current database roles/account state authorize requests; normal users, stale
  promoted tokens, refresh tokens and self-review cannot bypass the checks.

Baseline attempts did not execute tests: the Windows wrapper failed, sandbox JAR
cleanup hit AccessDeniedException, and offline default Surefire 3.5.6 / JUnit
platform launcher 6.0.3 were incomplete in the local cache. No downloads/install
were performed. Focused checks then used existing Maven 3.9.16, JDK 17.0.19 and
temporary command-line overrides to cached Surefire 3.5.4 and JUnit 5.12.2. The
project POM/dependency declarations were not changed.

Command (cached Maven executable, JAVA_HOME and repository path set explicitly):

```powershell
mvn -o "-Dmaven.repo.local=C:\Users\Admin\.m2\repository" "-Dmaven-surefire-plugin.version=3.5.4" "-Djunit-jupiter.version=5.12.2" "-Dtest=*Tests,!ApiApplicationTests" test
```

Final observed result: 92 tests passed, zero failures/errors/skips across 12
focused test classes, including decision-body validation failure cases.
This includes isolated real Spring Security filters and MockMvc, not a running
application or a socket server. Ordinary application/test sources compile under
Java 17 in that run. A security test configuration issue was corrected before the
passing run. Logs are local under ignored `target/`; do not commit them.

Final artifact checks passed: Agent Kit structure/links (25 Markdown files,
11 guard records), all eight Postman requests/JSON bodies with empty credential
variables, and `git diff --check`. No tracked applied migration changed.

Not run: normal declared Surefire/JUnit configuration (cache incomplete), full
application context/Testcontainers, database migrations and races, local/remote
HTTP, Firebase/MinIO, actual Flutter/admin UI, deployment, and database reset.
The user requested runtime checks later. No application credentials, secrets or
private account data were copied into the change.
