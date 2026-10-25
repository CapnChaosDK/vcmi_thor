#!/usr/bin/env python3
"""Reject engine divergence outside the documented second-screen integration seam."""
import json
from pathlib import Path
import subprocess


def unexpected_changes(root, policy):
    base = policy["upstream_commit"]
    subprocess.run(["git", "cat-file", "-e", base + "^{commit}"], cwd=root, check=True)
    paths = policy["protected_paths"]
    changed = subprocess.check_output(
        ["git", "diff", "--name-only", "--no-renames", "--ignore-submodules=all", base, "--", *paths],
        cwd=root, text=True).splitlines()
    changed += subprocess.check_output(
        ["git", "ls-files", "--others", "--exclude-standard", "--", *paths],
        cwd=root, text=True).splitlines()
    return sorted({path for path in changed
                   if path not in policy["allowed_files"]
                   and not any(path.startswith(prefix) for prefix in policy["allowed_prefixes"])})


def main():
    root = Path(__file__).resolve().parents[1]
    policy = json.loads((root / "CI/thor_upstream_base.json").read_text())
    unexpected = unexpected_changes(root, policy)
    if unexpected:
        raise SystemExit("Changes outside the Thor integration seam:\n" + "\n".join(unexpected))
    print("Protected engine, server, packet, serializer, AI and controller sources match upstream "
          + policy["upstream_commit"] + "; only documented Thor seams are allowed.")


if __name__ == "__main__":
    main()
