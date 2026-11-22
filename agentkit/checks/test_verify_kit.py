"""Exercise KIT-001 with valid/invalid isolated fixtures; no app dependencies."""

from pathlib import Path
import re
import shutil
import subprocess
import tempfile


REPO = Path(__file__).resolve().parents[2]
CHECKER = REPO / "agentkit/checks/verify-kit.ps1"


def run_check(root):
    return subprocess.run(
        ["powershell", "-NoProfile", "-File", str(CHECKER), "-RootPath", str(root)],
        capture_output=True,
        text=True,
    )


def require_pass(root, label):
    result = run_check(root)
    if result.returncode != 0:
        raise AssertionError((label, result.stdout, result.stderr))
    print("PASS:", label)


def reject_edit(root, relative, transform, expected):
    path = root / relative
    original = path.read_bytes()
    try:
        if transform is None:
            path.unlink()
        else:
            path.write_text(transform(original.decode("utf-8")), encoding="utf-8")
        result = run_check(root)
        if result.returncode != 1 or expected not in result.stdout:
            raise AssertionError((expected, result.returncode, result.stdout, result.stderr))
        print("PASS: invalid fixture rejected -", expected)
    finally:
        path.write_bytes(original)


def main():
    require_pass(REPO, "repository kit")
    temp_base = Path(tempfile.gettempdir()).resolve()
    fixture = Path(tempfile.mkdtemp(prefix="mm-connect-agentkit-", dir=temp_base)).resolve()
    try:
        if fixture.parent != temp_base or not fixture.name.startswith("mm-connect-agentkit-"):
            raise RuntimeError("Unexpected temporary fixture path")
        shutil.copytree(REPO / "agentkit", fixture / "agentkit")
        shutil.copy2(REPO / "AGENTS.md", fixture / "AGENTS.md")
        # Placeholder linked application paths only: never copy credentials or app data.
        documents = list((fixture / "agentkit").rglob("*.md")) + [fixture / "AGENTS.md"]
        for document in documents:
            targets = re.findall(
                r"(?<!!)\[[^\]\r\n]+\]\(([^\s)]+)\)",
                document.read_text(encoding="utf-8"),
            )
            for target in targets:
                if re.match(r"^[A-Za-z][A-Za-z0-9+.-]*:", target) or target.startswith("#"):
                    continue
                destination = (document.parent / target.split("#", 1)[0]).resolve()
                destination.relative_to(fixture)
                if not destination.exists():
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    destination.write_text("Isolated fixture placeholder.\n", encoding="utf-8")
        require_pass(fixture, "valid isolated kit")
        reject_edit(fixture, "AGENTS.md", None, "Missing required file: AGENTS.md")
        reject_edit(fixture, "agentkit/context/backend.md",
                    lambda s: s + "\n[Broken](missing-context.md)\n", "Broken local link:")
        reject_edit(fixture, "agentkit/agents/reviewer.md",
                    lambda s: s.replace("## Boundaries", "## Other"),
                    "Role reviewer is missing heading: Boundaries")
        reject_edit(fixture, "agentkit/checks/guards.md",
                    lambda s: s.replace("| Executable |", "| UnverifiedMagic |"),
                    "Unknown guard status for KIT-001")
        reject_edit(fixture, "agentkit/checks/guards.md",
                    lambda s: s.replace("| DB-001 |", "| KIT-001 |"), "Duplicate guard ID: KIT-001")
        reject_edit(fixture, "agentkit/rules/backend.md",
                    lambda s: s + "\n## NEW-001: Missing register entry\n",
                    "Backend rule has no guard record: NEW-001")
        reject_edit(fixture, "agentkit/context/backend.md",
                    lambda s: s + "\n```\n", "Unbalanced code fences:")
    finally:
        # Only remove the explicitly verified, disposable fixture created above.
        if (fixture.parent != temp_base or not fixture.name.startswith("mm-connect-agentkit-")
                or fixture == REPO or REPO in fixture.parents):
            raise RuntimeError("Refusing to remove an unexpected fixture path")
        shutil.rmtree(fixture)
    print("Verified repository kit, one valid fixture, and seven invalid fixtures; no backend tests run.")


if __name__ == "__main__":
    main()
