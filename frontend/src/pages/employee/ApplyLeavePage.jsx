import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api';
import { errorMessage, fieldErrors } from '../../api/client';
import Icon from '../../components/Icon';
import PageHeader from '../../components/PageHeader';
import { Alert, AsyncBoundary, Loading } from '../../components/States';
import StatusBadge from '../../components/StatusBadge';
import useAsync from '../../hooks/useAsync';
import { formatDate, formatRange, formatShortDate, num, plural, requestCode, todayIso } from '../../lib/format';

const EMPTY = { leaveTypeId: '', startDate: '', endDate: '', reason: '' };
const PREVIEW_DELAY_MS = 350;

function validate(form) {
  const errors = {};
  if (!form.leaveTypeId) errors.leaveTypeId = 'Choose a leave type.';
  if (!form.startDate) errors.startDate = 'Choose a start date.';
  else if (form.startDate < todayIso()) errors.startDate = 'Leave cannot start in the past.';
  if (!form.endDate) errors.endDate = 'Choose an end date.';
  else if (form.startDate && form.endDate < form.startDate) errors.endDate = 'End date cannot be before the start date.';
  if (!form.reason.trim()) errors.reason = 'Add a short reason.';
  else if (form.reason.length > 500) errors.reason = 'Keep the reason under 500 characters.';
  return errors;
}

