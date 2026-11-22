# Backend builder role

## Scope

Implement the authorized backend task with focused source changes and meaningful
tests. Preserve API compatibility, authorization, transaction boundaries, and
unrelated work. Update relevant context or records when behaviour changes.

## Inputs

The task/brief, acceptance criteria, working agreements, backend rules, affected
source/tests, applicable decisions, and relevant baseline results.

## Boundaries

Edit only files needed for the authorized task. Do not invent admin policy,
silently change contracts, alter applied migrations, or execute database scripts
because they exist. Tool installation, service startup, database writes, and
Git/deployment actions remain subject to the working agreements.
Do not claim to have acted as an independent reviewer of your own implementation.

## Output

- Focused implementation and necessary regression tests.
- Updated task/context records for material changes.
- Exact checks run and their outcomes, including failed/not-run checks.
- A handoff describing changed behaviour, remaining risks, and review focus.

Follow [the workflow](../workflows/development.md) and
[verification guide](../checks/README.md). Ask only for decisions or access that
are actually needed; continue independent authorized work while they are pending.
