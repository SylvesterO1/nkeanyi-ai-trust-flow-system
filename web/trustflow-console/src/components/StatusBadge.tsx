type StatusBadgeProps = {
  status?: string;
};

const statusClassMap: Record<string, string> = {
  UP: "success",
  ACTIVE: "success",
  PAID: "success",
  PROCESSED: "success",
  SUCCESS: "success",
  COMPLETED: "success",
  LOW: "success",
  RECEIVED: "info",
  RUNNING: "info",
  CHECKING: "warning",
  MEDIUM: "warning",
  NOT_STARTED: "neutral",
  DOWN: "danger",
  FAILED: "danger",
  HIGH: "danger",
  UNKNOWN: "neutral",
};

export function StatusBadge({ status = "UNKNOWN" }: StatusBadgeProps) {
  const normalized = status.toUpperCase();
  const className = statusClassMap[normalized] ?? "neutral";

  return <span className={`status-badge ${className}`}>{normalized}</span>;
}
