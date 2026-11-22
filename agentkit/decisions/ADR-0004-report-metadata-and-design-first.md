# ADR-0004: Backend-defined reports and design-first frontend changes

- Status: accepted direction; source implemented, runtime checks pending
- Date: 2026-11-21
- Decision owner: repository owner, signed-in user-report request
- Related: [TASK-0007](../tasks/TASK-0007-signed-in-member-report.md)

## Decision

Report APIs supply recursive catalog groups, column keys/labels/types/status
labels, explicitly selected row values, and server pagination. The first report
shows member sign-ins/onboarding/verification for both unrestricted staff roles.
The future renderer will be generic for the agreed closed set of data types.
No executable renderer strings, database field reflection or arbitrary SQL inputs.

New frontend features first update only their relevant parts in the existing
`admin-portal/ui-design/index.html`. The owner reviews that design before
production React implementation begins. Backend work may continue while that
review is pending when separately authorized, as it is for this report.

## Consequences and verification

Supported column additions/labels/order can change on the backend alone after
the generic frontend is built; new interactions/types need frontend changes.
Last-login tracking needs additive V9 and cannot recover historical times.
Existing migrations and Flutter token shapes are preserved. Tests/runtime
validation remain deferred; inspect the task for exact compilation evidence.
