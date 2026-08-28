'use client';

import Link from 'next/link';
import { useState } from 'react';
import { ApiError, authApi } from '@/lib/api';
import { useRouter } from 'next/navigation';
import { useToast } from '@/components/Toast';
import { useLanguage } from '@/components/LanguageProvider';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';

function calculatePasswordStrength(password) {
  let score = 0;
  if (!password) return { score: 0, label: '', color: '' };
  if (password.length >= 8) score += 1;
  if (password.length >= 12) score += 1;
  if (/[0-9]/.test(password)) score += 1;
  if (/[^A-Za-z0-9]/.test(password) || (/[A-Z]/.test(password) && /[a-z]/.test(password))) score += 1;

  const levels = [
    { label: 'Very Weak', color: '#ef4444' },
    { label: 'Weak', color: '#ef4444' },
    { label: 'Fair', color: '#f97316' },
    { label: 'Good', color: '#eab308' },
    { label: 'Strong', color: '#10b981' },
  ];

  return { score, ...levels[score] };
}

export default function SignupPage() {
  const [form, setForm] = useState({
    name: '',
    username: '',
    password: '',
    confirmPassword: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [agreed, setAgreed] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const router = useRouter();
  const toast = useToast();
  const { t } = useLanguage();

  const strength = calculatePasswordStrength(form.password);

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);

    if (form.name.trim().length === 0) {
      setError(t('signup.usernameTaken'));
      return;
    }
    if (form.username.trim().length < 3) {
      setError(t('signup.usernameTaken'));
      return;
    }
    if (form.password.length < 8) {
      setError(t('signup.passwordMin'));
      return;
    }
    if (form.password !== form.confirmPassword) {
      setError(t('signup.passwordMin'));
      return;
    }
    if (!agreed) {
      setError(t('signup.passwordMin'));
      return;
    }

    setLoading(true);
    try {
      await authApi.signup(form.username.trim(), form.password, form.name.trim());
      toast.success(t('signup.creating') + '! Redirecting to sign in...');
      window.setTimeout(() => router.replace('/'), 1000);
    } catch (caught) {
      const msg = caught instanceof ApiError && caught.status === 409
        ? t('signup.usernameTaken')
        : caught instanceof ApiError && (caught.status === 400 || caught.status === 422)
          ? Object.values(caught.validationErrors).join(' ') || t('signup.passwordMin')
          : caught instanceof Error ? caught.message : t('signup.creating');
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="signup-layout">
      <header className="topbar">
        <div className="brand-wrap">
          <div className="brand-mark">A</div>
          <div>
            <div className="brand-name">{t('common.brand')}</div>
            <div className="brand-subtitle">{t('common.platform')}</div>
          </div>
        </div>

        <nav className="nav">
          <Link href="/">{t('common.signIn')}</Link>
          <Link href="/#features">{t('nav.features')}</Link>
          <Link href="/#modules">{t('nav.modules')}</Link>
        </nav>

        <div className="top-actions">
          <LanguageSwitcher />
          <Link href="/" className="btn btn-ghost">
            {t('signup.alreadyHave')}
          </Link>
        </div>
      </header>

      <section className="signup-grid">
        <div className="signup-hero-card">
          <span className="eyebrow">{t('signup.eyebrow')}</span>
          <h1>{t('signup.heroTitle')}</h1>
          <p className="subtitle">
            Next-generation deterministic chemical prediction, SCCS cosmetic safety evaluations, and automated toxicology workflows in one integrated workspace.
          </p>

          <div className="perks-list">
            <div className="perk-row">
              <div className="perk-icon-wrap">⚡</div>
              <div className="perk-text">
                <strong>ATOM Kinetics Yield Engine</strong>
                <span>Deterministic reaction rate, temperature decay, concentration penalty, and catalyst efficiency calculations.</span>
              </div>
            </div>

            <div className="perk-row">
              <div className="perk-icon-wrap">🛡️</div>
              <div className="perk-text">
                <strong>SCCS MoS & SED Safety Evaluations</strong>
                <span>Instant Margin of Safety calculations ($MoS \ge 100$) and automated regulatory toxicology reporting.</span>
              </div>
            </div>

            <div className="perk-row">
              <div className="perk-icon-wrap">🌐</div>
              <div className="perk-text">
                <strong>PubChem Live GHS Integration</strong>
                <span>Live hazard classifications, molecular formulas, signal words, and CAS lookup.</span>
              </div>
            </div>

            <div className="perk-row">
              <div className="perk-icon-wrap">🔒</div>
              <div className="perk-text">
                <strong>Role-Based Access & Security</strong>
                <span>Encrypted HttpOnly sessions, double-submit CSRF tokens, and comprehensive audit logs.</span>
              </div>
            </div>
          </div>

          <div className="signup-stats-strip">
            <div>
              <strong>99.9%</strong>
              <span>Uptime SLA</span>
            </div>
            <div>
              <strong>&lt; 50ms</strong>
              <span>Engine Latency</span>
            </div>
            <div>
              <strong>100%</strong>
              <span>Deterministic AI</span>
            </div>
          </div>
        </div>

        <div className="signup-form-card">
          <div className="card-head">
            <span className="mini-label" style={{ color: 'var(--primary)', fontWeight: 600, fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Free Platform Access
            </span>
            <h2>{t('signup.createYour')}</h2>
            <p>Start running AI predictions and toxicology workflows in minutes.</p>
          </div>

          <form onSubmit={handleSubmit} className="stack-form">
            <label>
              <span>{t('signup.fullName')} *</span>
              <div className="input-field-wrap">
                <span className="input-icon-left">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                    <circle cx="12" cy="7" r="4" />
                  </svg>
                </span>
                <input
                  required
                  placeholder={t('signup.fullNamePlaceholder')}
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                />
              </div>
            </label>

            <label>
              <span>{t('home.username')} *</span>
              <div className="input-field-wrap">
                <span className="input-icon-left">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="4" />
                    <path d="M16 8v5a3 3 0 0 0 6 0v-1a10 10 0 1 0-3.92 7.94" />
                  </svg>
                </span>
                <input
                  required
                  minLength={3}
                  placeholder={t('signup.usernamePlaceholder')}
                  value={form.username}
                  onChange={(e) => setForm({ ...form, username: e.target.value })}
                />
              </div>
            </label>

            <label>
              <span>{t('home.password')} *</span>
              <div className="input-field-wrap">
                <span className="input-icon-left">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </span>
                <input
                  required
                  type={showPassword ? 'text' : 'password'}
                  minLength={8}
                  placeholder={t('signup.passwordPlaceholder')}
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                />
                <button
                  type="button"
                  className="password-toggle-btn"
                  onClick={() => setShowPassword(!showPassword)}
                  title={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? '🙈' : '👁️'}
                </button>
              </div>

              {form.password.length > 0 && (
                <div className="strength-meter">
                  <div className="strength-bars">
                    {[1, 2, 3, 4].map((level) => (
                      <div
                        key={level}
                        className={`strength-bar ${strength.score >= level ? `filled-${strength.score}` : ''}`}
                      />
                    ))}
                  </div>
                  <div className="strength-label" style={{ color: strength.color }}>
                    <span>Strength: {strength.label}</span>
                    <span>{form.password.length}/8+ chars</span>
                  </div>
                </div>
              )}
            </label>

            <label>
              <span>{t('home.password')} *</span>
              <div className="input-field-wrap">
                <span className="input-icon-left">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M21 2l-2 2m-1.5 1.5L10 13l-4 4-2 2H2v-2l2-2 4-4 7.5-7.5" />
                  </svg>
                </span>
                <input
                  required
                  type={showPassword ? 'text' : 'password'}
                  minLength={8}
                  placeholder={t('signup.passwordPlaceholder')}
                  value={form.confirmPassword}
                  onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
                />
              </div>
              {form.confirmPassword && form.password !== form.confirmPassword && (
                <small style={{ color: '#ef4444', marginTop: 4, display: 'block' }}>
                  Passwords do not match
                </small>
              )}
              {form.confirmPassword && form.password === form.confirmPassword && (
                <small style={{ color: '#10b981', marginTop: 4, display: 'block' }}>
                  ✓ Passwords match
                </small>
              )}
            </label>

            <label className="terms-row">
              <input
                type="checkbox"
                checked={agreed}
                onChange={(e) => setAgreed(e.target.checked)}
              />
              <span>
                I agree to the <a href="#" style={{ color: 'var(--primary)', textDecoration: 'underline' }}>Terms of Service</a> & <a href="#" style={{ color: 'var(--primary)', textDecoration: 'underline' }}>Privacy Policy</a>
              </span>
            </label>

            <button className="btn btn-primary fluid large" type="submit" disabled={loading}>
              {loading ? t('signup.creating') : t('signup.createBtn') + ' →'}
            </button>
          </form>

          {error && <div className="error-box" role="alert" style={{ marginTop: 16 }}>{error}</div>}

          <p style={{ textAlign: 'center', margin: '20px 0 0', fontSize: 14, color: 'var(--muted)' }}>
            {t('signup.alreadyHave')}{' '}
            <Link href="/" style={{ color: 'var(--primary)', fontWeight: 600 }}>
              {t('common.signIn')}
            </Link>
          </p>
        </div>
      </section>
    </main>
  );
}
