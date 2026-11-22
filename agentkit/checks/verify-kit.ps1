[CmdletBinding()]
param(
    [string]$RootPath
)

# Read-only documentation integrity check. Does not run application/toolchain code.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($RootPath)) {
    $scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
    $RootPath = Join-Path $scriptDirectory '../..'
}

$repoRoot = (Resolve-Path -LiteralPath $RootPath).Path
$rootPrefix = $repoRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
$kitDirectory = Join-Path $repoRoot 'agentkit'
$failures = [Collections.Generic.List[string]]::new()

$requiredPaths = @(
    'AGENTS.md',
    'agentkit/README.md',
    'agentkit/context/backend.md',
    'agentkit/context/known-issues.md',
    'agentkit/context/admin-portal.md',
    'agentkit/rules/working-agreements.md',
    'agentkit/rules/backend.md',
    'agentkit/agents/analyst.md',
    'agentkit/agents/backend-builder.md',
    'agentkit/agents/reviewer.md',
    'agentkit/workflows/development.md',
    'agentkit/templates/task.md',
    'agentkit/templates/review.md',
    'agentkit/templates/decision.md',
    'agentkit/decisions/README.md',
    'agentkit/decisions/ADR-0001-repository-and-agentkit.md',
    'agentkit/checks/README.md',
    'agentkit/checks/guards.md',
    'agentkit/checks/verify-kit.ps1',
    'agentkit/checks/test_verify_kit.py',
    'agentkit/tasks/TASK-0001-agentkit-foundation.md'
)

foreach ($relativePath in $requiredPaths) {
    if (-not (Test-Path -LiteralPath (Join-Path $repoRoot $relativePath) -PathType Leaf)) {
        $failures.Add("Missing required file: $relativePath")
    }
}

$documents = @()
if (Test-Path -LiteralPath $kitDirectory -PathType Container) {
    $documents += @(Get-ChildItem -LiteralPath $kitDirectory -Filter '*.md' -Recurse -File)
}
$entryPath = Join-Path $repoRoot 'AGENTS.md'
if (Test-Path -LiteralPath $entryPath -PathType Leaf) {
    $documents += Get-Item -LiteralPath $entryPath
}

foreach ($document in $documents) {
    $relativeName = $document.FullName.Substring($rootPrefix.Length)
    $content = Get-Content -LiteralPath $document.FullName -Raw -Encoding UTF8
    if ([string]::IsNullOrWhiteSpace($content)) {
        $failures.Add("Empty document: $relativeName")
        continue
    }

    if ([regex]::Matches($content, '(?m)^\s*```').Count % 2 -ne 0) {
        $failures.Add("Unbalanced code fences: $relativeName")
    }

    $links = [regex]::Matches($content, '(?<!!)\[[^\]\r\n]+\]\((?<target>[^\s)]+)\)')
    foreach ($link in $links) {
        $target = $link.Groups['target'].Value
        if ($target -match '^[a-zA-Z][a-zA-Z0-9+.-]*:' -or $target.StartsWith('#')) {
            continue
        }
        $localTarget = [Uri]::UnescapeDataString(($target -split '#', 2)[0])
        $candidatePath = [IO.Path]::GetFullPath((Join-Path $document.DirectoryName $localTarget))
        $insideRepo = $candidatePath.Equals($repoRoot, [StringComparison]::OrdinalIgnoreCase) -or
            $candidatePath.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)
        if (-not $insideRepo) {
            $failures.Add("Local link leaves repository: $relativeName -> $target")
        } elseif (-not (Test-Path -LiteralPath $candidatePath)) {
            $failures.Add("Broken local link: $relativeName -> $target")
        }
    }
}

foreach ($role in @('analyst', 'backend-builder', 'reviewer')) {
    $rolePath = Join-Path $kitDirectory "agents/$role.md"
    if (Test-Path -LiteralPath $rolePath -PathType Leaf) {
        $content = Get-Content -LiteralPath $rolePath -Raw -Encoding UTF8
        foreach ($heading in @('Scope', 'Inputs', 'Boundaries', 'Output')) {
            if ($content -notmatch ('(?m)^## ' + [regex]::Escape($heading) + '\s*$')) {
                $failures.Add("Role $role is missing heading: $heading")
            }
        }
    }
}

$guardIds = [Collections.Generic.HashSet[string]]::new()
$guardPath = Join-Path $kitDirectory 'checks/guards.md'
if (Test-Path -LiteralPath $guardPath -PathType Leaf) {
    $allowedStatuses = @('Executable', 'Existing tests', 'Partially covered', 'Manual convention', 'Planned')
    $rows = @(Get-Content -LiteralPath $guardPath -Encoding UTF8 |
        Where-Object { $_ -match '^\|\s*[A-Z]+-\d{3}\s*\|' })
    foreach ($row in $rows) {
        $fields = $row.Split('|')
        if ($fields.Count -ne 6) {
            $failures.Add("Malformed guard row: $row")
            continue
        }
        $guardId = $fields[1].Trim()
        if (-not $guardIds.Add($guardId)) {
            $failures.Add("Duplicate guard ID: $guardId")
        }
        if ($allowedStatuses -notcontains $fields[4].Trim()) {
            $failures.Add("Unknown guard status for ${guardId}: $($fields[4].Trim())")
        }
    }
    if (-not $guardIds.Contains('KIT-001')) {
        $failures.Add('Guard register is missing KIT-001.')
    }
}

$backendRules = Join-Path $kitDirectory 'rules/backend.md'
if (Test-Path -LiteralPath $backendRules -PathType Leaf) {
    $content = Get-Content -LiteralPath $backendRules -Raw -Encoding UTF8
    foreach ($rule in [regex]::Matches($content, '(?m)^## ([A-Z]+-\d{3}):')) {
        $ruleId = $rule.Groups[1].Value
        if (-not $guardIds.Contains($ruleId)) {
            $failures.Add("Backend rule has no guard record: $ruleId")
        }
    }
}

if ($failures.Count -gt 0) {
    foreach ($failure in $failures) {
        Write-Output "FAIL: $failure"
    }
    exit 1
}

Write-Output "PASS: Agent Kit structure ($($documents.Count) Markdown files, $($guardIds.Count) guard records)."
Write-Output 'Local file links checked; Markdown anchors and runtime behaviour are not evaluated.'
Write-Output 'Backend tests, services, database scripts, CI, and deployments were not run.'
exit 0
