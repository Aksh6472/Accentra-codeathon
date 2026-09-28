import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth/AuthContext';
import Icon from './Icon';

const NAV = {
  EMPLOYEE: [
    { section: 'My leave' },
    { to: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
    { to: '/apply', label: 'Apply for leave', icon: 'plus' },
    { to: '/my-leaves', label: 'My requests', icon: 'list' },
  ],
  MANAGER: [
    { section: 'Team' },
    { to: '/manager', label: 'Dashboard', icon: 'dashboard', end: true },
    { to: '/manager/approvals', label: 'Approvals', icon: 'inbox' },
    { to: '/team-calendar', label: 'Team calendar', icon: 'calendar' },
  ],
  HR: [
    { section: 'Approvals' },
    { to: '/hr', label: 'Dashboard', icon: 'dashboard', end: true },
    { to: '/hr/approvals', label: 'HR approvals', icon: 'inbox' },
    { to: '/hr/escalations', label: 'Escalations', icon: 'escalate' },
    { to: '/hr/leaves', label: 'All leave', icon: 'list' },
    { to: '/team-calendar', label: 'Team calendar', icon: 'calendar' },
    { section: 'Insights' },
    { to: '/hr/analytics', label: 'Analytics', icon: 'chart' },
    { to: '/hr/audit', label: 'Audit history', icon: 'clock' },
    { section: 'Configuration' },
    { to: '/hr/policies', label: 'Policies & settings', icon: 'settings' },
    { to: '/hr/holidays', label: 'Holidays', icon: 'sun' },
  ],
};

const ROLE_LABEL = { EMPLOYEE: 'Employee', MANAGER: 'Manager', HR: 'HR' };
const UNREAD_POLL_MS = 30000;

function initials(name) {
  return name.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase();
}

export default function Layout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [navOpen, setNavOpen] = useState(false);
  const [unread, setUnread] = useState(0);

  useEffect(() => setNavOpen(false), [location.pathname]);

  useEffect(() => {
    let cancelled = false;
    const load = () =>
      api.unreadCount().then((r) => !cancelled && setUnread(r.unreadCount)).catch(() => {});
    load();
    const timer = setInterval(load, UNREAD_POLL_MS);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [location.pathname]);

  return (
    <div className={`shell ${navOpen ? 'nav-open' : ''}`}>
      <aside className="sidebar" aria-label="Main navigation">
        <div className="brand">
          <span className="brand-mark"><Icon name="calendar" size={16} /></span>
          LeaveFlow
        </div>
        <nav>
          {NAV[user.role].map((item) =>
            item.section ? (
              <div key={item.section} className="nav-section">{item.section}</div>
            ) : (
              <NavLink key={item.to} to={item.to} end={item.end} className="nav-link">
                <Icon name={item.icon} />
                {item.label}
              </NavLink>
            ),
          )}
          <div className="nav-section">Account</div>
          <NavLink to="/notifications" className="nav-link">
            <Icon name="bell" />
            Notifications
            {unread > 0 && <span className="nav-count">{unread}</span>}
          </NavLink>
        </nav>
        <div className="sidebar-footer">
          <div className="user-chip">
            <span className="avatar">{initials(user.fullName)}</span>
            <div>
              <div className="name">{user.fullName}</div>
              <div className="role">{ROLE_LABEL[user.role]}{user.teamName ? ` · ${user.teamName}` : ''}</div>
            </div>
          </div>
          <button type="button" className="nav-link btn-ghost" style={{ width: '100%', border: 0, background: 'none', cursor: 'pointer', font: 'inherit' }} onClick={logout}>
            <Icon name="logout" />
            Sign out
          </button>
        </div>
      </aside>
      {navOpen && <div className="nav-scrim" onClick={() => setNavOpen(false)} aria-hidden="true" />}
      <div className="main">
        <header className="topbar">
          <button type="button" className="btn btn-ghost menu-button" aria-label="Open navigation" onClick={() => setNavOpen(true)}>
            <Icon name="menu" />
          </button>
          <span className="muted small">{ROLE_LABEL[user.role]} workspace</span>
          <span className="spacer" />
          <Link to="/notifications" className="bell" aria-label={`Notifications${unread ? `, ${unread} unread` : ''}`}>
            <Icon name="bell" size={20} />
            {unread > 0 && <span className="bell-badge">{unread > 99 ? '99+' : unread}</span>}
          </Link>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
