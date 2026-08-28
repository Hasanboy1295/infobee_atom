'use client';

import Link from 'next/link';
import { useAuth } from '@/components/AuthProvider';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { NotificationBell } from '@/components/NotificationBell';
import { useLanguage } from '@/components/LanguageProvider';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';

function ProfileContent() {
  const { user, logout } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const { t } = useLanguage();

  return (
    <main className="dashboard-shell">
      <header className="dashboard-topbar">
        <div className="brand-wrap">
          <div className="brand-mark">A</div>
          <div>
            <div className="brand-name">{t('common.brand')}</div>
            <div className="brand-subtitle">{t('profile.title')}</div>
          </div>
        </div>
        <nav className="dashboard-nav">
          <Link href="/user">{t('common.workspace')}</Link>
          <Link href="/atom">ATOM</Link>
          <Link href="/cpsr">CPSR</Link>
          <Link href="/profile" className="nav-link active">{t('common.profile')}</Link>
          {isAdmin && <Link href="/admin">{t('common.admin')}</Link>}
          <LanguageSwitcher />
          <NotificationBell />
          <button type="button" className="link-button" onClick={logout}>
            {t('common.signOut')}
          </button>
        </nav>
      </header>

      <section className="summary-row">
        <div className="summary-card accent welcome-card">
          <span>{t('profile.accountInfo')}</span>
          <strong>{user?.fullName || user?.username}</strong>
          <small>{user?.role} · {t('common.active')}</small>
        </div>
      </section>

      <section className="panel" style={{ maxWidth: 720 }}>
        <div className="panel-header compact">
          <div>
            <p className="panel-eyebrow">{t('profile.accountInfo')}</p>
            <h2>{t('profile.title')}</h2>
          </div>
        </div>

        <div className="calc-grid" style={{ marginBottom: 20 }}>
          <div className="summary-card">
            <span>{t('profile.username')}</span>
            <strong style={{ fontSize: '1.1rem' }}>@{user?.username}</strong>
          </div>
          <div className="summary-card">
            <span>{t('profile.fullName')}</span>
            <strong style={{ fontSize: '1.1rem' }}>{user?.fullName || '—'}</strong>
          </div>
          <div className="summary-card">
            <span>{t('profile.role')}</span>
            <strong style={{ fontSize: '1.1rem' }}><span className="role-pill">{user?.role}</span></strong>
          </div>
          <div className="summary-card">
            <span>{t('profile.status')}</span>
            <strong style={{ fontSize: '1.1rem', color: '#34d399' }}>{t('common.enabled')} & {t('common.active')}</strong>
          </div>
        </div>

        <div className="panel-header compact" style={{ marginTop: 24 }}>
          <div>
            <p className="panel-eyebrow">Security & Session</p>
            <h3>Session info</h3>
          </div>
        </div>
        <p style={{ color: 'var(--muted)', fontSize: 14 }}>
          Your session is secured using HttpOnly encrypted cookies and Double-Submit CSRF protection.
          Tokens are automatically validated against the server session repository.
        </p>

        <div className="action-row" style={{ marginTop: 20 }}>
          <Link href="/user" className="btn btn-secondary">
            {t('common.workspace')}
          </Link>
          <button type="button" className="btn btn-danger" onClick={logout}>
            {t('common.signOut')}
          </button>
        </div>
      </section>
    </main>
  );
}

export default function ProfilePage() {
  return (
    <ProtectedRoute>
      <ProfileContent />
    </ProtectedRoute>
  );
}
