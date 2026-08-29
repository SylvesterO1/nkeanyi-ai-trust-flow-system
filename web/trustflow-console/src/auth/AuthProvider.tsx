import { createContext, useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import { setKeycloakTokenSnapshot } from "./authTokenStore";
import { keycloak, keycloakConfig } from "./keycloak";

type AuthContextValue = {
  getToken(): Promise<string | undefined>;
  isAuthenticated: boolean;
  isLoading: boolean;
  login(): void;
  logout(): void;
  refreshToken(): Promise<string | undefined>;
  roles: string[];
  scopes: string[];
  token?: string;
  tokenExpiresAt?: number;
  username?: string;
};

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

type AuthProviderProps = {
  children: ReactNode;
};

function readRoles() {
  const realmRoles = keycloak.tokenParsed?.realm_access?.roles ?? [];
  const clientRoles =
    keycloak.tokenParsed?.resource_access?.[keycloakConfig.clientId]?.roles ?? [];

  return Array.from(new Set([...realmRoles, ...clientRoles]));
}

function readScopes() {
  const scope = keycloak.tokenParsed?.scope;
  return typeof scope === "string" ? scope.split(" ").filter(Boolean) : [];
}

function readUsername() {
  return (
    keycloak.tokenParsed?.preferred_username ??
    keycloak.tokenParsed?.name ??
    keycloak.tokenParsed?.email ??
    undefined
  );
}

export function AuthProvider({ children }: AuthProviderProps) {
  const initializedRef = useRef(false);
  const refreshTimerRef = useRef<number | undefined>(undefined);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [roles, setRoles] = useState<string[]>([]);
  const [scopes, setScopes] = useState<string[]>([]);
  const [token, setToken] = useState<string | undefined>();
  const [tokenExpiresAt, setTokenExpiresAt] = useState<number | undefined>();
  const [username, setUsername] = useState<string | undefined>();

  const syncAuthState = useCallback(() => {
    const nextToken = keycloak.token;
    setIsAuthenticated(Boolean(keycloak.authenticated));
    setKeycloakTokenSnapshot(nextToken);
    setRoles(readRoles());
    setScopes(readScopes());
    setToken(nextToken);
    setTokenExpiresAt(keycloak.tokenParsed?.exp);
    setUsername(readUsername());
  }, []);

  const refreshToken = useCallback(async () => {
    if (!keycloak.authenticated) {
      syncAuthState();
      return undefined;
    }

    try {
      await keycloak.updateToken(60);
      syncAuthState();
      return keycloak.token;
    } catch {
      syncAuthState();
      return undefined;
    }
  }, [syncAuthState]);

  const getToken = useCallback(async () => refreshToken(), [refreshToken]);

  useEffect(() => {
    if (initializedRef.current) {
      return;
    }

    initializedRef.current = true;

    keycloak
      .init({
        checkLoginIframe: false,
        onLoad: "check-sso",
        pkceMethod: "S256",
        silentCheckSsoFallback: false,
      })
      .then(() => {
        syncAuthState();
      })
      .catch(() => {
        syncAuthState();
      })
      .finally(() => {
        setIsLoading(false);
      });

    keycloak.onAuthSuccess = syncAuthState;
    keycloak.onAuthLogout = syncAuthState;
    keycloak.onTokenExpired = () => {
      void refreshToken();
    };
  }, [refreshToken, syncAuthState]);

  useEffect(() => {
    window.clearInterval(refreshTimerRef.current);
    refreshTimerRef.current = window.setInterval(() => {
      void refreshToken();
    }, 30_000);

    return () => {
      window.clearInterval(refreshTimerRef.current);
    };
  }, [refreshToken]);

  const value = useMemo<AuthContextValue>(
    () => ({
      getToken,
      isAuthenticated,
      isLoading,
      login: () => keycloak.login(),
      logout: () => keycloak.logout({ redirectUri: window.location.origin }),
      refreshToken,
      roles,
      scopes,
      token,
      tokenExpiresAt,
      username,
    }),
    [
      getToken,
      isAuthenticated,
      isLoading,
      refreshToken,
      roles,
      scopes,
      token,
      tokenExpiresAt,
      username,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
