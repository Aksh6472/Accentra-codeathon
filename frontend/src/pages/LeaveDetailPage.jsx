import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, performLeaveAction } from '../api';
import { useAuth } from '../auth/AuthContext';
import ApprovalStepper from '../components/ApprovalStepper';
import { BalanceCard } from '../components/BalanceCards';
import ConflictPanel from '../components/ConflictPanel';
import HistoryTimeline from '../components/HistoryTimeline';
import Icon from '../components/Icon';
import LeaveTypeChip from '../components/LeaveTypeChip';
import { ConfirmDialog } from '../components/Modal';
import StatusBadge from '../components/StatusBadge';
import { Alert, AsyncBoundary } from '../components/States';
import useAsync from '../hooks/useAsync';
import { formatDateTime, formatRange, initials, num, plural, requestCode } from '../lib/format';

const ACTIONS = {
  MANAGER_APPROVE: {
    label: 'Approve Request', tone: 'good', icon: 'check', title: 'Approve leave request',
    message: 'The request moves to HR for final approval.', commentLabel: 'Comment for the employee',
    success: 'Approved. The request is now with HR.',
  },
  MANAGER_REJECT: {
    label: 'Reject Request', tone: 'danger', icon: 'x', title: 'Reject leave request',
    message: 'The employee will be notified and the pending days released.', commentLabel: 'Reason for rejection',
    commentRequired: true, success: 'Request rejected.',
  },
  HR_APPROVE: {
    label: 'Approve Request', tone: 'good', icon: 'check', title: 'Give final approval',
    message: 'This is the final approval. The days move from pending to used in the employee\'s balance.',
    commentLabel: 'Comment', success: 'Leave approved and balance updated.',
  },
  HR_REJECT: {
    label: 'Reject Request', tone: 'danger', icon: 'x', title: 'Reject leave request',
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

// Negative actions first so the primary (approve) action sits at the end of the action bar.
const ORDER = ['WITHDRAW', 'REQUEST_CANCELLATION', 'MANAGER_REJECT', 'HR_REJECT', 'REJECT_CANCELLATION',
  'MANAGER_APPROVE', 'HR_APPROVE', 'APPROVE_CANCELLATION'];
// Statuses in which this request's days are counted in the balance (reserved as pending, or used).
const HOLDS_DAYS = ['PENDING_MANAGER', 'PENDING_HR', 'ESCALATED', 'APPROVED', 'CANCEL_REQUESTED'];

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
        const holdsDays = HOLDS_DAYS.includes(request.status);
        const isReview = actions.some((a) => a.startsWith('MANAGER_') || a.startsWith('HR_'));
        return (
          <div className="stack">
            <div>
              <button type="button" className="btn btn-ghost btn-sm" style={{ marginLeft: -12 }} onClick={() => navigate(-1)}>
                <Icon name="arrowLeft" size={16} /> Back
              </button>
            </div>

            <section className="card">
              <div className="hero">
                <div style={{ minWidth: 0 }}>
                  <span className="eyebrow">Leave Request · <span className="code">{requestCode(request.id)}</span></span>
                  <div className="hero-title">
                    {isOwner ? request.leaveTypeName : `${request.employeeName} · ${request.leaveTypeName}`}
                  </div>
                  <div className="hero-meta">
                    <span><Icon name="calendar" size={15} />{formatRange(request.startDate, request.endDate)}</span>
                    <span><Icon name="clock" size={15} />{plural(request.days, 'working day')}</span>
                    <span><Icon name="user" size={15} />Approver: {request.approverName}</span>
                  </div>
                </div>
                <StatusBadge status={request.status} />
              </div>
              <hr className="divider" />
              <div className="card-body">
                <ApprovalStepper status={request.status} history={history} />
              </div>
            </section>

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
                  <div className="card-header"><h2>Request Details</h2></div>
                  <div className="card-body stack" style={{ gap: 20 }}>
                    {!isOwner && (
                      <div className="cell-person">
                        <span className="avatar lg">{initials(request.employeeName)}</span>
                        <div>
                          <div className="cell-main" style={{ fontSize: 15 }}>{request.employeeName}</div>
                          <div className="cell-sub">{request.employeeCode} · {request.teamName || 'No team'}</div>
                        </div>
                      </div>
                    )}
                    <dl className="detail-grid">
                      <div><dt>Leave type</dt><dd><LeaveTypeChip code={request.leaveTypeCode} name={request.leaveTypeName} /></dd></div>
                      <div><dt>Dates</dt><dd>{formatRange(request.startDate, request.endDate)}</dd></div>
                      <div><dt>Days charged</dt><dd className="num">{plural(request.days, 'working day')}</dd></div>
                      <div><dt>Submitted</dt><dd>{formatDateTime(request.createdAt)}</dd></div>
                      <div><dt>Last updated</dt><dd>{formatDateTime(request.updatedAt)}</dd></div>
                      <div>
                        <dt>Team absence at submission</dt>
                        <dd className="row" style={{ gap: 6 }}>
                          <span className="num">{num(request.teamAbsencePercent)}%</span>
                          {request.teamLeaveWarning && <span className="warning-pill"><Icon name="alert" size={12} />Above threshold</span>}
                          {!request.teamLeaveWarning && request.hasTeamConflict && <span className="info-pill">Overlap</span>}
                        </dd>
                      </div>
                    </dl>
                    <div>
                      <h3 style={{ marginBottom: 8, color: 'var(--text-2)', fontWeight: 500 }}>Reason</h3>
                      <div className="reason-box">{request.reason}</div>
                    </div>
                  </div>
                </section>

                {teamConflicts && (
                  <section className="card">
                    <div className="card-header">
                      <div>
                        <h2>Team Conflicts</h2>
                        <p>Live check against {request.teamName || 'the team'}'s current leave. Warnings never block a decision.</p>
                      </div>
                      {request.teamId && user.role !== 'EMPLOYEE' && (
                        <Link to={`/team-calendar?team=${request.teamId}&month=${request.startDate.slice(0, 7)}`} className="btn btn-sm">Open calendar</Link>
                      )}
                    </div>
                    <div className="card-body"><ConflictPanel analysis={teamConflicts} /></div>
                  </section>
                )}

                <section className="card">
                  <div className="card-header"><h2>Approval History</h2></div>
                  <div className="card-body"><HistoryTimeline history={history} /></div>
                </section>
              </div>

              <div className="stack">
                <section className="card">
                  <div className="card-header plain">
                    <h2>Balance Impact</h2>
                    <span className="muted small">{balance.leaveTypeName} · {balance.year}</span>
                  </div>
                  <div className="card-body" style={{ paddingTop: 8 }}>
                    <dl className="kv">
                      {holdsDays && (
                        <div><dt>Before this request</dt><dd>{num(Number(balance.remaining) + request.days)} days</dd></div>
                      )}
                      <div>
                        <dt>{request.status === 'APPROVED' || request.status === 'CANCEL_REQUESTED' ? 'This request (used)' : holdsDays ? 'This request (reserved)' : 'This request'}</dt>
                        <dd>{holdsDays ? `− ${plural(request.days, 'day')}` : 'Not charged'}</dd>
                      </div>
                      <div><dt>Total reserved</dt><dd>{num(balance.pending)} days</dd></div>
                      <div className="total"><dt>Available</dt><dd>{num(balance.remaining)} days</dd></div>
                    </dl>
                  </div>
                </section>
                <BalanceCard balance={balance} />
              </div>
            </div>

            {actions.length > 0 && (
              <div className={`sticky-actions ${isReview ? '' : 'static'}`}>
                <span className="muted small">
                  {isReview
                    ? `${isOwner ? 'Your' : `${request.employeeName}'s`} request is waiting for your decision.`
                    : 'Actions available for this request.'}
                </span>
                <div className="action-bar">
                  {actions.map((a) => (
                    <button key={a} type="button"
                      className={`btn ${ACTIONS[a].tone === 'good' ? 'btn-primary' : 'btn-danger'}`}
                      onClick={() => { setSuccess(null); setPending(a); }}>
                      <Icon name={ACTIONS[a].icon} size={16} /> {ACTIONS[a].label}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {config && (
              <ConfirmDialog
                title={config.title}
                message={
                  approvingWithWarning
                    ? `${config.message} Warning: ${teamConflicts.warningMessage}`
                    : config.message
                }
                confirmLabel={config.label}
                tone={config.tone === 'good' ? 'primary' : config.tone}
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
