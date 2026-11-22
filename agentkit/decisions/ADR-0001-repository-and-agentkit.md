# ADR-0001: Repository and Agent Kit boundaries

- Status: accepted direction; foundation implementation only
- Date: 2026-11-19
- Decision owner: project owner, confirmed during repository/Agent Kit scoping
- Task: [TASK-0001](../tasks/TASK-0001-agentkit-foundation.md)

## Context

The current Git repository contains the Spring Boot backend at its root. The
owner plans an admin portal and wants durable rules/context so development does
not depend on a long chat. Shared repository ownership currently fits this
product; separate repository access boundaries have not been requested.

## Decision

- Keep the backend, future admin portal, and Agent Kit in one product repository.
- Use separate application folders in the target layout and independent build/
  deployment pipelines. Do not use permanent backend/admin/Agent Kit branches.
- Use short-lived task branches, pull requests, checks, and a stable main branch
  as the intended development practice. GitHub protections are not configured by
  writing this decision.
- Keep the root AGENTS.md as a short entry point to tracked Agent Kit guidance.
  Scope rules by application and keep actual tests beside application code.
- Start with analyst, builder, and reviewer role guides. These are not installed
  executable agents or enforced permission profiles.

## Alternatives and consequences

Separate repositories could serve distinct teams or restricted source access,
but would require deliberate coordination of shared rules and API contracts.
Revisit if ownership, access, or product boundaries change.

The eventual layout includes `backend/`, `admin-portal/`, and `agentkit/`. Only
`agentkit/` is added now. Backend relocation needs a separate structural task
checking IDE, Maven Wrapper, Docker paths, and deployment configuration. Preserve
migration contents during a move. Portal framework, admin authorization, AWS
services, CI pipelines, and production approvals remain separate decisions/work.

Owner clarification on 2026-11-20: the target backend folder remains `api/`
alongside `admin-portal/` and root shared `agentkit/`, rather than being renamed
`backend/`. The local sibling portal now exists; Git-root relocation is still
pending. See [ADR-0003](ADR-0003-portal-ui-and-staff-auth-direction.md) for the
subsequent UI/auth direction and portal-kit scope.

## Verification

The foundation checker verifies the kit's required files, links, and record/role
structure. It does not prove branch protection or deployment isolation. Those
need observed checks when the corresponding infrastructure is implemented.
