import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, performLeaveAction } from '../api';
import { useAuth } from '../auth/AuthContext';
import { BalanceCard } from '../components/BalanceCards';
import ConflictPanel from '../components/ConflictPanel';
import HistoryTimeline from '../components/HistoryTimeline';
import Icon from '../components/Icon';
import LeaveTypeChip from '../components/LeaveTypeChip';
import { ConfirmDialog } from '../components/Modal';
import StatusBadge from '../components/StatusBadge';
import { Alert, AsyncBoundary } from '../components/States';
import useAsync from '../hooks/useAsync';
import { formatDate, formatDateTime, num, plural } from '../lib/format';

const ACTIONS = {
  MANAGER_APPROVE: {
    label: 'Approve', tone: 'good', icon: 'check', title: 'Approve leave request',
    message: 'The request moves to HR for final approval.', commentLabel: 'Comment for the employee',
    success: 'Approved. The request is now with HR.',
  },
  MANAGER_REJECT: {
    label: 'Reject', tone: 'danger', icon: 'x', title: 'Reject leave request',
    message: 'The employee will be notified and the pending days released.', commentLabel: 'Reason for rejection',
    commentRequired: true, success: 'Request rejected.',
  },
  HR_APPROVE: {
    label: 'Approve', tone: 'good', icon: 'check', title: 'Give final approval',
    message: 'This is the final approval. The days move from pending to used in the employee\'s balance.',
    commentLabel: 'Comment', success: 'Leave approved and balance updated.',
  },
  HR_REJECT: {
    label: 'Reject', tone: 'danger', icon: 'x', title: 'Reject leave request',
    message: 'The employee will be notified and the pending days released.', commentLabel: 'Reason for rejection',
    commentRequired: true, success: 'Request rejected.',
  },
  WITHDRAW: {
    label: 'Withdraw request', tone: 'danger', icon: 'x', title: 'Withdraw this request?',
    message: 'The request is cancelled immediately and the pending days are returned to your balance.',
    commentLabel: 'Note', success: 'Request withdrawn.',
  },
  REQUEST_CANCELLATION: {
    label: 'Request cancellation', tone: 'danger', icon: 'x', title: 'Cancel approved leave?',
    message: 'Your manager (or HR) must confirm. The days return to your balance once the cancellation is approved.',
    commentLabel: 'Reason', success: 'Cancellation requested.',
  },
  APPROVE_CANCELLATION: {
    label: 'Approve cancellation', tone: 'good', icon: 'check', title: 'Approve cancellation',
    message: 'The leave is cancelled and the days are restored to the employee\'s balance.',
    commentLabel: 'Comment', success: 'Cancellation approved; balance restored.',
  },
  REJECT_CANCELLATION: {
    label: 'Decline cancellation', tone: 'danger', icon: 'x', title: 'Decline cancellation',
    message: 'The leave stays approved.', commentLabel: 'Reason', commentRequired: true,
    success: 'Cancellation declined.',
  },
};

const ORDER = ['MANAGER_APPROVE', 'HR_APPROVE', 'APPROVE_CANCELLATION', 'MANAGER_REJECT', 'HR_REJECT',
  'REJECT_CANCELLATION', 'REQUEST_CANCELLATION', 'WITHDRAW'];

