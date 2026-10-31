import tempfile
import unittest
from pathlib import Path
from workspace import Workspace, AccessDenied

class ReadPolicyTests(unittest.TestCase):
    def setUp(self):
        t = tempfile.TemporaryDirectory()
        self.addCleanup(t.cleanup)
        self.root = Path(t.name)
        (self.root / "README.md").write_text("hello\nworld\n")
        self.workspace = Workspace(self.root)

    def test_file_listing(self):
        self.assertIn("README.md", self.workspace.list_files()["files"])

    def test_line_evidence(self):
        result = self.workspace.read_file("README.md", 2, 1)
        self.assertEqual(result["excerpt"], "2: world")
        self.assertEqual(len(result["sha256"]), 64)

    def test_path_traversal(self):
        for path in ("/etc/passwd", "../secret.md"):
            with self.assertRaises(AccessDenied):
                self.workspace.read_file(path)

    def test_secret_file(self):
        (self.root / ".env").write_text("API_KEY=secret")
        self.assertNotIn(".env", self.workspace.list_files()["files"])
        with self.assertRaises(AccessDenied):
            self.workspace.read_file(".env")

    def test_inline_secret(self):
        (self.root / "config.yml").write_text("password: secret-value\n")
        with self.assertRaises(AccessDenied):
            self.workspace.read_file("config.yml")

    def test_symlink(self):
        (self.root / "alias.md").symlink_to(self.root / "README.md")
        with self.assertRaises(AccessDenied):
            self.workspace.read_file("alias.md")

    def test_limits(self):
        with self.assertRaises(AccessDenied):
            self.workspace.list_files(300)
        with self.assertRaises(AccessDenied):
            self.workspace.read_file("README.md", max_lines=200)

if __name__ == "__main__":
    unittest.main()
