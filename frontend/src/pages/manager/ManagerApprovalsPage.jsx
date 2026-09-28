import { useSearchParams } from 'react-router-dom';
import { api } from '../../api';
import LeaveTable from '../../components/LeaveTable';
import PageHeader from '../../components/PageHeader';
import { AsyncBoundary } from '../../components/States';
import useAsync from '../../hooks/useAsync';

const TABS = [
  { key: 'pending', label: 'Awaiting you', empty: 'No requests are waiting for your decision.' },
  { key: 'escalated', label: 'Escalated', empty: 'No requests have been escalated to HR.' },
  { key: 'history', label: 'Decided', empty: 'Requests you have acted on will appear here.' },
];

export default function ManagerApprovalsPage() {
  const [params, setParams] = useSearchParams();
  const active = TABS.find((t) => t.key === params.get('tab')) || TABS[0];
  const state = useAsync(() => Promise.all([api.managerPending(), api.managerEscalated(), api.managerHistory()]), []);

  return (
    <>
      <PageHeader title="Approval Queue" subtitle="Open a request to see the balance, team conflicts and history, then approve or reject it." />
      <AsyncBoundary state={state}>
        {([pending, escalated, history]) => {
          const lists = { pending, escalated, history };
          return (
            <>
              <div className="tabs" role="tablist">
                {TABS.map((t) => (
                  <button key={t.key} type="button" role="tab" aria-selected={t.key === active.key}
                    className={`tab ${t.key === active.key ? 'active' : ''}`}
                    onClick={() => setParams({ tab: t.key }, { replace: true })}>
                    {t.label} <span className="count">{lists[t.key].length}</span>
                  </button>
                ))}
              </div>
              <div className="card">
                <LeaveTable leaves={lists[active.key]} showEmployee showWarnings={active.key !== 'history'}
                  actionLabel={active.key === 'pending' ? 'Review' : 'View'}
                  emptyTitle="Nothing here" emptyText={active.empty} />
              </div>
            </>
          );
        }}
      </AsyncBoundary>
    </>
  );
}
