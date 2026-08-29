type DemoChecklistItem = {
  done: boolean;
  label: string;
};

type DemoChecklistProps = {
  items: DemoChecklistItem[];
};

export function DemoChecklist({ items }: DemoChecklistProps) {
  return (
    <section className="panel checklist-panel">
      <div className="panel-header">
        <div>
          <h3>MVP17 Live Demo Checklist</h3>
          <p>Use this panel during the live browser walkthrough.</p>
        </div>
      </div>
      <ul className="checklist">
        {items.map((item) => (
          <li key={item.label} className={item.done ? "done" : ""}>
            <span>{item.done ? "PASS" : "TODO"}</span>
            {item.label}
          </li>
        ))}
      </ul>
    </section>
  );
}
