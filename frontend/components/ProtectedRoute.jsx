'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from './AuthProvider';

export function ProtectedRoute({ children, adminOnly = false }) {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) router.replace('/');
    else if (!loading && adminOnly && user?.role !== 'ADMIN') router.replace('/user');
  }, [adminOnly, loading, router, user]);

  if (loading || !user || (adminOnly && user.role !== 'ADMIN')) {
    return <main className="dashboard-shell"><div className="empty-box">Checking your session...</div></main>;
  }
  return <>{children}</>;
}
