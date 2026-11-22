# Admin reports API

Source implemented 2026-11-21. Production React UI is not implemented: the owner
will review report-only changes to the sibling `admin-portal/ui-design/index.html`
first. No live HTTP/database validation or migration execution is claimed.

## Access and endpoints

Both active Admin and Superadmin receive `REPORTS_READ` after the required staff
password change. Use separate staff bearer authentication. Flutter/member tokens
remain rejected. No public report access. Success responses use `Cache-Control:
no-store`; the existing staff chain rejects restricted-password sessions too.

- `GET /v1/admin/reports`: `{items: [...]}` recursive catalog.
- `GET /v1/admin/reports/signed-in-users?page=0&size=20`: metadata and rows.
- Page is zero-based, size 1-100. Invalid pagination returns 400, unknown report
  returns 404 after authorization. Unauthorized/forbidden requests use staff
  authentication errors. There are no search, arbitrary SQL, sort or export inputs.

Catalog nodes have `id`, `label`, `kind` (`GROUP` or `REPORT`), nullable `reportId`,
and `children`. Current hierarchy is Members -> Signed-in users. Groups can nest;
adding another report requires an authorized implementation, not merely a label.

## Dynamic report contract

```json
{
  "reportId": "signed-in-users",
  "title": "Signed-in users",
  "columns": [
    {"key":"name","label":"Name","dataType":"TEXT","nullable":true,"valueLabels":{}},
    {"key":"email","label":"Email","dataType":"EMAIL","nullable":true,"valueLabels":{}},
    {"key":"createdAt","label":"Created at","dataType":"DATETIME","nullable":false,"valueLabels":{}},
    {"key":"lastLoginAt","label":"Last login","dataType":"DATETIME","nullable":true,"valueLabels":{}},
    {"key":"onboarded","label":"Onboarded","dataType":"BOOLEAN","nullable":false,"valueLabels":{}},
    {"key":"onboardingStatus","label":"Onboarding status","dataType":"STATUS","nullable":false,
     "valueLabels":{"NOT_SUBMITTED":"Not submitted","PENDING":"Awaiting approval","APPROVED":"Approved","CHANGES_REQUIRED":"Corrections requested","SUPERSEDED":"Superseded"}},
    {"key":"verified","label":"Verified","dataType":"BOOLEAN","nullable":false,"valueLabels":{}}
  ],
  "content": [{
    "rowId":"11111111-1111-4111-8111-111111111111",
    "name":"Example Member",
    "email":"member@example.test",
    "createdAt":"2026-11-21T08:00:00Z",
    "lastLoginAt":"2026-11-21T08:05:00Z",
    "onboarded":false,
    "onboardingStatus":"NOT_SUBMITTED",
    "verified":false
  }],
  "pageNumber":0,"pageSize":20,"totalElements":1,"totalPages":1,"isLast":true
}
```

Example is illustrative. Column order is array order, row keys match `key`, and
`rowId` is an opaque member identifier, not an additional displayed column.
Types are a closed declarative vocabulary, never executable renderers/HTML.
Future frontend should map these supported types to generic Community columns,
format booleans/dates, handle nulls, and use `valueLabels` for statuses. Changing
labels/order or adding columns of supported types can then be backend-only.
New behavior/custom data types still require frontend support. No row reflection
or whole User serialization exposes phone numbers, Firebase UID, hashes or tokens.

## Meaning and data boundaries

- One row per non-deleted `ROLE_USER` account with Firebase binding or a recorded
  successful login; staff and database-only fixtures without sign-in evidence
  are excluded. Inactive members remain visible for reporting.
- Newest account first, with UUID as the deterministic tie-breaker. Count and
  bounded page queries share a read-only repeatable-read transaction; no per-row
  Java repository calls. Pages are independent snapshots, not an export snapshot.
- Name prefers stored profile name, falling back to provider display name.
  This includes initial private submissions for staff reporting. An approved
  member's name remains the approved name during later edit review.
- `onboarded` means a profile exists, not that first approval occurred.
  `onboardingStatus` matches `/v1/profiles/me/verification`: latest review status,
  otherwise APPROVED for verified accounts or NOT_SUBMITTED. Legacy profiles can
  be onboarded=true with NOT_SUBMITTED. Later edits can be PENDING and verified=true.
- `lastLoginAt` is the last successful backend Firebase session exchange, not
  last activity or a Google login that never reached the backend. It starts
  tracking after V9; historical values stay null. Do not backfill from updated_at.
- Names/emails may be null (e.g. phone OTP); frontend should display a neutral
  missing-value label. Provider names are untrusted display text, never HTML.

## Migration and verification

[V9](../../src/main/resources/db/migration/V9__add_member_sign_in_reporting.sql)
adds nullable sign-in name and last-login fields plus the member paging index.
It has not been executed. Apply through the user's normal authorized Flyway
startup before deploying this source. Existing V1-V8 scripts are unchanged.

Flutter Firebase verification, identity binding and token response are retained;
[AppAuthService](../../src/main/java/com/MM_CONNECT/api/auth/AppAuthService.java)
now records the reporting fields only after successful token generation. Login
failures roll back the transaction and do not become successful report activity.

Sources: [controller](../../src/main/java/com/MM_CONNECT/api/admin/report/AdminReportController.java),
[DTOs](../../src/main/java/com/MM_CONNECT/api/admin/report/ReportDtos.java),
[service](../../src/main/java/com/MM_CONNECT/api/admin/report/AdminReportService.java),
[queries](../../src/main/java/com/MM_CONNECT/api/admin/report/MemberReportRepository.java).
See [task/evidence](../tasks/TASK-0007-signed-in-member-report.md).
