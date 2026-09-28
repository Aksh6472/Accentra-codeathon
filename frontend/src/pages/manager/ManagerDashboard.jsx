import { Link } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import Icon from '../../components/Icon';
import LeaveTable from '../../components/LeaveTable';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import TeamAvailabilityCard from '../../components/TeamAvailabilityCard';
import { AsyncBoundary } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { firstName, greeting, num, plural, todayIso } from '../../lib/format';

async function loadDashboard() {
  const today = todayIso();
  const [pending, escalated, teams] = await Promise.all([api.managerPending(), api.managerEscalated(), api.teams()]);
  const todays = await Promise.all(teams.map((t) => api.teamCalendar(t.id, today, today)));
  return { pending, escalated, teams, todays };
}

export default function ManagerDashboard() {
  const { user } = useAuth();
  const state = useAsync(loadDashboard, []);

  return (
    <AsyncBoundary state={state}>
      {({ pending, escalated, teams, todays }) => {
        const conflicts = pending.filter((l) => l.teamLeaveWarning || l.hasTeamConflict);
        const teamSize = teams.reduce((sum, t) => sum + t.memberCount, 0);
        const awayToday = todays.reduce((sum, c) => sum + (c.days[0]?.unavailableCount || 0), 0);
        return (
          <>
            <PageHeader
              title={`${greeting()}, ${firstName(user.fullName)}`}
              subtitle={pending.length
                ? `You have ${plural(pending.length, 'leave request')} requiring attention.`
                : 'You are all caught up. No requests are waiting for you.'}
            >
              <Link to="/manager/approvals" className="btn btn-primary"><Icon name="inbox" size={16} /> Approval Queue</Link>
            </PageHeader>
            <div className="stack">
              <div className="grid grid-4">
                <StatCard label="Pending Approvals" value={pending.length} icon="inbox" tone="warn" to="/manager/approvals"
                  hint="Requests & cancellations" />
                <StatCard label="Team Members" value={teamSize} icon="users" to="/manager/team"
                  hint={teams.map((t) => t.name).join(', ') || 'No team'} />
                <StatCard label="On Leave Today" value={awayToday} icon="calendar" tone="good" to="/team-calendar"
                  hint={teamSize ? `${num((awayToday / teamSize) * 100)}% of your team` : 'No team'} />
                <StatCard label="Conflicts" value={conflicts.length} icon="alert" tone="critical"
                  hint="Pending requests with overlap" />
              </div>

              <section className="card">
                <div className="card-header">
                  <div>
                    <h2>Pending Approvals</h2>
                    <p>Oldest first. Requests not reviewed within the escalation timeout move to HR automatically.</p>
                  </div>
                  <Link to="/manager/approvals" className="btn btn-sm">Open queue</Link>
                </div>
                <LeaveTable leaves={pending.slice(0, 8)} showEmployee showWarnings actionLabel="Review"
                  emptyTitle="Nothing waiting for you" emptyText="New requests from your team will appear here." />
              </section>

              {teams.length > 0 && <TeamAvailabilityCard teams={teams} />}

              {escalated.length > 0 && (
                <section className="card">
                  <div className="card-header">
                    <div>
                      <h2>Escalated to HR</h2>
                      <p>These were not reviewed in time. HR now owns the decision.</p>
                    </div>
                  </div>
                  <LeaveTable leaves={escalated} showEmployee compact />
                </section>
              )}
            </div>
          </>
        );
      }}
    </AsyncBoundary>
  );
}
