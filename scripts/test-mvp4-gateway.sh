#!/usr/bin/env bash

echo "=========================================="
echo " MVP 4 Gateway Smoke Test"
echo "=========================================="
echo

echo "1. API Gateway Health"
curl -i http://localhost:8080/actuator/health
echo
echo

echo "2. Gateway Routes"
curl -s http://localhost:8080/actuator/gateway/routes | jq
echo
echo

echo "3. Evidence through Gateway"
curl -i \
  -H "X-Correlation-Id: corr-demo-001" \
  -H "X-Tenant-Id: tenant-demo-001" \
  http://localhost:8080/api/v1/evidence/correlation/corr-demo-001
echo
echo

echo "4. Knowledge Answer through Gateway"
curl -i -X POST http://localhost:8080/api/v1/knowledge/answer \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: corr-mvp4-knowledge-001" \
  -H "X-Tenant-Id: tenant-demo-001" \
  -d '{
    "question": "What is the Nkeanyi AI Trust Flow System?"
  }'
echo
echo

echo "5. Payment through Gateway"
curl -i -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: idem-mvp4-001" \
  -H "X-Correlation-Id: corr-mvp4-payment-001" \
  -H "X-Tenant-Id: tenant-demo-001" \
  -d '{
    "tenantId": "tenant-demo-001",
    "customerId": "cust-demo-001",
    "amount": 125.50,
    "currency": "USD",
    "paymentMethod": "CARD",
    "description": "MVP 4 gateway payment test"
  }'
echo
echo

echo "=========================================="
echo " MVP 4 Smoke Test Complete"
echo "=========================================="
