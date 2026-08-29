import type { EvidenceRecord } from "../types/api";
import { StatusBadge } from "./StatusBadge";

type EvidenceTableProps = {
  records: EvidenceRecord[];
};

function getTimestamp(record: EvidenceRecord) {
  return record.createdAt ?? record.timestamp ?? "";
}

export function EvidenceTable({ records }: EvidenceTableProps) {
  if (records.length === 0) {
    return (
      <div className="empty-state">
        <strong>No evidence records found.</strong>
        <p>Run a workflow or search by a valid correlation ID to view compliance evidence.</p>
      </div>
    );
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Type</th>
            <th>Decision</th>
            <th>Risk</th>
            <th>Service</th>
            <th>Topic</th>
            <th>Correlation ID</th>
            <th>Payment ID</th>
            <th>Timestamp</th>
          </tr>
        </thead>
        <tbody>
          {records.map((record, index) => (
            <tr key={record.id ?? `${record.correlationId}-${index}`}>
              <td>{record.evidenceType ?? "Evidence"}</td>
              <td>
                <StatusBadge status={record.decision ?? "UNKNOWN"} />
              </td>
              <td>
                <StatusBadge status={record.riskLevel ?? "UNKNOWN"} />
              </td>
              <td>{record.sourceService ?? "-"}</td>
              <td>{record.sourceTopic ?? "-"}</td>
              <td>{record.correlationId ?? "-"}</td>
              <td>{record.paymentId ?? "-"}</td>
              <td>{getTimestamp(record) || "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
