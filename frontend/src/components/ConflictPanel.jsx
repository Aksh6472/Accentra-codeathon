import { formatDate, formatRange, formatShortDate, num } from '../lib/format';
import { Alert } from './States';
import StatusBadge from './StatusBadge';

/** Team availability impact of a request. Advisory only: it never blocks a decision. */
export default function ConflictPanel({ analysis, compact = false }) {
  if (!analysis) return null;
  if (!analysis.teamId) {
    return <p className="muted">This employee is not assigned to a team, so no team conflicts apply.</p>;
  }
  const threshold = Number(analysis.thresholdPercent);
  const peak = Number(analysis.peakAbsencePercent);

  return (
    <div className="stack" style={{ gap: 14 }}>
      {analysis.teamLeaveWarning ? (
        <Alert tone="serious" title="Excessive team absence">
          {analysis.warningMessage} This is a warning only — the decision stays with the manager.
        </Alert>
      ) : analysis.hasConflict ? (
        <Alert tone="warn" title="Overlapping team leave">{analysis.warningMessage}</Alert>
      ) : (
        <Alert tone="success" title="No team conflicts">
          Peak absence {num(peak)}% of {analysis.teamName} ({analysis.teamSize} people), below the {num(threshold)}% threshold.
        </Alert>
      )}

      <div>
        <div className="row" style={{ justifyContent: 'space-between', marginBottom: 6 }}>
          <span className="small muted">
            Peak absence {analysis.peakDate ? `on ${formatDate(analysis.peakDate)}` : ''}: <strong className="num">{analysis.peakUnavailable} of {analysis.teamSize}</strong>
          </span>
          <span className="small muted">Threshold {num(threshold)}%</span>
        </div>
        <div className="threshold-meter" role="img" aria-label={`Peak team absence ${num(peak)} percent, threshold ${num(threshold)} percent`}>
          <div className={`fill ${peak > threshold ? 'over' : ''}`} style={{ width: `${Math.min(100, peak)}%` }} />
          <div className="mark" style={{ left: `${Math.min(100, threshold)}%` }} title={`Threshold ${num(threshold)}%`} />
        </div>
      </div>

      {analysis.affectedDates.length > 0 && (
        <div>
          <h3 style={{ marginBottom: 8 }}>Affected working days</h3>
          <div className="absence-days">
            {analysis.affectedDates.slice(0, compact ? 6 : 20).map((day) => (
              <div key={day.date} className={`absence-day ${day.exceedsThreshold ? 'over' : ''}`}>
                <div className="d">{formatShortDate(day.date)}</div>
                <div className="p">{num(day.absencePercent)}%</div>
                <div className="n">{day.unavailableCount} of {day.teamSize} away</div>
                {day.employeeNames.length > 0 && <div className="n muted" title={day.employeeNames.join(', ')}>{day.employeeNames.join(', ')}</div>}
              </div>
            ))}
          </div>
        </div>
      )}

      {analysis.overlappingLeaves.length > 0 && (
        <div>
          <h3 style={{ marginBottom: 8 }}>Overlapping requests</h3>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Employee</th><th>Type</th><th>Dates</th><th>Status</th></tr>
              </thead>
              <tbody>
                {analysis.overlappingLeaves.map((o) => (
                  <tr key={o.requestId}>
                    <td className="cell-main">{o.employeeName}</td>
                    <td>{o.leaveTypeName}</td>
                    <td className="nowrap">{formatRange(o.startDate, o.endDate)}</td>
                    <td><StatusBadge status={o.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
