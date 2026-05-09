#!/usr/bin/env bash
set -e

GATEWAY_URL="http://localhost:8080"
EVIDENCE_PATH="/api/v1/evidence/correlation/corr-demo-001"

echo "======================================"
echo "N-AITFS MVP 5 Smoke Test"
echo "======================================"

echo
echo "1. Gateway health should return 200"
curl -i "$GATEWAY_URL/actuator/health"

echo
echo
echo "2. Protected evidence endpoint without tenant should return 400"
curl -i "$GATEWAY_URL$EVIDENCE_PATH"

echo
echo
echo "3. Protected evidence endpoint with tenant but no token should return 401 if JWT security is active"
curl -i \
  -H "X-Tenant-Id: tenant-demo-001" \
  -H "X-Correlation-Id: corr-demo-001" \
  "$GATEWAY_URL$EVIDENCE_PATH"

echo
echo
echo "Smoke test completed."
