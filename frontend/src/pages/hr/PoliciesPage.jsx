import { useEffect, useState } from 'react';
import { api } from '../../api';
import { errorMessage } from '../../api/client';
import LeaveTypeChip from '../../components/LeaveTypeChip';
import { Modal } from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import { Alert, AsyncBoundary } from '../../components/States';
import Icon from '../../components/Icon';
import useAsync from '../../hooks/useAsync';
import { formatDateTime, num } from '../../lib/format';

export default function PoliciesPage() {
  const policies = useAsync(() => api.policies(), []);
  const [editing, setEditing] = useState(null);
  const [flash, setFlash] = useState(null);

  return (
    <>
      <PageHeader title="Policies & settings" subtitle="Leave entitlements and workflow rules are stored in the database and take effect immediately.">
        <button type="button" className="btn btn-primary" onClick={() => setEditing({})}>
          <Icon name="plus" size={16} /> New leave type
        </button>
      </PageHeader>
      <div className="stack">
        {flash && <Alert tone="success">{flash}</Alert>}
        <WorkflowSettingsCard onSaved={setFlash} />
        <section className="card">
          <div className="card-header">
            <div>
              <h2>Leave types &amp; entitlements</h2>
              <p>Changing an entitlement re-allocates this year's balances (pro-rated by joining date).</p>
            </div>
          </div>
          <AsyncBoundary state={policies}>
            {(list) => (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Leave type</th><th>Code</th><th className="right">Days / year</th><th>Pro-rated</th>
                      <th>Weekends</th><th>Holidays</th><th>Status</th><th>Updated</th><th />
                    </tr>
                  </thead>
                  <tbody>
                    {list.map((p) => (
                      <tr key={p.id}>
                        <td>
                          <LeaveTypeChip code={p.leaveTypeCode} name={p.leaveTypeName} />
                          {p.description && <div className="cell-sub">{p.description}</div>}
                        </td>
                        <td><code>{p.leaveTypeCode}</code></td>
                        <td className="right num cell-main">{num(p.annualEntitlement)}</td>
                        <td>{p.prorated ? 'Yes' : 'No'}</td>
                        <td>{p.countWeekends ? 'Counted' : 'Excluded'}</td>
                        <td>{p.countHolidays ? 'Counted' : 'Excluded'}</td>
                        <td>{p.active ? <span className="badge badge-approved">Active</span> : <span className="badge badge-cancelled">Inactive</span>}</td>
                        <td className="muted nowrap">{formatDateTime(p.updatedAt)}</td>
                        <td className="right"><button type="button" className="btn btn-sm" onClick={() => setEditing(p)}>Edit</button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </AsyncBoundary>
        </section>
      </div>
      {editing && (
        <PolicyDialog
          policy={editing}
          onClose={() => setEditing(null)}
          onSaved={(message) => {
            setEditing(null);
            setFlash(message);
            policies.reload();
          }}
        />
      )}
    </>
  );
}

function WorkflowSettingsCard({ onSaved }) {
  const state = useAsync(() => api.settings(), []);
  const [form, setForm] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (state.data) {
      setForm({
        escalationTimeoutMinutes: String(state.data.escalationTimeoutMinutes),
        teamAbsenceThresholdPercent: String(Number(state.data.teamAbsenceThresholdPercent)),
      });
    }
  }, [state.data]);

  const save = async (e) => {
    e.preventDefault();
    const timeout = Number(form.escalationTimeoutMinutes);
    const threshold = Number(form.teamAbsenceThresholdPercent);
    if (!Number.isInteger(timeout) || timeout < 1 || timeout > 43200) {
      setError('Escalation timeout must be a whole number of minutes between 1 and 43200.');
      return;
    }
    if (!(threshold >= 1 && threshold <= 100)) {
      setError('Team absence threshold must be between 1 and 100%.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await api.updateSettings({ escalationTimeoutMinutes: timeout, teamAbsenceThresholdPercent: threshold });
      onSaved('Workflow settings saved.');
      state.reload();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="card">
      <div className="card-header">
        <div>
          <h2>Workflow settings</h2>
          <p>Escalation timeout and the team absence warning threshold.</p>
        </div>
      </div>
      <AsyncBoundary state={state}>
        {() =>
          form && (
            <form className="card-body form-grid" onSubmit={save} noValidate>
              <div className="field">
                <label htmlFor="timeout">Manager approval timeout (minutes)</label>
                <input id="timeout" type="number" min="1" max="43200" className="input" value={form.escalationTimeoutMinutes}
                  onChange={(e) => setForm({ ...form, escalationTimeoutMinutes: e.target.value })} />
                <span className="hint">1440 = 24 hours. Set to 1–2 minutes to demonstrate automatic escalation.</span>
              </div>
              <div className="field">
                <label htmlFor="threshold">Team absence threshold (%)</label>
                <input id="threshold" type="number" min="1" max="100" step="0.5" className="input" value={form.teamAbsenceThresholdPercent}
                  onChange={(e) => setForm({ ...form, teamAbsenceThresholdPercent: e.target.value })} />
                <span className="hint">Managers see a warning when absence would exceed this. Requests are never auto-rejected.</span>
              </div>
              {error && <div className="full"><Alert tone="error">{error}</Alert></div>}
              <div className="full form-actions">
                <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save settings'}</button>
              </div>
            </form>
          )
        }
      </AsyncBoundary>
    </section>
  );
}

function PolicyDialog({ policy, onClose, onSaved }) {
  const isNew = !policy.id;
  const [form, setForm] = useState({
    code: policy.leaveTypeCode || '',
    name: policy.leaveTypeName || '',
    description: policy.description || '',
    annualEntitlement: policy.annualEntitlement != null ? String(Number(policy.annualEntitlement)) : '',
    prorated: policy.prorated ?? true,
    countWeekends: policy.countWeekends ?? false,
    countHolidays: policy.countHolidays ?? false,
    active: policy.active ?? true,
  });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  const save = async (e) => {
    e.preventDefault();
    const entitlement = Number(form.annualEntitlement);
    if (!form.name.trim()) return setError('Name is required.');
    if (isNew && !/^[A-Za-z][A-Za-z0-9_]{1,29}$/.test(form.code)) return setError('Code must be 2–30 letters, digits or underscores.');
    if (form.annualEntitlement === '' || !(entitlement >= 0 && entitlement <= 365) || Math.round(entitlement * 10) !== entitlement * 10) {
      return setError('Annual entitlement must be between 0 and 365 with at most one decimal place.');
    }
    setBusy(true);
    setError(null);
    const body = {
      name: form.name.trim(),
      description: form.description.trim() || null,
      annualEntitlement: entitlement,
      prorated: form.prorated,
      countWeekends: form.countWeekends,
      countHolidays: form.countHolidays,
    };
    try {
      if (isNew) {
        await api.createPolicy({ ...body, code: form.code.trim() });
        onSaved(`${body.name} created.`);
      } else {
        await api.updatePolicy(policy.id, { ...body, active: form.active });
        onSaved(`${body.name} updated; this year's balances were re-allocated.`);
      }
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  return (
    <Modal title={isNew ? 'New leave type' : `Edit ${policy.leaveTypeName}`} onClose={busy ? () => {} : onClose}>
      <form className="stack" style={{ gap: 14 }} onSubmit={save} noValidate>
        <div className="form-grid">
          {isNew && (
            <div className="field">
              <label htmlFor="p-code">Code</label>
              <input id="p-code" className="input" value={form.code} onChange={set('code')} placeholder="e.g. PARENTAL" />
            </div>
          )}
          <div className={`field ${isNew ? '' : 'full'}`}>
            <label htmlFor="p-name">Name</label>
            <input id="p-name" className="input" maxLength={60} value={form.name} onChange={set('name')} />
          </div>
          <div className="field full">
            <label htmlFor="p-desc">Description</label>
            <input id="p-desc" className="input" maxLength={255} value={form.description} onChange={set('description')} />
          </div>
          <div className="field full">
            <label htmlFor="p-ent">Annual entitlement (days)</label>
            <input id="p-ent" type="number" min="0" max="365" step="0.5" className="input" value={form.annualEntitlement} onChange={set('annualEntitlement')} />
          </div>
        </div>
        <label className="checkbox"><input type="checkbox" checked={form.prorated} onChange={set('prorated')} />
          <span><strong>Pro-rate for mid-year joiners</strong><br /><span className="muted">Entitlement × eligible months ÷ 12, rounded to 0.5 day.</span></span>
        </label>
        <label className="checkbox"><input type="checkbox" checked={form.countWeekends} onChange={set('countWeekends')} />
          <span><strong>Charge weekends</strong><br /><span className="muted">Off by default: Saturdays and Sundays are excluded.</span></span>
        </label>
        <label className="checkbox"><input type="checkbox" checked={form.countHolidays} onChange={set('countHolidays')} />
          <span><strong>Charge public holidays</strong><br /><span className="muted">Off by default: configured holidays are excluded.</span></span>
        </label>
        {!isNew && (
          <label className="checkbox"><input type="checkbox" checked={form.active} onChange={set('active')} />
            <span><strong>Active</strong><br /><span className="muted">Inactive types cannot be requested.</span></span>
          </label>
        )}
        {error && <Alert tone="error">{error}</Alert>}
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose} disabled={busy}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </form>
    </Modal>
  );
}
