const checklistItems = [
  "Web console shell",
  "Dashboard",
  "Health page",
  "Documents page",
  "Knowledge page",
  "Payments page",
  "Evidence page",
  "Trust Flow Demo page",
  "Billing page",
  "Demo token persistence",
  "Demo identity persistence",
  "Live workflow readiness",
  "Commercial demo script",
];

export function Mvp17CompletionChecklist() {
  return (
    <section className="panel">
      <h3>MVP17 Completion Checklist</h3>
      <ul className="mvp-checklist">
        {checklistItems.map((item) => (
          <li key={item}>
            <span>Done</span>
            {item}
          </li>
        ))}
      </ul>
    </section>
  );
}
