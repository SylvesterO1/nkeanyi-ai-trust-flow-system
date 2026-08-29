#!/usr/bin/env bash
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
REALM="${REALM:-payment-platform}"
CLIENT_ID="${CLIENT_ID:-naitfs-client}"
USERNAME="${USERNAME:-test-user}"
PASSWORD="${PASSWORD:-test-password}"
TOKEN_SCOPE="${TOKEN_SCOPE:-openid profile email documents.write knowledge.read payments.write payments.read evidence.read}"

TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  -d "grant_type=password" \
  -d "scope=$TOKEN_SCOPE")

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty')

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
  echo "ERROR: Failed to generate token."
  echo "$TOKEN_RESPONSE" | jq .
  exit 1
fi

echo "$ACCESS_TOKEN" > .token

echo "Token generated successfully."
echo ""
echo "Saved token to:"
echo "  .token"
echo ""
echo "Token preview:"
echo "$ACCESS_TOKEN" | cut -c1-100
echo ""
echo ""
echo "Decoded token scope:"
echo "$ACCESS_TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq '{scope, preferred_username, realm_access, resource_access}' || true
