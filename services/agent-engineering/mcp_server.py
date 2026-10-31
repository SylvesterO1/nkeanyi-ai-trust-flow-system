"""NAITFAIS local read-only MCP server."""
from mcp.server.fastmcp import FastMCP
from workspace import from_env

mcp = FastMCP("NAITFAIS Repository Evidence")

@mcp.tool()
def list_repository_files(limit: int = 100) -> dict:
    """List approved source files without secret files or build outputs."""
    return from_env().list_files(limit)

@mcp.tool()
def read_repository_file(path: str, start_line: int = 1, max_lines: int = 60) -> dict:
    """Return bounded, line-numbered file evidence with SHA-256."""
    return from_env().read_file(path, start_line, max_lines)

@mcp.resource("naitfais://capabilities")
def capabilities() -> str:
    return "Read-only repo list and read tools; no shell, writes, payments, or deployment."

if __name__ == "__main__":
    mcp.run()
