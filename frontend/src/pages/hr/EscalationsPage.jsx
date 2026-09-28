import { Link } from 'react-router-dom';
import { api } from '../../api';
import LeaveTable from '../../components/LeaveTable';
import PageHeader from '../../components/PageHeader';
import { Alert, AsyncBoundary } from '../../components/States';
import useAsync from '../../hooks/useAsync';

export default function EscalationsPage() {
  const state = useAsync(() => Promise.all([api.hrEscalated(), api.settings()]), []);
  return (
    <>
      <PageHeader title="Escalations" subtitle="Requests that stayed with a manager longer than the escalation timeout. HR decides these directly." />
      <AsyncBoundary state={state}>
        {([leaves, settings]) => (
          <div className="stack">
            <Alert tone="info">
              The scheduler escalates requests pending manager approval for more than{' '}
              <strong>{settings.escalationTimeoutMinutes} minutes</strong>. Change the timeout in{' '}
              <Link to="/hr/policies">Policies &amp; settings</Link>.
            </Alert>
            <div className="card">
              <LeaveTable leaves={leaves} showEmployee showWarnings emptyTitle="No escalations"
                emptyText="Escalated requests will appear here automatically." />
            </div>
          </div>
        )}
      </AsyncBoundary>
    </>
  );
}
