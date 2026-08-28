'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { ApiError } from '@/lib/api';
import { useAuth } from '@/components/AuthProvider';
import { useRouter } from 'next/navigation';
import { useLanguage } from '@/components/LanguageProvider';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';

export default function HomePage() {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const { user, login, loading: sessionLoading } = useAuth();
  const router = useRouter();
  const { t } = useLanguage();

  useEffect(() => {
    if (!sessionLoading && user) router.replace(user.role === 'ADMIN' ? '/admin' : '/user');
  }, [router, sessionLoading, user]);

  async function handleLogin(event) {
    event.preventDefault();
    setError(null);
    if (!username.trim() || !password) {
      setError(t('home.requiredFields'));
      return;
    }
    setLoading(true);
    try {
      const loggedInUser = await login(username, password);
      router.replace(loggedInUser.role === 'ADMIN' ? '/admin' : '/user');
    } catch (caught) {
      setError(caught instanceof ApiError && caught.status === 401
        ? t('home.invalidCredentials')
        : caught instanceof Error ? caught.message : t('home.unableToSign'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="landing-shell">
      <header className="topbar">
        <div className="brand-wrap">
          <div className="brand-mark">A</div>
          <div>
            <div className="brand-name">{t('common.brand')}</div>
            <div className="brand-subtitle">{t('common.platform')}</div>
          </div>
        </div>

        <nav className="nav">
          <a href="#features">{t('nav.features')}</a>
          <a href="#modules">{t('nav.modules')}</a>
          <a href="#security">{t('nav.security')}</a>
        </nav>

        <div className="top-actions">
          <LanguageSwitcher />
          <Link href="/" className="btn btn-ghost">{t('common.signIn')}</Link>
          <Link href="/signup" className="btn btn-primary">{t('common.signUp')}</Link>
        </div>
      </header>

      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow">{t('home.eyebrow')}</span>
          <h1>{t('home.heroTitle')}</h1>
          <p>{t('home.heroDesc')}</p>

          <div className="cta-row">
            <Link href="/signup" className="btn btn-primary large">{t('home.getStarted')}</Link>
            <Link href="/user" className="btn btn-ghost large">{t('common.workspace')}</Link>
          </div>

          <div className="stats">
            <div>
              <strong>{t('home.stats247')}</strong>
              <span>{t('home.statsAi')}</span>
            </div>
            <div>
              <strong>99.9%</strong>
              <span>{t('home.statsUptime')}</span>
            </div>
            <div>
              <strong>6+</strong>
              <span>{t('home.statsModules')}</span>
            </div>
          </div>
        </div>

        <div className="auth-card">
          <div className="card-header">
            <div>
              <p className="mini-label">{t('home.welcomeBack')}</p>
              <h2>{t('common.signIn')}</h2>
            </div>
            <div className="status-pill">Live</div>
          </div>

          <form onSubmit={handleLogin} className="auth-form">
            <label>
              <span>{t('home.username')}</span>
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder={t('home.enterUsername')}
              />
            </label>

            <label>
              <span>{t('home.password')}</span>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder={t('home.enterPassword')}
              />
            </label>

            <div className="form-row">
              <label className="remember">
                <input type="checkbox" defaultChecked />
                {t('home.rememberMe')}
              </label>
              <span>{t('home.secureSession')}</span>
            </div>

            <button className="btn btn-primary fluid" type="submit" disabled={loading}>
              {loading ? t('home.signingIn') : t('home.signInBtn')}
            </button>
          </form>

          <Link href="/signup" className="btn btn-secondary fluid">{t('home.createAccount')}</Link>

          {error && <div className="error-box" role="alert">{error}</div>}
        </div>
      </section>

      <section className="feature-grid" id="features">
        <div className="feature-box">
          <span>01</span>
          <h3>{t('home.feature1Title')}</h3>
          <p>{t('home.feature1Desc')}</p>
        </div>
        <div className="feature-box">
          <span>02</span>
          <h3>{t('home.feature2Title')}</h3>
          <p>{t('home.feature2Desc')}</p>
        </div>
        <div className="feature-box">
          <span>03</span>
          <h3>{t('home.feature3Title')}</h3>
          <p>{t('home.feature3Desc')}</p>
        </div>
      </section>

      <section className="modules" id="modules">
        <h2>{t('home.modulesTitle')}</h2>
        <ul>
          <li>{t('home.moduleLogin')}</li>
          <li>{t('home.moduleAdmin')}</li>
          <li>{t('home.moduleUser')}</li>
          <li>{t('home.moduleAtom')}</li>
          <li>{t('home.moduleCpsr')}</li>
          <li>{t('home.moduleAnalytics')}</li>
        </ul>
      </section>
    </main>
  );
}
