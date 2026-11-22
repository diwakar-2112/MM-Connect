# ADR-0003: Portal UI constraints and target staff password login

- Status: accepted; staff authentication implemented in source, portal integration pending
- Date: 2026-11-20
- Related: [repository boundaries](ADR-0001-repository-and-agentkit.md),
  [member verification](ADR-0002-member-verification.md),
  [current admin scope](../context/admin-portal.md)

## Owner direction

The portal will use React + TypeScript, actual Material UI controls and one shared
MUI theme. AG Grid Community is the only approved external UI component library,
for application tables. Use verified free features, never Enterprise packages or
trial licences. Follow the owner's supplied design iteration rather than
generating unrelated custom controls or a replacement visual direction.

Staff login will use username/password credentials, with a privately seeded
Superadmin creating Admin accounts and initial passwords. This supersedes the
intended Firebase staff-login approach in ADR-0002; member Firebase authentication
is unchanged. The prototype direction includes first-password-change enforcement.
Detailed password/recovery/session/MFA policies and endpoints remain to be built
and agreed where needed. Do not infer implementation from this decision.

## Current implementation and boundaries (updated 2026-11-20)

The backend now has an independent staff security chain, username/password APIs,
opt-in private Superadmin bootstrap, Admin credential management and first-password
change enforcement. Flutter Firebase authentication source/contract is unchanged.
See the [staff API](../context/staff-auth-api.md) and
[task/evidence](../tasks/TASK-0006-staff-password-authentication.md). No database,
bootstrap or server was run. Existing verification response DTOs remain unchanged.

The user created `F:/MM-CONNECT/admin-portal/ui-design/index.html` and authorized
portal-specific guidance in that sibling folder. The shared kit currently remains
inside `api/agentkit/`. Keep a single canonical copy of general rules, reference
it from portal instructions, and preserve the design reference.

The confirmed target repository root is `MM-CONNECT/`, containing `api/`,
`admin-portal/` and the shared `agentkit/`. Git still starts inside `api/` today;
restructuring and versioning the sibling folder remain separate work. No nested
portal repository, backend relocation, UI scaffold, dependency installation,
server/database connection or deployment is included in this kit setup.
