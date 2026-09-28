import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api';
import Icon from '../components/Icon';
import { leaveTypeColor } from '../components/LeaveTypeChip';
import PageHeader from '../components/PageHeader';
import { Alert, AsyncBoundary, EmptyState } from '../components/States';
import useAsync from '../hooks/useAsync';
import { STATUS_LABELS, formatDate, num, parseDate, toIsoDate, todayIso } from '../lib/format';

const TENTATIVE = new Set(['PENDING_MANAGER', 'PENDING_HR', 'ESCALATED', 'CANCEL_REQUESTED']);
const monthFmt = new Intl.DateTimeFormat(undefined, { month: 'long', year: 'numeric' });
const dowFmt = new Intl.DateTimeFormat(undefined, { weekday: 'narrow' });

function monthBounds(month) {
  const [y, m] = month.split('-').map(Number);
  return { from: toIsoDate(new Date(y, m - 1, 1)), to: toIsoDate(new Date(y, m, 0)) };
}

function shiftMonth(month, delta) {
  const [y, m] = month.split('-').map(Number);
  const d = new Date(y, m - 1 + delta, 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

export default function TeamCalendarPage() {
  const [params, setParams] = useSearchParams();
  const month = params.get('month') || todayIso().slice(0, 7);
  const teams = useAsync(() => api.teams(), []);
  const teamId = params.get('team') || teams.data?.[0]?.id;

  const update = (changes) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([k, v]) => next.set(k, v));
    setParams(next, { replace: true });
  };

  return (
    <>
      <PageHeader title="Team calendar" subtitle="Who is away, overlapping leave and days where absence exceeds the team threshold.">
        {teams.data?.length > 1 && (
          <select className="select" style={{ width: 200 }} aria-label="Team" value={teamId || ''}
            onChange={(e) => update({ team: e.target.value })}>
            {teams.data.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        )}
        <div className="row" style={{ gap: 4 }}>
          <button type="button" className="btn btn-sm" aria-label="Previous month" onClick={() => update({ month: shiftMonth(month, -1) })}>
            <Icon name="chevronLeft" size={16} />
          </button>
          <strong style={{ minWidth: 130, textAlign: 'center' }}>{monthFmt.format(parseDate(`${month}-01`))}</strong>
          <button type="button" className="btn btn-sm" aria-label="Next month" onClick={() => update({ month: shiftMonth(month, 1) })}>
            <Icon name="chevronRight" size={16} />
          </button>
          <button type="button" className="btn btn-sm" onClick={() => update({ month: todayIso().slice(0, 7) })}>Today</button>
        </div>
      </PageHeader>
      <AsyncBoundary state={teams}>
        {(list) =>
          list.length === 0 ? (
            <div className="card"><EmptyState icon="users" title="No teams">You don't manage any teams yet.</EmptyState></div>
          ) : (
            <TeamMonth teamId={teamId} month={month} />
          )
        }
      </AsyncBoundary>
    </>
  );
}

function TeamMonth({ teamId, month }) {
  const { from, to } = monthBounds(month);
  const state = useAsync(() => Promise.all([api.teamCalendar(teamId, from, to), api.teamConflicts(teamId, from, to)]),
    [teamId, from, to]);

  return (
    <AsyncBoundary state={state} loadingLabel="Loading calendar…">
      {([calendar, conflicts]) => <CalendarView calendar={calendar} conflicts={conflicts} />}
    </AsyncBoundary>
  );
}

function CalendarView({ calendar, conflicts }) {
  const today = todayIso();
  const threshold = Number(calendar.thresholdPercent);
  const types = useMemo(() => {
    const map = new Map();
    calendar.leaves.forEach((l) => map.set(l.leaveTypeCode, l.leaveTypeName));
    return [...map.entries()];
  }, [calendar.leaves]);

  const leaveOn = (employeeId, date) =>
    calendar.leaves.find((l) => l.employeeId === employeeId && l.startDate <= date && l.endDate >= date);

  const overDays = calendar.days.filter((d) => d.exceedsThreshold);
  const onLeaveToday = calendar.days.find((d) => d.date === today);

  return (
    <div className="stack">
      <div className="grid grid-4">
        <div className="card stat"><span className="stat-label">Team size</span><span className="stat-value">{calendar.team.memberCount}</span><span className="stat-hint">{calendar.team.name}</span></div>
        <div className="card stat"><span className="stat-label">Away today</span><span className="stat-value">{onLeaveToday ? onLeaveToday.unavailableCount : '—'}</span><span className="stat-hint">{onLeaveToday ? `${num(onLeaveToday.absencePercent)}% of team` : 'Today is outside this month'}</span></div>
        <div className="card stat tone-warn"><span className="stat-label">Overlap days</span><span className="stat-value">{conflicts.conflictDays.length}</span><span className="stat-hint">2+ people away</span></div>
        <div className={`card stat ${overDays.length ? 'tone-critical' : ''}`}><span className="stat-label">Over threshold</span><span className="stat-value">{overDays.length}</span><span className="stat-hint">Days above {num(threshold)}%</span></div>
      </div>

      {overDays.length > 0 && (
        <Alert tone="serious" title="Excessive absence periods">
          {overDays.map((d) => `${formatDate(d.date)} (${num(d.absencePercent)}%)`).join(', ')} exceed the {num(threshold)}% team absence threshold.
        </Alert>
      )}

      <section className="card">
        <div className="card-header">
          <h2>{calendar.team.name}</h2>
          <div className="calendar-legend">
            {types.map(([code, name]) => (
              <span key={code}><i className="swatch" style={{ background: leaveTypeColor(code), borderColor: 'transparent' }} />{name}</span>
            ))}
            <span><i className="swatch leave-block tentative" style={{ backgroundColor: 'var(--text-muted)', minHeight: 0 }} />Pending / tentative</span>
            <span><i className="swatch" style={{ background: 'var(--weekend)' }} />Weekend</span>
            <span><i className="swatch" style={{ background: 'var(--holiday)' }} />Holiday</span>
            <span><i className="swatch" style={{ background: 'var(--serious-soft)', borderColor: 'var(--serious)' }} />Over threshold</span>
          </div>
        </div>
        <div className="calendar-wrap">
          <table className="calendar">
            <thead>
              <tr>
                <th className="name-col" scope="col">Team member</th>
                {calendar.days.map((d) => (
                  <th key={d.date} scope="col" title={d.holidayName || undefined}
                    className={`${d.weekend ? 'weekend' : ''} ${d.holidayName ? 'holiday' : ''} ${d.date === today ? 'today-col' : ''}`}>
                    <span className="dow">{dowFmt.format(parseDate(d.date))}</span>
                    {Number(d.date.slice(8))}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              <tr className="absence-row">
                <th className="name-col" scope="row">Team absence</th>
                {calendar.days.map((d) => {
                  const off = d.weekend || d.holidayName;
                  const cls = d.exceedsThreshold ? 'over' : d.unavailableCount > 1 ? 'conflict' : '';
                  return (
                    <td key={d.date} className={`absence-cell ${cls} ${off ? (d.weekend ? 'weekend' : 'holiday') : ''}`}
                      title={off ? d.holidayName || 'Weekend' : `${d.unavailableCount} away (${num(d.absencePercent)}%)${d.employeeNames.length ? `: ${d.employeeNames.join(', ')}` : ''}`}>
                      {off || d.unavailableCount === 0 ? '' : `${Math.round(Number(d.absencePercent))}%`}
                    </td>
                  );
                })}
              </tr>
              {calendar.members.map((m) => (
                <tr key={m.employeeId}>
                  <th className="name-col" scope="row">
                    <div className="cell-main">{m.fullName}</div>
                    <div className="cell-sub">{m.jobTitle}</div>
                  </th>
                  {calendar.days.map((d) => {
                    const off = d.weekend || d.holidayName;
                    const leave = !off && leaveOn(m.employeeId, d.date);
                    return (
                      <td key={d.date} className={`day ${d.weekend ? 'weekend' : ''} ${d.holidayName ? 'holiday' : ''} ${d.exceedsThreshold && leave ? 'absence-cell over' : ''}`}>
                        {leave && (
                          <span className={`leave-block ${TENTATIVE.has(leave.status) ? 'tentative' : ''}`}
                            style={{ backgroundColor: leaveTypeColor(leave.leaveTypeCode) }}
                            title={`${m.fullName}: ${leave.leaveTypeName}, ${STATUS_LABELS[leave.status]}`}>
                            {leave.leaveTypeCode[0]}
                          </span>
                        )}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="card">
        <div className="card-header">
          <div>
            <h2>Conflicts this month</h2>
            <p>Working days where two or more people are away, or absence is above {num(threshold)}%.</p>
          </div>
        </div>
        {conflicts.conflictDays.length === 0 ? (
          <EmptyState icon="check" title="No conflicts">Team availability looks healthy this month.</EmptyState>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Date</th><th>Away</th><th>Absence</th><th>Who</th></tr></thead>
              <tbody>
                {conflicts.conflictDays.map((d) => (
                  <tr key={d.date}>
                    <td className="nowrap cell-main">{formatDate(d.date)}</td>
                    <td className="num">{d.unavailableCount} of {d.teamSize}</td>
                    <td>
                      {d.exceedsThreshold
                        ? <span className="warning-pill"><Icon name="alert" size={12} />{num(d.absencePercent)}%</span>
                        : <span className="num">{num(d.absencePercent)}%</span>}
                    </td>
                    <td>{d.employeeNames.join(', ')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
