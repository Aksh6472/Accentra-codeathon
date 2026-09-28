import { Link } from 'react-router-dom';
import { formatShortDate, initials, parseDate, todayIso } from '../lib/format';
import { EmptyState } from './States';
import { leaveTypeColor } from './LeaveTypeChip';

const dow = new Intl.DateTimeFormat(undefined, { weekday: 'narrow' });

/**
 * Compact horizontal "who is away" view over a team calendar response. Overlaps are highlighted
 * as information only — they never block anything.
 */
export default function AvailabilityTimeline({ calendar, linkLeaves = true }) {
  const { days, leaves, members } = calendar;
  const n = days.length;
  const today = todayIso();
  const cols = { gridTemplateColumns: `repeat(${n}, minmax(0, 1fr))` };
  const index = Object.fromEntries(days.map((d, i) => [d.date, i]));
  const byEmployee = members
    .map((m) => ({ ...m, leaves: leaves.filter((l) => l.employeeId === m.employeeId) }))
    .filter((m) => m.leaves.length > 0);

  if (byEmployee.length === 0) {
    return <EmptyState icon="calendar" title="Everyone is available">No leave booked in this period.</EmptyState>;
  }

  const clamp = (iso, edge) => (index[iso] !== undefined ? index[iso] : edge);
  return (
    <div className="avail">
      <div />
      <div className="avail-days" style={cols} aria-hidden="true">
        {days.map((d) => {
          const date = parseDate(d.date);
          return (
            <span key={d.date} className={`${d.date === today ? 'today' : ''} ${d.weekend ? 'weekend' : ''}`}>
              {dow.format(date)} {date.getDate()}
            </span>
          );
        })}
      </div>
      {byEmployee.map((m) => (
        <div key={m.employeeId} style={{ display: 'contents' }}>
          <div className="avail-name">
            <span className="avatar sm">{initials(m.fullName)}</span>
            <span title={m.fullName}>{m.fullName}</span>
          </div>
          <div className="avail-track" style={cols}>
            {days.map((d) => (
              <i key={d.date} className={`${d.weekend ? 'weekend' : ''} ${d.date === today ? 'today' : ''} ${d.exceedsThreshold ? 'over' : ''}`} />
            ))}
            {m.leaves.map((l) => {
              const start = l.startDate < days[0].date ? 0 : clamp(l.startDate, 0);
              const end = l.endDate > days[n - 1].date ? n - 1 : clamp(l.endDate, n - 1);
              const color = leaveTypeColor(l.leaveTypeCode);
              const style = {
                left: `calc(${(start / n) * 100}% + 2px)`,
                width: `calc(${((end - start + 1) / n) * 100}% - 4px)`,
                background: `color-mix(in srgb, ${color} 22%, var(--surface))`,
                borderColor: `color-mix(in srgb, ${color} 55%, transparent)`,
              };
              const label = `${l.employeeName}: ${l.leaveTypeName}, ${formatShortDate(l.startDate)} – ${formatShortDate(l.endDate)}${l.status === 'APPROVED' ? '' : ' (pending)'}`;
              const className = `avail-bar ${l.status === 'APPROVED' ? '' : 'tentative'}`;
              return linkLeaves ? (
                <Link key={l.requestId} to={`/leaves/${l.requestId}`} className={className} style={style} title={label}>{l.leaveTypeName}</Link>
              ) : (
                <span key={l.requestId} className={className} style={style} title={label}>{l.leaveTypeName}</span>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}

/** The busiest working day in a calendar window, or null when nobody overlaps. */
export function busiestDay(days) {
  const top = days.filter((d) => !d.weekend && d.unavailableCount >= 2)
    .sort((a, b) => b.unavailableCount - a.unavailableCount)[0];
  return top || null;
}
