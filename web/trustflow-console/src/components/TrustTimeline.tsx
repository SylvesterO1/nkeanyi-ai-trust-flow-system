import type { EvidenceRecord } from "../types/api";
import { StatusBadge } from "./StatusBadge";

type TrustTimelineProps = {
  records: EvidenceRecord[];
};

const defaultSteps = [
  "Document Extracted",
  "Knowledge Answer Generated",
  "Payment Received",
  "Payment Completed",
  "Compliance Evidence Recorded",
];

function timestampOf(record: EvidenceRecord) {
  return record.createdAt ?? record.timestamp ?? "";
}

export function TrustTimeline({ records }: TrustTimelineProps) {
  const sortedRecords = [...records].sort((a, b) =>
    timestampOf(a).localeCompare(timestampOf(b)),
  );

  if (sortedRecords.length === 0) {
    return (
      <ol className="timeline">
        {defaultSteps.map((step) => (
          <li key={step}>
            <span className="timeline-dot" />
            <div>
              <strong>{step}</strong>
              <p>Expected trust-flow milestone</p>
            </div>
          </li>
        ))}
      </ol>
    );
  }

  return (
    <ol className="timeline">
      {sortedRecords.map((record, index) => (
        <li key={record.id ?? `${record.evidenceType}-${index}`}>
          <span className="timeline-dot" />
          <div>
            <strong>{record.evidenceType ?? record.decision ?? "Evidence Recorded"}</strong>
            <p>
              {record.sourceService ?? "NAITFS"} recorded
              {record.correlationId ? ` correlation ${record.correlationId}` : " trust evidence"}.
            </p>
            <div className="timeline-meta">
              <StatusBadge status={record.decision ?? "UNKNOWN"} />
              <span>{timestampOf(record) || "Timestamp pending"}</span>
            </div>
          </div>
        </li>
      ))}
    </ol>
  );
}
