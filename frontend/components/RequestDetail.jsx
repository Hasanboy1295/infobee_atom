'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { adminApi, ApiError, requestsApi } from '@/lib/api';
import { useAuth } from '@/components/AuthProvider';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { AttachmentsPanel } from '@/components/AttachmentsPanel';
import { AtomPredictionsPanel } from '@/components/AtomPredictionsPanel';
import { EvaluationsPanel, LlmPanel, SubstancesPanel } from '@/components/CpsrPanels';
import { NotificationBell } from '@/components/NotificationBell';
import { useToast } from '@/components/Toast';
import { ConfirmModal } from '@/components/Modal';

export function RequestDetail({ type, id }) {
  const { user, logout, loading: authLoading } = useAuth();
  const toast = useToast();
  const [request, setRequest] = useState(null);
  const [comments, setComments] = useState([]);
  const [history, setHistory] = useState([]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [comment, setComment] = useState('');
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [evaluatorOptions, setEvaluatorOptions] = useState([]);

  // Confirm delete modal
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [loaded, loadedComments, loadedHistory] = await Promise.all([
        requestsApi.get(type, id),
        requestsApi.comments(type, id),
        requestsApi.history(type, id),
      ]);
      setRequest(loaded);
      setTitle(loaded.title);
      setDescription(loaded.description);
      setComments(loadedComments);
      setHistory(loadedHistory);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load request.');
    } finally {
      setLoading(false);
    }
  }, [id, type]);

  useEffect(() => {
    if (!authLoading && user) void load();
  }, [authLoading, load, user]);

  useEffect(() => {
    if (user?.role !== 'ADMIN') return undefined;
    let active = true;
    adminApi.users({ page: 0, size: 100 })
      .then((result) => {
        if (active) setEvaluatorOptions(result.content || []);
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, [user]);

  async function update(event) {
    event.preventDefault();
    setSaving(true);
    try {
      await requestsApi.update(type, id, title, description);
      toast.success('Request updated successfully.');
      await load();
    } catch (caught) {
      const msg = caught instanceof ApiError ? caught.message : 'Unable to update request.';
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  async function transition(action) {
    setSaving(true);
    try {
      await requestsApi.transition(type, id, action, note);
      setNote('');
      toast.success(`Request status updated: ${action}`);
      await load();
    } catch (caught) {
      const msg = caught instanceof ApiError ? caught.message : 'Unable to change status.';
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  async function addComment(event) {
    event.preventDefault();
    if (!comment.trim()) return;
    setSaving(true);
    try {
      await requestsApi.addComment(type, id, comment);
      setComment('');
      toast.success('Comment added.');
      await load();
    } catch (caught) {
      const msg = caught instanceof ApiError ? caught.message : 'Unable to add comment.';
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    setSaving(true);
    try {
      await requestsApi.remove(type, id);
      toast.success('Draft request deleted.');
      window.location.href = `/${type.toLowerCase()}`;
    } catch (caught) {
      const msg = caught instanceof ApiError ? caught.message : 'Unable to delete request.';
      setError(msg);
      toast.error(msg);
      setSaving(false);
      setDeleteModalOpen(false);
    }
  }

  if (loading) {
    return (
      <ProtectedRoute>
        <main className="dashboard-shell">
          <div className="empty-box">Loading {type} request details...</div>
        </main>
      </ProtectedRoute>
    );
  }

  if (!request) {
    return (
      <ProtectedRoute>
        <main className="dashboard-shell">
          <div className="error-box">{error || 'Request not found.'}</div>
        </main>
      </ProtectedRoute>
    );
  }

  const editable = request.status === 'DRAFT' || request.status === 'REJECTED';
  const isOwner = user?.id === request.ownerId;
  const isAdmin = user?.role === 'ADMIN';
  const actions = [];
  if ((isOwner || isAdmin) && editable) actions.push('submit');
  if (isAdmin && request.status === 'SUBMITTED') actions.push('review');
  if (isAdmin && (request.status === 'SUBMITTED' || request.status === 'UNDER_REVIEW')) actions.push('approve', 'reject');
  if ((isOwner || isAdmin) && request.status !== 'APPROVED' && request.status !== 'CANCELLED') actions.push('cancel');

  return (
    <ProtectedRoute>
      <main className="dashboard-shell">
        <header className="dashboard-topbar">
          <div className="brand-wrap">
            <div className="brand-mark">H</div>
            <div>
              <div className="brand-name">ATOM</div>
              <div className="brand-subtitle">{type} request #{id}</div>
            </div>
          </div>
          <nav className="dashboard-nav">
            <Link href={`/${type.toLowerCase()}`}>Back to {type} list</Link>
            <Link href="/user">Workspace</Link>
            <Link href="/profile">Profile</Link>
            {isAdmin && <Link href="/admin">Admin</Link>}
            <NotificationBell />
            <button type="button" className="link-button" onClick={logout}>Sign out</button>
          </nav>
        </header>

        {error && <div className="error-box" role="alert">{error}</div>}

        {/* Main Request Header & Actions */}
        <section className="panel">
          <div className="panel-header">
            <div>
              <p className="panel-eyebrow">{request.ownerUsername || 'Owner'} · {request.type}</p>
              <h2>{request.title}</h2>
            </div>
            <span className="role-pill">{request.status}</span>
          </div>

          <p className="request-description">{request.description}</p>
          <small style={{ color: 'var(--muted)', display: 'block', margin: '8px 0 14px' }}>
            Created {new Date(request.createdAt).toLocaleString()} · Updated {new Date(request.updatedAt).toLocaleString()}
          </small>

          {request.evaluationResult && (
            <div className="response-box" style={{ marginBottom: 14 }}>
              <strong>Evaluation Summary:</strong> {request.evaluationResult}
            </div>
          )}

          {/* Action buttons */}
          {actions.length > 0 && (
            <div className="action-row request-actions">
              {actions.map((action) => (
                <button
                  key={action}
                  className={action === 'reject' || action === 'cancel' ? 'btn btn-danger' : 'btn btn-secondary'}
                  disabled={saving}
                  onClick={() => void transition(action)}
                >
                  {action.toUpperCase()}
                </button>
              ))}
            </div>
          )}

          {/* Edit Request Form for Owner */}
          {editable && isOwner && (
            <form className="stack-form" onSubmit={update} style={{ marginTop: 20 }}>
              <h3>Edit request details</h3>
              <label>
                <span>Title</span>
                <input
                  required
                  maxLength={200}
                  value={title}
                  onChange={(event) => setTitle(event.target.value)}
                />
              </label>
              <label>
                <span>Description</span>
                <textarea
                  required
                  value={description}
                  onChange={(event) => setDescription(event.target.value)}
                />
              </label>
              <div className="action-row">
                <button className="btn btn-primary" disabled={saving}>
                  {saving ? 'Saving...' : 'Save changes'}
                </button>
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={() => setDeleteModalOpen(true)}
                  disabled={saving}
                >
                  Delete draft
                </button>
              </div>
            </form>
          )}

          <label className="note-field" style={{ marginTop: 14 }}>
            <span>Transition note (optional)</span>
            <textarea
              value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="Provide context or instructions for status review..."
              style={{ minHeight: 70 }}
            />
          </label>
        </section>

        {/* Comments & History */}
        <section className="two-col">
          <div className="panel">
            <div className="panel-header compact">
              <h3>Comments ({comments.length})</h3>
            </div>
            <form className="mini-form" onSubmit={addComment}>
              <textarea
                required
                value={comment}
                onChange={(event) => setComment(event.target.value)}
                placeholder="Write a comment..."
                style={{ minHeight: 60 }}
              />
              <button className="btn btn-secondary" disabled={saving}>
                Comment
              </button>
            </form>
            {comments.length === 0 ? (
              <div className="empty-box small-pad">No comments yet.</div>
            ) : (
              <ul className="tiny-list">
                {comments.map((item) => (
                  <li key={item.id}>
                    <strong>{item.authorUsername}</strong>
                    <br />
                    {item.body}
                    <small className="table-subtitle">{new Date(item.createdAt).toLocaleString()}</small>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="panel">
            <div className="panel-header compact">
              <h3>Audit History ({history.length})</h3>
            </div>
            {history.length === 0 ? (
              <div className="empty-box small-pad">No status history records.</div>
            ) : (
              <ul className="tiny-list">
                {history.map((item) => (
                  <li key={item.id}>
                    <strong>{item.fromStatus || 'NEW'} → {item.toStatus}</strong>
                    <br />
                    {item.actorUsername}
                    {item.note ? `: ${item.note}` : ''}
                    <small className="table-subtitle">{new Date(item.createdAt).toLocaleString()}</small>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>

        {/* Module Sub-Panels */}
        <section className="module-grid single-column">
          {type === 'ATOM' && <AtomPredictionsPanel requestId={id} />}
          {type === 'CPSR' && <SubstancesPanel requestId={id} />}
          {type === 'CPSR' && <EvaluationsPanel requestId={id} users={evaluatorOptions} />}
          {type === 'CPSR' && <LlmPanel requestId={id} />}
          <AttachmentsPanel type={type} requestId={id} />
        </section>

        {/* Confirm Delete Modal */}
        <ConfirmModal
          isOpen={deleteModalOpen}
          onClose={() => setDeleteModalOpen(false)}
          onConfirm={confirmDelete}
          title="Delete Draft Request"
          message={`Are you sure you want to permanently delete this ${type} draft (#${id})? This action cannot be undone.`}
          confirmText="Delete"
          confirmDanger
          busy={saving}
        />
      </main>
    </ProtectedRoute>
  );
}
