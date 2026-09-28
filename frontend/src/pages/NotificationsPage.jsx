import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { errorMessage } from '../api/client';
import PageHeader from '../components/PageHeader';
import { Alert, AsyncBoundary, EmptyState } from '../components/States';
import useAsync from '../hooks/useAsync';
import { formatDateTime, relativeTime } from '../lib/format';

export default function NotificationsPage() {
  const state = useAsync(() => api.notifications(), []);
  const [error, setError] = useState(null);

  const markAll = async () => {
    try {
      await api.markAllNotificationsRead();
      state.reload();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const open = async (n) => {
    if (!n.read) {
      api.markNotificationRead(n.id).then(state.reload).catch(() => {});
    }
  };

  return (
    <>
      <PageHeader title="Notifications" subtitle="Updates about leave requests you are involved in.">
        {state.data?.unreadCount > 0 && (
          <button type="button" className="btn" onClick={markAll}>Mark all as read</button>
        )}
      </PageHeader>
      {error && <Alert tone="error">{error}</Alert>}
      <div className="card">
        <AsyncBoundary state={state}>
          {(data) =>
            data.notifications.length === 0 ? (
              <EmptyState icon="bell" title="You're all caught up">New notifications will appear here.</EmptyState>
            ) : (
              <ul className="notif-list">
                {data.notifications.map((n) => (
                  <li key={n.id} className={`notif ${n.read ? 'read' : 'unread'}`}>
                    <span className="dot" aria-hidden="true" />
                    <div className="body">
                      <div className="title">{n.title}{!n.read && <span className="sr-only"> (unread)</span>}</div>
                      <p>{n.message}</p>
                      {n.leaveRequestId && (
                        <Link to={`/leaves/${n.leaveRequestId}`} className="small" onClick={() => open(n)}>
                          View request
                        </Link>
                      )}
                    </div>
                    <div className="stack" style={{ gap: 6, alignItems: 'flex-end' }}>
                      <span className="time" title={formatDateTime(n.createdAt)}>{relativeTime(n.createdAt)}</span>
                      {!n.read && (
                        <button type="button" className="btn btn-sm btn-ghost" onClick={() => open(n)}>Mark read</button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            )
          }
        </AsyncBoundary>
      </div>
    </>
  );
}
