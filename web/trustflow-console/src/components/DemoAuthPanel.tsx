import { useState } from "react";
import { useAuth } from "../auth/useAuth";
import { clearDemoToken, getDemoToken, setDemoToken } from "../utils/demoStorage";

type DemoAuthPanelProps = {
  compact?: boolean;
};

export function DemoAuthPanel({ compact = false }: DemoAuthPanelProps) {
  const auth = useAuth();
  const [draftToken, setDraftToken] = useState(getDemoToken);
  const [tokenPresent, setTokenPresent] = useState(() => Boolean(getDemoToken()));

  function saveToken() {
    setDemoToken(draftToken);
    setTokenPresent(Boolean(getDemoToken()));
  }

  function clearToken() {
    clearDemoToken();
    setDraftToken("");
    setTokenPresent(false);
  }

  return (
    <section className={compact ? "demo-auth-panel compact-panel" : "panel demo-auth-panel"}>
      <div className="panel-header">
        <div>
          <h3>Development Token Fallback</h3>
          <span className="demo-data-badge">Development-only fallback. Not for production.</span>
          <p>
            {auth.isAuthenticated
              ? "Using Keycloak token. Manual demo token is not required."
              : "Use this only when testing local APIs without browser login."}
          </p>
        </div>
        <span className={`status-badge ${tokenPresent ? "success" : "warning"}`}>
          {auth.isAuthenticated ? "KEYCLOAK ACTIVE" : tokenPresent ? "TOKEN STORED" : "NO TOKEN"}
        </span>
      </div>
      <div className="form-grid token-grid">
        <label className="wide-field">
          Demo Bearer Token
          <textarea
            value={draftToken}
            onChange={(event) => setDraftToken(event.target.value)}
            placeholder="Paste demo bearer token"
            rows={compact ? 2 : 3}
          />
        </label>
      </div>
      <div className="button-row">
        <button type="button" onClick={saveToken}>
          Save Token
        </button>
        <button type="button" className="secondary-button" onClick={clearToken} disabled={!tokenPresent && !draftToken}>
          Clear Token
        </button>
      </div>
    </section>
  );
}
