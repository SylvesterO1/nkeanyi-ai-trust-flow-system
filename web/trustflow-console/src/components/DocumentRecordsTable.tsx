import type { DocumentRecord } from "../types/api";

type DocumentRecordsTableProps = {
  records: DocumentRecord[];
  onSelect(record: DocumentRecord): void;
};

function formatConfidence(confidence?: number) {
  return confidence == null ? "-" : `${Math.round(confidence * 100)}%`;
}

function formatDate(value?: string) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

export function DocumentRecordsTable({ records, onSelect }: DocumentRecordsTableProps) {
  return (
    <div className="table-wrap records-table">
      <table>
        <thead>
          <tr>
            <th>Document ID</th>
            <th>Filename</th>
            <th>Status</th>
            <th>Extractor</th>
            <th>Confidence</th>
            <th>Correlation ID</th>
            <th>Created At</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {records.length === 0 ? (
            <tr>
              <td colSpan={8}>
                <div className="empty-state table-empty-state">
                  <strong>No document records yet.</strong>
                  <p>Process a document to create an extraction record tied to your tenant and correlation ID.</p>
                </div>
              </td>
            </tr>
          ) : (
            records.map((record) => (
              <tr key={record.documentId}>
                <td>{record.documentId}</td>
                <td>{record.originalFilename ?? "-"}</td>
                <td>{record.status ?? "-"}</td>
                <td>{record.extractorUsed ?? "-"}</td>
                <td>{formatConfidence(record.confidence)}</td>
                <td>{record.correlationId ?? "-"}</td>
                <td>{formatDate(record.createdAt)}</td>
                <td>
                  <button type="button" className="secondary-button" onClick={() => onSelect(record)}>
                    View Details
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
