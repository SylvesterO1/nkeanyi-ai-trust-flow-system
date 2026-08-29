type JsonViewerProps = {
  data: unknown;
  title?: string;
};

export function JsonViewer({ data, title = "Technical Details / Raw API Response" }: JsonViewerProps) {
  return (
    <details className="technical-details">
      <summary>{title}</summary>
      <pre className="json-viewer">
        {data == null ? "No data available." : JSON.stringify(data, null, 2)}
      </pre>
    </details>
  );
}
