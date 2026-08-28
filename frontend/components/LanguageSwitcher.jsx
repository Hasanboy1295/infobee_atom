'use client';

import { useLanguage } from '@/components/LanguageProvider';

export function LanguageSwitcher() {
  const { lang, setLang } = useLanguage();

  return (
    <div className="lang-switcher">
      <button
        type="button"
        className={`lang-btn ${lang === 'ko' ? 'active' : ''}`}
        onClick={() => setLang('ko')}
        title="한국어"
      >
        <span className="flag-icon">🇰🇷</span>
        <span className="lang-label">KO</span>
      </button>
      <div className="lang-divider" />
      <button
        type="button"
        className={`lang-btn ${lang === 'en' ? 'active' : ''}`}
        onClick={() => setLang('en')}
        title="English"
      >
        <span className="flag-icon">🇺🇸</span>
        <span className="lang-label">EN</span>
      </button>
    </div>
  );
}
