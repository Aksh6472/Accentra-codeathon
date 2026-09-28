import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../../api';
import Icon from '../../components/Icon';
import LeaveTypeChip from '../../components/LeaveTypeChip';
import { AsyncBoundary } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { formatDate, initials, num } from '../../lib/format';

const ROLE_LABEL = { EMPLOYEE: 'Employee', MANAGER: 'Manager', HR: 'HR' };
const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

/** Which month accrual starts in, mirroring LeaveProrationService's mid-month cut-off. */
function accrualStart(joiningDate, year, cutoffDay) {
  const [y, m, d] = joiningDate.split('-').map(Number);
  if (y < year) return `January ${year} (joined in an earlier year)`;
  if (y > year) return null;
  const month = d <= cutoffDay ? m : m + 1;
  return month > 12 ? null : `${MONTHS[month - 1]} ${year}`;
}

export default function EmployeeDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);
  const state = useAsync(() => api.hrEmployeeBalances(id, year), [id, year]);

  return (
    <div className="stack">
      <div>
        <button type="button" className="btn btn-ghost btn-sm" style={{ marginLeft: -12 }} onClick={() => navigate(-1)}>
          <Icon name="arrowLeft" size={16} /> Employees
        </button>
      </div>
      <AsyncBoundary state={state} loadingLabel="Loading employee…">
        {({ profile: p, eligibleMonths, midMonthCutoffDay, balances }) => {
          const start = accrualStart(p.joiningDate, year, midMonthCutoffDay);
          const midYear = eligibleMonths > 0 && eligibleMonths < 12;
          return (
            <>
              <section className="card">
                <div className="hero">
                  <div className="cell-person" style={{ gap: 16 }}>
                    <span className="avatar lg">{initials(p.fullName)}</span>
                    <div>
                      <div className="hero-title" style={{ marginTop: 0 }}>{p.fullName}</div>
                      <div className="muted">{p.jobTitle || ROLE_LABEL[p.role]} · <span className="code">{p.employeeCode}</span></div>
                    </div>
                  </div>
                  <select className="select" style={{ width: 110 }} aria-label="Year" value={year} onChange={(e) => setYear(Number(e.target.value))}>
                    {[currentYear - 1, currentYear, currentYear + 1].map((y) => <option key={y} value={y}>{y}</option>)}
                  </select>
                </div>
                <hr className="divider" />
                <div className="card-body">
                  <dl className="detail-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))' }}>
                    <div><dt>Email</dt><dd>{p.email}</dd></div>
                    <div><dt>Department</dt><dd>{p.teamName || '—'}</dd></div>
                    <div><dt>Manager</dt><dd>{p.managerName || '—'}</dd></div>
                    <div><dt>Role</dt><dd>{ROLE_LABEL[p.role]}</dd></div>
                    <div><dt>Join date</dt><dd>{formatDate(p.joiningDate)}</dd></div>
                  </dl>
                </div>
              </section>

              <div className="split">
                <section className="card">
                  <div className="card-header">
                    <div>
                      <h2>Leave Balances {year}</h2>
                      <p>Reserved days are held for requests still awaiting approval.</p>
                    </div>
                  </div>
                  <div className="table-wrap">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Leave type</th><th className="right">Annual</th><th className="right">Allocated</th>
                          <th className="right">Used</th><th className="right">Reserved</th><th className="right">Available</th>
                        </tr>
                      </thead>
                      <tbody>
                        {balances.map(({ balance: b }) => (
                          <tr key={b.leaveTypeId}>
                            <td><LeaveTypeChip code={b.leaveTypeCode} name={b.leaveTypeName} /></td>
                            <td className="right num muted">{num(b.annualEntitlement)}</td>
                            <td className="right num">{num(b.allocated)}</td>
                            <td className="right num">{num(b.used)}</td>
                            <td className="right num">{num(b.pending)}</td>
                            <td className="right num"><strong>{num(b.remaining)}</strong></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </section>

                <section className="card">
                  <div className="card-header plain">
                    <h2>How the allocation was calculated</h2>
                  </div>
                  <div className="card-body stack" style={{ gap: 16, paddingTop: 8 }}>
                    <dl className="kv">
                      <div><dt>Join date</dt><dd>{formatDate(p.joiningDate)}</dd></div>
                      <div><dt>Accrual starts</dt><dd>{start || 'Not employed this year'}</dd></div>
                      <div><dt>Eligible months in {year}</dt><dd>{eligibleMonths} of 12</dd></div>
                      <div><dt>Rounding</dt><dd>Nearest 0.5 day</dd></div>
                    </dl>
                    <div className="stack-sm">
                      {balances.map(({ balance: b, prorated, exactProrated }) => (
                        <div key={b.leaveTypeId} className="reason-box" style={{ whiteSpace: 'normal' }}>
                          <div className="row" style={{ justifyContent: 'space-between', marginBottom: 4 }}>
                            <LeaveTypeChip code={b.leaveTypeCode} name={b.leaveTypeName} />
                            <strong className="num">{num(b.allocated)} days</strong>
                          </div>
                          <div className="small muted num">
                            {prorated
                              ? <>{num(b.annualEntitlement)} × {eligibleMonths} ÷ 12 = {Number(exactProrated).toFixed(2)} → rounded to {num(b.allocated)}</>
                              : <>Not pro-rated: full {num(b.annualEntitlement)}-day entitlement</>}
                          </div>
                        </div>
                      ))}
                    </div>
                    <p className="small muted">
                      {midYear ? 'Mid-year joiner: ' : ''}A joining month counts in full when the employee joins on or before
                      the {midMonthCutoffDay}th; otherwise accrual starts the following month.
                    </p>
                  </div>
                </section>
              </div>
            </>
          );
        }}
      </AsyncBoundary>
    </div>
  );
}
