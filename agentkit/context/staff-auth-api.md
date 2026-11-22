# Staff password authentication

Implemented in source on 2026-11-20. Runtime migration, bootstrap and live HTTP
testing are not run. See [task/evidence](../tasks/TASK-0006-staff-password-authentication.md).

## Separate identities and tokens

Flutter keeps `POST /v1/auth/session`, its Firebase ID-token body and existing
access/refresh response. Member authentication source files are unchanged.

Staff use independent `staff_accounts` credentials and new users with no Firebase,
phone or email binding. Roles still come from `users`, allowing the existing
verification/audit APIs to use staff UUIDs without altering review DTOs.
Member/Firebase tokens cannot authenticate `/v1/admin/**`. Staff tokens cannot
authenticate protected member APIs. The staff chain runs before the unchanged
member chain; the member filter is disabled as a standalone servlet registration
so it runs only in its intended security chain.

Legacy Firebase-provisioned staff do not automatically gain password credentials.
Their Firebase tokens no longer authenticate admin routes. Provision independent
staff accounts through the new flow; no migration converts or promotes members.

## Endpoints

Only session creation is public. Protected endpoints use
`Authorization: Bearer <staff access token>`. Success responses have `Cache-Control:
no-store`. Validation/domain errors use the existing ProblemDetail handler.

| Method | Path | Result / permission |
| --- | --- | --- |
| POST | `/v1/admin/auth/session` | 200, username/password session |
| GET | `/v1/admin/auth/me` | 200, identity, username, role, required-change flag and permissions |
| POST | `/v1/admin/auth/change-password` | 200, change own password and return a replacement token |
| POST | `/v1/admin/auth/logout` | 204, invalidate all this staff account's existing tokens |
| POST | `/v1/admin/staff` | 201, Superadmin creates an Admin only |
| GET | `/v1/admin/staff?page=0&size=20` | 200, Superadmin lists staff; size 1-100 |
| POST | `/v1/admin/staff/{userId}/reset-password` | 200, Superadmin resets an active Admin only |

Existing `/v1/admin/me` and verification routes retain their response shapes;
they now require an unrestricted staff password session. Use `/auth/me` for the
new username/password-change fields. No role-change, suspension or public staff
registration endpoint is added. No endpoint creates another Superadmin.

Login body:

```json
{ "username": "YOUR_STAFF_USERNAME", "password": "YOUR_PRIVATE_PASSWORD" }
```

Session response (illustrative, not captured):

```json
{
  "accessToken": "EXAMPLE_STAFF_ACCESS_TOKEN",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "passwordChangeRequired": true,
  "staff": {
    "userId": "11111111-1111-4111-8111-111111111111",
    "username": "example.admin",
    "role": "ROLE_ADMIN",
    "passwordChangeRequired": true,
    "permissions": []
  }
}
```

Change-password body: `currentPassword`, `newPassword`. Create-Admin body:
`username`, `temporaryPassword`. Reset body: `temporaryPassword`.
Passwords are not returned by create/reset. Share temporary credentials privately.

## Initial sign-in, change and recovery

1. A newly created or reset account signs in with its temporary password.
2. When `passwordChangeRequired=true`, only `/auth/me`, `/auth/change-password`
   and `/auth/logout` are allowed with that token. Direct review/management
   requests return 403 `PASSWORD_CHANGE_REQUIRED` too.
3. A successful password change returns a replacement access token and clears the
   restriction. Replace the old token; it is now invalid.
4. After that, Admins can review members. Superadmins additionally receive
   `STAFF_MANAGE` and can create/list/reset Admins.
5. Superadmin reset revokes an Admin's old tokens and requires another first
   password change. It cannot reset self or another Superadmin. Superadmin
   recovery is a separately authorized operator process; bootstrap is not recovery.

Staff sessions use a distinct token type/audience and credential version, with a
15-minute default lifetime (`security.staff.access-token-minutes`, allowed 1-60).
Password change, reset and logout invalidate previously issued staff tokens.
Each request checks live account/role/deletion and credential version. An operation
already authorized and in progress is not retroactively cancelled by logout.
There is no staff refresh token or cookie. Sign in again after expiry. Frontend
session storage/flow implementation is a separate step; do not store staff tokens
or passwords in browser localStorage as part of the appearance preference model.

Reporting addition (2026-11-21): unrestricted Admin/Superadmin identities also
receive `REPORTS_READ`. Report catalog/data endpoints remain under the same
staff chain and first-password-change gate. See [the report API](admin-reports-api.md).
Member Firebase token response/identity checks are retained; only successful
login reporting fields are now recorded by the member service.

Usernames are case-insensitive and stored lowercase: 3-64 ASCII letters/digits,
dots, underscores or hyphens, beginning with a letter/digit. Login ignores outer
username whitespace. Passwords are never trimmed. New/temporary passwords need
15-128 characters and cannot be blank; own change/reset must use a different
password. Hashes use Spring Security's salted PBKDF2 defaults, not plaintext.

After five failed known-account attempts, sign-in/password change is blocked for
15 minutes. A row lock serializes attempts; expired locks reset the counter.
Unknown usernames still run a password-hash comparison. Invalid credentials,
inactive/deleted/nonstaff identities and locked accounts all return the same
401 error. This account-based throttle is not a distributed IP/edge rate limiter.

## Private Superadmin bootstrap (operator runs later)

V8 adds credential tables and a metadata lock row, but no staff users. Bootstrap
is off by default. For an explicitly authorized initial application startup, set
these values in private external runtime configuration:

```properties
security.staff.bootstrap.enabled=true
security.staff.bootstrap.username=REPLACE_WITH_PRIVATE_USERNAME
security.staff.bootstrap.password=REPLACE_WITH_PRIVATE_INITIAL_PASSWORD
```

Replace both placeholders. Do not commit that configured file or put credentials
in a command line, migration, Postman export or frontend environment. Existing
private `security.jwt.secret-key` signs the distinctly scoped tokens; no new
hardcoded signing secret is added.

Bootstrap locks the metadata row and creates one independent Superadmin only if
no staff Superadmin exists. Existing accounts, including inactive/deleted
Superadmins, prevent reseeding. A conflicting Admin username fails; it is never
promoted. The initial Superadmin also must change its initial password.
After successful provisioning, disable bootstrap and remove its initial password
from runtime settings. Leaving the flag on does not reset a password on restart.

## Verification boundaries

Offline service/security tests use mocked repositories; real PBKDF2 and real
Spring Security filter chains are exercised separately. Transaction durability,
database races/constraints and bootstrap startup must still be checked against
authorized disposable PostgreSQL. No Firebase/storage/server was contacted.
Current development CORS and other existing deployment configuration are unchanged.

Sources: [controller](../../src/main/java/com/MM_CONNECT/api/admin/auth/StaffAuthController.java),
[service](../../src/main/java/com/MM_CONNECT/api/admin/auth/StaffAuthService.java),
[security](../../src/main/java/com/MM_CONNECT/api/admin/auth/StaffSecurityConfig.java),
[V8](../../src/main/resources/db/migration/V8__add_staff_password_authentication.sql).
