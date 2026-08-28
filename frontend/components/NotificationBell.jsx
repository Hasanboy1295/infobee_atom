'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { notificationsApi } from '@/lib/api';

function timeAgo(iso) {
  const seconds = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loadedOnce, setLoadedOnce] = useState(false);
  const rootRef = useRef(null);

  const refresh = useCallback(async () => {
    try {
      const summary = await notificationsApi.summary();
      setUnreadCount(Number(summary?.unreadCount ?? 0));
    } catch { /* session may be gone; ignore */ }
  }, []);

  const openPanel = useCallback(async () => {
    setOpen((current) => !current);
    if (!open) {
      try {
        const page = await notificationsApi.list({ page: 0, size: 10 });
        setItems(page.content || []);
        setLoadedOnce(true);
      } catch { /* ignore */ }
    }
  }, [open]);

  useEffect(() => {
    refresh();
    const timer = window.setInterval(refresh, 30000);
    return () => window.clearInterval(timer);
  }, [refresh]);

  useEffect(() => {
    if (!open) return undefined;
    function onClickOutside(event) {
      if (rootRef.current && !rootRef.current.contains(event.target)) setOpen(false);
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, [open]);

  async function markRead(item) {
    try {
      if (!item.read) await notificationsApi.markRead(item.id);
      setItems((list) => list.map((entry) => (entry.id === item.id ? { ...entry, read: true } : entry)));
      refresh();
    } catch { /* ignore */ }
  }

  async function markAll() {
    try {
      await notificationsApi.markAllRead();
      setItems((list) => list.map((entry) => ({ ...entry, read: true })));
      refresh();
    } catch { /* ignore */ }
  }

  return (
    <div className="bell-wrap" ref={rootRef}>
      <button type="button" className="bell-button" aria-label="Notifications" onClick={() => void openPanel()}>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M12 3a6 6 0 0 0-6 6v3.2l-1.4 2.9a1 1 0 0 0 .9 1.4h13a1 1 0 0 0 .9-1.4L18 12.2V9a6 6 0 0 0-6-6Zm-2 15a2 2 0 0 0 4 0" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        {unreadCount > 0 && <span className="bell-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
      </button>
      {open && (
        <div className="bell-panel">
          <div className="bell-header">
            <strong>Notifications</strong>
            <button type="button" className="icon-button" onClick={() => void markAll()}>Mark all read</button>
          </div>
          {!loadedOnce ? <div className="empty-box small-pad">Loading...</div>
            : items.length === 0 ? <div className="empty-box small-pad">No notifications.</div>
              : (
                <ul className="tiny-list bell-list">
                  {items.map((item) => (
                    <li key={item.id} className={item.read ? '' : 'unread'} onClick={() => void markRead(item)}>
                      <strong>{item.title}</strong>
                      <span>{item.message}</span>
                      <small className="table-subtitle">{timeAgo(item.createdAt)}</small>
                    </li>
                  ))}
                </ul>
              )}
        </div>
      )}
    </div>
  );
}
