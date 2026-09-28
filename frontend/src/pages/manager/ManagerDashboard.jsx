import { Link } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import Icon from '../../components/Icon';
import LeaveTable from '../../components/LeaveTable';
import LeaveTypeChip from '../../components/LeaveTypeChip';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import StatusBadge from '../../components/StatusBadge';
import { Alert, AsyncBoundary, EmptyState } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { formatDate, formatRange, num, toIsoDate, todayIso } from '../../lib/format';

const LOOKAHEAD_DAYS = 30;

async function loadDashboard() {
  const from = todayIso();
  const to = toIsoDate(new Date(Date.now() + LOOKAHEAD_DAYS * 86400000));
  const [pending, escalated, teams] = await Promise.all([api.managerPending(), api.managerEscalated(), api.teams()]);
  const calendars = await Promise.all(teams.map((t) => api.teamCalendar(t.id, from, to)));
  return { pending, escalated, calendars };
}

export default function ManagerDashboard() {
  const { user } = useAuth();
  const state = useAsync(loadDashboard, []);

  return (
    <>
      <PageHeader title={`Good day, ${user.fullName.split(' ')[0]}`} subtitle="Approvals, team availability and conflicts for your direct reports.">
        <Link to="/manager/approvals" className="btn btn-primary"><Icon name="inbox" size={16} /> Review approvals</Link>
      </PageHeader>
      <AsyncBoundary state={state}>
        {({ pending, escalated, calendars }) => {
          const today = todayIso();
          const warnings = pending.filter((l) => l.teamLeaveWarning || l.hasTeamConflict);
          const awayToday = calendars.reduce((sum, c) => sum + (c.days.find((d) => d.date === today)?.unavailableCount || 0), 0);
          const teamSize = calendars.reduce((sum, c) => sum + c.team.memberCount, 0);
          const overDays = calendars.flatMap((c) => c.days.filter((d) => d.exceedsThreshold).map((d) => ({ ...d, team: c.team.name })));
          const upcoming = calendars
            .flatMap((c) => c.leaves)
            .filter((l) => l.endDate >= today)
            .sort((a, b) => a.startDate.localeCompare(b.startDate));
          return (
            <div className="stack">
              <div className="grid grid-4">
                <StatCard label="Pending approvals" value={pending.length} tone="warn" to="/manager/approvals" hint="Requests & cancellations" />
                <StatCard label="Escalated to HR" value={escalated.length} tone="critical" to="/manager/approvals?tab=escalated" hint="Not reviewed in time" />
                <StatCard label="Conflict warnings" value={warnings.length} tone="primary" hint="Pending requests with overlap" />
                <StatCard label="Away today" value={`${awayToday}/${teamSize}`} tone="good" to="/team-calendar" hint={teamSize ? `${num((awayToday / teamSize) * 100)}% of your team` : 'No team'} />
              </div>

              {overDays.length > 0 && (
                <Alert tone="serious" title={`Excessive team absence in the next ${LOOKAHEAD_DAYS} days`}>
                  {overDays.slice(0, 5).map((d) => `${formatDate(d.date)} — ${d.team} ${num(d.absencePercent)}%`).join(' · ')}
                  {overDays.length > 5 ? ` · and ${overDays.length - 5} more` : ''}. <Link to="/team-calendar">Open the team calendar</Link>
                </Alert>
              )}

              <section className="card">
                <div className="card-header">
                  <div>
                    <h2>Pending approvals</h2>
                    <p>Oldest first. Requests not reviewed within the escalation timeout go to HR automatically.</p>
                  </div>
                  <Link to="/manager/approvals" className="small">Open queue</Link>
                </div>
                <LeaveTable leaves={pending.slice(0, 8)} showEmployee showWarnings emptyTitle="Nothing waiting for you" emptyText="New requests from your team will appear here." />
              </section>

              <div className="stack">
                <section className="card">
                  <div className="card-header">
                    <h2>Upcoming team leave</h2>
                    <Link to="/team-calendar" className="small">Team calendar</Link>
                  </div>
                  {upcoming.length === 0 ? (
                    <EmptyState icon="calendar" title="No leave in the next 30 days" />
                  ) : (
                    <div className="table-wrap">
                      <table className="table">
                        <thead><tr><th>Employee</th><th>Type</th><th>Dates</th><th>Status</th></tr></thead>
                        <tbody>
                          {upcoming.slice(0, 8).map((l) => (
                            <tr key={l.requestId}>
                              <td className="cell-main"><Link to={`/leaves/${l.requestId}`}>{l.employeeName}</Link></td>
                              <td><LeaveTypeChip code={l.leaveTypeCode} name={l.leaveTypeName} /></td>
                              <td className="nowrap">{formatRange(l.startDate, l.endDate)}</td>
                              <td><StatusBadge status={l.status} /></td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </section>
                <section className="card">
                  <div className="card-header"><h2>Escalated requests</h2></div>
                  <LeaveTable leaves={escalated} showEmployee emptyTitle="No escalations" emptyText="Requests you don't review in time are escalated to HR and listed here." />
                </section>
              </div>
            </div>
          );
        }}
      </AsyncBoundary>
    </>
  );
}
