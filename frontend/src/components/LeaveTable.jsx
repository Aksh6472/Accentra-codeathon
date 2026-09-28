import { Link, useNavigate } from 'react-router-dom';
import { formatRange, initials, num, plural, relativeTime, requestCode } from '../lib/format';
import Icon from './Icon';
import LeaveTypeChip from './LeaveTypeChip';
import StatusBadge from './StatusBadge';
import { EmptyState } from './States';

/**
 * Leave request list. Rows open the request detail page, where decisions are made.
 * `showEmployee` adds the employee column (manager/HR views); `actionLabel` names the row action.
 */
export default function LeaveTable({
  leaves,
  showEmployee = false,
  emptyTitle = 'No requests',
  emptyText,
  showWarnings = false,
  actionLabel = 'View',
  compact = false,
}) {
  const navigate = useNavigate();
  if (!leaves.length) {
    return <EmptyState title={emptyTitle}>{emptyText}</EmptyState>;
  }
  const open = (id) => navigate(`/leaves/${id}`);
  return (
    <div className="table-wrap">
      <table className={`table ${compact ? 'dense' : ''}`}>
        <thead>
          <tr>
            {showEmployee ? <th>Employee</th> : <th>Request</th>}
            <th>Leave type</th>
            <th>Dates</th>
            <th className="right">Days</th>
            {showWarnings && <th>Conflict</th>}
            <th>Status</th>
            {!compact && <th className="right"><span className="sr-only">Action</span></th>}
          </tr>
        </thead>
        <tbody>
          {leaves.map((leave) => (
            <tr
              key={leave.id}
              className="clickable"
              tabIndex={0}
              onClick={() => open(leave.id)}
              onKeyDown={(e) => e.key === 'Enter' && open(leave.id)}
            >
              {showEmployee ? (
                <td>
                  <div className="cell-person">
                    <span className="avatar sm">{initials(leave.employeeName)}</span>
                    <div>
                      <div className="cell-main">{leave.employeeName}</div>
                      <div className="cell-sub">{requestCode(leave.id)} · {leave.teamName || 'No team'}</div>
                    </div>
                  </div>
                </td>
              ) : (
                <td>
                  <div className="cell-main code">{requestCode(leave.id)}</div>
                  <div className="cell-sub nowrap" title={leave.createdAt}>{relativeTime(leave.createdAt)}</div>
                </td>
              )}
              <td><LeaveTypeChip code={leave.leaveTypeCode} name={leave.leaveTypeName} /></td>
              <td className="nowrap">{formatRange(leave.startDate, leave.endDate)}</td>
              <td className="right num nowrap">{plural(leave.days, 'day')}</td>
              {showWarnings && (
                <td>
                  {leave.teamLeaveWarning ? (
                    <span className="warning-pill"><Icon name="alert" size={12} />{num(leave.teamAbsencePercent)}% away</span>
                  ) : leave.hasTeamConflict ? (
                    <span className="info-pill">Overlap</span>
                  ) : (
                    <span className="muted small">None</span>
                  )}
                </td>
              )}
              <td><StatusBadge status={leave.status} /></td>
              {!compact && (
                <td className="right">
                  <Link to={`/leaves/${leave.id}`} className="btn btn-sm" onClick={(e) => e.stopPropagation()}>
                    {actionLabel}
                  </Link>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
      <span className="sr-only">{plural(leaves.length, 'request')}</span>
    </div>
  );
}
