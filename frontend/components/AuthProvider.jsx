'use client';

import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { authApi, clearSession, saveSession } from '@/lib/api';

const AuthContext = createContext(undefined);

export function AuthProvider({ children }) {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    authApi.me()
      .then((user) => setSession({ user }))
      .catch(() => {
        clearSession();
        setSession(null);
      })
      .finally(() => setLoading(false));
    const handleExpired = () => setSession(null);
    window.addEventListener('atom-auth-expired', handleExpired);
    return () => window.removeEventListener('atom-auth-expired', handleExpired);
  }, []);

  async function login(username, password) {
    const nextSession = await authApi.login(username.trim(), password);
    saveSession(nextSession);
    setSession(nextSession);
    return nextSession.user;
  }

  async function logout() {
    try {
      await authApi.logout();
    } finally {
      clearSession();
      setSession(null);
    }
  }

  const value = useMemo(() => ({ user: session?.user || null, loading, login, logout }), [session, loading]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
