'use client';

import { useCallback, useEffect, useState } from 'react';
import { ApiError, predictionsApi } from '@/lib/api';

const ACTIVE_STATUSES = ['QUEUED', 'RUNNING'];
const DEFAULT_CONDITIONS = '{\n  "temperatureC": 25,\n  "concentrationMgMl": 10,\n  "ph": 7\n}';

function parseConditions(text) {
  const trimmed = text.trim();
  if (!trimmed) return { error: 'Enter experiment conditions as JSON.' };
  try {
    const parsed = JSON.parse(trimmed);
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      return { error: 'Conditions must be a JSON object.' };
    }
    return { value: parsed };
  } catch {
    return { error: 'Conditions must be valid JSON.' };
  }
}

function ResultView({ prediction }) {
  let parsed = null;
  try { parsed = JSON.parse(prediction.resultData); } catch { parsed = null; }
  return (
    <div className="response-box">
      {parsed ? (
        <ul className="tiny-list result-list">
          <li><strong>Recommendation:</strong> {parsed.recommendation}</li>
          <li><strong>Predicted yield:</strong> {parsed.predictedYieldPercent}%</li>
          <li><strong>Confidence:</strong> {parsed.confidence}</li>
          <li><strong>Model:</strong> {parsed.modelVersion}</li>
          <li>{parsed.notes}</li>
        </ul>
      ) : prediction.resultData}
    </div>
  );
}

export function AtomPredictionsPanel({ requestId }) {
  const [items, setItems] = useState([]);
  const [conditionsText, setConditionsText] = useState(DEFAULT_CONDITIONS);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    try {
      setItems(await predictionsApi.list(requestId));
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load predictions.');
    } finally { setLoading(false); }
  }, [requestId]);

  useEffect(() => { void load(); }, [load]);

  useEffect(() => {
    if (!items.some((item) => ACTIVE_STATUSES.includes(item.status))) return undefined;
    const timer = window.setInterval(() => void load(), 3000);
    return () => window.clearInterval(timer);
  }, [items, load]);

  async function create(event) {
    event.preventDefault();
    const conditions = parseConditions(conditionsText);
    if (conditions.error) { setError(conditions.error); return; }
    setSaving(true); setError(null);
    try {
      await predictionsApi.create(requestId, JSON.stringify(conditions.value));
      await load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to create prediction.');
    } finally { setSaving(false); }
  }

  async function act(item, action) {
    setSaving(true);
    try {
      await action(item.id);
      await load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Operation failed.');
    } finally { setSaving(false); }
  }

  async function downloadResult(item) {
    setSaving(true);
    try {
      const payload = await predictionsApi.result(requestId, item.id);
      const blob = new Blob([String(payload.data ?? '')], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = payload.filename || `prediction-${item.id}.json`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to download result.');
    } finally { setSaving(false); }
  }

  return (
    <div className="panel subpanel">
      <div className="panel-header compact">
        <div><p className="panel-eyebrow">AI experiment</p><h3>ATOM predictions</h3></div>
      </div>
      <form className="mini-form" onSubmit={create}>
        <label className="note-field">
          <span>Experiment conditions (JSON)</span>
          <textarea
            value={conditionsText}
            onChange={(event) => setConditionsText(event.target.value)}
            spellCheck="false"
            style={{ minHeight: 90 }}
          />
        </label>
        <button className="btn btn-primary small" disabled={saving}>New prediction</button>
      </form>
      {error && <div className="error-box" role="alert">{error}</div>}
      {loading ? <div className="empty-box small-pad">Loading predictions...</div>
        : items.length === 0 ? <div className="empty-box small-pad">No predictions yet. Create one above.</div>
          : (
            <ul className="tiny-list">
              {items.map((item) => (
                <li key={item.id}>
                  <span className="role-pill">{item.status}</span>
                  {' '}prediction #{item.id}
                  {item.modelVersion && <small className="table-subtitle">model {item.modelVersion}{item.executionTimeMs ? ` · ${item.executionTimeMs} ms` : ''}</small>}
                  {item.errorMessage && <div className="table-subtitle">{item.errorMessage}</div>}
                  {ACTIVE_STATUSES.includes(item.status) && <small className="table-subtitle">Processing by AI service...</small>}
                  {item.resultData && <ResultView prediction={item} />}
                  <span className="action-row request-actions small-actions">
                    {(item.status === 'INPUT_READY' || item.status === 'FAILED') &&
                      <button type="button" className="btn btn-secondary small" disabled={saving} onClick={() => void act(item, (id) => predictionsApi.submit(requestId, id))}>
                        {item.status === 'FAILED' ? 'Retry' : 'Run AI'}
                      </button>}
                    {item.resultData &&
                      <button type="button" className="btn btn-ghost small" disabled={saving} onClick={() => void downloadResult(item)}>Download result</button>}
                    {!['COMPLETED', 'CANCELLED'].includes(item.status) && item.status !== 'INPUT_READY' &&
                      <button type="button" className="btn btn-danger small" disabled={saving} onClick={() => void act(item, (id) => predictionsApi.cancel(requestId, id))}>Cancel</button>}
                  </span>
                </li>
              ))}
            </ul>
          )}
    </div>
  );
}
