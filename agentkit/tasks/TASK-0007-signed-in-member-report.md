# TASK-0007: Signed-in member report and report design

- Date: 2026-11-21
- Status: backend source and report prototype complete; design review and runtime validation pending
- Authorized: backend report APIs/tracking and report-only prototype updates.
  Production React UI waits for the owner's design review.
- Existing owner instruction: defer automated/live testing; use source review
  and available offline compilation/syntax checks only. No services, database,
  migrations or seeds will run.

## Scope and decisions

Both unrestricted staff roles can read a Reports -> Members -> Signed-in users
catalog and HTTP-paginated report. Metadata supplies stable keys, labels, types,
nullability and status labels; rows contain only explicitly selected values.
Future fields using supported data types need backend changes only. New custom
renderers or interactions still require frontend development.

Columns: name, email, created at, last login, onboarded, onboarding status,
verified. Onboarded means a profile exists (submitted, not necessarily approved).
Onboarding status follows the existing member verification-status contract.
Verified remains independent when an approved member submits later edits.

Include non-deleted ROLE_USER accounts with a Firebase binding or recorded login;
exclude staff and database-only fixtures without sign-in evidence. Keep inactive
members for reporting. Order created_at DESC, id DESC; size 1-100, default 20.
Track successful backend Firebase session exchange, not every API request or
provider-only sign-in. Historical last login remains null, never guessed from
updated_at. Prefer submitted/canonical profile name, then provider display name;
name/email may be null for phone-only accounts. No contact/hash/token/UID leakage
beyond the requested email and an opaque member row identifier.

The design reference gains only report components/navigation and fictional data.
Nested catalog groups support future subreports without inventing their content.
The new design-first rule applies to subsequent frontend features.

## Delivered source and evidence

- [Report contract](../context/admin-reports-api.md): catalog, metadata/values,
  access, pagination, state meanings and limitations. Two read-only requests were
  added to the existing credential-free Admin Postman collection.
- [ADR-0004](../decisions/ADR-0004-report-metadata-and-design-first.md) records the
  metadata direction and the owner's design-first workflow. Portal instructions
  and design task were updated too; no production React source was changed.
- Design-ready notification sent immediately after the report-only update,
  before backend implementation continued. Prototype JSX parsed successfully
  using installed `@babel/parser`; no new dependency was installed.
- Removing only the report insertion and navigation edits reconstructs the
  exact previous prototype SHA256:
  `7BBF1E653107E4722B234BE6B57E3AAAA613AC3FCF036686C9F120B0CBBFACB0`.
  This establishes source preservation, not visual correctness.
- Final offline Java compilation passed: Maven 3.9.16 / JDK 17.0.19 compiled all
  76 main source files with release 17. Existing JwtAuthFilter deprecation warning
  remains. No tests ran. Initial sandbox attempts failed due to default cache
  location/access; the cached toolchain outside the sandbox compiled successfully.
- Scoped `git diff --check` passed. V1/V7 hashes still match the recorded baseline;
  no existing migration was edited. Existing `.gitattributes` negative-pattern
  warnings are unrelated and were left unchanged.

```powershell
mvn -o "-Dmaven.repo.local=C:\Users\Admin\.m2\repository" "-Dmaven.test.skip=true" compile
```

The executable was the existing cached Maven 3.9.16 under `.m2/wrapper/dists`;
no Maven/dependency download or POM change was needed.

- Not run per owner instruction: automated tests, live HTTP/Firebase, PostgreSQL,
  browser checks, migration execution or services. No credentials were inspected
  or printed. No Git staging/commit/push/deployment occurred.
- V9 must be applied by the owner through normal authorized Flyway startup before
  running this updated backend. Historical last-login values remain null.

## Required later validation

Check report denial for member/anonymous/password-restricted sessions, both staff
roles' access, nullable phone-only identities, no staff/fixture/hash/token leaks,
successful versus failed login tracking, status/verification independence during
edits, consistent count/page results and pagination bounds/404 handling. Confirm
SQL joins/indexes and V9 against authorized PostgreSQL; compile is not SQL proof.
Review the prototype's nested navigation/grid states in a browser. Implement the
generic React report renderer only after explicit owner design approval.
