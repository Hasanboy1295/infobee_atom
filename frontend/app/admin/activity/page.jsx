'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { activityLogsApi, ApiError } from '@/lib/api';
import { useAuth } from '@/components/AuthProvider';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { useLanguage } from '@/components/LanguageProvider';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';

const ACTION_TYPES = [
  '', 'REQUEST_CREATED', 'REQUEST_UPDATED', 'REQUEST_DELETED', 'STATUS_CHANGED',
  'LOGIN', 'LOGOUT', 'LOGIN_FAILED', 'FILE_UPLOADED', 'FILE_DELETED',
];

function AdminActivityContent() {
  const { logout, user } = useAuth();
  const { t } = useLanguage();
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [action, setAction] = useState('');
  const [actorId, setActorId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try {
      const result = await activityLogsApi.list({
        page,
        size: 50,
        action: action || undefined,
        actorId: actorId === '' ? undefined : Number(actorId),
      });
      setItems(result.content || []);
      setTotalPages(result.totalPages || 0);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : t('activity.noActivity'));
    } finally { setLoading(false); }
  }, [action, actorId, page, t]);

  useEffect(() => { void load(); }, [load]);

  return (
    <main className="dashboard-shell">
      <header className="dashboard-topbar">
        <div className="brand-wrap"><div className="brand-mark">A</div><div><div className="brand-name">{t('common.brand')}</div><div className="brand-subtitle">{t('activity.auditTrail')}</div></div></div>
        <nav className="dashboard-nav">
          <Link href="/user">{t('common.workspace')}</Link>
          <Link href="/atom">ATOM</Link>
          <Link href="/cpsr">CPSR</Link>
          <Link href="/admin">{t('common.admin')}</Link>
          <Link href="/admin/activity" className="nav-link active">{t('nav.auditLog')}</Link>
          <Link href="/profile">{t('common.profile')}</Link>
          <LanguageSwitcher />
          <button className="link-button" onClick={logout}>{t('common.signOut')}</button>
        </nav>
      </header>
      <section className="panel">
        <div className="panel-header compact">
          <div><p className="panel-eyebrow">{t('admin.administration')}</p><h2>{t('activity.auditTrail')}</h2></div>
          <span className="action-row">
            <select value={action} onChange={(event) => { setPage(0); setAction(event.target.value); }}>
              {ACTION_TYPES.map((type) => <option key={type || 'ALL'} value={type}>{type || t('activity.allActions')}</option>)}
            </select>
            <input className="search-input" style={{ width: 140 }} type="number" placeholder="Actor ID" value={actorId} onChange={(event) => { setPage(0); setActorId(event.target.value); }} />
          </span>
        </div>
        {error && <div className="error-box" role="alert">{error}</div>}
        {loading ? <div className="empty-box">{t('common.loading')}</div>
          : items.length === 0 ? <div className="empty-box">{t('activity.noActivity')}</div>
            : (
              <div className="table-wrap">
                <table className="data-table">
                  <thead><tr><th>{t('activity.time')}</th><th>{t('activity.user')}</th><th>{t('activity.action')}</th><th>Target</th><th>{t('activity.details')}</th></tr></thead>
                  <tbody>
                    {items.map((item) => (
                      <tr key={item.id}>
                        <td>{new Date(item.createdAt).toLocaleString()}</td>
                        <td>{item.actorUsername || `#${item.actorId}`}</td>
                        <td><span className="role-pill">{item.action}</span></td>
                        <td>{item.targetType ? `${item.targetType} #${item.targetId}` : '—'}</td>
                        <td>{item.detail || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
        <div className="pagination">
          <button className="btn btn-ghost small" disabled={page === 0} onClick={() => setPage(page - 1)}>{t('common.previous')}</button>
          <span>{t('common.page')} {page + 1} {t('common.of')} {Math.max(totalPages, 1)}</span>
          <button className="btn btn-ghost small" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>{t('common.next')}</button>
        </div>
      </section>
    </main>
  );
}

export default function AdminActivityPage() {
  return <ProtectedRoute adminOnly><AdminActivityContent /></ProtectedRoute>;
}
