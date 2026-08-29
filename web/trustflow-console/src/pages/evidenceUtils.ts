import type { EvidenceRecord, EvidenceSearchResponse } from "../types/api";

function isRecord(value: unknown): value is EvidenceRecord {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

export function extractEvidenceRecords(response: EvidenceSearchResponse | null): EvidenceRecord[] {
  if (!response) {
    return [];
  }

  if (Array.isArray(response)) {
    return response.filter(isRecord);
  }

  const candidates = [response.records, response.evidence, response.items, response.data];

  for (const candidate of candidates) {
    if (Array.isArray(candidate)) {
      return candidate.filter(isRecord);
    }

    if (isRecord(candidate)) {
      return [candidate];
    }
  }

  return isRecord(response) && (response.id || response.evidenceType || response.correlationId)
    ? [response]
    : [];
}
