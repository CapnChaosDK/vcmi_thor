import subprocess
import tempfile
import unittest
from pathlib import Path

from thor_upstream_base import unexpected_changes


class UpstreamScopeTest(unittest.TestCase):
    def test_detects_committed_modified_and_untracked_engine_changes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            def git(*args):
                return subprocess.check_output(["git", *args], cwd=root, text=True).strip()
            git("init", "-q")
            git("config", "user.name", "Scope test")
            git("config", "user.email", "scope@example.invalid")
            (root / "server").mkdir()
            (root / "lib/thor").mkdir(parents=True)
            (root / "server/rules.cpp").write_text("upstream\n")
            (root / "lib/bridge.cpp").write_text("bridge\n")
            git("add", ".")
            git("commit", "-qm", "upstream")
            policy = {"upstream_commit": git("rev-parse", "HEAD"),
                      "protected_paths": ["server", "lib"],
                      "allowed_files": {"lib/bridge.cpp": "JNI"},
                      "allowed_prefixes": {"lib/thor/": "snapshots"}}
            (root / "lib/bridge.cpp").write_text("changed bridge\n")
            (root / "lib/thor/view.cpp").write_text("new view\n")
            self.assertEqual(unexpected_changes(root, policy), [])
            (root / "server/rules.cpp").write_text("fork rules\n")
            (root / "server/custom.cpp").write_text("new packet\n")
            self.assertEqual(unexpected_changes(root, policy), ["server/custom.cpp", "server/rules.cpp"])
            git("add", ".")
            git("commit", "-qm", "divergence")
            self.assertEqual(unexpected_changes(root, policy), ["server/custom.cpp", "server/rules.cpp"])


if __name__ == "__main__":
    unittest.main()
