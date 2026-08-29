import { useState } from "react";
import { keycloakConfig } from "../auth/keycloak";
import { useAuth } from "../auth/useAuth";
import { DemoAuthPanel } from "../components/DemoAuthPanel";
import { PageHeader, ProductCard, StatusPill } from "../components/ProductUI";

function tokenPreview(token?: string) {
  if (!token) {
    return "No token";
  }

  if (token.length <= 34) {
    return token;
  }

  return `${token.slice(0, 20)}...${token.slice(-10)}`;
}

function formatExpiry(expiry?: number) {
  if (!expiry) {
    return "Not available";
  }

  return new Date(expiry * 1000).toLocaleString();
}

export function AuthDebugPage() {
  const auth = useAuth();
  const [showTokenPreview, setShowTokenPreview] = useState(false);

  return (
    <section className="page-stack">
      <PageHeader
        title="Auth Diagnostics"
        subtitle="Admin diagnostics for Keycloak session state, realm configuration, and token availability."
      />

      <ProductCard
        title="Session Status"
        subtitle="Browser login state without exposing the full access token."
        badge={<StatusPill status={auth.isAuthenticated ? "ACTIVE" : "UNKNOWN"} />}
      >
        <div className="summary-grid">
          <div>
            <span>Authenticated</span>
            <strong>{auth.isAuthenticated ? "Yes" : "No"}</strong>
          </div>
          <div>
            <span>Username</span>
            <strong>{auth.username ?? "Not logged in"}</strong>
          </div>
          <div>
            <span>Token Present</span>
            <strong>{auth.token ? "Yes" : "No"}</strong>
          </div>
          <div>
            <span>Keycloak URL</span>
            <strong>{keycloakConfig.url}</strong>
          </div>
          <div>
            <span>Realm</span>
            <strong>{keycloakConfig.realm}</strong>
          </div>
          <div>
            <span>Client ID</span>
            <strong>{keycloakConfig.clientId}</strong>
          </div>
          <div>
            <span>Token Expiry</span>
            <strong>{formatExpiry(auth.tokenExpiresAt)}</strong>
          </div>
          <div>
            <span>Roles</span>
            <strong>{auth.roles.length ? auth.roles.join(", ") : "None reported"}</strong>
          </div>
          <div>
            <span>Scopes</span>
            <strong>{auth.scopes.length ? auth.scopes.join(", ") : "None reported"}</strong>
          </div>
        </div>
        <div className="button-row">
          <button type="button" className="secondary-button" onClick={() => setShowTokenPreview((value) => !value)}>
            {showTokenPreview ? "Hide Token Preview" : "Show Token Preview"}
          </button>
          <button type="button" onClick={auth.login} disabled={auth.isAuthenticated || auth.isLoading}>
            Login
          </button>
          <button type="button" className="secondary-button" onClick={auth.logout} disabled={!auth.isAuthenticated}>
            Logout
          </button>
        </div>
        {showTokenPreview && (
          <div className="token-preview">
            <span>Token preview</span>
            <code>{tokenPreview(auth.token)}</code>
          </div>
        )}
      </ProductCard>

      <DemoAuthPanel />
    </section>
  );
}
