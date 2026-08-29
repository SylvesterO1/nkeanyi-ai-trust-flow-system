import { apiGet } from "./client";
import type { HealthResponse } from "../types/api";

export function getGatewayHealth() {
  return apiGet<HealthResponse>("/actuator/health");
}
