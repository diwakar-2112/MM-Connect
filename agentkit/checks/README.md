# Verification guide

Run the cheapest checks that actually address the change. A command listed here
is not authorization to install tools, download dependencies, start services, or
mutate a database. Confirm the environment is available and allowed first.

## Kit consistency (read-only, no application startup)

From the repository root on Windows:

```powershell
powershell -NoProfile -File agentkit/checks/verify-kit.ps1
```

This checks required kit files, non-empty Markdown, local link targets, code-fence
balance, role headings, and guard IDs/statuses. It does not verify Java behaviour,
security enforcement, source truth, or CI configuration.

When changing the checker, verify a valid kit passes and isolated fixtures with
missing files/broken links/invalid role or guard records fail. Never mutate the
actual application to manufacture a failing example. A fixture can be checked
with `-RootPath` pointing to its isolated repository root.

The reproducible checker verification uses an already-installed Python 3 and
Windows PowerShell. It checks this repository, then one valid and seven invalid
isolated fixtures, and removes only its own temporary fixture:

```powershell
python agentkit/checks/test_verify_kit.py
```

This is tooling verification, not an application test suite. Do not install Python
solely to run it without authorization. Markdown fragment anchors are not checked.

## Focused backend tests

Only when the JDK, Maven distribution, plugins, and dependencies are already
available and test execution is authorized. The Wrapper itself can download
Maven if uncached, even when Maven receives `-o`; do not let that bypass the
no-install/download agreement.

```powershell
.\mvnw.cmd -o "-Dtest=DiscoveryControllerTests,ProfileControllerTests,ProfileViewServiceTests,OnboardingRequestValidationTests,PhotoUploadServiceTests,MediaControllerTests" test
```

```bash
./mvnw -o '-Dtest=DiscoveryControllerTests,ProfileControllerTests,ProfileViewServiceTests,OnboardingRequestValidationTests,PhotoUploadServiceTests,MediaControllerTests' test
```

These are standalone validation/service/MockMvc tests. Compilation and test
plugins still need the local toolchain/cache. Missing dependencies are a not-run
result, not permission to install or a test success.

## Full application and integration checks

[ApiApplicationTests](../../src/test/java/com/MM_CONNECT/api/ApiApplicationTests.java)
uses SpringBootTest and
[TestcontainersConfiguration](../../src/test/java/com/MM_CONNECT/api/TestcontainersConfiguration.java).
The full suite can start application infrastructure and needs Firebase/runtime
configuration. Do not run it as a default documentation or foundation check.

Real PostgreSQL concurrency, MinIO object access, Firebase integration, and
Flutter/admin-client tests require an explicitly approved environment. Use
disposable fixtures; do not alter an existing tester's accounts to run checks.

## Evidence

Use the [task](../templates/task.md) and [review](../templates/review.md) templates.
Record exact commands, snapshot/environment, outcome, and limits. Static source
inspection, Markdown checks, mock tests, integration tests, and human acceptance
are different evidence. See [guard statuses](guards.md) for what exists today.
