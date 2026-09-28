const dateFmt = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
const shortFmt = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short' });
const dateTimeFmt = new Intl.DateTimeFormat(undefined, {
  day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
});

/** Parses an ISO yyyy-mm-dd string as a local date (avoids UTC shifting). */
export function parseDate(iso) {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function toIsoDate(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export const todayIso = () => toIsoDate(new Date());

export const formatDate = (iso) => (iso ? dateFmt.format(parseDate(iso)) : '—');
export const formatShortDate = (iso) => (iso ? shortFmt.format(parseDate(iso)) : '—');
export const formatDateTime = (instant) => (instant ? dateTimeFmt.format(new Date(instant)) : '—');

export function formatRange(start, end) {
  if (!start) return '—';
  if (start === end) return formatDate(start);
  return `${formatShortDate(start)} – ${formatDate(end)}`;
}

export function relativeTime(instant) {
  const diff = (Date.now() - new Date(instant).getTime()) / 1000;
  if (diff < 60) return 'just now';
  if (diff < 3600) return `${Math.floor(diff / 60)} min ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} h ago`;
  if (diff < 7 * 86400) return `${Math.floor(diff / 86400)} d ago`;
  return formatDateTime(instant);
}

export const plural = (n, word) => `${n} ${word}${Number(n) === 1 ? '' : 's'}`;

export const num = (value) => {
  const n = Number(value);
  return Number.isInteger(n) ? String(n) : n.toFixed(1);
};

export const STATUS_LABELS = {
  PENDING_MANAGER: 'Pending manager',
  PENDING_HR: 'Pending HR',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  ESCALATED: 'Escalated',
  CANCEL_REQUESTED: 'Cancel requested',
  CANCELLED: 'Cancelled',
};

export const ACTION_LABELS = {
  CREATED: 'Submitted',
  MANAGER_APPROVED: 'Manager approved',
  MANAGER_REJECTED: 'Manager rejected',
  HR_APPROVED: 'HR approved',
  HR_REJECTED: 'HR rejected',
  ESCALATED: 'Escalated to HR',
  WITHDRAWN: 'Withdrawn',
  CANCELLATION_REQUESTED: 'Cancellation requested',
  CANCELLATION_APPROVED: 'Cancellation approved',
  CANCELLATION_REJECTED: 'Cancellation declined',
  BALANCE_UPDATED: 'Balance updated',
  POLICY_CREATED: 'Policy created',
  POLICY_UPDATED: 'Policy updated',
  SETTINGS_UPDATED: 'Settings updated',
  HOLIDAY_ADDED: 'Holiday added',
  HOLIDAY_REMOVED: 'Holiday removed',
};

export const humanize = (value) =>
  value ? value.toLowerCase().replace(/_/g, ' ').replace(/^\w/, (c) => c.toUpperCase()) : '';
