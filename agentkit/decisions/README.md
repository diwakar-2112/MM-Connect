# Decision index

Keep decisions small and durable. Use [the template](../templates/decision.md),
number records sequentially, and link them from related tasks. Preserve superseded
records with a pointer to their replacement rather than rewriting history.

| ID | Status | Decision |
| --- | --- | --- |
| ADR-0001 | Accepted direction; only foundation implemented | [Repository and Agent Kit boundaries](ADR-0001-repository-and-agentkit.md) |
| ADR-0002 | Accepted; source implemented, runtime not verified | [First approval and later profile edit reviews](ADR-0002-member-verification.md) |
| ADR-0003 | Accepted target; application changes pending | [Portal UI constraints and staff password login](ADR-0003-portal-ui-and-staff-auth-direction.md) |

Do not create an ADR for every implementation detail. Record significant choices
that a future developer might otherwise unknowingly reverse.
