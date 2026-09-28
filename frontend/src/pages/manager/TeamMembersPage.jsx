import { Link } from 'react-router-dom';
import { api } from '../../api';
import LeaveTypeChip from '../../components/LeaveTypeChip';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import { AsyncBoundary, EmptyState } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { addDays, formatRange, initials, todayIso } from '../../lib/format';

const LOOKAHEAD_DAYS = 60;

async function loadTeams() {
  const from = todayIso();
  const to = addDays(from, LOOKAHEAD_DAYS);
  const teams = await api.teams();
  return Promise.all(teams.map((t) => api.teamCalendar(t.id, from, to)));
}

export default function TeamMembersPage() {
  const state = useAsync(loadTeams, []);
  return (
    <>
      <PageHeader title="Team Members" subtitle={`Your direct reports, who is away today and their next leave in the coming ${LOOKAHEAD_DAYS} days.`}>
        <Link to="/team-calendar" className="btn">Team Calendar</Link>
      </PageHeader>
      <AsyncBoundary state={state}>
        {(calendars) =>
          calendars.length === 0 ? (
            <div className="card"><EmptyState icon="users" title="No teams">You don't manage a team yet.</EmptyState></div>
          ) : (
            <div className="stack">
              {calendars.map((c) => {
                const today = todayIso();
                return (
                  <section key={c.team.id} className="card">
                    <div className="card-header">
                      <div>
                        <h2>{c.team.name}</h2>
                        <p>{c.members.length} members{c.team.description ? ` · ${c.team.description}` : ''}</p>
                      </div>
                    </div>
                    {c.members.length === 0 ? (
                      <EmptyState icon="users" title="No members" />
                    ) : (
                      <div className="table-wrap">
                        <table className="table">
                          <thead><tr><th>Employee</th><th>Today</th><th>Next leave</th><th>Status</th></tr></thead>
                          <tbody>
                            {c.members.map((m) => {
                              const mine = c.leaves.filter((l) => l.employeeId === m.employeeId)
                                .sort((a, b) => a.startDate.localeCompare(b.startDate));
                              const current = mine.find((l) => l.startDate <= today && l.endDate >= today);
                              const next = current || mine[0];
                              return (
                                <tr key={m.employeeId}>
                                  <td>
                                    <div className="cell-person">
                                      <span className="avatar sm">{initials(m.fullName)}</span>
                                      <div>
                                        <div className="cell-main">{m.fullName}</div>
                                        <div className="cell-sub">{m.jobTitle || '—'}</div>
                                      </div>
                                    </div>
                                  </td>
                                  <td>
                                    {current
                                      ? <span className="badge badge-cancel plain">On leave</span>
                                      : <span className="badge badge-approved plain">Available</span>}
                                  </td>
                                  <td>
                                    {next ? (
                                      <Link to={`/leaves/${next.requestId}`} style={{ color: 'inherit' }}>
                                        <div className="nowrap">{formatRange(next.startDate, next.endDate)}</div>
                                        <div className="cell-sub"><LeaveTypeChip code={next.leaveTypeCode} name={next.leaveTypeName} /></div>
                                      </Link>
                                    ) : <span className="muted">None booked</span>}
                                  </td>
                                  <td>{next ? <StatusBadge status={next.status} /> : <span className="muted">—</span>}</td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </section>
                );
              })}
            </div>
          )
        }
      </AsyncBoundary>
    </>
  );
}
