import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../../api';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import { AsyncBoundary, EmptyState } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { ACTION_LABELS, formatDateTime } from '../../lib/format';

const PAGE_SIZE = 25;

export default function AuditLogPage() {
  const [params, setParams] = useSearchParams();
  const action = params.get('action') || '';
  const page = Number(params.get('page') || 0);
  const state = useAsync(() => api.audit({ action: action || undefined, page, size: PAGE_SIZE }), [action, page]);

  const update = (changes) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([k, v]) => (v === '' || v == null ? next.delete(k) : next.set(k, v)));
    setParams(next, { replace: true });
  };

  return (
    <>
      <PageHeader title="Audit history" subtitle="Every state change, balance movement and configuration change, newest first.">
        <select className="select" style={{ width: 220 }} aria-label="Filter by action" value={action}
          onChange={(e) => update({ action: e.target.value, page: '' })}>
          <option value="">All actions</option>
          {Object.entries(ACTION_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </PageHeader>
      <div className="card">
        <AsyncBoundary state={state}>
          {(result) =>
            result.content.length === 0 ? (
              <EmptyState icon="clock" title="No audit entries" />
            ) : (
              <>
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr><th>When</th><th>Actor</th><th>Action</th><th>Request</th><th>Status change</th><th>Details</th></tr>
                    </thead>
                    <tbody>
                      {result.content.map((e) => (
                        <tr key={e.id}>
                          <td className="nowrap muted">{formatDateTime(e.createdAt)}</td>
                          <td><div className="cell-main">{e.actorName}</div><div className="cell-sub">{e.actorRole.toLowerCase()}</div></td>
                          <td className="nowrap">{ACTION_LABELS[e.action] || e.action}</td>
                          <td>{e.leaveRequestId ? <Link to={`/leaves/${e.leaveRequestId}`}>#{e.leaveRequestId}</Link> : <span className="muted">—</span>}</td>
                          <td className="nowrap">
                            {e.newStatus && e.previousStatus !== e.newStatus ? (
                              <>{e.previousStatus && <><StatusBadge status={e.previousStatus} /> → </>}<StatusBadge status={e.newStatus} /></>
                            ) : <span className="muted">—</span>}
                          </td>
                          <td style={{ maxWidth: 360 }}>{e.comment || <span className="muted">—</span>}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div className="card-body row" style={{ justifyContent: 'space-between' }}>
                  <span className="muted small">
                    Page {result.page + 1} of {Math.max(1, result.totalPages)} · {result.totalElements} entries
                  </span>
                  <div className="row">
                    <button type="button" className="btn btn-sm" disabled={page === 0} onClick={() => update({ page: page - 1 || '' })}>Previous</button>
                    <button type="button" className="btn btn-sm" disabled={page + 1 >= result.totalPages} onClick={() => update({ page: page + 1 })}>Next</button>
                  </div>
                </div>
              </>
            )
          }
        </AsyncBoundary>
      </div>
    </>
  );
}