export default function LeaveDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const state = useAsync(() => api.leave(id), [id]);
  const [pending, setPending] = useState(null);
  const [success, setSuccess] = useState(null);

  return (
    <AsyncBoundary state={state} loadingLabel="Loading request…">
      {({ request, balance, teamConflicts, history }) => {
        const actions = ORDER.filter((a) => request.availableActions.includes(a));
        const isOwner = request.employeeId === user.employeeId;
        const config = pending && ACTIONS[pending];
        const approvingWithWarning = pending?.includes('APPROVE') && teamConflicts?.teamLeaveWarning;
        return (
          <div className="stack">
            <div>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => navigate(-1)}>
                <Icon name="arrowLeft" size={16} /> Back
              </button>
            </div>
            <div className="page-header" style={{ marginBottom: 0 }}>
              <div>
                <div className="row" style={{ gap: 10 }}>
                  <h1>{isOwner ? 'Your leave request' : `${request.employeeName}'s leave request`}</h1>
                  <StatusBadge status={request.status} />
                </div>
                <p>Request #{request.id} · submitted {formatDateTime(request.createdAt)}</p>
              </div>
              {actions.length > 0 && (
                <div className="action-bar">
                  {actions.map((a) => (
                    <button key={a} type="button"
                      className={`btn ${ACTIONS[a].tone === 'good' ? 'btn-good' : 'btn-danger'}`}
                      onClick={() => { setSuccess(null); setPending(a); }}>
                      <Icon name={ACTIONS[a].icon} size={16} /> {ACTIONS[a].label}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {success && <Alert tone="success">{success}</Alert>}
            {request.status === 'ESCALATED' && (
              <Alert tone="warn" title="Escalated to HR">
                No manager decision was made within the configured timeout, so HR now owns this request
                {request.escalatedAt ? ` (escalated ${formatDateTime(request.escalatedAt)})` : ''}.
              </Alert>
            )}

            <div className="split">
              <div className="stack">
                <section className="card">
                  <div className="card-header"><h2>Request details</h2></div>
                  <div className="card-body stack">
                    <dl className="detail-grid">
                      <div><dt>Employee</dt><dd>{request.employeeName}</dd><span className="cell-sub">{request.employeeCode} · {request.teamName || 'No team'}</span></div>
                      <div><dt>Leave type</dt><dd><LeaveTypeChip code={request.leaveTypeCode} name={request.leaveTypeName} /></dd></div>
                      <div><dt>Days charged</dt><dd className="num">{plural(request.days, 'working day')}</dd></div>
                      <div><dt>Start date</dt><dd>{formatDate(request.startDate)}</dd></div>
                      <div><dt>End date</dt><dd>{formatDate(request.endDate)}</dd></div>
                      <div><dt>Approving manager</dt><dd>{request.approverName}</dd></div>
                      <div>
                        <dt>Team absence at submission</dt>
                        <dd className="row" style={{ gap: 6 }}>
                          <span className="num">{num(request.teamAbsencePercent)}%</span>
                          {request.teamLeaveWarning && <span className="warning-pill"><Icon name="alert" size={12} />Above threshold</span>}
                          {!request.teamLeaveWarning && request.hasTeamConflict && <span className="info-pill">Overlap</span>}
                        </dd>
                      </div>
                      <div><dt>Last updated</dt><dd>{formatDateTime(request.updatedAt)}</dd></div>
                    </dl>
                    <div>
                      <h3 style={{ marginBottom: 6 }}>Reason</h3>
                      <div className="reason-box">{request.reason}</div>
                    </div>
                  </div>
                </section>

                {teamConflicts && (
                  <section className="card">
                    <div className="card-header">
                      <div>
                        <h2>Team availability</h2>
                        <p>Live check against {request.teamName || 'the team'}'s current leave.</p>
                      </div>
                      {request.teamId && <Link to={`/team-calendar?team=${request.teamId}&month=${request.startDate.slice(0, 7)}`} className="btn btn-sm">Open calendar</Link>}
                    </div>
                    <div className="card-body"><ConflictPanel analysis={teamConflicts} /></div>
                  </section>
                )}
              </div>

              <div className="stack">
                <section>
                  <h2 style={{ marginBottom: 10 }}>{isOwner ? 'Your balance' : 'Employee balance'} ({balance.year})</h2>
                  <BalanceCard balance={balance} />
                </section>
                <section className="card">
                  <div className="card-header"><h2>Approval history</h2></div>
                  <div className="card-body"><HistoryTimeline history={history} /></div>
                </section>
              </div>
            </div>

            {config && (
              <ConfirmDialog
                title={config.title}
                message={
                  approvingWithWarning
                    ? `${config.message} Warning: ${teamConflicts.warningMessage}`
                    : config.message
                }
                confirmLabel={config.label}
                tone={config.tone}
                commentLabel={config.commentLabel}
                commentRequired={config.commentRequired}
                onClose={() => setPending(null)}
                onConfirm={async (comment) => {
                  await performLeaveAction(pending, request.id, user.role, comment);
                  setPending(null);
                  setSuccess(config.success);
                  state.reload();
                }}
              />
            )}
          </div>
        );
      }}
    </AsyncBoundary>
  );
}
