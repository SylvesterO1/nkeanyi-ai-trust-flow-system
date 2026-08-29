#!/usr/bin/env bash
set -euo pipefail

check_service() {
  local name="$1"
  local url="$2"

  printf "%-38s %s ... " "$name" "$url"

  local response
  if ! response="$(curl -fsS --max-time 5 "$url" 2>/dev/null)"; then
    echo "DOWN"
    return 1
  fi

  if echo "$response" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    echo "UP"
    return 0
  fi

  echo "NOT UP: $response"
  return 1
}

failures=0

API_GATEWAY_URL="${API_GATEWAY_URL:-http://localhost:8080}"
PAYMENT_ORCHESTRATION_SERVICE_URL="${PAYMENT_ORCHESTRATION_SERVICE_URL:-http://localhost:8082}"
COMPLIANCE_EVIDENCE_SERVICE_URL="${COMPLIANCE_EVIDENCE_SERVICE_URL:-http://localhost:8084}"
KNOWLEDGE_SERVICE_URL="${KNOWLEDGE_SERVICE_URL:-http://localhost:8088}"
DOCUMENT_INTELLIGENCE_SERVICE_URL="${DOCUMENT_INTELLIGENCE_SERVICE_URL:-http://localhost:8089}"

check_service "api-gateway" "$API_GATEWAY_URL/actuator/health" || failures=$((failures + 1))
check_service "payment-orchestration-service" "$PAYMENT_ORCHESTRATION_SERVICE_URL/actuator/health" || failures=$((failures + 1))
check_service "compliance-evidence-service" "$COMPLIANCE_EVIDENCE_SERVICE_URL/actuator/health" || failures=$((failures + 1))
check_service "knowledge-service" "$KNOWLEDGE_SERVICE_URL/actuator/health" || failures=$((failures + 1))
check_service "document-intelligence-service" "$DOCUMENT_INTELLIGENCE_SERVICE_URL/actuator/health" || failures=$((failures + 1))

if [ "$failures" -ne 0 ]; then
  echo ""
  echo "$failures service health check(s) failed."
  exit 1
fi

echo ""
echo "All N-AITFS service health checks are UP."
