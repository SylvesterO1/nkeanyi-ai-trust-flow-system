#!/usr/bin/env bash

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
PID_DIR="$ROOT_DIR/.pids"

mkdir -p "$LOG_DIR" "$PID_DIR"

echo "=========================================="
echo " Starting N-AITFS services"
echo " Project root: $ROOT_DIR"
echo "=========================================="
echo

check_port() {
  PORT="$1"
  SERVICE="$2"

  if lsof -i :"$PORT" >/dev/null 2>&1; then
    echo "ERROR: Port $PORT is already in use. Cannot start $SERVICE."
    echo
    echo "Process using port $PORT:"
    lsof -i :"$PORT"
    echo
    echo "Fix: stop the process first, or run:"
    echo "kill -9 \$(lsof -ti :$PORT)"
    echo
    exit 1
  fi
}

start_service() {
  SERVICE_NAME="$1"
  PORT="$2"
  SERVICE_PATH="$ROOT_DIR/services/$SERVICE_NAME"
  LOG_FILE="$LOG_DIR/$SERVICE_NAME.log"
  PID_FILE="$PID_DIR/$SERVICE_NAME.pid"

  echo "Checking $SERVICE_NAME on port $PORT..."
  check_port "$PORT" "$SERVICE_NAME"

  if [ ! -d "$SERVICE_PATH" ]; then
    echo "ERROR: Service folder not found: $SERVICE_PATH"
    exit 1
  fi

  if [ ! -f "$SERVICE_PATH/pom.xml" ]; then
    echo "ERROR: No pom.xml found in: $SERVICE_PATH"
    exit 1
  fi

  echo "Starting $SERVICE_NAME..."
  echo "Log: $LOG_FILE"

  (
    cd "$SERVICE_PATH"
    mvn spring-boot:run
  ) > "$LOG_FILE" 2>&1 &

  PID=$!
  echo "$PID" > "$PID_FILE"

  echo "$SERVICE_NAME started with PID $PID"
  echo
  sleep 5
}

start_service "document-intelligence-service" "8081"
start_service "knowledge-service" "8082"
start_service "payment-orchestration-service" "8083"
start_service "compliance-evidence-service" "8084"
start_service "api-gateway" "8080"

echo "=========================================="
echo " All services were started."
echo "=========================================="
echo
echo "Check ports:"
echo "lsof -i :8080"
echo "lsof -i :8081"
echo "lsof -i :8082"
echo "lsof -i :8083"
echo "lsof -i :8084"
echo
echo "View logs:"
echo "tail -f logs/api-gateway.log"
echo "tail -f logs/document-intelligence-service.log"
echo "tail -f logs/knowledge-service.log"
echo "tail -f logs/payment-orchestration-service.log"
echo "tail -f logs/compliance-evidence-service.log"
