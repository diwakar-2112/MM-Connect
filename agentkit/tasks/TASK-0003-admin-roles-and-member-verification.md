# TASK-0003: Staff roles and member verification behaviour

- Status: first-approval/edit-access policy confirmed; detailed permissions and publication rules remain open
- Date: 2026-11-19
- Related: [initial scope](TASK-0002-admin-backend-scope.md), [admin context](../context/admin-portal.md)
- Scope: role intent, verification lifecycle, and member-visible API behaviour
- Excluded: UI design, code, migrations, data changes, and deployment

## Owner-confirmed direction

- Superadmin has overall product administration control, including blocking a
  member account. Account-level suspension is distinct from a member-to-member
  block interaction.
- Admin handles reports/data and member verification based on reviewing details.
- Superadmin has every defined admin capability; admin access is restricted.
- First verification is required for member access/publication. Pending users
  retain the locked pre-onboarding experience and discovery previews.
- Once first approved, a member's later profile edits require review without
  relocking their account. Superadmin suspension remains a separate access gate.
- Exact permission boundaries, identity/provisioning, and privileged-action policy
  remain to be agreed. Full product administration does not imply bypassing audit,
  member consent/contact rules, or arbitrary database access.

The owner's subsequent clarification supersedes the original recommendation to
keep initial unverified profiles visible/usable. No application code has yet been
changed to implement the confirmed policy.

## Current backend facts

- [User](../../src/main/java/com/MM_CONNECT/api/auth/User.java) has a role and
  active/verified flags, but no implemented staff workflows.
- [Onboarding](../../src/main/java/com/MM_CONNECT/api/profile/OnboardingManagerService.java)
  activates matching data and returns UNDER_REVIEW without persisting review state.
- [Selected-profile details](../../src/main/java/com/MM_CONNECT/api/profile/dto/ProfileViewResponse.java)
  expose the target's `verified` boolean.
- [MyProfileResponse](../../src/main/java/com/MM_CONNECT/api/profile/dto/MyProfileResponse.java)
  has no explicit account/verification status metadata.
- [DiscoveryCardDto](../../src/main/java/com/MM_CONNECT/api/matching/DiscoveryCardDto.java)
  has no verification field; [TokenResponse](../../src/main/java/com/MM_CONNECT/api/auth/dto/TokenResponse.java)
  has no member verification state. The Flutter UI is outside this repository;
  existing badge rendering has not been inspected.
- Gallery ALL_VERIFIED requires the viewer to be verified in
  [ProfileViewService](../../src/main/java/com/MM_CONNECT/api/profile/ProfileViewService.java).
  Older discovery/media access does not consistently enforce this policy.

## Proposed permission split

This is a suggested default-deny boundary, not the approved full matrix or code.

| Capability | Superadmin | Admin |
| --- | --- | --- |
| Read dashboard/reports | Yes | Yes, within approved scope |
| Read member details for review | Yes | Only approved review fields |
| Verify / request member corrections | Yes | Yes |
| Review photos | Yes | Proposed; confirm moderation responsibility |
| Suspend/reactivate member accounts | Yes | No by default; may recommend/escalate |
| Provision staff / change staff permissions | Yes | No |
| Change platform-wide settings | Yes | No by default |
| View audit history | Full authorized scope | Limited scope if needed |
| Delete accounts / export sensitive data / impersonate members | Define separately | Not included |

Implement hierarchy/permissions explicitly on the backend. Role names alone do
not imply inheritance. Protect against self-promotion and removal of the last
usable superadmin. Privileged actions need attributable audit records.
Do not assume "reports" means a user-complaint feature: analytics and submitted
abuse reports are different workflows; neither admin workflow exists today.

## Confirmed access lifecycle and proposed publication behaviour

Keep submitted details so staff can review them. Initial submission alone no
longer unlocks member access under the confirmed target policy. Do not return an
already-submitted member to an empty onboarding form while approval is pending.

| Review situation | Member's own status | Other members | Member access / profileLocked |
| --- | --- | --- | --- |
| Initial PENDING / IN_REVIEW | Initial verification pending | Profile unpublished | Locked / true; previews and own submission/status remain available |
| Initial CHANGES_REQUIRED | Correction reason and resubmit action | Profile unpublished | Locked / true |
| First approval complete | Details reviewed | Approved profile visible if active | Unlocked / false |
| Later edits pending review | Changes under review | Last approved version recommended; final publication policy open | Remains unlocked / false |
| Later edits need corrections | Safe correction feedback | Last approved version recommended | Remains unlocked / false |

IN_REVIEW is optional for a small first release. Do not claim a review deadline
or notification delivery unless operationally supported. Refreshing member status
can be the initial update mechanism; push/email is separate work.

Initial-review gating requires changes to the current member APIs; define exactly
which actions need first approval. Later review states must not accidentally
remove established access. Photos remain separately moderated, and older
discovery/media routes need consistent authorization/privacy enforcement.

## Keep independent state separate

- Onboarding completion: whether profile/preferences are saved.
- Verification: whether the agreed member details have been reviewed.
- Account state: active versus suspended/deleted.
- Photo moderation: approval/rejection of each image and its visibility policy.

Track first approval independently from an edit review's current status.
`profileLocked` is true until first approval and false afterward during later edit
reviews, while the account remains enabled. Verification does not automatically
approve every photo. Suspension is an independent, audited access decision.

The review is based on user-entered details unless independent evidence is
explicitly added. Prefer a label such as "Profile reviewed" or explanatory badge
copy indicating team review. Do not imply identity, income, qualifications, or
background checks were independently proven by inspecting a filled-in profile.

## Proposed backend work after policy approval

- Persist verification status, reviewer, reviewed/submitted timestamps, user-safe
  correction feedback, private reviewer notes, review revision/version, and history.
  Derive the compatibility `verified` boolean and access eligibility consistently;
  do not set established access false merely because an edit review is pending.
- Add authorized review actions and audit records; prevent concurrent/stale review
  decisions from approving a different profile revision than the one inspected.
- Expose status and safe next actions to the member, for example additive metadata
  on `/v1/profiles/me` or a dedicated account endpoint. Public cards need only the
  agreed badge indicator, not private notes or correction reasons.
- Keep account/current-permission checks consistent, including existing sessions.
- Decide which fields require re-review and which version is visible to other
  members. Avoid presenting unreviewed edits as the approved version. The first-
  approval access rule must not depend solely on the latest review's status.
- Align photo signing/access across discovery, profile details, and media routes.

No proposed field or endpoint in this record is implemented or a final contract.

## Remaining owner decisions

1. Confirm the exact first-approval action matrix, reviewed field list, and whether
   public details stay at the last approved version while edits are pending.
2. Confirm badge meaning, review evidence, and photo-access policy.
3. Finalize controlled admin permissions, staff provisioning, report scope, and
   treatment of existing members when first-approval enforcement is enabled.

## Verification and handoff

Source-inspected DTOs, onboarding behaviour, and gallery policy. No Flutter source
was available, so existing UI behaviour is not claimed. No Java/runtime tests,
service startup, external service calls, or database operations were performed.
`powershell -NoProfile -File agentkit/checks/verify-kit.ps1` passed with 21 Markdown
files and 10 guard records. The application-source Git diff remained empty.
These checks cover document links/structure only. Application behaviour remains
unchanged; this record supports the next product decision.
