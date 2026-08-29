import { useCallback, useEffect, useMemo, useState } from "react";

export const DEMO_TOKEN_KEY = "naitfs.demoToken";
export const TENANT_ID_KEY = "naitfs.tenantId";
export const CORRELATION_ID_KEY = "naitfs.correlationId";
export const IDEMPOTENCY_KEY = "naitfs.idempotencyKey";

export type DemoIdentity = {
  tenantId: string;
  correlationId: string;
  idempotencyKey: string;
};

const DEMO_TOKEN_CHANGED_EVENT = "naitfs-demo-token-changed";
const DEMO_IDENTITY_CHANGED_EVENT = "naitfs-demo-identity-changed";

function readStorageValue(key: string, fallback = "") {
  if (typeof window === "undefined") {
    return fallback;
  }

  return window.localStorage.getItem(key) ?? fallback;
}

function writeStorageValue(key: string, value: string) {
  if (typeof window === "undefined") {
    return;
  }

  if (value) {
    window.localStorage.setItem(key, value);
  } else {
    window.localStorage.removeItem(key);
  }
}

function emitStorageChange(eventName: string) {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(eventName));
  }
}

export function getDemoToken(): string {
  return readStorageValue(DEMO_TOKEN_KEY);
}

export function setDemoToken(token: string): void {
  writeStorageValue(DEMO_TOKEN_KEY, token.trim());
  emitStorageChange(DEMO_TOKEN_CHANGED_EVENT);
}

export function clearDemoToken(): void {
  writeStorageValue(DEMO_TOKEN_KEY, "");
  emitStorageChange(DEMO_TOKEN_CHANGED_EVENT);
}

export function generateDemoIdentity(): DemoIdentity {
  const timestamp = Date.now();

  return {
    tenantId: "tenant-web-demo-001",
    correlationId: `corr-web-${timestamp}`,
    idempotencyKey: `idem-web-${timestamp}`,
  };
}

export function getDemoIdentity(): DemoIdentity {
  const generated = generateDemoIdentity();
  const tenantId = readStorageValue(TENANT_ID_KEY);
  const correlationId = readStorageValue(CORRELATION_ID_KEY);
  const idempotencyKey = readStorageValue(IDEMPOTENCY_KEY);

  if (!tenantId) {
    writeStorageValue(TENANT_ID_KEY, generated.tenantId);
  }

  if (!correlationId) {
    writeStorageValue(CORRELATION_ID_KEY, generated.correlationId);
  }

  if (!idempotencyKey) {
    writeStorageValue(IDEMPOTENCY_KEY, generated.idempotencyKey);
  }

  return {
    tenantId: tenantId || generated.tenantId,
    correlationId: correlationId || generated.correlationId,
    idempotencyKey: idempotencyKey || generated.idempotencyKey,
  };
}

export function saveDemoIdentity(identity: DemoIdentity): void {
  writeStorageValue(TENANT_ID_KEY, identity.tenantId);
  writeStorageValue(CORRELATION_ID_KEY, identity.correlationId);
  writeStorageValue(IDEMPOTENCY_KEY, identity.idempotencyKey);
  emitStorageChange(DEMO_IDENTITY_CHANGED_EVENT);
}

export function clearDemoIdentity(): void {
  writeStorageValue(TENANT_ID_KEY, "");
  writeStorageValue(CORRELATION_ID_KEY, "");
  writeStorageValue(IDEMPOTENCY_KEY, "");
  emitStorageChange(DEMO_IDENTITY_CHANGED_EVENT);
}

export function useDemoToken() {
  const [token, setTokenState] = useState(getDemoToken);

  useEffect(() => {
    function syncToken() {
      setTokenState(getDemoToken());
    }

    function syncTokenFromStorage(event: StorageEvent) {
      if (event.key === DEMO_TOKEN_KEY) {
        syncToken();
      }
    }

    window.addEventListener(DEMO_TOKEN_CHANGED_EVENT, syncToken);
    window.addEventListener("storage", syncTokenFromStorage);

    return () => {
      window.removeEventListener(DEMO_TOKEN_CHANGED_EVENT, syncToken);
      window.removeEventListener("storage", syncTokenFromStorage);
    };
  }, []);

  return {
    clearToken: clearDemoToken,
    setToken: setDemoToken,
    token,
  };
}

export function useDemoIdentity() {
  const [identity, setIdentityState] = useState(getDemoIdentity);

  useEffect(() => {
    function syncIdentity() {
      setIdentityState(getDemoIdentity());
    }

    function syncIdentityFromStorage(event: StorageEvent) {
      if ([TENANT_ID_KEY, CORRELATION_ID_KEY, IDEMPOTENCY_KEY].includes(event.key ?? "")) {
        syncIdentity();
      }
    }

    window.addEventListener(DEMO_IDENTITY_CHANGED_EVENT, syncIdentity);
    window.addEventListener("storage", syncIdentityFromStorage);

    return () => {
      window.removeEventListener(DEMO_IDENTITY_CHANGED_EVENT, syncIdentity);
      window.removeEventListener("storage", syncIdentityFromStorage);
    };
  }, []);

  const setIdentity = useCallback((field: keyof DemoIdentity, value: string) => {
    setIdentityState((current) => {
      const nextIdentity = {
        ...current,
        [field]: value,
      };
      saveDemoIdentity(nextIdentity);
      return nextIdentity;
    });
  }, []);

  const setDemoIdentity = useCallback((identityValue: DemoIdentity) => {
    saveDemoIdentity(identityValue);
    setIdentityState(identityValue);
  }, []);

  const generateNewIdentity = useCallback(() => {
    const nextIdentity = generateDemoIdentity();
    saveDemoIdentity(nextIdentity);
    setIdentityState(nextIdentity);
  }, []);

  const reuseExistingIdentity = useCallback(() => {
    setIdentityState(getDemoIdentity());
  }, []);

  const clearIdentity = useCallback(() => {
    clearDemoIdentity();
    const nextIdentity = getDemoIdentity();
    saveDemoIdentity(nextIdentity);
    setIdentityState(nextIdentity);
  }, []);

  return useMemo(
    () => ({
      ...identity,
      clearIdentity,
      generateNewIdentity,
      reuseExistingIdentity,
      setDemoIdentity,
      setIdentity,
    }),
    [clearIdentity, generateNewIdentity, identity, reuseExistingIdentity, setDemoIdentity, setIdentity],
  );
}
