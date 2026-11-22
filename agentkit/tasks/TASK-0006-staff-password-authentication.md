# TASK-0006: Separate staff password authentication

- Status: source and focused offline verification complete; runtime/client checks pending
- Date: 2026-11-20
- Owner request: continue the admin authentication work; preserve Flutter Firebase login.

## Scope

Dedicated staff username/password sessions and security chain, private opt-in
Superadmin bootstrap, Superadmin-only Admin creation/list/reset, required first
password change, own password change and logout. Add V8; never edit V1-V7.
No application/server/database startup, seed execution, frontend implementation,
Git operations, package installation or deployment is authorized by this task.

## Design and acceptance

- Preserve the member AuthController, AppAuthService, JwtService and JwtAuthFilter
  source and existing Firebase session request/response contract.
- Staff and member tokens cannot authenticate each other's API families.
- Store staff credentials separately; hash passwords, return no hashes or secrets.
- New/reset Admins must change passwords before reviewing or managing staff.
- Only Superadmin creates/resets Admins; requests cannot assign Superadmin.
- Current active/deleted/role state and credential version are checked per request.
- Bootstrap is disabled by default, reads private runtime configuration, creates
  once under a database lock, and never resets an existing Superadmin on restart.
- Password changes/resets/logout invalidate previously issued staff tokens.
- Failed sign-ins use generic errors and persistent account-based throttling.
- Focused offline tests must cover cross-token separation, first-change bypass,
  role boundaries, bootstrap idempotency and unchanged member login behavior.

## Baseline and verification

The initial sandboxed offline Maven baseline could not compile tests due to a
local-cache JAR access error. Retrying the same cached toolchain outside the
sandbox succeeded. No dependencies downloaded.

The outside-sandbox baseline passed 9 existing authentication/security tests.
Then 39 focused staff/Firebase tests passed. A final combined password-change /
filter / logout test was added and the broader regression run passed **123 tests,
zero failures/errors/skips**. No source POM/dependency changes were needed.

Existing cached toolchain: Maven 3.9.16, JDK 17.0.19. Command, with JAVA_HOME and
the cached Maven executable selected explicitly:

```powershell
mvn -o "-Dmaven.repo.local=C:\Users\Admin\.m2\repository" "-Dmaven-surefire-plugin.version=3.5.4" "-Djunit-jupiter.version=5.12.2" "-Dtest=*Tests,!ApiApplicationTests" test
```

Normal declared Surefire/JUnit execution remains unverified with the incomplete
local cache; these are the same temporary cached overrides as TASK-0005.
The existing Flutter AuthController, AppAuthService, JwtService and JwtAuthFilter
are preserved byte-for-byte; hashes are checked at handoff. V1-V7 are not edited.

## Delivered behavior and boundaries

See [the API handoff](../context/staff-auth-api.md) and the updated credential-free
[Postman collection](../../api-clients/postman/MM-CONNECT-Admin-R1.1.postman_collection.json).
New passwords use 15-128 characters without composition rules; passwords are not
trimmed. Salted PBKDF2 is from already-installed Spring Security. Staff tokens
expire after 15 minutes by default and carry distinct type/audience/version.
Bootstrap is off by default and reads only private runtime credentials. Its DB
lock prevents concurrent instance seeding by design; that race is not live-tested.
Existing Firebase staff role-promotion instructions are superseded for admin access.

Login/account failure counters persist through a transaction configured not to
roll back deliberate authentication failures. This transaction behavior is not
proven by repository mocks. Five failures block sign-in/password-change for 15
minutes; unknown/locked/disabled accounts use generic 401 responses.
This is account-based throttling, not edge/IP protection. Staff logout invalidates
all that account's existing staff tokens. No staff refresh cookie/token is issued.

No Superadmin was created, bootstrap was not executed, and no configured secrets
were copied into source. Staff creation never accepts a role or promotes members.
The portal's instruction/context handoff was updated; its application source and
design were not changed. UI integration remains the next reviewable step.

## Final artifact checks

- Flutter authentication snapshots: all four original source-file hashes match.
  Captured V1/V7 hashes match; no applied migration was edited.
- `git diff --check`: passed.
- Shared Agent Kit verifier: passed, 28 Markdown files / 11 guard records.
- Portal static verifier: passed, 11 Markdown files / 10 guards / 11 source files;
  all 14 isolated portal guard fixture tests passed after context-only updates.
- Postman: 14 requests and 18 scripts parse. Four credential pre-request scripts
  safely serialize quote/backslash/newline fixture values as JSON and reject
  missing values. All credential/token variables in the exported file are empty.
- No Git stage/commit/push, dependency installation, server startup, database
  connection, migration/seed execution, UI code change or deployment performed.

Runtime SQL/schema/concurrency, Firebase/MinIO, network HTTP and browser tests
remain not run. Offline mocks do not establish database race behavior.
