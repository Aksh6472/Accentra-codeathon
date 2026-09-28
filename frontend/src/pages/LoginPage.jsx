import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { homePathFor, useAuth } from '../auth/AuthContext';
import Icon from '../components/Icon';
import { Alert } from '../components/States';

// Demo accounts are listed for the codeathon demo; hide them with VITE_SHOW_DEMO_ACCOUNTS=false.
const SHOW_DEMO = import.meta.env.VITE_SHOW_DEMO_ACCOUNTS !== 'false';
const DEMO_ACCOUNTS = [
  { email: 'employee1@demo.com', role: 'Employee', name: 'Aarav Patel' },
  { email: 'employee2@demo.com', role: 'Employee · new joiner', name: 'Priya Nair' },
  { email: 'manager@demo.com', role: 'Manager', name: 'Marcus Chen' },
  { email: 'hr@demo.com', role: 'HR', name: 'Hannah Reed' },
];

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={homePathFor(user.role)} replace />;

  const submit = async (e) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError('Enter your email and password.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const signedIn = await login(email.trim(), password);
      navigate(location.state?.from || homePathFor(signedIn.role), { replace: true });
    } catch (err) {
      setError(errorMessage(err, 'Sign-in failed.'));
      setBusy(false);
    }
  };

  return (
    <div className="login-page">
      <section className="login-hero">
        <div className="brand" style={{ padding: 0 }}>
          <span className="brand-mark"><Icon name="calendar" size={16} /></span>
          LeaveFlow
        </div>
        <div>
          <h1>Leave management that keeps teams running.</h1>
          <ul>
            <li>Two-stage approval: manager, then HR</li>
            <li>Pro-rated balances from each employee's joining date</li>
            <li>Team conflict warnings before anyone approves</li>
            <li>Automatic escalation when approvals stall</li>
            <li>Full audit trail of every decision</li>
          </ul>
        </div>
        <p className="small" style={{ color: '#8fa0bb' }}>Monolithic Spring Boot API · React dashboard</p>
      </section>
      <section className="login-form-wrap">
        <div className="card login-card">
          <div className="card-body stack">
            <div>
              <h1>Sign in</h1>
              <p className="muted">Use your work email and password.</p>
            </div>
            <form className="stack" style={{ gap: 14 }} onSubmit={submit} noValidate>
              <div className="field">
                <label htmlFor="email">Email</label>
                <input id="email" type="email" className="input" autoComplete="username" value={email}
                  onChange={(e) => setEmail(e.target.value)} />
              </div>
              <div className="field">
                <label htmlFor="password">Password</label>
                <input id="password" type="password" className="input" autoComplete="current-password" value={password}
                  onChange={(e) => setPassword(e.target.value)} />
              </div>
              {error && <Alert tone="error">{error}</Alert>}
              <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
                {busy ? 'Signing in…' : 'Sign in'}
              </button>
            </form>
            {SHOW_DEMO && (
              <div className="stack" style={{ gap: 8 }}>
                <p className="small muted">Demo accounts (password <code>Demo@1234</code>) — click to fill:</p>
                <div className="demo-accounts">
                  {DEMO_ACCOUNTS.map((a) => (
                    <button key={a.email} type="button" onClick={() => { setEmail(a.email); setPassword('Demo@1234'); setError(null); }}>
                      <span className="r">{a.role}</span>
                      {a.name}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
