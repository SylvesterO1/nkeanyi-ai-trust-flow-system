#!/usr/bin/env bash

echo "=========================================="
echo " N-AITFS Service Health Check"
echo "=========================================="
echo

echo "API Gateway :8080"
curl -s http://localhost:8080/actuator/health | jq
echo

echo "Document Intelligence :8081"
curl -s http://localhost:8081/actuator/health | jq
echo

echo "Knowledge Service :8082"
curl -s http://localhost:8082/actuator/health | jq
echo

echo "Payment Orchestration :8083"
curl -s http://localhost:8083/actuator/health | jq
echo

echo "Compliance Evidence :8084"
curl -s http://localhost:8084/actuator/health | jq
echo
