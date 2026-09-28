import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { session, setAuthToken, setUnauthorizedHandler } from '../api/client';
import { api } from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => (session.token ? session.user : null));

  const logout = useCallback(() => {
    session.clear();
    setAuthToken(null);
    setUser(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);
  }, [logout]);

  const login = useCallback(async (email, password) => {
    const result = await api.login(email, password);
    session.save(result.token, result.user);
    setAuthToken(result.token);
    setUser(result.user);
    return result.user;
  }, []);

  const value = useMemo(() => ({ user, login, logout }), [user, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}

export function homePathFor(role) {
  if (role === 'HR') return '/hr';
  if (role === 'MANAGER') return '/manager';
  return '/dashboard';
}
