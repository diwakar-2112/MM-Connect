# Admin foundation and member verification API — R1.1

Staff authentication was superseded on 2026-11-20 by the separate
[staff password API](staff-auth-api.md). Verification payloads remain unchanged.
Use a staff password access token for every admin endpoint below; Flutter's
Firebase session contract remains unchanged.

Source implementation, 2026-11-19. No application/server/database was started or
connected during delivery. Apply the new migration through the normal authorized
deployment process before using the changed application. Keep V1–V6 unchanged.

See [the accepted policy](../decisions/ADR-0002-member-verification.md) and
[verification evidence](../tasks/TASK-0005-admin-verification-implementation.md).
Import the credential-free [Postman collection](../../api-clients/postman/MM-CONNECT-Admin-R1.1.postman_collection.json).

## Scope and roles

| Operation | Member | ADMIN | SUPERADMIN |
| --- | --- | --- | --- |
| Own submission, photos and status | Own account | Own member account if applicable | Own member account if applicable |
| Discovery previews | Yes, while active | Same member policy | Same member policy |
| Full member actions | After first approval | Same member policy | Same member policy |
| Review queue, submission details and review photos | No | Yes | Yes |
| Approve / request corrections | No | Yes, except self | Yes, except self |
| Change roles, suspend accounts, reporting or photo moderation | No endpoint | No endpoint | Future scope, no endpoint yet |

Staff do not need a matrimonial profile to access the staff APIs. Dedicated
username/password sessions authenticate them; a current database role authorizes
them. Account state, role and credential version are checked on every request.
First-password-change restrictions apply to these verification routes too.
Member/Firebase and refresh tokens cannot authenticate admin endpoints.

## Endpoints

All endpoints below require a backend access token. Staff responses and member
status use `Cache-Control: no-store`.

| Method | Path | Behaviour |
| --- | --- | --- |
| GET | `/v1/admin/me` | Staff UUID, role and current verification permissions |
| GET | `/v1/admin/verifications?status=PENDING&page=0&size=20` | Paginated review summaries; size 1–100 |
| GET | `/v1/admin/verifications/{reviewId}` | Exact submitted revision, version and current photos; audited read |
| POST | `/v1/admin/verifications/{reviewId}/approve` | Approve the inspected revision and publish its profile data |
| POST | `/v1/admin/verifications/{reviewId}/request-changes` | Save member-visible correction instructions |
| GET | `/v1/profiles/me/verification` | Own onboarding/approval state, latest review and pending draft |

Approve body:

```json
{ "expectedVersion": 0 }
```

Request-corrections body:

```json
{ "expectedVersion": 0, "feedback": "Please correct your current city." }
```

Use the version returned when inspecting the review, not a hardcoded `0` in a
real client. A changed, decided or superseded revision returns 409 with
`code: "STALE_REVIEW"`; reload before deciding. Missing/negative versions and
blank/overlong correction feedback return 400. Feedback is public to that member;
do not use it for private staff notes. Private reviewer notes are not implemented.

Queue `content` contains `reviewId`, `userId`, `status`, `submittedAt`, `version`.
Pagination uses `pageNumber`, `pageSize`, `totalElements`, `totalPages`, `isLast`.
Detail contains `review`, `submission` (the full onboarding DTO), `reviewerId`,
`reviewedAt`, `feedback`, and `photos`. Photos are current account photos, not an
immutable photo revision; member verification never approves a photo. Queue/detail
can include PENDING, APPROVED, CHANGES_REQUIRED and SUPERSEDED revisions.

## Member integration changes

POST `/v1/onboarding/complete` remains 201 with the existing response field names.
It now returns `status: "UNDER_REVIEW"`, `discoveryFeedUnlocked: false` until
first approval. The field now refers to full member access; discovery previews
remain available. Duplicate onboarding still returns 400; use the full-profile
PUT `/v1/profiles/me` to correct or resubmit an existing profile.

PUT remains 200 and takes the same full onboarding DTO. It returns UNDER_REVIEW,
queues a new revision and supersedes any pending revision. For an approved member,
`discoveryFeedUnlocked` stays true, canonical public data stays unchanged, and the
proposed payload is available as `pendingSubmission` on the status endpoint.
All profile and partner-preference fields in that DTO are reviewed in this release.

Illustrative status during an approved member's edit review:

```json
{
  "onboardingComplete": true,
  "verified": true,
  "profileLocked": false,
  "status": "PENDING",
  "firstApprovedAt": "2026-11-19T10:00:00Z",
  "reviewId": "11111111-1111-4111-8111-111111111111",
  "version": 0,
  "feedback": null,
  "pendingSubmission": {}
}
```

`pendingSubmission` above is abbreviated; live responses contain the full write
DTO. It is null after approval or if no revision exists. Poll status and re-fetch
discovery after approval. `onboardingComplete=true` with PENDING means show review
status, not an empty onboarding form. CHANGES_REQUIRED includes safe feedback;
resubmit through PUT. An existing legacy profile with no review has NOT_SUBMITTED;
submit its full details through PUT once to enter the review queue. No bulk
legacy-approval or automatic approval occurs in the migration.

Before first approval:

- Discovery returns eligible approved cards with `profileLocked=true`.
- Full selected-profile details, shortlist/pass and all connection routes are
  blocked server-side. Existing profiles return 403 `VERIFICATION_REQUIRED`;
  missing profiles return 403 `ONBOARDING_REQUIRED`.
- Unverified targets are unpublished, including direct detail lookups, shortlist
  results and connection actions. Blocks remain bilateral.
- Own profile, submission/status and photo upload/preview remain available.

After first approval, discovery unlocks and the approved profile is published
while the account is active. Later pending/correction review states preserve
access and the approved public version. Private `/v1/profiles/me` returns canonical
details; use `pendingSubmission` to populate the edit form when a draft exists.

Discovery and media-view signing now enforce approved-photo visibility. Unverified
preview viewers cannot see ALL_VERIFIED photos; cards may have `mainPhotoUrl=null`.
Owners can see their own pending photos. Staff review detail can sign current
review photos under staff authorization and records an audit event. Previously
issued signed URLs remain usable until their storage expiry (60 minutes); changing
an API permission does not revoke an existing storage signature.

Photo confirmation now rejects foreign-account keys and external URLs. Continue
using the exact own `fileKey` issued by upload-url. The five-photo limit is unchanged.
Storage-object existence and unconfirmed-object cleanup are still not implemented.

## Private staff provisioning

Use the opt-in private Superadmin bootstrap, then Superadmin-only Admin creation
described in the [staff handoff](staff-auth-api.md). No account was seeded during
development. The former Firebase/operator role-promotion procedure is superseded
for admin access; it does not create password credentials. Member roles and
Firebase custom claims never provision staff credentials.

## Validation boundaries

No reports/dashboard, account suspension API, photo
moderation API, notification delivery or KYC/document checks are implemented here.
Staff credential management is now described separately in the staff handoff.
Existing configured JWT credentials and permissive development CORS remain
deployment concerns. Reviewing user-entered details does not prove identity,
qualifications or income independently. Database transaction/concurrency, live
Firebase/storage access and Flutter behaviour require later authorized testing.
