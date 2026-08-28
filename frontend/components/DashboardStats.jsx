'use client';

import { useEffect, useState } from 'react';
import { statsApi } from '@/lib/api';

const DEFAULT_STATS = {
  totalUsers: 0, activeUsers: 0, totalDepartments: 0, totalRoles: 0,
  totalAtomRequests: 0, totalCpsrRequests: 0,
  atomByStatus: {}, cpsrByStatus: {}, totalComments: 0, totalHistoryEntries: 0,
};

export function DashboardStats({ showUsers = false }) {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    statsApi.dashboard()
      .then((data) => { if (active) setStats({ ...DEFAULT_STATS, ...data }); })
      .catch((caught) => { if (active) setError(caught.message || 'Unable to load statistics.'); });
    return () => { active = false; };
  }, []);

  if (error) return <div className="empty-box">{error}</div>;
  if (!stats) return <div className="empty-box">Loading statistics...</div>;

  const atomActive = Object.entries(stats.atomByStatus || {})
    .reduce((sum, [, count]) => sum + count, 0);
  const cpsrActive = Object.entries(stats.cpsrByStatus || {})
    .reduce((sum, [, count]) => sum + count, 0);

  return (
    <section className="dashboard-grid">
      <div className="summary-card accent"><span>ATOM</span><strong>{stats.totalAtomRequests}</strong><small>{atomActive} in workflow</small></div>
      <div className="summary-card"><span>CPSR</span><strong>{stats.totalCpsrRequests}</strong><small>{cpsrActive} in workflow</small></div>
      <div className="summary-card"><span>Activity</span><strong>{stats.totalComments + stats.totalHistoryEntries}</strong><small>{stats.totalComments} comments · {stats.totalHistoryEntries} transitions</small></div>
      {showUsers && (
        <div className="summary-card"><span>Users</span><strong>{stats.totalUsers}</strong><small>{stats.activeUsers} active · {stats.totalDepartments} departments · {stats.totalRoles} roles</small></div>
      )}
    </section>
  );
}
