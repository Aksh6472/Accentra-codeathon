import { Link } from 'react-router-dom';
import { api } from '../../api';
import HistoryTimeline from '../../components/HistoryTimeline';
import Icon from '../../components/Icon';
import LeaveTable from '../../components/LeaveTable';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import { AsyncBoundary } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { num } from '../../lib/format';
import { TeamAvailabilityTable } from './AnalyticsPage';

export default function HrDashboard() {
  const state = useAsync(
    () => Promise.all([api.hrPending(), api.hrEscalated(), api.analytics(), api.audit({ size: 6 })]),
    [],
  );

  return (
    <>
      <PageHeader title="HR overview" subtitle="Final approvals, escalations and organisation-wide leave health.">
        <Link to="/hr/approvals" className="btn btn-primary"><Icon name="inbox" size={16} /> HR approval queue</Link>
      </PageHeader>
      <AsyncBoundary state={state}>
        {([pending, escalated, analytics, audit]) => (
          <div className="stack">
            <div className="grid grid-4">
              <StatCard label="Pending HR approval" value={pending.length} tone="warn" to="/hr/approvals" hint="Manager-approved & cancellations" />
              <StatCard label="Escalated" value={escalated.length} tone="critical" to="/hr/escalations" hint="Manager timeout exceeded" />
              <StatCard label={`Requests ${analytics.year}`} value={analytics.totalRequests} tone="primary" to="/hr/analytics" hint={`${analytics.approvedDays} approved days`} />
              <StatCard label="Leave utilisation" value={`${num(analytics.overallUtilization.utilizationPercent)}%`} tone="good" to="/hr/analytics" hint="Used ÷ allocated days" />
            </div>

            <div className="stack">
              <section className="card">
                <div className="card-header">
                  <h2>Escalated requests</h2>
                  <Link to="/hr/escalations" className="small">View all</Link>
                </div>
                <LeaveTable leaves={escalated.slice(0, 5)} showEmployee emptyTitle="No escalations" emptyText="Requests managers don't review in time appear here." />
              </section>
              <section className="card">
                <div className="card-header">
                  <h2>Awaiting HR approval</h2>
                  <Link to="/hr/approvals" className="small">Open queue</Link>
                </div>
                <LeaveTable leaves={pending.slice(0, 5)} showEmployee emptyTitle="Queue is clear" emptyText="Manager-approved requests appear here." />
              </section>
            </div>

            <div className="split">
              <section className="card">
                <div className="card-header">
                  <div>
                    <h2>Team availability</h2>
                    <p>Today and the next 30 days.</p>
                  </div>
                  <Link to="/team-calendar" className="small">Team calendar</Link>
                </div>
                <TeamAvailabilityTable teams={analytics.teams} />
              </section>
              <section className="card">
                <div className="card-header">
                  <h2>Recent activity</h2>
                  <Link to="/hr/audit" className="small">Audit history</Link>
                </div>
                <div className="card-body"><HistoryTimeline history={audit.content} /></div>
              </section>
            </div>
          </div>
        )}
      </AsyncBoundary>
    </>
  );
}
