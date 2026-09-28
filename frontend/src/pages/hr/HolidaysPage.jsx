import { useState } from 'react';
import { api } from '../../api';
import { errorMessage } from '../../api/client';
import Icon from '../../components/Icon';
import { ConfirmDialog } from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import { Alert, AsyncBoundary, EmptyState } from '../../components/States';
import useAsync from '../../hooks/useAsync';
import { formatDate, parseDate } from '../../lib/format';

const weekday = new Intl.DateTimeFormat(undefined, { weekday: 'long' });

export default function HolidaysPage() {
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);
  const state = useAsync(() => api.holidays(year), [year]);
  const [form, setForm] = useState({ name: '', date: '' });
  const [error, setError] = useState(null);
  const [flash, setFlash] = useState(null);
  const [busy, setBusy] = useState(false);
  const [removing, setRemoving] = useState(null);

  const add = async (e) => {
    e.preventDefault();
    if (!form.name.trim() || !form.date) {
      setError('Enter a name and a date.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const created = await api.createHoliday({ name: form.name.trim(), date: form.date });
      setFlash(`${created.name} added on ${formatDate(created.date)}.`);
      setForm({ name: '', date: '' });
      if (Number(created.date.slice(0, 4)) !== year) setYear(Number(created.date.slice(0, 4)));
      else state.reload();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader title="Holiday calendar" subtitle="Public holidays are excluded from leave-day calculations for new requests.">
        <select className="select" style={{ width: 120 }} aria-label="Year" value={year} onChange={(e) => setYear(Number(e.target.value))}>
          {[currentYear - 1, currentYear, currentYear + 1].map((y) => <option key={y} value={y}>{y}</option>)}
        </select>
      </PageHeader>
      <div className="split">
        <section className="card">
          <div className="card-header"><h2>Holidays in {year}</h2></div>
          <AsyncBoundary state={state}>
            {(holidays) =>
              holidays.length === 0 ? (
                <EmptyState icon="sun" title="No holidays configured" />
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead><tr><th>Date</th><th>Day</th><th>Holiday</th><th /></tr></thead>
                    <tbody>
                      {holidays.map((h) => (
                        <tr key={h.id}>
                          <td className="nowrap cell-main">{formatDate(h.date)}</td>
                          <td className="muted">{weekday.format(parseDate(h.date))}</td>
                          <td>{h.name}</td>
                          <td className="right">
                            <button type="button" className="btn btn-sm btn-danger" onClick={() => setRemoving(h)} aria-label={`Remove ${h.name}`}>
                              <Icon name="trash" size={14} /> Remove
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )
            }
          </AsyncBoundary>
        </section>
        <section className="card">
          <div className="card-header"><h2>Add a holiday</h2></div>
          <form className="card-body stack" style={{ gap: 14 }} onSubmit={add} noValidate>
            <div className="field">
              <label htmlFor="h-name">Name</label>
              <input id="h-name" className="input" maxLength={100} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </div>
            <div className="field">
              <label htmlFor="h-date">Date</label>
              <input id="h-date" type="date" className="input" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} />
              <span className="hint">Only one holiday per date is allowed.</span>
            </div>
            {error && <Alert tone="error">{error}</Alert>}
            {flash && <Alert tone="success">{flash}</Alert>}
            <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Adding…' : 'Add holiday'}</button>
          </form>
        </section>
      </div>
      {removing && (
        <ConfirmDialog
          title="Remove holiday?"
          message={`${removing.name} on ${formatDate(removing.date)} will no longer be excluded from new leave requests.`}
          confirmLabel="Remove"
          tone="danger"
          onClose={() => setRemoving(null)}
          onConfirm={async () => {
            await api.deleteHoliday(removing.id);
            setFlash(`${removing.name} removed.`);
            setRemoving(null);
            state.reload();
          }}
        />
      )}
    </>
  );
}
