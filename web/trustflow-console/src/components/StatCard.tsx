type StatCardProps = {
  label: string;
  value: string;
  helperText?: string;
};

export function StatCard({ label, value, helperText }: StatCardProps) {
  return (
    <article className="stat-card">
      <span>{label}</span>
      <strong>{value}</strong>
      {helperText && <p>{helperText}</p>}
    </article>
  );
}
