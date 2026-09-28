import { num } from '../lib/format';
import LeaveTypeChip from './LeaveTypeChip';

export function BalanceCard({ balance }) {
  const allocated = Number(balance.allocated) || 0;
  const pct = (v) => (allocated > 0 ? Math.min(100, (Number(v) / allocated) * 100) : 0);
  const prorated = Number(balance.allocated) < Number(balance.annualEntitlement);
  return (
    <div className="card balance-card">
      <div className="balance-top">
        <LeaveTypeChip code={balance.leaveTypeCode} name={balance.leaveTypeName} />
        {prorated && <span className="badge badge-info plain" title="Pro-rated from your joining date">Pro-rated</span>}
      </div>
      <div className="balance-remaining num">
        {num(balance.remaining)}
        <small>of {num(balance.allocated)} days left</small>
      </div>
      <div
        className="meter"
        role="img"
        aria-label={`${num(balance.used)} used and ${num(balance.pending)} reserved of ${num(balance.allocated)} allocated`}
      >
        {Number(balance.used) > 0 && <span className="used" style={{ width: `${pct(balance.used)}%` }} />}
        {Number(balance.pending) > 0 && <span className="pending" style={{ width: `${pct(balance.pending)}%` }} />}
      </div>
      <dl className="balance-legend">
        <div>
          <dt>Annual</dt>
          <dd>{num(balance.annualEntitlement)}</dd>
        </div>
        <div>
          <dt>Allocated</dt>
          <dd>{num(balance.allocated)}</dd>
        </div>
        <div>
          <dt><span className="key" style={{ background: 'var(--primary)' }} />Used</dt>
          <dd>{num(balance.used)}</dd>
        </div>
        <div>
          <dt><span className="key" style={{ background: 'var(--warn-mark)' }} />Reserved</dt>
          <dd>{num(balance.pending)}</dd>
        </div>
      </dl>
    </div>
  );
}

export default function BalanceCards({ balances }) {
  return (
    <div className="grid grid-3">
      {balances.map((b) => (
        <BalanceCard key={b.leaveTypeId} balance={b} />
      ))}
    </div>
  );
}

/** Compact per-type balance list for side columns: available days, a used/reserved meter and the breakdown. */
export function BalanceSummary({ balances }) {
  return (
    <div className="stack" style={{ gap: 20 }}>
      {balances.map((b) => {
        const allocated = Number(b.allocated) || 0;
        const pct = (v) => (allocated > 0 ? Math.min(100, (Number(v) / allocated) * 100) : 0);
        return (
          <div key={b.leaveTypeId} className="stack-sm" style={{ gap: 10 }}>
            <div className="balance-top">
              <LeaveTypeChip code={b.leaveTypeCode} name={b.leaveTypeName} />
              <span className="num"><strong style={{ fontSize: 16 }}>{num(b.remaining)}</strong> <span className="muted small">days available</span></span>
            </div>
            <div className="meter" role="img"
              aria-label={`${num(b.used)} used and ${num(b.pending)} reserved of ${num(b.allocated)} allocated`}>
              {Number(b.used) > 0 && <span className="used" style={{ width: `${pct(b.used)}%` }} />}
              {Number(b.pending) > 0 && <span className="pending" style={{ width: `${pct(b.pending)}%` }} />}
            </div>
            <div className="row small muted" style={{ gap: 14 }}>
              <span>Allocated <strong className="num" style={{ color: 'var(--text)' }}>{num(b.allocated)}</strong></span>
              <span><span className="key" style={{ background: 'var(--primary)' }} />Used <strong className="num" style={{ color: 'var(--text)' }}>{num(b.used)}</strong></span>
              <span><span className="key" style={{ background: 'var(--warn-mark)' }} />Reserved <strong className="num" style={{ color: 'var(--text)' }}>{num(b.pending)}</strong></span>
            </div>
          </div>
        );
      })}
    </div>
  );
}
