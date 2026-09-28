import { Link } from 'react-router-dom';

export default function StatCard({ label, value, hint, tone, to }) {
  const className = `card stat ${tone ? `tone-${tone}` : ''}`;
  const content = (
    <>
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
      {hint && <span className="stat-hint">{hint}</span>}
    </>
  );
  return to ? (
    <Link to={to} className={className} style={{ color: 'inherit' }}>
      {content}
    </Link>
  ) : (
    <div className={className}>{content}</div>
  );
}
