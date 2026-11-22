# Backend rules

Apply to backend Java, SQL, API contracts, security, storage, and configuration.
Existing gaps are listed in [known issues](../context/known-issues.md); this policy
does not mean every legacy path already satisfies it. See the
[guard register](../checks/guards.md) for evidence and enforcement status.

## DB-001: Preserve migration history

Do not change or delete applied Flyway scripts. Add a new version for schema
changes, preserving existing data and forward compatibility. Never disable
validation or edit Flyway history as an automatic workaround. Repair/reset/seed
operations require a named target and explicit authorization. Keep development
fixtures in `scripts/dev`, outside the automatic migration directory.

## API-001: Preserve approved contracts

Use explicit DTOs and existing field names/statuses unless a contract change is
approved. Inspect callers, tests, and handoff examples before changing a DTO.
`annualIncomeInr` is numeric INR or null, not an income-band string. Full-profile
PUT and partial PATCH have different semantics; do not silently interchange them.
Preserve useful controller validation/domain errors without leaking internals.

## AUTH-001: Enforce profile access on the server

Do not rely on Flutter to enforce access. Selected-profile details require an
active, non-deleted, onboarded viewer and an available target; bilateral blocks
return the unavailable-target response. Missing onboarding returns 403 with
`ONBOARDING_REQUIRED` before target details/photos are read. Discovery previews
retain their common `profileLocked` flag, independent of individual cards.

## PRIV-001: Keep private profile fields out of member-facing details

Keep exact DOB, income, contact/account fields, partner preferences, and storage
keys out of the selected-profile DTO. Preserve community visibility and the
approved photo policy. Private `/me` is a different model. Do not generalize this
endpoint's protections to older routes that have open privacy issues.

## MEDIA-001: Enforce the saved-photo quota atomically

At most five confirmed photos per account, including main photos and all
moderation states. Check before signing a PUT URL and again when confirming.
Confirmation must serialize the count/save for the account inside a transaction;
do not replace the account row lock with an unchecked count-then-save sequence.
The quota error remains 409 `PHOTO_LIMIT_REACHED`, with `maxPhotos: 5`.

## MEDIA-002: Keep storage keys and signed URLs distinct

Store object keys and sign URLs on the backend. Do not store arbitrary external
URLs in `storage_key`, persist expiring URLs as permanent references, rewrite a
signed URL's host/path, or make a private bucket public as a default fix.
File ownership/existence and authorization gaps need their own scoped fix.

## CONN-001: Protect connection state and contact reveal

Only the intended participant may perform a request action. Contact reveal needs
both participation and ACCEPTED status. Protect these checks on the backend.
Current decline/resend limitations are not a complete state-machine design;
obtain a decision before changing retry or terminal-state behaviour.

## SEC-001: Keep runtime credentials out of committed material

Use private runtime configuration for credentials. Never copy actual values into
the kit, tests, logs, generated examples, or handoffs. Do not weaken authorization
or CORS to make an unexplained test pass. Missing global enforcement/scanning is
documented debt, not a reason to claim the project is deployment-hardened.

## VERIFY-001: Report actual evidence

Use the verification guide. Test new rules with failure cases. Confirm concurrency
with real PostgreSQL when authorized; confirm signed storage access with the
configured storage service when authorized. Verify client behaviour in the real
client when the client/environment are available. Record unavailable checks as
not run and do not manufacture screenshots, test counts, or success claims.

## VERIF-001: Separate first approval from later edit reviews

Initial unverified members retain the
locked pre-onboarding preview experience and remain unpublished until approved.
First approval unlocks access; reviewing an approved member's later edits must
not relock them. Suspension is a separate access decision. Do not derive member
eligibility solely from the latest edit review's PENDING status.
See [the confirmed lifecycle](../tasks/TASK-0003-admin-roles-and-member-verification.md)
and [delivery assumptions](../tasks/TASK-0004-admin-api-delivery-estimate.md).
R1.1 reviews all fields in the full onboarding DTO. Pending edits keep the last
approved public version. See [ADR-0002](../decisions/ADR-0002-member-verification.md)
and the [implementation handoff](../context/admin-verification-api.md). Mock-based
checks do not establish database concurrency or live client behaviour.
