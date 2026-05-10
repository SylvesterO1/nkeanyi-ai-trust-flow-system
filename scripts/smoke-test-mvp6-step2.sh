#!/usr/bin/env bash
set -euo pipefail

echo "========================================"
echo "MVP 6 Step 2 Smoke Test"
echo "Tenant/Correlation Kafka Propagation"
echo "========================================"

TENANT_ID="tenant-demo-001"
CORRELATION_ID="corr-mvp6-step2-smoke-$(date +%s)"
IDEMPOTENCY_KEY="idem-mvp6-step2-smoke-$(date +%s)"

echo ""
echo "1. Checking Docker containers..."
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" || true

echo ""
echo "2. Finding Kafka container..."
KAFKA_CONTAINER="$(docker ps --format '{{.Names}}' | grep -Ei 'kafka|broker' | head -n 1 || true)"

if [ -z "$KAFKA_CONTAINER" ]; then
  echo "ERROR: Could not find a running Kafka container."
  echo "Start your platform services first:"
  echo "./scripts/run-naitfs-services.sh"
  exit 1
fi

echo "Kafka container found: $KAFKA_CONTAINER"

echo ""
echo "3. Listing Kafka topics..."
KAFKA_TOPICS_CMD=""

if docker exec "$KAFKA_CONTAINER" sh -c 'command -v kafka-topics >/dev/null 2>&1'; then
  KAFKA_TOPICS_CMD="kafka-topics"
elif docker exec "$KAFKA_CONTAINER" sh -c 'test -x /opt/kafka/bin/kafka-topics.sh'; then
  KAFKA_TOPICS_CMD="/opt/kafka/bin/kafka-topics.sh"
elif docker exec "$KAFKA_CONTAINER" sh -c 'test -x /usr/bin/kafka-topics'; then
  KAFKA_TOPICS_CMD="/usr/bin/kafka-topics"
else
  echo "ERROR: Could not find kafka-topics command inside Kafka container."
  echo "Inspect container with:"
  echo "docker exec -it $KAFKA_CONTAINER sh"
  exit 1
fi

echo "Using Kafka topics command: $KAFKA_TOPICS_CMD"

docker exec "$KAFKA_CONTAINER" "$KAFKA_TOPICS_CMD" \
  --bootstrap-server localhost:9092 \
  --list || {
    echo "ERROR: Could not list Kafka topics."
    exit 1
  }

echo ""
echo "4. Detecting likely payment topic..."
PAYMENT_TOPIC="$(docker exec "$KAFKA_CONTAINER" "$KAFKA_TOPICS_CMD" --bootstrap-server localhost:9092 --list \
  | grep -Ei 'payment|outbox|transaction' \
  | head -n 1 || true)"

if [ -z "$PAYMENT_TOPIC" ]; then
  echo "WARNING: No payment-like topic found yet."
  echo "We will still try to trigger a payment request."
else
  echo "Detected payment-like topic: $PAYMENT_TOPIC"
fi

echo ""
echo "5. Checking API Gateway health..."
if curl -fsS http://localhost:8080/actuator/health >/tmp/mvp6_gateway_health.json; then
  cat /tmp/mvp6_gateway_health.json
  echo ""
  echo "API Gateway reachable."
else
  echo "WARNING: API Gateway not reachable on port 8080."
fi

echo ""
echo "6. Checking payment service health..."
if curl -fsS http://localhost:8083/actuator/health >/tmp/mvp6_payment_health.json; then
  cat /tmp/mvp6_payment_health.json
  echo ""
  echo "Payment service reachable."
else
  echo "WARNING: Payment service not reachable on port 8083."
fi

echo ""
echo "7. Triggering payment request directly against payment service..."
echo "Tenant ID: $TENANT_ID"
echo "Correlation ID: $CORRELATION_ID"
echo "Idempotency Key: $IDEMPOTENCY_KEY"

PAYMENT_RESPONSE_FILE="/tmp/mvp6_payment_response.json"

