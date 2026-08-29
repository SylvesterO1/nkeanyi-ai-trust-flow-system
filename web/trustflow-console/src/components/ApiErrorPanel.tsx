import { ApiError } from "../api/client";

type ApiErrorPanelProps = {
  error: unknown;
};

function formatBody(body: unknown) {
  if (body == null) {
    return "";
  }

  return typeof body === "string" ? body : JSON.stringify(body, null, 2);
}

export function ApiErrorPanel({ error }: ApiErrorPanelProps) {
  if (!error) {
    return null;
  }

  if (!(error instanceof ApiError)) {
    const message = error instanceof Error ? error.message : String(error);

    return (
      <div className="error-box api-error-panel">
        <strong>What failed</strong>
        <p>{message}</p>
        <p>
          <strong>Suggested next action:</strong> Review the form values and retry the workflow.
        </p>
      </div>
    );
  }

  const body = formatBody(error.responseBody);

  return (
    <div className="error-box api-error-panel">
      <strong>What failed</strong>
      <p>{error.message}</p>
      <dl>
        <div>
          <dt>HTTP status</dt>
          <dd>{error.status ?? 0}</dd>
        </div>
        <div>
          <dt>Method</dt>
          <dd>{error.method}</dd>
        </div>
        <div>
          <dt>Endpoint path</dt>
          <dd>{error.path}</dd>
        </div>
        <div>
          <dt>Likely explanation</dt>
          <dd>{error.likelyCause}</dd>
        </div>
        <div>
          <dt>Suggested next action</dt>
          <dd>Confirm the backend service is running, the token is valid, and the tenant/correlation context is present.</dd>
        </div>
      </dl>
      {body && (
        <details className="technical-details">
          <summary>Technical error details</summary>
          <pre>{body}</pre>
        </details>
      )}
    </div>
  );
}
