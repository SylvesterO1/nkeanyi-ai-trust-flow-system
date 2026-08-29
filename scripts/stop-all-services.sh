#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT_DIR/.pids"
SERVICES=(
  "api-gateway"
  "payment-orchestration-service"
  "compliance-evidence-service"
  "knowledge-service"
  "document-intelligence-service"
)

stop_pid() {
  local name="$1"
  local pid="$2"

  if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
    echo "Stopping $name with PID $pid"
    kill "$pid" || true
    for _ in {1..20}; do
      if ! kill -0 "$pid" 2>/dev/null; then
        break
      fi
      sleep 0.5
    done

    if kill -0 "$pid" 2>/dev/null; then
      echo "$name did not stop after 10 seconds; leaving PID $pid running"
    fi
  else
    echo "$name is not running"
  fi
}

echo "Stopping N-AITFS Spring Boot services..."

if [ -d "$PID_DIR" ]; then
  for pidfile in "$PID_DIR"/*.pid; do
    [ -e "$pidfile" ] || continue
    PID="$(cat "$pidfile" || true)"
    NAME="$(basename "$pidfile" .pid)"

    stop_pid "$NAME" "$PID"

    rm -f "$pidfile"
  done
fi

echo ""
echo "Checking for N-AITFS Maven processes without PID files..."
for service in "${SERVICES[@]}"; do
  pattern="maven.multiModuleProjectDirectory=$ROOT_DIR .* -pl services/$service spring-boot:run"
  while IFS= read -r pid; do
    [ -n "$pid" ] || continue
    stop_pid "$service" "$pid"
  done < <(pgrep -f "$pattern" || true)
done

echo ""
echo "Remaining Maven Spring Boot processes, if any:"
ps aux | grep "spring-boot:run" | grep -v grep || true

echo ""
echo "Done."
