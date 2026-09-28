import { useSearchParams } from 'react-router-dom';
import { api } from '../../api';
import LeaveTable from '../../components/LeaveTable';
import PageHeader from '../../components/PageHeader';
import { AsyncBoundary } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { STATUS_LABELS } from '../../lib/format';

export default function AllLeavesPage() {
  const [params, setParams] = useSearchParams();
  const status = params.get('status') || '';
  const teamId = params.get('team') || '';
  const teams = useAsync(() => api.teams(), []);
  const state = useAsync(
    () => api.hrLeaves({ status: status || undefined, teamId: teamId || undefined }),
    [status, teamId],
  );

  const update = (key, value) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  };

  return (
    <>
      <PageHeader title="All leave" subtitle="Every leave request across the organisation.">
        <select className="select" style={{ width: 190 }} aria-label="Filter by status" value={status}
          onChange={(e) => update('status', e.target.value)}>
          <option value="">All statuses</option>
          {Object.entries(STATUS_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select className="select" style={{ width: 170 }} aria-label="Filter by team" value={teamId}
          onChange={(e) => update('team', e.target.value)}>
          <option value="">All teams</option>
          {(teams.data || []).map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
        </select>
      </PageHeader>
      <div className="card">
        <AsyncBoundary state={state}>
          {(leaves) => (
            <LeaveTable leaves={leaves} showEmployee showWarnings emptyTitle="No matching requests"
              emptyText="Try a different status or team." />
          )}
        </AsyncBoundary>
      </div>
    </>
  );
}
