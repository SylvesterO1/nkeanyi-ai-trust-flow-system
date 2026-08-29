#!/usr/bin/env bash
set -euo pipefail

KEYCLOAK_CONTAINER="${KEYCLOAK_CONTAINER:-naitfs-keycloak}"
KEYCLOAK_ADMIN_URL="${KEYCLOAK_ADMIN_URL:-http://localhost:8080}"
KEYCLOAK_ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
REALM="${REALM:-payment-platform}"
CLIENT_ID="${CLIENT_ID:-naitfs-client}"
USERNAME="${USERNAME:-test-user}"
PASSWORD="${PASSWORD:-test-password}"
USER_EMAIL="${USER_EMAIL:-test-user@naitfs.local}"
USER_FIRST_NAME="${USER_FIRST_NAME:-Test}"
USER_LAST_NAME="${USER_LAST_NAME:-User}"
TOKEN_SCOPE="${TOKEN_SCOPE:-openid profile email documents.write knowledge.read payments.write payments.read evidence.read}"

echo "1. Login to Keycloak admin CLI..."
docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh config credentials \
  --server "$KEYCLOAK_ADMIN_URL" \
  --realm master \
  --user "$KEYCLOAK_ADMIN_USER" \
  --password "$KEYCLOAK_ADMIN_PASSWORD"

echo ""
echo "2. Resolve user ID..."
USER_ID=$(docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh get users \
  -r "$REALM" \
  -q username="$USERNAME" \
  | jq -r '.[0].id // empty')

if [ -z "$USER_ID" ]; then
  echo "ERROR: User not found: $USERNAME"
  exit 1
fi

echo "USER_ID=$USER_ID"

echo ""
echo "3. Show user before repair..."
docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh get users/"$USER_ID" \
  -r "$REALM" \
  | jq '{username, enabled, emailVerified, firstName, lastName, email, requiredActions}'

echo ""
echo "4. Force user profile complete and clear required actions..."
cat <<JSON | docker exec -i "$KEYCLOAK_CONTAINER" sh -c "cat > /tmp/user-repair.json && /opt/keycloak/bin/kcadm.sh update users/$USER_ID -r $REALM -f /tmp/user-repair.json"
{
  "username": "$USERNAME",
  "enabled": true,
  "emailVerified": true,
  "firstName": "$USER_FIRST_NAME",
  "lastName": "$USER_LAST_NAME",
  "email": "$USER_EMAIL",
  "requiredActions": []
}
JSON

echo ""
echo "5. Reset password as NON-temporary..."
docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh set-password \
  -r "$REALM" \
  --userid "$USER_ID" \
  --new-password "$PASSWORD" \
  --temporary=false

echo ""
echo "6. Show user after repair..."
docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh get users/"$USER_ID" \
  -r "$REALM" \
  | jq '{username, enabled, emailVerified, firstName, lastName, email, requiredActions}'

echo ""
echo "7. Generate token..."
TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  -d "scope=$TOKEN_SCOPE")

echo "$TOKEN_RESPONSE" | jq .

TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty')

if [ -z "$TOKEN" ]; then
  echo ""
  echo "ERROR: Token still failed."
  exit 1
fi

echo "$TOKEN" > /tmp/naitfs-token.txt

echo ""
echo "TOKEN GENERATED SUCCESSFULLY"
echo "Saved to /tmp/naitfs-token.txt"
echo "$TOKEN" | cut -c1-100
