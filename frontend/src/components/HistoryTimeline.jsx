import { ACTION_LABELS, STATUS_LABELS, formatDateTime } from '../lib/format';

const DOT = {
  HR_APPROVED: 'good',
  MANAGER_APPROVED: 'good',
  CANCELLATION_APPROVED: 'good',
  MANAGER_REJECTED: 'bad',
  HR_REJECTED: 'bad',
  CANCELLATION_REJECTED: 'bad',
  ESCALATED: 'warn',
  BALANCE_UPDATED: 'system',
};

export default function HistoryTimeline({ history }) {
  if (!history?.length) return <p className="muted">No history yet.</p>;
  return (
    <ol className="timeline">
      {history.map((entry) => (
        <li key={entry.id}>
          <span className={`dot ${DOT[entry.action] || ''}`} aria-hidden="true" />
          <div className="title">{ACTION_LABELS[entry.action] || entry.action}</div>
          <div className="meta">
            {entry.actorName} · {entry.actorRole.toLowerCase()} · {formatDateTime(entry.createdAt)}
            {entry.previousStatus !== entry.newStatus && entry.newStatus && (
              <> · {entry.previousStatus ? `${STATUS_LABELS[entry.previousStatus]} → ` : ''}{STATUS_LABELS[entry.newStatus]}</>
            )}
          </div>
          {entry.comment && <div className="comment">{entry.comment}</div>}
        </li>
      ))}
    </ol>
  );
}
