'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError, filesApi } from '@/lib/api';

function formatSize(bytes) {
  if (!Number.isFinite(bytes)) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1048576).toFixed(1)} MB`;
}

export function AttachmentsPanel({ type, requestId }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const inputRef = useRef(null);

  const load = useCallback(async () => {
    try {
      setItems(await filesApi.list(type, requestId));
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load attachments.');
    } finally { setLoading(false); }
  }, [requestId, type]);

  useEffect(() => { void load(); }, [load]);

  async function upload(event) {
    event.preventDefault();
    const file = inputRef.current?.files?.[0];
    if (!file) return;
    setUploading(true); setError(null);
    try {
      await filesApi.upload(type, requestId, file);
      if (inputRef.current) inputRef.current.value = '';
      await load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Upload failed.');
    } finally { setUploading(false); }
  }

  async function remove(item) {
    if (!window.confirm(`Delete attachment "${item.originalFilename}"?`)) return;
    try { await filesApi.remove(item.id); await load(); } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to delete attachment.');
    }
  }

  return (
    <div className="panel subpanel">
      <div className="panel-header compact"><h3>Attachments</h3></div>
      <form className="mini-form upload-form" onSubmit={upload}>
        <input ref={inputRef} type="file" required />
        <button className="btn btn-secondary small" disabled={uploading}>{uploading ? 'Uploading...' : 'Upload'}</button>
      </form>
      {error && <div className="error-box" role="alert">{error}</div>}
      {loading ? <div className="empty-box small-pad">Loading attachments...</div>
        : items.length === 0 ? <div className="empty-box small-pad">No attachments yet.</div>
          : (
            <ul className="tiny-list">
              {items.map((item) => (
                <li key={item.id}>
                  <strong>{item.originalFilename}</strong>
                  <span> · {formatSize(item.size)} · by {item.uploadedByByUsername}</span>
                  <small className="table-subtitle">{new Date(item.createdAt).toLocaleString()}</small>
                  <span className="icon-actions">
                    <a className="icon-button" href={filesApi.downloadUrl(item.storedFilename)}>Download</a>
                    <button type="button" className="icon-button" onClick={() => void remove(item)}>Delete</button>
                  </span>
                </li>
              ))}
            </ul>
          )}
    </div>
  );
}
