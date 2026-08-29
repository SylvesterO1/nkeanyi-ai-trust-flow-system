#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/platform/docker/docker-compose.naitfs.yml"

echo "============================================================"
echo "Starting NAITFS Platform"
echo "============================================================"
echo "Root directory: $ROOT_DIR"

cd "$ROOT_DIR"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "ERROR: Docker compose file not found:"
  echo "$COMPOSE_FILE"
  echo
  echo "Make sure you are inside the NAITFS project root."
  exit 1
fi

wait_for_tcp() {
  local name="$1"
  local host="$2"
  local port="$3"
  local max_attempts="${4:-60}"

  echo "Waiting for $name at $host:$port..."

  for attempt in $(seq 1 "$max_attempts"); do
    if nc -z "$host" "$port" >/dev/null 2>&1; then
      echo "$name is reachable."
      return 0
    fi

    sleep 2
  done

  echo "ERROR: $name did not become reachable at $host:$port"
  exit 1
}

wait_for_http() {
  local name="$1"
  local url="$2"
  local max_attempts="${3:-60}"

  echo "Waiting for $name at $url..."

  for attempt in $(seq 1 "$max_attempts"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "$name is reachable."
      return 0
    fi

    sleep 2
  done

  echo "ERROR: $name did not become reachable at $url"
  exit 1
}

wait_for_health() {
  local name="$1"
  local url="$2"
  local max_attempts="${3:-90}"

  echo "Waiting for $name health at $url..."

  for attempt in $(seq 1 "$max_attempts"); do
    body="$(curl -fsS "$url" 2>/dev/null || true)"

    if echo "$body" | grep -q '"status":"UP"'; then
      echo "$name is UP."
      return 0
    fi

    sleep 2
  done

  echo "ERROR: $name did not report UP"
  echo "Last response:"
  curl -s "$url" || true
  echo
  exit 1
}

echo
echo "Starting Docker infrastructure..."
docker compose -f "$COMPOSE_FILE" up -d

echo
echo "Waiting for infrastructure..."

wait_for_tcp "Postgres" "localhost" "5432"
wait_for_tcp "Kafka" "localhost" "9092"
wait_for_http "Keycloak" "http://localhost:8081/realms/master"
wait_for_http "Qdrant" "http://localhost:6333/healthz"
wait_for_http "MinIO" "http://localhost:9000/minio/health/live"
wait_for_http "Jaeger" "http://localhost:16686"

echo
echo "Running Keycloak demo repair/setup if available..."

if [[ -x "$ROOT_DIR/scripts/repair-keycloak-demo.sh" ]]; then
  "$ROOT_DIR/scripts/repair-keycloak-demo.sh"
elif [[ -x "$ROOT_DIR/scripts/setup-keycloak-payment-platform.sh" ]]; then
  "$ROOT_DIR/scripts/setup-keycloak-payment-platform.sh"
else
  echo "WARNING: No Keycloak repair/setup script found."
  echo "Continuing without Keycloak repair."
fi

echo
echo "Starting Spring Boot services..."

if [[ -x "$ROOT_DIR/scripts/start-all-services.sh" ]]; then
  "$ROOT_DIR/scripts/start-all-services.sh"
elif [[ -x "$ROOT_DIR/scripts/run-naitfs-services.sh" ]]; then
  "$ROOT_DIR/scripts/run-naitfs-services.sh"
else
  echo "ERROR: No service startup script found."
  echo "Expected one of:"
  echo "  - scripts/start-all-services.sh"
  echo "  - scripts/run-naitfs-services.sh"
  exit 1
fi

echo
echo "Waiting for Spring Boot services to become UP..."

wait_for_health "api-gateway" "http://localhost:8080/actuator/health"
wait_for_health "payment-orchestration-service" "http://localhost:8082/actuator/health"
wait_for_health "compliance-evidence-service" "http://localhost:8084/actuator/health"
wait_for_health "knowledge-service" "http://localhost:8088/actuator/health"
wait_for_health "document-intelligence-service" "http://localhost:8089/actuator/health"

cat <<URLS

============================================================
NAITFS SERVICE URLS
============================================================

API Gateway:                     http://localhost:8080
Payment Orchestration Service:   http://localhost:8082
Compliance Evidence Service:     http://localhost:8084
Knowledge Service:               http://localhost:8088
Document Intelligence Service:   http://localhost:8089

Keycloak:                        http://localhost:8081
Qdrant:                          http://localhost:6333
MinIO:                           http://localhost:9000
Jaeger:                          http://localhost:16686
Adminer:                         http://localhost:8086

============================================================
READY FOR DEMO
============================================================

URLS
