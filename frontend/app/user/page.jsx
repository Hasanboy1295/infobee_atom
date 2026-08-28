'use client';

import Link from 'next/link';
import { useAuth } from '@/components/AuthProvider';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { NotificationBell } from '@/components/NotificationBell';
import { DashboardStats } from '@/components/DashboardStats';
import { useLanguage } from '@/components/LanguageProvider';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';

function UserContent() {
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
            <div className="brand-subtitle">{t('common.workspace')}</div>
          </div>
        </div>
        <nav className="dashboard-nav">
          <Link href="/user" className="nav-link active">{t('common.workspace')}</Link>
          <Link href="/atom">ATOM</Link>
          <Link href="/cpsr">CPSR</Link>
          <Link href="/profile">{t('common.profile')}</Link>
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
          <span>{t('user.welcomeBack')},</span>
          <strong>{user?.fullName || user?.username}</strong>
          <small>@{user?.username} · {user?.role}</small>
        </div>
      </section>

      <DashboardStats />

      <section className="module-grid">
        <Link className="module-box module-link" href="/atom">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <h3>🧪 {t('user.atomTitle')}</h3>
            <span className="role-pill">{t('user.aiEngine')}</span>
          </div>
          <p>{t('user.atomDesc')}</p>
        </Link>

        <Link className="module-box module-link" href="/cpsr">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <h3>🛡️ {t('user.cpsrTitle')}</h3>
            <span className="role-pill">{t('user.toxicology')}</span>
          </div>
          <p>{t('user.cpsrDesc')}</p>
        </Link>
      </section>
    </main>
  );
}

export default function UserPage() {
  return (
    <ProtectedRoute>
      <UserContent />
    </ProtectedRoute>
  );
}
