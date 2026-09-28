import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../../api';
import Icon from '../../components/Icon';
import LeaveTable from '../../components/LeaveTable';
import PageHeader from '../../components/PageHeader';
import { AsyncBoundary } from '../../components/States';
import useAsync from '../../hooks/useAsync';

const FILTERS = [
  { key: 'all', label: 'All', match: () => true },
  {
    key: 'pending',
    label: 'Pending',
    match: (l) => ['PENDING_MANAGER', 'PENDING_HR', 'ESCALATED', 'CANCEL_REQUESTED'].includes(l.status),
  },
  { key: 'APPROVED', label: 'Approved', match: (l) => l.status === 'APPROVED' },
  { key: 'REJECTED', label: 'Rejected', match: (l) => l.status === 'REJECTED' },
  { key: 'CANCELLED', label: 'Cancelled', match: (l) => l.status === 'CANCELLED' },
];

export default function MyLeavesPage() {
  const [params, setParams] = useSearchParams();
  const active = FILTERS.find((f) => f.key === params.get('status')) || FILTERS[0];
  const state = useAsync(() => api.myLeaves(), []);

  return (
    <>
      <PageHeader title="My leave requests" subtitle="Your full leave history. Open a request to see its approval history or cancel it.">
        <Link to="/apply" className="btn btn-primary"><Icon name="plus" size={16} /> Apply for leave</Link>
      </PageHeader>
      <AsyncBoundary state={state}>
        {(leaves) => (
          <>
            <div className="tabs" role="tablist">
              {FILTERS.map((f) => (
                <button
                  key={f.key}
                  type="button"
                  role="tab"
                  aria-selected={f.key === active.key}
                  className={`tab ${f.key === active.key ? 'active' : ''}`}
                  onClick={() => setParams(f.key === 'all' ? {} : { status: f.key }, { replace: true })}
                >
                  {f.label} <span className="count">{leaves.filter(f.match).length}</span>
                </button>
              ))}
            </div>
            <div className="card">
              <LeaveTable
                leaves={leaves.filter(active.match)}
                emptyTitle="No requests here"
                emptyText="Requests matching this filter will appear here."
              />
            </div>
          </>
        )}
      </AsyncBoundary>
    </>
  );
}