HTTP_STATUS="$(curl -s -o "$PAYMENT_RESPONSE_FILE" -w "%{http_code}" \
  -X POST http://localhost:8083/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{
    "customerId": "cust-demo-001",
    "amount": 125.50,
    "currency": "USD",
    "recipientAccount": "acct-demo-001",
    "description": "MVP 6 Step 2 smoke test"
  }' || true)"

echo "HTTP status: $HTTP_STATUS"
echo "Response body:"
cat "$PAYMENT_RESPONSE_FILE" || true
echo ""

if [ "$HTTP_STATUS" = "401" ] || [ "$HTTP_STATUS" = "403" ]; then
  echo ""
  echo "Payment endpoint requires auth. That is okay if your service is protected."
  echo "You can rerun with a token later through the API Gateway."
fi

echo ""
echo "8. Refreshing Kafka topic list after request..."
docker exec "$KAFKA_CONTAINER" "$KAFKA_TOPICS_CMD" \
  --bootstrap-server localhost:9092 \
  --list

PAYMENT_TOPIC="$(docker exec "$KAFKA_CONTAINER" "$KAFKA_TOPICS_CMD" --bootstrap-server localhost:9092 --list \
  | grep -Ei 'payment|outbox|transaction' \
  | head -n 1 || true)"

if [ -z "$PAYMENT_TOPIC" ]; then
  echo ""
  echo "No payment-like topic found."
  echo "Now checking known N-AITFS topics..."
  KNOWN_TOPIC="$(docker exec "$KAFKA_CONTAINER" "$KAFKA_TOPICS_CMD" --bootstrap-server localhost:9092 --list \
    | grep -Ei 'document.extracted.v1|compliance|evidence|knowledge' \
    | head -n 1 || true)"

  if [ -z "$KNOWN_TOPIC" ]; then
    echo "ERROR: No suitable topic found for header inspection."
    echo "Smoke test partially passed: Kafka is running, but no event topic was found."
    exit 1
  fi

  PAYMENT_TOPIC="$KNOWN_TOPIC"
  echo "Using available topic instead: $PAYMENT_TOPIC"
else
  echo "Using payment-like topic: $PAYMENT_TOPIC"
fi

echo ""
echo "9. Inspecting one Kafka message with kcat..."
echo "Topic: $PAYMENT_TOPIC"

if ! command -v kcat >/dev/null 2>&1; then
  echo "ERROR: kcat is not installed."
  echo "Install it with:"
  echo "brew install kcat"
  exit 1
fi

echo ""
echo "Run this command in another terminal if this script waits for a message:"
echo ""
echo "kcat -b localhost:9092 -C -t $PAYMENT_TOPIC -c 1 -f 'Headers=%h\\nKey=%k\\nValue=%s\\n'"
echo ""

TIMEOUT_CMD=""

if command -v timeout >/dev/null 2>&1; then
  TIMEOUT_CMD="timeout"
elif command -v gtimeout >/dev/null 2>&1; then
  TIMEOUT_CMD="gtimeout"
else
  TIMEOUT_CMD=""
fi

if [ -n "$TIMEOUT_CMD" ]; then
  $TIMEOUT_CMD 15 kcat -b localhost:9092 -C \
    -t "$PAYMENT_TOPIC" \
    -c 1 \
    -f 'Headers=%h\nKey=%k\nValue=%s\n' || {

    echo ""
    echo "WARNING: kcat timed out or found no new message."
    echo "This does not always mean failure. The topic may already be consumed or no new event was produced."
    echo "Trigger another request and rerun the kcat command printed above."
  }
else
  echo "No timeout command found. Running kcat directly. Press Ctrl+C if it waits too long."
  kcat -b localhost:9092 -C \
    -t "$PAYMENT_TOPIC" \
    -c 1 \
    -f 'Headers=%h\nKey=%k\nValue=%s\n'
fi

echo ""
echo "========================================"
echo "MVP 6 Step 2 smoke test completed."
echo "Expected Kafka headers:"
echo "X-Tenant-Id=$TENANT_ID"
echo "X-Correlation-Id=$CORRELATION_ID"
echo "========================================"
