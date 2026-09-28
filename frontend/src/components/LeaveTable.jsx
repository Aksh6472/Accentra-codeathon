import { useNavigate } from 'react-router-dom';
import { formatRange, plural, relativeTime } from '../lib/format';
import Icon from './Icon';
import LeaveTypeChip from './LeaveTypeChip';
import StatusBadge from './StatusBadge';
import { EmptyState } from './States';

/**
 * Leave request list. Rows open the request detail page, where decisions are made.
 * `showEmployee` adds the employee column (manager/HR views).
 */
export default function LeaveTable({ leaves, showEmployee = false, emptyTitle = 'No requests', emptyText, showWarnings = false }) {
  const navigate = useNavigate();
  if (!leaves.length) {
    return <EmptyState title={emptyTitle}>{emptyText}</EmptyState>;
  }
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            {showEmployee && <th>Employee</th>}
            <th>Type</th>
            <th>Dates</th>
            <th className="right">Days</th>
            <th>Status</th>
            {showWarnings && <th>Team impact</th>}
            <th>Submitted</th>
          </tr>
        </thead>
        <tbody>
          {leaves.map((leave) => (
            <tr
              key={leave.id}
              className="clickable"
              tabIndex={0}
              onClick={() => navigate(`/leaves/${leave.id}`)}
              onKeyDown={(e) => e.key === 'Enter' && navigate(`/leaves/${leave.id}`)}
            >
              {showEmployee && (
                <td>
                  <div className="cell-main">{leave.employeeName}</div>
                  <div className="cell-sub">{leave.teamName || '—'}</div>
                </td>
              )}
              <td><LeaveTypeChip code={leave.leaveTypeCode} name={leave.leaveTypeName} /></td>
              <td className="nowrap">{formatRange(leave.startDate, leave.endDate)}</td>
              <td className="right num">{leave.days}</td>
              <td><StatusBadge status={leave.status} /></td>
              {showWarnings && (
                <td>
                  {leave.teamLeaveWarning ? (
                    <span className="warning-pill"><Icon name="alert" size={12} />{Number(leave.teamAbsencePercent)}% absent</span>
                  ) : leave.hasTeamConflict ? (
                    <span className="info-pill">Overlap</span>
                  ) : (
                    <span className="muted small">—</span>
                  )}
                </td>
              )}
              <td className="muted nowrap" title={leave.createdAt}>{relativeTime(leave.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <span className="sr-only">{plural(leaves.length, 'request')}</span>
    </div>
  );
}
