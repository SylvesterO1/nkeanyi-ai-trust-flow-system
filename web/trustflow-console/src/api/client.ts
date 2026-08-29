import { getKeycloakTokenSnapshot } from "../auth/authTokenStore";
import type { RequestOptions } from "../types/api";
import { getDemoToken } from "../utils/demoStorage";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export class ApiError extends Error {
  status?: number;
  method: string;
  path: string;
  likelyCause: string;
  responseBody?: unknown;

  constructor({
    message,
    method,
    path,
    responseBody,
    status,
  }: {
    message: string;
    method: string;
    path: string;
    responseBody?: unknown;
    status?: number;
  }) {
    super(message);
    this.name = "ApiError";
    this.method = method;
    this.path = path;
    this.likelyCause = getLikelyCause(status);
    this.responseBody = responseBody;
    this.status = status;
  }
}

function getLikelyCause(status?: number) {
  if (!status) {
    return "Gateway down or CORS issue";
  }

  if (status === 401) {
    return "Missing or invalid bearer token";
  }

  if (status === 403) {
    return "Token exists but lacks required role/scope";
  }

  if (status === 400) {
    return "Backend validation error";
  }

  if (status >= 500) {
    return "Backend service error";
  }

  return "Request failed";
}

export function buildRequestHeaders(
  options?: RequestOptions,
  includeContentType = false,
): HeadersInit {
  const headers: Record<string, string> = {};

  if (includeContentType) {
    headers["Content-Type"] = "application/json";
  }

  if (options?.tenantId) {
    headers["X-Tenant-Id"] = options.tenantId;
  }

  if (options?.correlationId) {
    headers["X-Correlation-Id"] = options.correlationId;
  }

  if (options?.idempotencyKey) {
    headers["Idempotency-Key"] = options.idempotencyKey;
  }

  const authorizationToken = getKeycloakTokenSnapshot() ?? options?.authorizationToken ?? getDemoToken();

  if (authorizationToken) {
    headers.Authorization = authorizationToken.startsWith("Bearer ")
      ? authorizationToken
      : `Bearer ${authorizationToken}`;
  }

  return headers;
}

export async function readApiResponse<T>(
  response: Response,
  method: string,
  path: string,
): Promise<T> {
  const text = await response.text();
  let data: unknown = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    const message =
      data && typeof data === "object" && "message" in data && typeof data.message === "string"
        ? data.message
        : typeof data === "string"
          ? data
        : `${method} ${path} failed with status ${response.status}`;
    throw new ApiError({
      message,
      method,
      path,
      responseBody: data,
      status: response.status,
    });
  }

  return data as T;
}

export async function apiGet<T>(path: string, options?: RequestOptions): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      headers: buildRequestHeaders(options),
    });
  } catch (error) {
    throw new ApiError({
      message: error instanceof Error ? error.message : "Network request failed.",
      method: "GET",
      path,
    });
  }

  return readApiResponse<T>(response, "GET", path);
}

export async function apiPost<T>(
  path: string,
  body: unknown,
  options?: RequestOptions,
): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: "POST",
      headers: buildRequestHeaders(options, true),
      body: JSON.stringify(body),
    });
  } catch (error) {
    throw new ApiError({
      message: error instanceof Error ? error.message : "Network request failed.",
      method: "POST",
      path,
    });
  }

  return readApiResponse<T>(response, "POST", path);
}

export async function apiPostForm<T>(
  path: string,
  body: FormData,
  options?: RequestOptions,
): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: "POST",
      headers: buildRequestHeaders(options),
      body,
    });
  } catch (error) {
    throw new ApiError({
      message: error instanceof Error ? error.message : "Network request failed.",
      method: "POST",
      path,
    });
  }

  return readApiResponse<T>(response, "POST", path);
}
