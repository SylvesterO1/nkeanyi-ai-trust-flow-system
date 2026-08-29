let keycloakToken: string | undefined;

export function setKeycloakTokenSnapshot(token?: string) {
  keycloakToken = token;
}

export function getKeycloakTokenSnapshot() {
  return keycloakToken;
}
