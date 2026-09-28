import { useState } from 'react';
import { api } from '../../api';
import { BarChart, HBarList } from '../../components/Charts';
import Icon from '../../components/Icon';
import LeaveTypeChip, { leaveTypeColor } from '../../components/LeaveTypeChip';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import { AsyncBoundary, EmptyState } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { STATUS_LABELS, num } from '../../lib/format';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export function TeamAvailabilityTable({ teams }) {
  if (!teams.length) return <EmptyState icon="users" title="No teams" />;
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Team</th>
            <th className="right">Size</th>
            <th className="right">Away today</th>
            <th style={{ minWidth: 180 }}>Peak absence, next 30 days</th>
            <th className="right">Approved days</th>
          </tr>
        </thead>
        <tbody>
          {teams.map((t) => {
            const peak = Number(t.peakAbsencePercentNext30Days);
            return (
              <tr key={t.teamId}>
                <td className="cell-main">{t.name}</td>
                <td className="right num">{t.teamSize}</td>
                <td className="right num">{t.onLeaveToday} <span className="muted">({num(t.absencePercentToday)}%)</span></td>
                <td>
                  <div className="row" style={{ gap: 8, flexWrap: 'nowrap' }}>
                    <div className="threshold-meter" style={{ flex: 1 }} aria-hidden="true">
                      <div className={`fill ${t.exceedsThresholdNext30Days ? 'over' : ''}`} style={{ width: `${Math.min(100, peak)}%` }} />
                    </div>
                    <span className="num" style={{ minWidth: 46, textAlign: 'right' }}>{num(peak)}%</span>
                    {t.exceedsThresholdNext30Days && <span className="warning-pill"><Icon name="alert" size={12} />Over</span>}
                  </div>
                </td>
                <td className="right num">{t.approvedDays}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

export default function AnalyticsPage() {
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);
  const state = useAsync(() => api.analytics(year), [year]);

  return (
    <>
      <PageHeader title="Leave analytics" subtitle="Requests are counted in the year and month they start. Utilisation = used ÷ allocated days.">
        <select className="select" style={{ width: 120 }} aria-label="Year" value={year} onChange={(e) => setYear(Number(e.target.value))}>
          {[currentYear - 1, currentYear, currentYear + 1].map((y) => <option key={y} value={y}>{y}</option>)}
        </select>
      </PageHeader>
      <AsyncBoundary state={state} loadingLabel="Crunching numbers…">
        {(a) => {
          const s = a.requestsByStatus;
          const pending = (s.PENDING_MANAGER || 0) + (s.PENDING_HR || 0);
          const statusData = Object.keys(STATUS_LABELS).map((k) => ({ label: STATUS_LABELS[k], value: s[k] || 0 }));
          return (
            <div className="stack">
              <div className="grid grid-3">
                <StatCard label="Total requests" value={a.totalRequests} tone="primary" hint={`${a.approvedDays} approved days`} />
                <StatCard label="Approved" value={(s.APPROVED || 0) + (s.CANCEL_REQUESTED || 0)} tone="good" hint="Incl. pending cancellation" />
                <StatCard label="Pending" value={pending} tone="warn" hint={`${s.PENDING_MANAGER || 0} manager · ${s.PENDING_HR || 0} HR`} />
                <StatCard label="Rejected" value={s.REJECTED || 0} tone="critical" />
                <StatCard label="Escalated" value={s.ESCALATED || 0} tone="critical" hint="Awaiting HR" />
                <StatCard label="Utilisation" value={`${num(a.overallUtilization.utilizationPercent)}%`} tone="good"
                  hint={`${num(a.overallUtilization.used)} of ${num(a.overallUtilization.allocated)} days used`} />
              </div>

              <div className="grid grid-2">
                <section className="card">
                  <div className="card-header"><div><h2>Approved leave days by month</h2><p>{a.year}, by start month</p></div></div>
                  <div className="card-body">
                    <BarChart ariaLabel={`Approved leave days per month in ${a.year}`} unit=" days"
                      data={a.monthlyApprovedDays.map((m) => ({ label: MONTHS[m.month - 1], value: m.approvedDays, detail: `${MONTHS[m.month - 1]} ${a.year}` }))} />
                  </div>
                </section>
                <section className="card">
                  <div className="card-header"><h2>Requests by status</h2></div>
                  <div className="card-body">
                    <HBarList data={statusData} />
                  </div>
                </section>
              </div>

              <section className="card">
                <div className="card-header"><div><h2>Leave type distribution &amp; utilisation</h2><p>Approved days and share of allocated balance used</p></div></div>
                <div className="card-body stack">
                  <HBarList
                    data={a.leaveTypeDistribution.map((t) => ({ label: t.name, value: t.approvedDays, code: t.code, detail: `${t.requests} requests` }))}
                    format={(v) => `${v} d`}
                    colorFor={(d) => leaveTypeColor(d.code)}
                  />
                  <div className="table-wrap">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Leave type</th><th className="right">Requests</th><th className="right">Approved days</th>
                          <th className="right">Allocated</th><th className="right">Used</th><th className="right">Pending</th><th className="right">Utilisation</th>
                        </tr>
                      </thead>
                      <tbody>
                        {a.leaveTypeDistribution.map((t) => (
                          <tr key={t.code}>
                            <td><LeaveTypeChip code={t.code} name={t.name} /></td>
                            <td className="right num">{t.requests}</td>
                            <td className="right num">{t.approvedDays}</td>
                            <td className="right num">{num(t.utilization.allocated)}</td>
                            <td className="right num">{num(t.utilization.used)}</td>
                            <td className="right num">{num(t.utilization.pending)}</td>
                            <td className="right num">{num(t.utilization.utilizationPercent)}%</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </section>

              <section className="card">
                <div className="card-header"><div><h2>Team absence</h2><p>Absence today and the peak over the next 30 days</p></div></div>
                <TeamAvailabilityTable teams={a.teams} />
              </section>
            </div>
          );
        }}
      </AsyncBoundary>
    </>
  );
}
