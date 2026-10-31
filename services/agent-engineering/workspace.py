"""Read-only repository evidence access. Local prototype, not a sandbox."""
from __future__ import annotations
import hashlib
import os
from pathlib import Path
import re

SUFFIXES = {".java", ".py", ".kt", ".js", ".jsx", ".ts", ".tsx",
            ".xml", ".json", ".yaml", ".yml", ".md", ".txt",
            ".properties", ".toml", ".gradle", ".sql", ".sh", ".tf"}
SPECIAL = {"Dockerfile", "Makefile", ".gitignore", "Jenkinsfile"}
BLOCK_DIRS = {".git", ".idea", ".venv", "venv", "node_modules", "dist", "build",
              "target", "__pycache__", ".terraform", ".aws", ".ssh", ".kube",
              "secrets", "credentials", "private", ".gradle", ".m2"}
BLOCK_NAMES = {"id_rsa", "id_ed25519", "credentials", "credentials.json",
               "application-local.properties", "application-prod.yml",
               "application-prod.properties", "terraform.tfstate", "token"}
BLOCK_SUFFIXES = {".pem", ".key", ".p12", ".pfx", ".jks", ".db", ".env"}
SECRET = re.compile(r"-----BEGIN .*PRIVATE KEY-----|(?im:^\s*(?:password|client_secret|api[_-]?key|access[_-]?token)\s*[:=]\s*(?!\$\{|\$\(|#|\*|null\b)\S+)")

class AccessDenied(ValueError):
    pass

class Workspace:
    def __init__(self, root):
        self.root = Path(root).expanduser().resolve(strict=True)
        if not self.root.is_dir():
            raise AccessDenied("Root must be a directory")

    @staticmethod
    def allowed(path):
        if path.name.lower().startswith(".env") or path.name.lower() in BLOCK_NAMES:
            return False
        if any(p.lower() in BLOCK_DIRS for p in path.parts[:-1]):
            return False
        if path.suffix.lower() in BLOCK_SUFFIXES:
            return False
        return path.suffix.lower() in SUFFIXES or path.name in SPECIAL

    def checked(self, name):
        rel = Path(name)
        if rel.is_absolute() or not rel.parts or ".." in rel.parts or not self.allowed(rel):
            raise AccessDenied("Disallowed relative path")
        cur = self.root
        for part in rel.parts:
            cur /= part
            if cur.is_symlink():
                raise AccessDenied("Symlink access denied")
        path = cur.resolve(strict=True)
        if not path.is_relative_to(self.root) or not path.is_file() or path.stat().st_size > 131072:
            raise AccessDenied("File outside policy")
        return path

    def list_files(self, limit=100):
        if not 1 <= limit <= 250:
            raise AccessDenied("Invalid limit")
        found = []
        for root, dirs, files in os.walk(self.root, followlinks=False):
            dirs[:] = sorted(d for d in dirs if d.lower() not in BLOCK_DIRS
                             and not (Path(root) / d).is_symlink())
            for name in sorted(files):
                path = Path(root) / name
                rel = path.relative_to(self.root)
                if not path.is_symlink() and self.allowed(rel):
                    try:
                        if path.stat().st_size <= 131072:
                            found.append(rel.as_posix())
                    except OSError:
                        continue
                if len(found) >= limit:
                    return {"files": found, "truncated": True}
        return {"files": found, "truncated": False}

    def read_file(self, name, start_line=1, max_lines=60):
        if start_line < 1 or not 1 <= max_lines <= 120:
            raise AccessDenied("Invalid line bounds")
        raw = self.checked(name).read_bytes()
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError as exc:
            raise AccessDenied("Non UTF-8") from exc
        if "\x00" in text or SECRET.search(text):
            raise AccessDenied("Potential secret-bearing content")
        lines = text.splitlines()
        selected = lines[start_line-1:start_line-1+max_lines]
        excerpt = "\n".join(f"{i}: {line}" for i, line in enumerate(selected, start_line))
        return {"path": name, "start_line": start_line,
                "end_line": min(len(lines), start_line+max_lines-1),
                "sha256": hashlib.sha256(raw).hexdigest(),
                "excerpt": excerpt[:12000]}

def from_env():
    root = os.getenv("NAITFAIS_REPO_ROOT")
    if not root:
        raise AccessDenied("NAITFAIS_REPO_ROOT must be explicitly set")
    return Workspace(root)
