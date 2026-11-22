# ADR-0002: First approval and later profile edit reviews

The member-review policy below remains current. Its original Firebase staff
provisioning direction is superseded by [ADR-0003](ADR-0003-portal-ui-and-staff-auth-direction.md)
and the [separate staff password API](../context/staff-auth-api.md).

- Status: accepted product policy; implemented in R1.1 source, not deployed
- Date: 2026-11-19
- Related: [implementation task](../tasks/TASK-0005-admin-verification-implementation.md)

## Decision

Initial onboarding saves a private submission. An active account can browse
locked discovery previews, read its own profile/status and upload its own photos.
Full member access and publication require first admin approval. This applies to
existing unverified members too; the owner plans to clear the development database
themselves. That statement does not authorize an agent to reset a database.

Later full-profile PUTs queue immutable review revisions. Keep the last approved
canonical profile public until approval. Pending edits and correction requests do
not revoke established first approval or relock an enabled member. Account
suspension and photo moderation remain separate decisions.

ADMIN and SUPERADMIN can inspect and decide reviews. No self-review is allowed.
Role assignment is private operator provisioning for this release. No public
request, Firebase custom claim or client-supplied role promotes a member.
Staff require a trusted Firebase UID binding and a fresh sign-in after promotion.
Superadmin-only future capabilities must use an explicit server permission check.

## Implementation consequences

Store full onboarding payload revisions in `profile_reviews`; lock the account
row for submissions and decisions, check the inspected review version, and allow
only one pending revision per member. Approvals update profile, preferences,
matching copy, member approval metadata and audit records in one transaction.
Member-facing status includes safe feedback and the latest pending submission.

The compatibility `users.is_verified` flag records established approval and stays
true during later edit reviews. `first_approved_at` records the first approval
performed by the new workflow; old verified rows may have no historical timestamp.
Deleting data, applying migrations, starting services and deployment are separate
operator actions. PostgreSQL concurrency and client behaviour remain unverified.
