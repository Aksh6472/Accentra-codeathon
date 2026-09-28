import { Link } from 'react-router-dom';
import { relativeTime } from '../lib/format';
import Icon from './Icon';
import { EmptyState } from './States';

const TONES = {
  good: ['MANAGER_APPROVED', 'HR_APPROVED', 'CANCELLATION_APPROVED'],
  bad: ['LEAVE_REJECTED', 'MANAGER_REJECTED', 'HR_REJECTED', 'CANCELLATION_REJECTED', 'LEAVE_WITHDRAWN', 'WITHDRAWN'],
  warn: ['LEAVE_ESCALATED', 'ESCALATED', 'TEAM_CONFLICT_WARNING'],
};
const ICONS = { good: 'check', bad: 'x', warn: 'alert' };

function toneOf(type) {
  return Object.keys(TONES).find((t) => TONES[t].includes(type)) || '';
}

/**
 * Compact event list. `items` are { id, type, title, detail, at, to, unread }; `type` picks the icon tone.
 */
export default function ActivityFeed({ items, emptyTitle = 'No recent activity' }) {
  if (!items.length) return <EmptyState icon="clock" title={emptyTitle} />;
  return (
    <ul className="activity">
      {items.map((item) => {
        const tone = toneOf(item.type);
        const title = item.to ? <Link to={item.to} style={{ color: 'inherit' }}>{item.title}</Link> : item.title;
        return (
          <li key={item.id}>
            <span className={`activity-icon ${tone}`} aria-hidden="true">
              <Icon name={ICONS[tone] || item.icon || 'send'} size={14} />
            </span>
            <div style={{ minWidth: 0 }}>
              <div className="title">{title}{item.unread && <span className="unread-dot" title="Unread" />}</div>
              <div className="meta">{[item.detail, relativeTime(item.at)].filter(Boolean).join(' · ')}</div>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
