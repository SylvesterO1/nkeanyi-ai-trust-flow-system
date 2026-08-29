#!/usr/bin/env bash

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "Starting N-AITFS services from: $ROOT_DIR"
echo

start_service() {
  SERVICE_NAME="$1"
  SERVICE_PATH="$ROOT_DIR/services/$SERVICE_NAME"

  echo "Starting $SERVICE_NAME ..."

  if [ ! -d "$SERVICE_PATH" ]; then
    echo "ERROR: $SERVICE_PATH does not exist"
    exit 1
  fi

  osascript <<APPLESCRIPT
tell application "Terminal"
    do script "cd '$SERVICE_PATH' && echo 'Starting $SERVICE_NAME' && mvn spring-boot:run"
end tell
APPLESCRIPT

  sleep 2
}

start_service "document-intelligence-service"
start_service "knowledge-service"
start_service "payment-orchestration-service"
start_service "compliance-evidence-service"
start_service "api-gateway"

echo
echo "All N-AITFS services are starting in separate Terminal windows."
echo "Wait until each one shows Spring Boot started successfully."
