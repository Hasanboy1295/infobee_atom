'use client';

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import ko from '@/lib/locales/ko.json';
import en from '@/lib/locales/en.json';

const translations = { ko, en };
const LanguageContext = createContext(undefined);

export function LanguageProvider({ children }) {
  const [lang, setLang] = useState('ko');
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem('atom-lang');
    if (saved === 'en' || saved === 'ko') setLang(saved);
    setReady(true);
  }, []);

  const setLangAndSave = useCallback((newLang) => {
    setLang(newLang);
    localStorage.setItem('atom-lang', newLang);
  }, []);

  const toggleLang = useCallback(() => {
    setLangAndSave(lang === 'ko' ? 'en' : 'ko');
  }, [lang, setLangAndSave]);

  const t = useCallback((path) => {
    const keys = path.split('.');
    let val = translations[lang];
    for (const k of keys) val = val?.[k];
    return val ?? path;
  }, [lang]);

  useEffect(() => { document.documentElement.lang = lang; }, [lang]);

  const value = useMemo(() => ({ lang, setLang: setLangAndSave, toggleLang, t, ready }), [lang, setLangAndSave, toggleLang, t, ready]);

  if (!ready) {
    return (
      <LanguageContext.Provider value={{ lang: 'ko', setLang: setLangAndSave, toggleLang, t: (path) => { let v = translations.ko; for (const k of path.split('.')) v = v?.[k]; return v ?? path; }, ready }}>
        {children}
      </LanguageContext.Provider>
    );
  }

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useLanguage() {
  const value = useContext(LanguageContext);
  if (!value) throw new Error('useLanguage must be used inside LanguageProvider');
  return value;
}
