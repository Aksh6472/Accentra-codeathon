import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../../api';
import Icon from '../../components/Icon';
import LeaveTypeChip from '../../components/LeaveTypeChip';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import { AsyncBoundary, EmptyState } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { STATUS_LABELS, formatRange, initials, num, plural, requestCode } from '../../lib/format';

const NO_FILTERS = { q: '', status: '', team: '', from: '', to: '' };

export default function HrApprovalsPage() {
  const state = useAsync(() => Promise.all([api.hrPending(), api.hrEscalated()]), []);
  const [filters, setFilters] = useState(NO_FILTERS);
  const set = (key) => (e) => setFilters((f) => ({ ...f, [key]: e.target.value }));

  return (
    <>
      <PageHeader
        title="HR Approval Queue"
        subtitle="Manager-approved requests, escalations and cancellation requests awaiting a final HR decision. Oldest first."
      />
      <AsyncBoundary state={state}>
        {([pending, escalated]) => (
          <QueueTable leaves={[...pending, ...escalated]} filters={filters} set={set} reset={() => setFilters(NO_FILTERS)} />
        )}
      </AsyncBoundary>
    </>
  );
}

function QueueTable({ leaves, filters, set, reset }) {
  const navigate = useNavigate();
  const teams = useMemo(() => [...new Set(leaves.map((l) => l.teamName).filter(Boolean))].sort(), [leaves]);
  const statuses = useMemo(() => [...new Set(leaves.map((l) => l.status))], [leaves]);
  const q = filters.q.trim().toLowerCase();
  const rows = leaves
    .filter((l) => !q || [l.employeeName, l.employeeCode, requestCode(l.id), l.leaveTypeName].some((v) => v?.toLowerCase().includes(q)))
    .filter((l) => !filters.status || l.status === filters.status)
    .filter((l) => !filters.team || l.teamName === filters.team)
    // Date range keeps any request that overlaps the chosen window.
    .filter((l) => !filters.from || l.endDate >= filters.from)
    .filter((l) => !filters.to || l.startDate <= filters.to)
    .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  const filtered = Object.values(filters).some(Boolean);

  return (
    <section className="card">
      <div className="toolbar">
        <div className="search">
          <Icon name="search" size={15} />
          <input className="input" placeholder="Search employee, ID or request…" aria-label="Search requests" value={filters.q} onChange={set('q')} />
        </div>
        <select className="select" aria-label="Status" value={filters.status} onChange={set('status')}>
          <option value="">All statuses</option>
          {statuses.map((s) => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
        </select>
        <select className="select" aria-label="Department" value={filters.team} onChange={set('team')}>
          <option value="">All departments</option>
          {teams.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
        <input type="date" className="input" aria-label="From date" value={filters.from} onChange={set('from')} />
        <span className="muted small">to</span>
        <input type="date" className="input" aria-label="To date" value={filters.to} min={filters.from || undefined} onChange={set('to')} />
        <span className="spacer" />
        {filtered && <button type="button" className="btn btn-sm btn-ghost" onClick={reset}>Clear filters</button>}
        <span className="muted small">{plural(rows.length, 'request')}</span>
      </div>
      {rows.length === 0 ? (
        <EmptyState title={filtered ? 'No requests match these filters' : 'Queue is clear'}>
          {filtered ? 'Try widening the date range or clearing a filter.' : 'Manager-approved requests will appear here.'}
        </EmptyState>
      ) : (
        <div className="table-wrap">
          <table className="table dense">
            <thead>
              <tr>
                <th>Request</th><th>Employee</th><th>Department</th><th>Manager</th><th>Dates</th>
                <th className="right">Days</th><th>Conflict</th><th>Status</th><th className="right"><span className="sr-only">Action</span></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((l) => (
                <tr key={l.id} className="clickable" tabIndex={0} onClick={() => navigate(`/leaves/${l.id}`)}
                  onKeyDown={(e) => e.key === 'Enter' && navigate(`/leaves/${l.id}`)}>
                  <td><span className="code">{requestCode(l.id)}</span></td>
                  <td>
                    <div className="cell-person">
                      <span className="avatar sm">{initials(l.employeeName)}</span>
                      <div>
                        <div className="cell-main nowrap">{l.employeeName}</div>
                        <div className="cell-sub"><LeaveTypeChip code={l.leaveTypeCode} name={l.leaveTypeName} /></div>
                      </div>
                    </div>
                  </td>
                  <td className="nowrap">{l.teamName || <span className="muted">—</span>}</td>
                  <td className="nowrap">{l.approverName}</td>
                  <td className="nowrap">{formatRange(l.startDate, l.endDate)}</td>
                  <td className="right num">{l.days}</td>
                  <td>
                    {l.teamLeaveWarning ? (
                      <span className="warning-pill"><Icon name="alert" size={12} />{num(l.teamAbsencePercent)}% away</span>
                    ) : l.hasTeamConflict ? <span className="info-pill">Overlap</span> : <span className="muted small">None</span>}
                  </td>
                  <td><StatusBadge status={l.status} /></td>
                  <td className="right">
                    <Link to={`/leaves/${l.id}`} className="btn btn-sm" onClick={(e) => e.stopPropagation()}>Review</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
