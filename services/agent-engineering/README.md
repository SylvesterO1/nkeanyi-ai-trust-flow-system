# NAITFAIS — Agent Engineering P0

A separate Python, read-only repository-inspection prototype inside the existing
Java/Spring Boot monorepo. It is **not** added to the Maven reactor, routed
through Spring Cloud Gateway, or deployed to production.

## Local setup

```bash
cd services/agent-engineering
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python -m unittest discover -s tests -v
export NAITFAIS_REPO_ROOT="$(cd ../.. && pwd)"
mcp dev mcp_server.py
```

The MCP Inspector requires Node.js and npm. Inspect read-only tools
`list_repository_files` and `read_repository_file` on an approved checkout.

## Engineering decisions

- Repository code is untrusted content, not agent instructions.
- Tools do not execute commands, change files, or initiate payments.
- A fixed root is required, with path traversal and symlinks rejected.
- Source excerpts include file line numbers and a SHA-256 digest.
- The read filter is defense in depth, NOT an OS sandbox; run untrusted
  repositories in isolated containers without credentials and with restricted egress.
- The code is a local P0 prototype. It has not been integrated with
  Keycloak, trusted tenant-to-workspace mapping, or compliance evidence storage.
- Do not expose the MCP server directly as an internet-facing endpoint.

## Next milestone: P0.2

Add a dedicated authenticated Java integration API via Gateway, check
`SCOPE_engineering.read` / `SCOPE_engineering.write`, enforce tenant membership
from a validated JWT, resolve workspace identifiers only on the server, and
write sanitized assessment records to the existing compliance evidence service.
Gateway tenant/correlation headers alone do not prove tenant ownership.
