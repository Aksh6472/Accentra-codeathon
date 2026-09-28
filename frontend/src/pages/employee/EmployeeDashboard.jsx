import { Link } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import BalanceCards from '../../components/BalanceCards';
import Icon from '../../components/Icon';
import LeaveTable from '../../components/LeaveTable';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import { AsyncBoundary, EmptyState } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { formatDate, relativeTime, todayIso } from '../../lib/format';

const PENDING = ['PENDING_MANAGER', 'PENDING_HR', 'ESCALATED', 'CANCEL_REQUESTED'];

export default function EmployeeDashboard() {
  const { user } = useAuth();
  const state = useAsync(() => Promise.all([api.myBalances(), api.myLeaves(), api.notifications()]), []);

  return (
    <>
      <PageHeader
        title={`Welcome back, ${user.fullName.split(' ')[0]}`}
        subtitle={`${user.teamName || 'No team'} · reporting to ${user.managerName || '—'} · joined ${formatDate(user.joiningDate)}`}
      >
        <Link to="/apply" className="btn btn-primary"><Icon name="plus" size={16} /> Apply for leave</Link>
      </PageHeader>
      <AsyncBoundary state={state}>
        {([balances, leaves, notifications]) => {
          const year = new Date().getFullYear();
          const thisYear = leaves.filter((l) => Number(l.startDate.slice(0, 4)) === year);
          const count = (statuses) => thisYear.filter((l) => statuses.includes(l.status)).length;
          const today = todayIso();
          const upcoming = leaves
            .filter((l) => l.endDate >= today && ['APPROVED', ...PENDING].includes(l.status))
            .sort((a, b) => a.startDate.localeCompare(b.startDate));
          return (
            <div className="stack">
              <section>
                <h2 style={{ marginBottom: 12 }}>Leave balance {year}</h2>
                <BalanceCards balances={balances} />
              </section>
              <div className="grid grid-3">
                <StatCard label="Pending requests" value={count(PENDING)} tone="warn" to="/my-leaves?status=pending" hint="Awaiting a decision" />
                <StatCard label="Approved" value={count(['APPROVED'])} tone="good" to="/my-leaves?status=APPROVED" hint={`In ${year}`} />
                <StatCard label="Rejected" value={count(['REJECTED'])} tone="critical" to="/my-leaves?status=REJECTED" hint={`In ${year}`} />
              </div>
              <div className="split">
                <section className="card">
                  <div className="card-header">
                    <h2>Upcoming leave</h2>
                    <Link to="/my-leaves" className="small">All requests</Link>
                  </div>
                  <LeaveTable
                    leaves={upcoming.slice(0, 6)}
                    emptyTitle="No upcoming leave"
                    emptyText="Approved and pending leave from today onwards appears here."
                  />
                </section>
                <section className="card">
                  <div className="card-header">
                    <h2>Notifications</h2>
                    <Link to="/notifications" className="small">View all</Link>
                  </div>
                  {notifications.notifications.length === 0 ? (
                    <EmptyState icon="bell" title="No notifications" />
                  ) : (
                    <ul className="notif-list">
                      {notifications.notifications.slice(0, 5).map((n) => (
                        <li key={n.id} className={`notif ${n.read ? 'read' : 'unread'}`}>
                          <span className="dot" aria-hidden="true" />
                          <div className="body">
                            <div className="title">{n.title}</div>
                            <p className="small">{n.message}</p>
                          </div>
                          <span className="time">{relativeTime(n.createdAt)}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </section>
              </div>
            </div>
          );
        }}
      </AsyncBoundary>
    </>
  );
}
