#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

rm -f .token scripts/.token web/trustflow-console/.env.local jobinfo1.pdf
rm -rf .pids web/trustflow-console/dist

echo "Public-safe cleanup complete. Review git diff before committing."
