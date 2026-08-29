import Keycloak from "keycloak-js";

export const keycloakConfig = {
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "naitfs-client",
  realm: import.meta.env.VITE_KEYCLOAK_REALM || "payment-platform",
  url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8081",
};

export const keycloak = new Keycloak(keycloakConfig);
