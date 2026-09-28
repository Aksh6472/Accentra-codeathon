import { useState } from 'react';

/**
 * Single-series vertical bar chart. One hue, value labels on non-zero bars, hover/focus tooltip.
 * `data`: [{ label, value, detail? }]
 */
export function BarChart({ data, unit = '', ariaLabel }) {
  const [active, setActive] = useState(null);
  const max = Math.max(1, ...data.map((d) => d.value));
  return (
    <figure className="chart" style={{ margin: 0 }} aria-label={ariaLabel}>
      <div className="bar-chart">
        {data.map((d, i) => (
          <div
            key={d.label}
            className="bar-col"
            tabIndex={0}
            onMouseEnter={() => setActive(i)}
            onMouseLeave={() => setActive(null)}
            onFocus={() => setActive(i)}
            onBlur={() => setActive(null)}
            aria-label={`${d.label}: ${d.value}${unit}`}
          >
            {active === i && (
              <div className="tooltip" role="tooltip">
                <strong>{d.detail || d.label}</strong>: {d.value}{unit}
              </div>
            )}
            {d.value > 0 && <span className="bar-value">{d.value}</span>}
            <div className="bar" style={{ height: `${(d.value / max) * 85}%` }} />
          </div>
        ))}
      </div>
      <div className="bar-axis" aria-hidden="true">
        {data.map((d) => (
          <span key={d.label}>{d.label}</span>
        ))}
      </div>
    </figure>
  );
}

/** Horizontal bars for ranked categories; every bar is labelled, so colour carries no meaning alone. */
export function HBarList({ data, format = (v) => v, max: fixedMax, colorFor }) {
  const [active, setActive] = useState(null);
  const max = fixedMax ?? Math.max(1, ...data.map((d) => d.value));
  return (
    <div className="hbar-list">
      {data.map((d, i) => (
        <div
          key={d.label}
          className="hbar-row"
          style={{ position: 'relative' }}
          onMouseEnter={() => setActive(i)}
          onMouseLeave={() => setActive(null)}
        >
          {active === i && d.detail && (
            <div className="tooltip" role="tooltip" style={{ left: '50%' }}>{d.detail}</div>
          )}
          <span className="hbar-label" title={d.label}>{d.label}</span>
          <div className="hbar-track" role="img" aria-label={`${d.label}: ${format(d.value)}`}>
            <div
              className="hbar-fill"
              style={{ width: `${Math.min(100, (d.value / max) * 100)}%`, background: colorFor ? colorFor(d) : undefined }}
            />
          </div>
          <span className="hbar-value">{format(d.value)}</span>
        </div>
      ))}
    </div>
  );
}
