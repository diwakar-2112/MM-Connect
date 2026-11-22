# Development workflow

## Feature work

1. Read the applicable entry instructions and task-relevant context. Check Git
   state, relevant source/callers, and known issues. Do not repeat unrelated reads.
2. For substantive work, create a focused record from
   [the task template](../templates/task.md): goal, scope, acceptance criteria,
   affected contracts, verification, and unresolved decisions. Small routine
   edits do not need a separate planning document.
3. Resolve product/security/data-policy choices with the user. Do not add an
   extra approval gate for routine steps already authorized by the task.
4. Establish a relevant baseline using permitted checks. Record failures that
   already exist. If the environment is unavailable, record that limitation.
5. Implement in the current structure. Add meaningful failure/regression cases.
   New global guards need both passing and failing isolated examples.
6. Run appropriate checks using the [verification guide](../checks/README.md).
   Run broader checks when changed dependencies or unresolved risks justify them.
7. Review the final combined change, preferably independently. Use the
   [review template](../templates/review.md). Fix material findings and rerun the
   affected checks; distinguish pre-existing issues from new regressions.
8. Update context/ADRs and task evidence only where material facts changed.
   Hand off the change with results, limitations, and remaining decisions.
9. Human review/merge and deployment are separate actions. Testers validate the
   deployed change in the actual client; findings enter the same task/fix loop.

## Existing-code bug fixes

Reproduce or characterize the failure -> trace callers and dependencies -> name
behaviour to preserve -> establish a relevant baseline -> make the focused fix ->
test the failure and regressions -> review. Do not rewrite adjacent modules to
avoid understanding the defect.

If a real browser/device is required, use the real client when available. This
repository contains no Flutter source or admin portal; API tests and Swagger are
not substitutes for observing those clients. Record client verification as not
run if its environment is unavailable.

## Database and environment work

Use new migrations for schema changes. Running migrations, seed scripts, repair,
reset, or deletion requires explicit authorization for the intended environment
and data scope. Never infer database access from file-edit permission. Build/test
against disposable infrastructure only when its startup is authorized.

## Completion and handoff

Check acceptance criteria against evidence. Record separately:

- Implementation status and reviewed snapshot.
- Checks passed, failed, or not run; exact commands and relevant prerequisites.
- Outstanding runtime/client validation and known limitations.
- User decisions still needed and follow-up tasks.

Do not call an unverified runtime requirement passed. Human acceptance, a successful
unit test, and deployment are different events.
