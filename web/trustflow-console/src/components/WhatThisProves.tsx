type WhatThisProvesProps = {
  points: string[];
  title: string;
};

export function WhatThisProves({ points, title }: WhatThisProvesProps) {
  return (
    <section className="panel proof-panel">
      <h3>{title}</h3>
      <ul className="proof-list">
        {points.map((point) => (
          <li key={point}>{point}</li>
        ))}
      </ul>
    </section>
  );
}