export default function ApplyLeavePage() {
  const policies = useAsync(() => api.policies(), []);
  // Current-year balances so the form can show what is available before dates are chosen.
  const balances = useAsync(() => api.myBalances(new Date().getFullYear()), []);
  const [form, setForm] = useState(EMPTY);
  const [touched, setTouched] = useState({});
  const [serverErrors, setServerErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(null);
  const [preview, setPreview] = useState({ data: null, loading: false, error: null });

  const errors = useMemo(() => ({ ...validate(form), ...serverErrors }), [form, serverErrors]);
  const canPreview = Boolean(form.leaveTypeId && form.startDate && form.endDate && form.endDate >= form.startDate);

  // Live preview: days charged, balance impact and team availability, recalculated as the form changes.
  useEffect(() => {
    if (!canPreview) {
      setPreview({ data: null, loading: false, error: null });
      return undefined;
    }
    let cancelled = false;
    setPreview((p) => ({ ...p, loading: true, error: null }));
    const timer = setTimeout(() => {
      api
        .previewLeave({ leaveTypeId: Number(form.leaveTypeId), startDate: form.startDate, endDate: form.endDate })
        .then((data) => !cancelled && setPreview({ data, loading: false, error: null }))
        .catch((err) => !cancelled && setPreview({ data: null, loading: false, error: errorMessage(err) }));
    }, PREVIEW_DELAY_MS);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [canPreview, form.leaveTypeId, form.startDate, form.endDate]);

  const set = (field) => (e) => {
    const value = e.target.value;
    setForm((f) => ({ ...f, [field]: value }));
    setServerErrors((s) => ({ ...s, [field]: undefined }));
  };
  const blur = (field) => () => setTouched((t) => ({ ...t, [field]: true }));
  const show = (field) => touched[field] && errors[field];

  const blocked = preview.data?.blockingIssues?.length > 0;

  const submit = async (e) => {
    e.preventDefault();
    setTouched({ leaveTypeId: true, startDate: true, endDate: true, reason: true });
    if (Object.values(validate(form)).some(Boolean)) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const result = await api.applyLeave({
        leaveTypeId: Number(form.leaveTypeId),
        startDate: form.startDate,
        endDate: form.endDate,
        reason: form.reason.trim(),
      });
      setSubmitted(result);
      balances.reload();
      setForm(EMPTY);
      setTouched({});
      setPreview({ data: null, loading: false, error: null });
    } catch (err) {
      setServerErrors(fieldErrors(err));
      setSubmitError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  if (submitted) {
    const { request, teamConflicts, balance } = submitted;
    return (
      <>
        <PageHeader title="Request submitted" subtitle={`Reference ${requestCode(request.id)}`} />
        <div className="card">
          <div className="card-body stack">
            <Alert tone="success" title="Your leave request was submitted">
              {request.leaveTypeName} for {formatRange(request.startDate, request.endDate)} ({plural(request.days, 'working day')}) is
              now waiting for {request.approverName}. You'll get a notification when it is decided.
            </Alert>
            <div className="row">
              <StatusBadge status={request.status} />
              <span className="muted">
                Remaining {balance.leaveTypeName} balance after this request: <strong>{num(balance.remaining)} days</strong>
              </span>
            </div>
            {teamConflicts?.teamLeaveWarning && (
              <Alert tone="serious" title="Your manager will see a team absence warning">{teamConflicts.warningMessage}</Alert>
            )}
            <div className="row">
              <Link to={`/leaves/${request.id}`} className="btn btn-primary">View request</Link>
              <button type="button" className="btn" onClick={() => setSubmitted(null)}>Apply for more leave</button>
            </div>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Apply for Leave" subtitle="Submit a leave request for manager approval." />
      <AsyncBoundary state={policies}>
        {(list) => {
          const active = list.filter((p) => p.active);
          const selected = active.find((p) => String(p.leaveTypeId) === String(form.leaveTypeId));
          const balanceFor = (typeId) => balances.data?.find((b) => String(b.leaveTypeId) === String(typeId));
          const selectedBalance = balanceFor(form.leaveTypeId);
          return (
            <div className="split">
              <form className="card" onSubmit={submit} noValidate>
                <div className="card-header">
                  <div>
                    <h2>Leave details</h2>
                    <p>Weekends and public holidays are not charged unless the leave policy says otherwise.</p>
                  </div>
                </div>
                <div className="card-body form-grid">
                  <div className="field full">
                    <label htmlFor="leaveType">Leave type</label>
                    <select
                      id="leaveType"
                      className={`select ${show('leaveTypeId') ? 'invalid' : ''}`}
                      value={form.leaveTypeId}
                      onChange={set('leaveTypeId')}
                      onBlur={blur('leaveTypeId')}
                      aria-invalid={!!show('leaveTypeId')}
                    >
                      <option value="">Select a leave type…</option>
                      {active.map((p) => {
                        const b = balanceFor(p.leaveTypeId);
                        return (
                          <option key={p.leaveTypeId} value={p.leaveTypeId}>
                            {p.leaveTypeName} — {b ? `${num(b.remaining)} days available` : `${num(p.annualEntitlement)} days/year`}
                          </option>
                        );
                      })}
                    </select>
                    {selectedBalance && (
                      <span className="hint">
                        You can apply for up to <strong>{plural(num(selectedBalance.remaining), 'day')}</strong> of{' '}
                        {selectedBalance.leaveTypeName} this year
                        {Number(selectedBalance.pending) > 0 && ` (${num(selectedBalance.pending)} already pending approval)`}.
                      </span>
                    )}
                    {selected?.description && <span className="hint">{selected.description}</span>}
                    {show('leaveTypeId') && <span className="error">{errors.leaveTypeId}</span>}
                  </div>
                  <div className="field">
                    <label htmlFor="startDate">Start date</label>
                    <input
                      id="startDate"
                      type="date"
                      className={`input ${show('startDate') ? 'invalid' : ''}`}
                      min={todayIso()}
                      value={form.startDate}
                      onChange={set('startDate')}
                      onBlur={blur('startDate')}
                      aria-invalid={!!show('startDate')}
                    />
                    {show('startDate') && <span className="error">{errors.startDate}</span>}
                  </div>
                  <div className="field">
                    <label htmlFor="endDate">End date</label>
                    <input
                      id="endDate"
                      type="date"
                      className={`input ${show('endDate') ? 'invalid' : ''}`}
                      min={form.startDate || todayIso()}
                      value={form.endDate}
                      onChange={set('endDate')}
                      onBlur={blur('endDate')}
                      aria-invalid={!!show('endDate')}
                    />
                    {show('endDate') && <span className="error">{errors.endDate}</span>}
                  </div>
                  <div className="full readout" aria-live="polite">
                    <span className="muted">Working days</span>
                    <strong>
                      {!canPreview ? '—' : preview.data ? plural(preview.data.chargeableDays, 'day') : '…'}
                    </strong>
                  </div>
                  <div className="field full">
                    <label htmlFor="reason">Reason</label>
                    <textarea
                      id="reason"
                      className={`textarea ${show('reason') ? 'invalid' : ''}`}
                      maxLength={500}
                      value={form.reason}
                      onChange={set('reason')}
                      onBlur={blur('reason')}
                      aria-invalid={!!show('reason')}
                      placeholder="e.g. Family wedding out of town"
                    />
                    <span className="hint">{form.reason.length}/500</span>
                    {show('reason') && <span className="error">{errors.reason}</span>}
                  </div>
                  {submitError && (
                    <div className="full"><Alert tone="error" title="Could not submit">{submitError}</Alert></div>
                  )}
                  <div className="full form-actions" style={{ borderTop: '1px solid var(--border)', paddingTop: 20 }}>
                    <Link to="/dashboard" className="btn">Cancel</Link>
                    <button type="submit" className="btn btn-primary" disabled={submitting || blocked}>
                      {submitting ? 'Submitting…' : 'Submit Request'}
                    </button>
                  </div>
                </div>
              </form>
              <PreviewPanel preview={preview} canPreview={canPreview} selectedBalance={selectedBalance} />
            </div>
          );
        }}
      </AsyncBoundary>
    </>
  );
}

function PreviewPanel({ preview, canPreview, selectedBalance }) {
  if (!canPreview) {
    return (
      <aside className="card">
        <div className="card-header plain"><h2>Leave Balance</h2></div>
        <div className="card-body" style={{ paddingTop: 10 }}>
          {selectedBalance ? (
            <dl className="kv">
              <div><dt>Available</dt><dd>{num(selectedBalance.remaining)} days</dd></div>
              <div><dt>Reserved for pending requests</dt><dd>{num(selectedBalance.pending)} days</dd></div>
            </dl>
          ) : null}
          <p className="muted small" style={{ marginTop: selectedBalance ? 12 : 0 }}>
            Pick a leave type and dates to see how many working days will be charged, your remaining balance and team availability.
          </p>
        </div>
      </aside>
    );
  }
  if (preview.loading && !preview.data) {
    return <aside className="card"><Loading label="Calculating…" /></aside>;
  }
  if (preview.error) {
    return <aside className="card"><div className="card-body"><Alert tone="error">{preview.error}</Alert></div></aside>;
  }
  const p = preview.data;
  if (!p) return null;
  const conflicts = p.teamConflicts;
  const busyDays = (conflicts?.affectedDates || []).filter((d) => d.unavailableCount > 0);
  return (
    <aside className="stack" aria-live="polite">
      <section className="card">
        <div className="card-header plain">
          <h2>Leave Balance</h2>
          {preview.loading && <div className="spinner" style={{ width: 16, height: 16 }} />}
        </div>
        <div className="card-body stack" style={{ gap: 16, paddingTop: 10 }}>
          <DaysVsBalance applying={p.chargeableDays} available={p.balance.remaining} sufficient={p.sufficientBalance} />
          <dl className="kv">
            <div><dt>Available</dt><dd>{num(p.balance.remaining)} days</dd></div>
            <div><dt>This request</dt><dd>{plural(p.chargeableDays, 'day')}</dd></div>
            <div className="total">
              <dt>Remaining</dt>
              <dd style={{ color: p.sufficientBalance ? undefined : 'var(--critical)' }}>{num(p.remainingAfter)} days</dd>
            </div>
          </dl>
          <p className="small muted">
            {p.calendarDays} calendar {p.calendarDays === 1 ? 'day' : 'days'}
            {p.weekendDays > 0 && ` · ${p.weekendDays} weekend excluded`}
            {p.holidayDays > 0 && ` · ${p.holidayDays} holiday excluded`}
          </p>
          {p.holidays.length > 0 && (
            <p className="small muted">
              <Icon name="sun" size={14} /> {p.holidays.map((h) => `${h.name} (${formatDate(h.date)})`).join(', ')}
            </p>
          )}
          {p.blockingIssues.map((issue) => <Alert key={issue} tone="error">{issue}</Alert>)}
        </div>
      </section>
      {conflicts?.teamId && (
        <section className="card">
          <div className="card-header plain"><h2>Team Conflicts</h2></div>
          <div className="card-body stack" style={{ gap: 14, paddingTop: 10 }}>
            {conflicts.hasConflict ? (
              <>
                <Alert tone={conflicts.teamLeaveWarning ? 'serious' : 'warn'} title="Conflict detected">
                  {conflicts.warningMessage}
                </Alert>
                <ul className="people-list">
                  {busyDays.slice(0, 6).map((d) => (
                    <li key={d.date}>
                      <Icon name="users" size={15} />
                      <span>{d.unavailableCount} of {d.teamSize} teammates away</span>
                      <span className="when">{formatShortDate(d.date)}</span>
                    </li>
                  ))}
                </ul>
              </>
            ) : (
              <Alert tone="success" title="No overlapping leave">Nobody else in {conflicts.teamName} is away on these dates.</Alert>
            )}
            <p className="small muted">Conflicts are shown as warnings and do not prevent submission.</p>
          </div>
        </section>
      )}
    </aside>
  );
}

/** Headline comparison of the days being requested against the balance still available. */
function DaysVsBalance({ applying, available, sufficient }) {
  const avail = Math.max(0, Number(available) || 0);
  const pct = avail > 0 ? Math.min(100, (applying / avail) * 100) : applying > 0 ? 100 : 0;
  return (
    <div className="days-vs-balance">
      <div className="days-vs-balance-figures">
        <div>
          <span className="label">Applying for</span>
          <strong className="num">{plural(applying, 'day')}</strong>
        </div>
        <div style={{ textAlign: 'right' }}>
          <span className="label">Available</span>
          <strong className="num">{plural(num(available), 'day')}</strong>
        </div>
      </div>
      <div
        className="meter"
        role="img"
        aria-label={`Applying for ${applying} of ${num(available)} available days`}
      >
        <span className={sufficient ? 'used' : 'over'} style={{ width: `${pct}%` }} />
      </div>
      {!sufficient && (
        <span className="small" style={{ color: 'var(--critical)' }}>
          {num(applying - Number(available))} day(s) more than your balance allows.
        </span>
      )}
    </div>
  );
}
