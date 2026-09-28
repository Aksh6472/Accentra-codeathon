import { Link } from 'react-router-dom';
import { api } from '../../api';
import ActivityFeed from '../../components/ActivityFeed';
import { HBarList } from '../../components/Charts';
import Icon from '../../components/Icon';
import LeaveTable from '../../components/LeaveTable';
import { leaveTypeColor } from '../../components/LeaveTypeChip';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import TeamAvailabilityCard from '../../components/TeamAvailabilityCard';
import { AsyncBoundary } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { ACTION_LABELS, STATUS_LABELS, addDays, num, requestCode, todayIso } from '../../lib/format';

const UPCOMING_DAYS = 30;
const ACTIVE = ['PENDING_MANAGER', 'PENDING_HR', 'ESCALATED', 'APPROVED', 'CANCEL_REQUESTED'];

export default function HrDashboard() {
  const state = useAsync(
    () => Promise.all([api.hrPending(), api.hrEscalated(), api.analytics(), api.audit({ size: 7 }), api.teams(), api.hrLeaves()]),
    [],
  );

  return (
    <>
      <PageHeader title="HR Overview" subtitle="Final approvals, escalations and organisation-wide leave health.">
        <Link to="/hr/approvals" className="btn btn-primary"><Icon name="inbox" size={16} /> Approval Queue</Link>
      </PageHeader>
      <AsyncBoundary state={state}>
        {([pending, escalated, analytics, audit, teams, allLeaves]) => {
          const today = todayIso();
          const horizon = addDays(today, UPCOMING_DAYS);
          const onLeaveToday = analytics.teams.reduce((sum, t) => sum + t.onLeaveToday, 0);
          const upcoming = allLeaves.filter((l) => ACTIVE.includes(l.status) && l.startDate > today && l.startDate <= horizon);
          const flagged = [...pending, ...escalated].filter((l) => l.teamLeaveWarning || l.hasTeamConflict);
          const statusData = Object.keys(STATUS_LABELS).map((k) => ({ label: STATUS_LABELS[k], value: analytics.requestsByStatus[k] || 0 }));
          const activity = audit.content.map((e) => ({
            id: e.id,
            type: e.action,
            title: `${e.actorName} · ${ACTION_LABELS[e.action] || e.action}`,
            detail: e.leaveRequestId ? requestCode(e.leaveRequestId) : null,
            at: e.createdAt,
            to: e.leaveRequestId ? `/leaves/${e.leaveRequestId}` : undefined,
          }));
          return (
            <div className="stack">
              <div className="grid grid-4">
                <StatCard label="Pending HR Reviews" value={pending.length} icon="inbox" tone="warn" to="/hr/approvals"
                  hint="Manager-approved & cancellations" />
                <StatCard label="Employees on Leave" value={onLeaveToday} icon="users" tone="good" to="/team-calendar"
                  hint="Away today across all teams" />
                <StatCard label="Upcoming Leaves" value={upcoming.length} icon="calendar" to="/hr/leaves"
                  hint={`Starting in the next ${UPCOMING_DAYS} days`} />
                <StatCard label="Flagged Conflicts" value={flagged.length} icon="alert" tone="critical" to="/hr/approvals"
                  hint={`${escalated.length} escalated request${escalated.length === 1 ? '' : 's'}`} />
              </div>

              <section className="card">
                <div className="card-header">
                  <div>
                    <h2>Pending HR Approvals</h2>
                    <p>Oldest first. Includes cancellation requests.</p>
                  </div>
                  <Link to="/hr/approvals" className="btn btn-sm">Open queue</Link>
                </div>
                <LeaveTable leaves={pending.slice(0, 6)} showEmployee showWarnings actionLabel="Review"
                  emptyTitle="Queue is clear" emptyText="Manager-approved requests will appear here." />
              </section>

              {escalated.length > 0 && (
                <section className="card">
                  <div className="card-header">
                    <div>
                      <h2>Escalated Requests</h2>
                      <p>No manager decision within the timeout. HR decides these directly.</p>
                    </div>
                    <Link to="/hr/escalations" className="btn btn-sm">View all</Link>
                  </div>
                  <LeaveTable leaves={escalated.slice(0, 5)} showEmployee showWarnings actionLabel="Review" compact />
                </section>
              )}

              {teams.length > 0 && (
                <TeamAvailabilityCard teams={teams} title="Leave Calendar" subtitle="Who is away across each team." />
              )}

              <div className="grid grid-2" style={{ alignItems: 'start' }}>
                <section className="card">
                  <div className="card-header">
                    <div>
                      <h2>Leave Statistics</h2>
                      <p>{analytics.year} · {analytics.totalRequests} requests · {num(analytics.overallUtilization.utilizationPercent)}% of allocated days used</p>
                    </div>
                    <Link to="/hr/analytics" className="btn btn-sm">Reports</Link>
                  </div>
                  <div className="card-body stack" style={{ gap: 22 }}>
                    <div>
                      <div className="eyebrow" style={{ marginBottom: 12 }}>Requests by status</div>
                      <HBarList data={statusData} />
                    </div>
                    <div>
                      <div className="eyebrow" style={{ marginBottom: 12 }}>Approved days by leave type</div>
                      <HBarList
                        data={analytics.leaveTypeDistribution.map((t) => ({ label: t.name, value: t.approvedDays, code: t.code, detail: `${t.requests} requests` }))}
                        format={(v) => `${v} d`}
                        colorFor={(d) => leaveTypeColor(d.code)}
                      />
                    </div>
                  </div>
                </section>
                <section className="card">
                  <div className="card-header">
                    <h2>Recent Activity</h2>
                    <Link to="/hr/audit" className="btn btn-sm">Audit log</Link>
                  </div>
                  <div className="card-body" style={{ paddingTop: 10 }}>
                    <ActivityFeed items={activity} />
                  </div>
                </section>
              </div>
            </div>
          );
        }}
      </AsyncBoundary>
    </>
  );
}
