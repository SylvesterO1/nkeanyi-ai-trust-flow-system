#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
COLLECTION="$ROOT_DIR/postman/NAITFS-MVP12-Regulated-Trust-Flow.postman_collection.json"
ENVIRONMENT="$ROOT_DIR/postman/NAITFS-MVP12-local.postman_environment.json"
RESULT_JSON="$ROOT_DIR/target/mvp12-postman-results.json"
FIXTURE_FILE="$ROOT_DIR/target/mvp12-control.txt"

fail() {
  echo "FAIL: $1"
  exit 1
}

if ! command -v newman >/dev/null 2>&1; then
  fail "newman is not installed. Install it with: npm install -g newman"
fi

if ! command -v curl >/dev/null 2>&1; then
  fail "curl is required to check gateway reachability"
fi

mkdir -p "$ROOT_DIR/target"

cat > "$FIXTURE_FILE" <<'DOC'
MVP12 Regulated Trust Flow Control

Vendor activation requires sanctions screening before payment release.
Payment execution must preserve tenant, correlation, and idempotency metadata.
Compliance evidence must retain document, grounded knowledge answer, payment received, and payment completed events.
DOC

STATUS="$(curl -sS -o /dev/null -w "%{http_code}" "$GATEWAY_URL/actuator/health" || true)"
if [ "$STATUS" != "200" ]; then
  fail "API Gateway is not reachable at $GATEWAY_URL/actuator/health (HTTP $STATUS)"
fi

echo "Running NAITFS MVP12 Postman validation through $GATEWAY_URL"
newman run "$COLLECTION" \
  --environment "$ENVIRONMENT" \
  --working-dir "$ROOT_DIR" \
  --env-var "gatewayUrl=$GATEWAY_URL" \
  --reporters cli,json \
  --reporter-json-export "$RESULT_JSON"

echo ""
echo "MVP12 Postman validation passed"
echo "Collection: $COLLECTION"
echo "Environment: $ENVIRONMENT"
echo "Results: $RESULT_JSON"
echo "READY FOR EXTERNAL API DEMO"
