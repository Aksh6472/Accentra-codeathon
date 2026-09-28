import { api } from '../../api';
import LeaveTable from '../../components/LeaveTable';
import PageHeader from '../../components/PageHeader';
import { AsyncBoundary } from '../../components/States';
import useAsync from '../../hooks/useAsync';

export default function HrApprovalsPage() {
  const state = useAsync(() => api.hrPending(), []);
  return (
    <>
      <PageHeader
        title="HR approval queue"
        subtitle="Requests approved by a manager and awaiting final HR approval, plus cancellation requests. Oldest first."
      />
      <div className="card">
        <AsyncBoundary state={state}>
          {(leaves) => (
            <LeaveTable leaves={leaves} showEmployee showWarnings emptyTitle="Queue is clear"
              emptyText="Manager-approved requests will appear here." />
          )}
        </AsyncBoundary>
      </div>
    </>
  );
}
