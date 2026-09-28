import { Link } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import ActivityFeed from '../../components/ActivityFeed';
import { BalanceSummary } from '../../components/BalanceCards';
import Icon from '../../components/Icon';
import LeaveTable from '../../components/LeaveTable';
import LeaveTypeChip from '../../components/LeaveTypeChip';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import StatusBadge from '../../components/StatusBadge';
import { AsyncBoundary, EmptyState } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { firstName, formatRange, num, plural, todayIso } from '../../lib/format';

const PENDING = ['PENDING_MANAGER', 'PENDING_HR', 'ESCALATED', 'CANCEL_REQUESTED'];
const sum = (list, field) => list.reduce((total, b) => total + Number(b[field] || 0), 0);

export default function EmployeeDashboard() {
  const { user } = useAuth();
  const state = useAsync(() => Promise.all([api.myBalances(), api.myLeaves(), api.notifications()]), []);

  return (
    <>
      <PageHeader title="Dashboard" subtitle={`Welcome back, ${firstName(user.fullName)}. Here's an overview of your leave.`}>
        <Link to="/apply" className="btn btn-primary"><Icon name="plus" size={16} /> Apply Leave</Link>
      </PageHeader>
      <AsyncBoundary state={state}>
        {([balances, leaves, notifications]) => {
          const year = new Date().getFullYear();
          const today = todayIso();
          const pendingRequests = leaves.filter((l) => PENDING.includes(l.status));
          const upcoming = leaves
            .filter((l) => l.endDate >= today && ['APPROVED', ...PENDING].includes(l.status))
            .sort((a, b) => a.startDate.localeCompare(b.startDate));
          const next = upcoming[0];
          const activity = notifications.notifications.slice(0, 6).map((n) => ({
            id: n.id,
            type: n.type,
            title: n.title,
            at: n.createdAt,
            unread: !n.read,
            to: n.leaveRequestId ? `/leaves/${n.leaveRequestId}` : undefined,
          }));
          return (
            <div className="stack">
              <div className="grid grid-4">
                <StatCard label="Available" value={num(sum(balances, 'remaining'))} unit="days" icon="calendarCheck"
                  hint={`Across ${plural(balances.length, 'leave type')}`} />
                <StatCard label="Reserved" value={num(sum(balances, 'pending'))} unit="days" icon="hourglass" tone="warn"
                  hint="Held for requests awaiting approval" />
                <StatCard label="Used" value={num(sum(balances, 'used'))} unit="days" icon="check" tone="good"
                  hint={`Approved leave taken in ${year}`} />
                <StatCard label="Pending" value={pendingRequests.length} unit={pendingRequests.length === 1 ? 'request' : 'requests'}
                  icon="inbox" tone="violet" to="/my-leaves?status=pending" hint="Waiting for a decision" />
              </div>

              <div className="split">
                <div className="stack">
                <section className="card">
                  <div className="card-header">
                    <div>
                      <h2>My Leave Requests</h2>
                      <p>Your most recent requests and where they are in the approval flow.</p>
                    </div>
                    <Link to="/my-leaves" className="btn btn-sm">View all</Link>
                  </div>
                  <LeaveTable
                    leaves={leaves.slice(0, 6)}
                    compact
                    emptyTitle="No leave requests yet"
                    emptyText="Requests you submit will appear here."
                  />
                </section>
                <section className="card">
                    <div className="card-header plain">
                      <h2>Recent Activity</h2>
                      <Link to="/notifications" className="small">View all</Link>
                    </div>
                    <div className="card-body" style={{ paddingTop: 6 }}>
                      <ActivityFeed items={activity} />
                    </div>
                  </section>
                </div>

                <div className="stack">
                  <section className="card">
                    <div className="card-header plain"><h2>Leave Balance</h2><span className="muted small">{year}</span></div>
                    <div className="card-body" style={{ paddingTop: 12 }}>
                      <BalanceSummary balances={balances} />
                    </div>
                  </section>
                  <section className="card">
                    <div className="card-header plain"><h2>Upcoming Leave</h2></div>
                    <div className="card-body" style={{ paddingTop: 12 }}>
                      {next ? (
                        <Link to={`/leaves/${next.id}`} className="stack-sm" style={{ color: 'inherit', gap: 8, textDecoration: 'none' }}>
                          <div className="row" style={{ justifyContent: 'space-between' }}>
                            <strong style={{ fontSize: 16 }}>{formatRange(next.startDate, next.endDate)}</strong>
                            <StatusBadge status={next.status} />
                          </div>
                          <div className="row small muted" style={{ gap: 10 }}>
                            <LeaveTypeChip code={next.leaveTypeCode} name={next.leaveTypeName} />
                            <span>·</span>
                            <span>{plural(next.days, 'working day')}</span>
                          </div>
                          {upcoming.length > 1 && <span className="small muted">+ {plural(upcoming.length - 1, 'more upcoming request')}</span>}
                        </Link>
                      ) : (
                        <EmptyState icon="calendar" title="Nothing booked">Approved and pending leave from today onwards appears here.</EmptyState>
                      )}
                    </div>
                  </section>
                </div>
              </div>
            </div>
          );
        }}
      </AsyncBoundary>
    </>
  );
}
