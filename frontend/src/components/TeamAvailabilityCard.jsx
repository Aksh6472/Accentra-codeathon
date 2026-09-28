import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import useAsync from '../hooks/useAsync';
import { addDays, formatDate, formatShortDate, parseDate, todayIso, toIsoDate } from '../lib/format';
import AvailabilityTimeline, { busiestDay } from './AvailabilityTimeline';
import Icon from './Icon';
import { Alert, AsyncBoundary, EmptyState } from './States';

const WINDOW_DAYS = 14;

function mondayOf(iso) {
  const d = parseDate(iso);
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  return toIsoDate(d);
}

/** Two-week "who is away" card for one of the viewer's teams. Overlaps are informational only. */
export default function TeamAvailabilityCard({ teams, title = 'Team Availability', subtitle = 'See when your teammates are away.' }) {
  const [teamId, setTeamId] = useState(teams[0]?.id);
  const [from, setFrom] = useState(() => mondayOf(todayIso()));
  const to = addDays(from, WINDOW_DAYS - 1);
  const state = useAsync(() => (teamId ? api.teamCalendar(teamId, from, to) : Promise.resolve(null)), [teamId, from]);
  const thisWeek = mondayOf(todayIso());

  return (
    <section className="card">
      <div className="card-header">
        <div>
          <h2>{title}</h2>
          <p>{subtitle}</p>
        </div>
        <div className="row" style={{ gap: 8 }}>
          {teams.length > 1 && (
            <select className="select" style={{ width: 'auto', height: 32 }} aria-label="Team" value={teamId}
              onChange={(e) => setTeamId(Number(e.target.value))}>
              {teams.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          )}
          <div className="row" style={{ gap: 4 }}>
            <button type="button" className="btn btn-sm" aria-label="Previous two weeks" onClick={() => setFrom(addDays(from, -WINDOW_DAYS))}>
              <Icon name="chevronLeft" size={15} />
            </button>
            <button type="button" className="btn btn-sm" disabled={from === thisWeek} onClick={() => setFrom(thisWeek)}>Today</button>
            <button type="button" className="btn btn-sm" aria-label="Next two weeks" onClick={() => setFrom(addDays(from, WINDOW_DAYS))}>
              <Icon name="chevronRight" size={15} />
            </button>
          </div>
        </div>
      </div>
      <div className="card-body">
        {!teamId ? (
          <EmptyState icon="users" title="No team assigned" />
        ) : (
          <AsyncBoundary state={state}>
            {(calendar) => {
              const peak = busiestDay(calendar.days);
              return (
                <div className="stack" style={{ gap: 16 }}>
                  <div className="row small muted" style={{ justifyContent: 'space-between' }}>
                    <span>{formatShortDate(from)} – {formatDate(to)} · {calendar.team.name}, {calendar.team.memberCount} people</span>
                    <Link to={`/team-calendar?team=${teamId}&month=${from.slice(0, 7)}`}>Full calendar</Link>
                  </div>
                  <AvailabilityTimeline calendar={calendar} />
                  {peak && (
                    <Alert tone={peak.exceedsThreshold ? 'serious' : 'warn'}
                      title={`${peak.unavailableCount} team members are unavailable on ${formatDate(peak.date)}`}>
                      {peak.employeeNames.join(', ')}. This is a warning only and never rejects leave automatically.
                    </Alert>
                  )}
                </div>
              );
            }}
          </AsyncBoundary>
        )}
      </div>
    </section>
  );
}
