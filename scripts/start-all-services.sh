#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

LOG_DIR="$ROOT_DIR/logs"
PID_DIR="$ROOT_DIR/.pids"

mkdir -p "$LOG_DIR" "$PID_DIR"

echo "=================================================="
echo " Starting N-AITFS services"
echo " Root: $ROOT_DIR"
echo " Logs: $LOG_DIR"
echo " PIDs: $PID_DIR"
echo "=================================================="

if [ ! -f "$ROOT_DIR/pom.xml" ]; then
  echo "ERROR: pom.xml not found at $ROOT_DIR"
  exit 1
fi

echo ""
"$ROOT_DIR/scripts/stop-all-services.sh"

echo ""
echo "Starting Docker infrastructure only..."

if [ -f "$ROOT_DIR/platform/docker/docker-compose.naitfs.yml" ]; then
  docker compose -f "$ROOT_DIR/platform/docker/docker-compose.naitfs.yml" up -d
else
  echo "ERROR: docker-compose file not found:"
  echo "$ROOT_DIR/platform/docker/docker-compose.naitfs.yml"
  exit 1
fi

echo ""
echo "Waiting for Docker infrastructure..."
docker compose -f "$ROOT_DIR/platform/docker/docker-compose.naitfs.yml" ps
sleep 15

start_service() {
  local name="$1"
  local module="$2"
  local port="$3"
  local log_file="$LOG_DIR/${name}.log"
  local pid_file="$PID_DIR/${name}.pid"

  echo ""
  echo "Starting $name..."
  echo "Module: $module"
  echo "Log: $log_file"

  if [ ! -d "$ROOT_DIR/$module" ]; then
    echo "ERROR: Module folder not found: $ROOT_DIR/$module"
    exit 1
  fi

  if lsof -tiTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "ERROR: Port $port is already in use before starting $name."
    echo "Run ./scripts/stop-all-services.sh, then check: lsof -nP -iTCP:$port -sTCP:LISTEN"
    exit 1
  fi

  nohup mvn -pl "$module" spring-boot:run -Dspring-boot.run.fork=false > "$log_file" 2>&1 &

  local pid=$!
  echo "$pid" > "$pid_file"
  disown "$pid" 2>/dev/null || true

  echo "$name started with PID $pid"
  sleep 10
}

start_service "document-intelligence-service" "services/document-intelligence-service" "8089"
start_service "knowledge-service" "services/knowledge-service" "8088"
start_service "payment-orchestration-service" "services/payment-orchestration-service" "8082"
start_service "compliance-evidence-service" "services/compliance-evidence-service" "8084"
start_service "api-gateway" "services/api-gateway" "8080"

echo ""
echo "=================================================="
echo " All N-AITFS services started once."
echo "=================================================="
echo ""
echo "Verify:"
echo "  ./scripts/check-health.sh"
echo ""
echo "Logs:"
echo "  tail -f logs/api-gateway.log"
echo "  tail -f logs/payment-orchestration-service.log"
echo ""
