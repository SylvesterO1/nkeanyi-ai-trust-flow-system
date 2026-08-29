#!/usr/bin/env bash
set -e

KEYCLOAK_CONTAINER="${KEYCLOAK_CONTAINER:-naitfs-keycloak}"
KEYCLOAK_ADMIN_URL="${KEYCLOAK_ADMIN_URL:-http://localhost:8080}"
KEYCLOAK_ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM="${REALM:-payment-platform}"
CLIENT_ID="${CLIENT_ID:-naitfs-client}"
USERNAME="${USERNAME:-test-user}"
PASSWORD="${PASSWORD:-test-password}"
USER_EMAIL="${USER_EMAIL:-test-user@naitfs.local}"
USER_FIRST_NAME="${USER_FIRST_NAME:-Test}"
USER_LAST_NAME="${USER_LAST_NAME:-User}"
CLIENT_SCOPES="${CLIENT_SCOPES:-documents.write knowledge.read payments.read payments.write evidence.read}"
FRONTEND_ROOT_URL="${FRONTEND_ROOT_URL:-http://localhost:5174}"
FRONTEND_FALLBACK_URL="${FRONTEND_FALLBACK_URL:-http://localhost:5173}"

echo "Login to Keycloak admin CLI..."
docker exec -i "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh config credentials \
  --server "$KEYCLOAK_ADMIN_URL" \
  --realm master \
  --user "$KEYCLOAK_ADMIN_USER" \
  --password "$KEYCLOAK_ADMIN_PASSWORD"

echo "Ensure realm exists..."
docker exec -i "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh create realms \
  -s realm="$REALM" \
  -s enabled=true 2>/dev/null || true

echo "Allow local HTTP browser redirects for demo realm..."
docker exec -i "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh update realms/"$REALM" \
  -s enabled=true \
  -s sslRequired=none

echo "Delete old client if it exists..."
CLIENT_UUID=$(docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh get clients \
  -r "$REALM" \
  -q clientId="$CLIENT_ID" \
  --fields id \
  --format csv | tail -n 1 | tr -d '"')

if [ -n "$CLIENT_UUID" ] && [ "$CLIENT_UUID" != "id" ]; then
  docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh delete clients/$CLIENT_UUID -r "$REALM"
fi

echo "Create clean public client..."
docker exec -i "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh create clients \
  -r "$REALM" \
  -s clientId="$CLIENT_ID" \
  -s enabled=true \
  -s publicClient=true \
  -s directAccessGrantsEnabled=true \
  -s standardFlowEnabled=true \
  -s serviceAccountsEnabled=false \
  -s protocol=openid-connect \
  -s rootUrl="$FRONTEND_ROOT_URL" \
  -s baseUrl="$FRONTEND_ROOT_URL" \
  -s "redirectUris=[\"$FRONTEND_ROOT_URL/*\",\"$FRONTEND_FALLBACK_URL/*\"]" \
  -s "webOrigins=[\"$FRONTEND_ROOT_URL\",\"$FRONTEND_FALLBACK_URL\",\"+\"]"

echo "Ensure OAuth client scopes exist..."
for SCOPE in $CLIENT_SCOPES; do
  docker exec -i "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh create client-scopes \
    -r "$REALM" \
    -s name="$SCOPE" \
    -s protocol=openid-connect \
    -s attributes.'"include.in.token.scope"'=true \
    -s attributes.'"display.on.consent.screen"'=false 2>/dev/null || true
done

echo "Attach required scopes to client as defaults..."
CLIENT_UUID=$(docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh get clients \
  -r "$REALM" \
  -q clientId="$CLIENT_ID" \
  --fields id \
  --format csv | tail -n 1 | tr -d '"')

for SCOPE in $CLIENT_SCOPES; do
  SCOPE_UUID=$(docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh get client-scopes \
    -r "$REALM" \
    --fields id,name \
    --format csv | awk -F, -v scope="\"$SCOPE\"" '$2 == scope {gsub(/"/, "", $1); print $1; exit}')

  if [ -n "$SCOPE_UUID" ]; then
    docker exec -i "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh update \
      clients/"$CLIENT_UUID"/default-client-scopes/"$SCOPE_UUID" \
      -r "$REALM" 2>/dev/null || true
  fi
done

echo "Delete old test user if it exists..."
USER_UUID=$(docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh get users \
  -r "$REALM" \
  -q username="$USERNAME" \
  --fields id \
  --format csv | tail -n 1 | tr -d '"')

if [ -n "$USER_UUID" ] && [ "$USER_UUID" != "id" ]; then
  docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh delete users/$USER_UUID -r "$REALM"
fi

echo "Create clean completed test user..."
docker exec -i "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh create users \
  -r "$REALM" \
  -s username="$USERNAME" \
  -s enabled=true \
  -s email="$USER_EMAIL" \
  -s emailVerified=true \
  -s firstName="$USER_FIRST_NAME" \
  -s lastName="$USER_LAST_NAME" \
  -s requiredActions='[]'

echo "Get user ID..."
USER_UUID=$(docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh get users \
  -r "$REALM" \
  -q username="$USERNAME" \
  --fields id \
  --format csv | tail -n 1 | tr -d '"')

echo "Force user setup completed..."
docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh update users/$USER_UUID \
  -r "$REALM" \
  -s username="$USERNAME" \
  -s enabled=true \
  -s email="$USER_EMAIL" \
  -s emailVerified=true \
  -s firstName="$USER_FIRST_NAME" \
  -s lastName="$USER_LAST_NAME" \
  -s requiredActions='[]'

echo "Set password..."
docker exec -i "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh set-password \
  -r "$REALM" \
  --username "$USERNAME" \
  --new-password "$PASSWORD" \
  --temporary=false

echo "Verify required actions are empty..."
docker exec "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kcadm.sh get users/$USER_UUID \
  -r "$REALM" \
  --fields username,email,emailVerified,enabled,requiredActions

echo "Keycloak demo repair completed."
