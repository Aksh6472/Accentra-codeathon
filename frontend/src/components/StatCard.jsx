import { Link } from 'react-router-dom';
import Icon from './Icon';

export default function StatCard({ label, value, unit, hint, tone, to, icon }) {
  const className = `card stat ${tone ? `tone-${tone}` : ''}`;
  const content = (
    <>
      <span className="stat-label">{label}</span>
      <span className="stat-value">
        {value}
        {unit && <small>{unit}</small>}
      </span>
      {icon && <span className="stat-icon" aria-hidden="true"><Icon name={icon} size={17} /></span>}
      {hint && <span className="stat-hint">{hint}</span>}
    </>
  );
  return to ? (
    <Link to={to} className={className}>
      {content}
    </Link>
  ) : (
    <div className={className}>{content}</div>
  );
}
