'use client';

import { useState } from 'react';
import { ApiError, exportApi } from '@/lib/api';

function downloadFile(filename, content, mimeType) {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

function jsonToCsv(items) {
  if (!items || !items.length) return '';
  const headers = ['id', 'type', 'title', 'status', 'ownerUsername', 'createdAt', 'updatedAt'];
  const rows = items.map((item) =>
    headers
      .map((header) => {
        const val = item[header] ?? '';
        const escaped = String(val).replace(/"/g, '""');
        return `"${escaped}"`;
      })
      .join(',')
  );
  return [headers.join(','), ...rows].join('\n');
}

export function ExportButtons({ type, filter = {} }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  async function exportFormat(format) {
    setBusy(true);
    setError(null);
    try {
      const payload = type === 'ATOM' ? await exportApi.atom(filter) : await exportApi.cpsr(filter);
      if (format === 'json') {
        downloadFile(`${type.toLowerCase()}-requests-export.json`, JSON.stringify(payload, null, 2), 'application/json');
      } else {
        const csvData = jsonToCsv(payload.data || []);
        downloadFile(`${type.toLowerCase()}-requests-export.csv`, csvData, 'text/csv;charset=utf-8;');
      }
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Export failed.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <span className="action-row" style={{ gap: 6 }}>
      <button
        type="button"
        className="btn btn-ghost small"
        disabled={busy}
        onClick={() => void exportFormat('json')}
        title="Download requests as JSON"
      >
        {busy ? 'Exporting...' : 'Export JSON'}
      </button>
      <button
        type="button"
        className="btn btn-ghost small"
        disabled={busy}
        onClick={() => void exportFormat('csv')}
        title="Download requests as CSV spreadsheet"
      >
        {busy ? 'Exporting...' : 'Export CSV'}
      </button>
      {error && <span className="table-subtitle" style={{ color: '#f87171' }}>{error}</span>}
    </span>
  );
}
