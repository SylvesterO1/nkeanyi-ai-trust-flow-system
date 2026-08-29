import { useAuth } from "../auth/useAuth";
import { useDemoToken } from "../utils/demoStorage";

export function AuthNotice() {
  const { token: keycloakToken } = useAuth();
  const { token: demoToken } = useDemoToken();

  if (keycloakToken || demoToken) {
    return null;
  }

  return (
    <section className="auth-notice">
      Login or provide a demo token to call secured backend endpoints.
    </section>
  );
}
