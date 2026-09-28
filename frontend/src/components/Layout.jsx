import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, matchPath, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth/AuthContext';
import { initials } from '../lib/format';
import Icon from './Icon';

const NAV = {
  EMPLOYEE: [
    {
      section: 'Workspace',
      items: [
        { to: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
        { to: '/my-leaves', label: 'My Leaves', icon: 'list' },
        { to: '/apply', label: 'Apply Leave', icon: 'plus' },
      ],
    },
  ],
  MANAGER: [
    {
      section: 'Workspace',
      items: [
        { to: '/manager', label: 'Dashboard', icon: 'dashboard', end: true },
        { to: '/manager/approvals', label: 'Approval Queue', icon: 'inbox', badge: 'approvals' },
      ],
    },
    {
      section: 'Management',
      items: [
        { to: '/team-calendar', label: 'Team Calendar', icon: 'calendar' },
        { to: '/manager/team', label: 'Team Members', icon: 'users' },
      ],
    },
  ],
  HR: [
    {
      section: 'Workspace',
      items: [
        { to: '/hr', label: 'Overview', icon: 'dashboard', end: true },
        { to: '/hr/approvals', label: 'Approval Queue', icon: 'inbox', badge: 'approvals' },
        { to: '/hr/escalations', label: 'Escalations', icon: 'escalate' },
        { to: '/hr/leaves', label: 'All Leave', icon: 'list' },
      ],
    },
    {
      section: 'Management',
      items: [
        { to: '/hr/employees', label: 'Employees', icon: 'users' },
        { to: '/team-calendar', label: 'Leave Calendar', icon: 'calendar' },
      ],
    },
    {
      section: 'Insights',
      items: [
        { to: '/hr/analytics', label: 'Reports', icon: 'chart' },
        { to: '/hr/audit', label: 'Audit Log', icon: 'shield' },
      ],
    },
    {
      section: 'Configuration',
      items: [
        { to: '/hr/policies', label: 'Policies & Settings', icon: 'settings' },
        { to: '/hr/holidays', label: 'Holidays', icon: 'sun' },
      ],
    },
  ],
};

/** Pages reached from inside the app (not in the sidebar), for the header breadcrumb. */
const EXTRA_TITLES = [
  { path: '/leaves/:id', section: 'Requests', label: 'Leave request' },
  { path: '/hr/employees/:id', section: 'Management', label: 'Employee' },
  { path: '/notifications', section: 'Account', label: 'Notifications' },
];

const ROLE_LABEL = { EMPLOYEE: 'Employee', MANAGER: 'Manager', HR: 'HR' };
const UNREAD_POLL_MS = 30000;

function currentPage(role, pathname) {
  for (const group of NAV[role]) {
    const item = group.items.find((i) => matchPath({ path: i.to, end: true }, pathname));
    if (item) return { section: group.section, label: item.label };
  }
  return EXTRA_TITLES.find((t) => matchPath({ path: t.path, end: true }, pathname)) || { section: 'LeaveFlow', label: '' };
}

function useTheme() {
  const [theme, setTheme] = useState(() => document.documentElement.getAttribute('data-theme') || 'light');
  const toggle = () => {
    const next = theme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    try {
      localStorage.setItem('lf-theme', next);
    } catch {
      // Storage unavailable (private mode): the choice lasts for this page view only.
    }
    setTheme(next);
  };
  return [theme, toggle];
}

/** Closes a popup when clicking outside `ref` or pressing Escape. */
function useDismiss(open, setOpen, ref) {
  useEffect(() => {
    if (!open) return undefined;
    const onDown = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false);
    const onKey = (e) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('mousedown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [open, setOpen, ref]);
}

function PageSearch({ role }) {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [cursor, setCursor] = useState(0);
  const ref = useRef(null);
  const inputRef = useRef(null);
  useDismiss(open, setOpen, ref);

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === '/' && !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName)) {
        e.preventDefault();
        inputRef.current?.focus();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, []);

  const pages = [
    ...NAV[role].flatMap((g) => g.items.map((i) => ({ ...i, section: g.section }))),
    { to: '/notifications', label: 'Notifications', icon: 'bell', section: 'Account' },
  ];
  const q = query.trim().toLowerCase();
  const results = q ? pages.filter((p) => `${p.label} ${p.section}`.toLowerCase().includes(q)) : pages;

  const go = (page) => {
    navigate(page.to);
    setQuery('');
    setOpen(false);
    inputRef.current?.blur();
  };

  return (
    <div className="search" ref={ref}>
      <Icon name="search" size={15} />
      <input
        ref={inputRef}
        className="input"
        placeholder="Jump to…"
        aria-label="Jump to a page"
        value={query}
        onChange={(e) => { setQuery(e.target.value); setCursor(0); setOpen(true); }}
        onFocus={() => setOpen(true)}
        onKeyDown={(e) => {
          if (e.key === 'ArrowDown') { e.preventDefault(); setCursor((c) => Math.min(c + 1, results.length - 1)); }
          if (e.key === 'ArrowUp') { e.preventDefault(); setCursor((c) => Math.max(c - 1, 0)); }
          if (e.key === 'Enter' && results[cursor]) go(results[cursor]);
        }}
      />
      {!open && <kbd>/</kbd>}
      {open && (
        <div className="menu" role="listbox">
          {results.length === 0 ? (
            <div className="menu-empty">No pages match “{query}”.</div>
          ) : (
            results.map((p, i) => (
              <button key={p.to} type="button" role="option" aria-selected={i === cursor}
                className={`menu-item ${i === cursor ? 'active' : ''}`}
                onMouseEnter={() => setCursor(i)} onClick={() => go(p)}>
                <Icon name={p.icon} size={15} />
                {p.label}
                <span className="sub">{p.section}</span>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}

function UserMenu({ user, logout, placement }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  useDismiss(open, setOpen, ref);
  const inSidebar = placement === 'sidebar';

  return (
    <div className={inSidebar ? undefined : 'topbar-user-wrap'} ref={ref} style={inSidebar ? { position: 'relative' } : undefined}>
      <button type="button" className={inSidebar ? 'user-chip' : 'topbar-user'} aria-haspopup="menu" aria-expanded={open}
        onClick={() => setOpen((o) => !o)}>
        <span className="avatar">{initials(user.fullName)}</span>
        <span className="who">
          <span className="name" style={{ display: 'block' }}>{user.fullName}</span>
          <span className="role" style={{ display: 'block' }}>{ROLE_LABEL[user.role]}{inSidebar && user.teamName ? ` · ${user.teamName}` : ''}</span>
        </span>
        <Icon name="chevronDown" size={15} className="chev" />
      </button>
      {open && (
        <div className={`menu ${inSidebar ? 'up' : ''}`} role="menu">
          <div className="menu-head">
            <div className="cell-main">{user.fullName}</div>
            <div className="cell-sub">{user.email}</div>
            {user.jobTitle && <div className="cell-sub">{user.jobTitle}{user.teamName ? ` · ${user.teamName}` : ''}</div>}
          </div>
          <Link to="/notifications" className="menu-item" role="menuitem" onClick={() => setOpen(false)}>
            <Icon name="bell" size={15} /> Notifications
          </Link>
          <button type="button" className="menu-item" role="menuitem" onClick={logout}>
            <Icon name="logout" size={15} /> Sign out
          </button>
        </div>
      )}
    </div>
  );
}

export default function Layout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [navOpen, setNavOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [approvals, setApprovals] = useState(null);
  const [theme, toggleTheme] = useTheme();
  const mainRef = useRef(null);

  useEffect(() => {
    setNavOpen(false);
    mainRef.current?.scrollTo(0, 0);
  }, [location.pathname]);

  useEffect(() => {
    let cancelled = false;
    const queue = { MANAGER: api.managerPending, HR: api.hrPending }[user.role];
    const load = () => {
      api.unreadCount().then((r) => !cancelled && setUnread(r.unreadCount)).catch(() => {});
      queue?.().then((list) => !cancelled && setApprovals(list.length)).catch(() => {});
    };
    load();
    const timer = setInterval(load, UNREAD_POLL_MS);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [location.pathname, user.role]);

  const page = currentPage(user.role, location.pathname);
  const counts = { approvals };

  return (
    <div className={`shell ${navOpen ? 'nav-open' : ''}`}>
      <aside className="sidebar" aria-label="Main navigation">
        <div className="brand">
          <span className="brand-mark"><Icon name="leaf" size={16} /></span>
          LEAVEFLOW
        </div>
        <nav>
          {NAV[user.role].map((group) => (
            <div key={group.section}>
              <div className="nav-section">{group.section}</div>
              {group.items.map((item) => (
                <NavLink key={item.to} to={item.to} end={item.end} className="nav-link">
                  <Icon name={item.icon} size={17} />
                  {item.label}
                  {item.badge && counts[item.badge] > 0 && <span className="nav-count">{counts[item.badge]}</span>}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="sidebar-footer">
          <NavLink to="/notifications" className="nav-link">
            <Icon name="bell" size={17} />
            Notifications
            {unread > 0 && <span className="nav-count">{unread}</span>}
          </NavLink>
          <UserMenu user={user} logout={logout} placement="sidebar" />
        </div>
      </aside>
      {navOpen && <div className="nav-scrim" onClick={() => setNavOpen(false)} aria-hidden="true" />}
      <div className="main" ref={mainRef}>
        <header className="topbar">
          <button type="button" className="btn btn-ghost menu-button" aria-label="Open navigation" onClick={() => setNavOpen(true)}>
            <Icon name="menu" />
          </button>
          <div className="crumbs">
            <span className="nowrap">{page.section}</span>
            {page.label && <><Icon name="chevronRight" size={14} /><strong>{page.label}</strong></>}
          </div>
          <span className="spacer" />
          <PageSearch role={user.role} />
          <Link to="/notifications" className="icon-btn" aria-label={`Notifications${unread ? `, ${unread} unread` : ''}`}>
            <Icon name="bell" size={18} />
            {unread > 0 && <span className="bell-badge">{unread > 99 ? '99+' : unread}</span>}
          </Link>
          <button type="button" className="icon-btn" onClick={toggleTheme}
            aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
            title={theme === 'dark' ? 'Light theme' : 'Dark theme'}>
            <Icon name={theme === 'dark' ? 'sun' : 'moon'} size={18} />
          </button>
          <span className="topbar-divider" />
          <UserMenu user={user} logout={logout} placement="header" />
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
