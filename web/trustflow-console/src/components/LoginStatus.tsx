import { useAuth } from "../auth/useAuth";
import { useDemoToken } from "../utils/demoStorage";
import { LogIn, LogOut, ShieldCheck } from "lucide-react";

export function LoginStatus() {
  const auth = useAuth();
  const { token } = useDemoToken();
  const statusLabel = auth.isAuthenticated
    ? "Logged in with Keycloak"
    : token
      ? "Demo token fallback active"
      : "Not logged in";
  const tokenSource = auth.token ? "Keycloak session" : token ? "Demo token fallback" : "No token";

  return (
    <div className="login-status">
      <span className={`login-dot ${auth.isAuthenticated ? "active" : token ? "fallback" : ""}`} />
      <div>
        <span>{statusLabel}</span>
        <strong>
          <ShieldCheck aria-hidden="true" size={14} />
          {tokenSource}
        </strong>
        {auth.isAuthenticated && <small>Logged in as {auth.username ?? "Keycloak user"}</small>}
      </div>
      {auth.isAuthenticated ? (
        <button type="button" className="secondary-button" onClick={auth.logout}>
          <LogOut aria-hidden="true" size={16} />
          Logout
        </button>
      ) : (
        <button type="button" onClick={auth.login} disabled={auth.isLoading}>
          <LogIn aria-hidden="true" size={16} />
          Login
        </button>
      )}
    </div>
  );
}
