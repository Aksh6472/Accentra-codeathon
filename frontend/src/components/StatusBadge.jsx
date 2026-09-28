import { STATUS_LABELS } from '../lib/format';

const TONES = {
  PENDING_MANAGER: 'badge-pending',
  PENDING_HR: 'badge-pending',
  APPROVED: 'badge-approved',
  REJECTED: 'badge-rejected',
  ESCALATED: 'badge-escalated',
  CANCEL_REQUESTED: 'badge-cancel',
  CANCELLED: 'badge-cancelled',
};

export default function StatusBadge({ status }) {
  return <span className={`badge ${TONES[status] || 'badge-info'}`}>{STATUS_LABELS[status] || status}</span>;
}
