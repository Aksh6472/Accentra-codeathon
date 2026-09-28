import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../../api';
import Icon from '../../components/Icon';
import PageHeader from '../../components/PageHeader';
import { AsyncBoundary, EmptyState } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { formatDate, initials, num, plural } from '../../lib/format';

const ROLE_LABEL = { EMPLOYEE: 'Employee', MANAGER: 'Manager', HR: 'HR' };
const total = (balances, field) => balances.reduce((sum, b) => sum + Number(b[field] || 0), 0);

export default function EmployeesPage() {
  const year = new Date().getFullYear();
  const state = useAsync(() => api.hrEmployees(year), [year]);
  const [q, setQ] = useState('');
  const [team, setTeam] = useState('');
  const [role, setRole] = useState('');

  return (
    <>
      <PageHeader title="Employees" subtitle={`Everyone in the organisation with their ${year} leave balance. Open an employee to see how it was calculated.`} />
      <AsyncBoundary state={state} loadingLabel="Loading employees…">
        {(rows) => <Directory rows={rows} year={year} q={q} setQ={setQ} team={team} setTeam={setTeam} role={role} setRole={setRole} />}
      </AsyncBoundary>
    </>
  );
}

function Directory({ rows, year, q, setQ, team, setTeam, role, setRole }) {
  const navigate = useNavigate();
  const teams = useMemo(() => [...new Set(rows.map((r) => r.profile.teamName).filter(Boolean))].sort(), [rows]);
  const needle = q.trim().toLowerCase();
  const visible = rows
    .filter(({ profile: p }) => !needle || [p.fullName, p.email, p.employeeCode, p.jobTitle].some((v) => v?.toLowerCase().includes(needle)))
    .filter(({ profile: p }) => !team || p.teamName === team)
    .filter(({ profile: p }) => !role || p.role === role);

  return (
    <section className="card">
      <div className="toolbar">
        <div className="search">
          <Icon name="search" size={15} />
          <input className="input" placeholder="Search name, email or ID…" aria-label="Search employees" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        <select className="select" aria-label="Department" value={team} onChange={(e) => setTeam(e.target.value)}>
          <option value="">All departments</option>
          {teams.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
        <select className="select" aria-label="Role" value={role} onChange={(e) => setRole(e.target.value)}>
          <option value="">All roles</option>
          {Object.entries(ROLE_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <span className="spacer" />
        <span className="muted small">{plural(visible.length, 'employee')}</span>
      </div>
      {visible.length === 0 ? (
        <EmptyState icon="users" title="No employees match" />
      ) : (
        <div className="table-wrap">
          <table className="table dense">
            <thead>
              <tr>
                <th>Employee</th><th>Employee ID</th><th>Department</th><th>Manager</th><th>Join date</th>
                <th>Leave balance {year}</th><th>Role</th><th className="right"><span className="sr-only">Action</span></th>
              </tr>
            </thead>
            <tbody>
              {visible.map(({ profile: p, balances }) => {
                const allocated = total(balances, 'allocated');
                const remaining = total(balances, 'remaining');
                const prorated = balances.some((b) => Number(b.allocated) < Number(b.annualEntitlement));
                const to = `/hr/employees/${p.employeeId}`;
                return (
                  <tr key={p.employeeId} className="clickable" tabIndex={0} onClick={() => navigate(to)}
                    onKeyDown={(e) => e.key === 'Enter' && navigate(to)}>
                    <td>
                      <div className="cell-person">
                        <span className="avatar sm">{initials(p.fullName)}</span>
                        <div>
                          <div className="cell-main nowrap">{p.fullName}</div>
                          <div className="cell-sub">{p.jobTitle || p.email}</div>
                        </div>
                      </div>
                    </td>
                    <td><span className="code">{p.employeeCode}</span></td>
                    <td className="nowrap">{p.teamName || <span className="muted">—</span>}</td>
                    <td className="nowrap">{p.managerName || <span className="muted">—</span>}</td>
                    <td className="nowrap">{formatDate(p.joiningDate)}</td>
                    <td style={{ minWidth: 170 }}>
                      <div className="row" style={{ gap: 6 }}>
                        <span className="num"><strong>{num(remaining)}</strong> <span className="muted">of {num(allocated)} days</span></span>
                        {prorated && <span className="badge badge-info plain" title="Pro-rated from the joining date">Pro-rated</span>}
                      </div>
                      <div className="meter" style={{ marginTop: 6 }}>
                        <span className="used" style={{ width: `${allocated ? ((allocated - remaining) / allocated) * 100 : 0}%` }} />
                      </div>
                    </td>
                    <td><span className="badge badge-cancelled plain">{ROLE_LABEL[p.role]}</span></td>
                    <td className="right">
                      <Link to={to} className="btn btn-sm" onClick={(e) => e.stopPropagation()}>View</Link>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
