'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { ApiError, batchApi, requestsApi } from '@/lib/api';
import { useAuth } from '@/components/AuthProvider';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { ExportButtons } from '@/components/ExportButtons';
import { NotificationBell } from '@/components/NotificationBell';
import { useToast } from '@/components/Toast';
import { ConfirmModal } from '@/components/Modal';

const STATUS_FILTERS = ['ALL', 'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED'];

export function RequestList({ type }) {
  const { logout, user, loading: authLoading } = useAuth();
  const toast = useToast();
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedIds, setSelectedIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [batchBusy, setBatchBusy] = useState(false);
  const [error, setError] = useState(null);

  // Confirm Modal state
  const [confirmState, setConfirmState] = useState({
    isOpen: false,
    title: '',
    message: '',
    confirmText: 'Confirm',
    confirmDanger: false,
    action: null,
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {
        page,
        size: 10,
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        search: search.trim() || undefined,
      };
      const result = await requestsApi.list(type, params);
      setItems(result.content || []);
      setTotalPages(result.totalPages || 0);
      setTotalElements(result.totalElements || 0);
      setSelectedIds([]);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load requests.');
    } finally {
      setLoading(false);
    }
  }, [page, search, statusFilter, type]);

  useEffect(() => {
    if (!authLoading && user) void load();
  }, [authLoading, load, user]);

  async function create(event) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await requestsApi.create(type, title, description);
      setTitle('');
      setDescription('');
      toast.success(`${type} request created successfully!`);
      if (page === 0) await load();
      else setPage(0);
    } catch (caught) {
      const msg = caught instanceof ApiError ? caught.message : 'Unable to create request.';
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  function toggleSelectAll(event) {
    if (event.target.checked) {
      setSelectedIds(items.map((i) => i.id));
    } else {
      setSelectedIds([]);
    }
  }

  function toggleSelectRow(id) {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  }

  function triggerBatchAction(actionName, isDanger = false) {
    if (!selectedIds.length) return;
    setConfirmState({
      isOpen: true,
      title: `Batch ${actionName}`,
      message: `Are you sure you want to ${actionName.toLowerCase()} ${selectedIds.length} selected request(s)?`,
      confirmText: actionName,
      confirmDanger: isDanger,
      action: async () => {
        setBatchBusy(true);
        try {
          if (actionName.toLowerCase() === 'delete') {
            if (type === 'ATOM') await batchApi.deleteAtom(selectedIds);
            else await batchApi.deleteCpsr(selectedIds);
            toast.success(`Successfully deleted ${selectedIds.length} request(s).`);
          } else {
            const transitionAction = actionName.toLowerCase();
            if (type === 'ATOM') await batchApi.transitionAtom(selectedIds, transitionAction);
            else await batchApi.transitionCpsr(selectedIds, transitionAction);
            toast.success(`Successfully transitioned ${selectedIds.length} request(s) to ${actionName}.`);
          }
          await load();
        } catch (caught) {
          const msg = caught instanceof ApiError ? caught.message : 'Batch operation failed.';
          toast.error(msg);
        } finally {
          setBatchBusy(false);
          setConfirmState((prev) => ({ ...prev, isOpen: false }));
        }
      },
    });
  }

  const label = type === 'ATOM' ? 'ATOM' : 'CPSR';
  const isAdmin = user?.role === 'ADMIN';

  return (
    <ProtectedRoute>
      <main className="dashboard-shell">
        <header className="dashboard-topbar">
          <div className="brand-wrap">
            <div className="brand-mark">H</div>
            <div>
              <div className="brand-name">ATOM</div>
              <div className="brand-subtitle">{label} requests workspace</div>
            </div>
          </div>
          <nav className="dashboard-nav">
            <Link href="/user">Workspace</Link>
            <Link href="/atom" className={type === 'ATOM' ? 'nav-link active' : ''}>ATOM</Link>
            <Link href="/cpsr" className={type === 'CPSR' ? 'nav-link active' : ''}>CPSR</Link>
            <Link href="/profile">Profile</Link>
            {isAdmin && <Link href="/admin">Admin</Link>}
            <NotificationBell />
            <button type="button" className="link-button" onClick={logout}>Sign out</button>
          </nav>
        </header>

        <section className="panel two-col">
          {/* Create Form */}
          <form className="stack-form" onSubmit={create}>
            <div className="panel-header compact">
              <div>
                <p className="panel-eyebrow">New request</p>
                <h2>Create {label}</h2>
              </div>
            </div>
            <label>
              <span>Title *</span>
              <input
                required
                maxLength={200}
                placeholder="e.g. Synthesis Optimization v2"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            </label>
            <label>
              <span>Description *</span>
              <textarea
                required
                placeholder="Detailed objectives, constraints, or formulation notes..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </label>
            <button className="btn btn-primary" disabled={saving}>
              {saving ? 'Creating...' : `Create ${label} request`}
            </button>
          </form>

          {/* List and Filters */}
          <div>
            <div className="panel-header compact">
              <div>
                <p className="panel-eyebrow">Requests list</p>
                <h2>{label} requests ({totalElements})</h2>
              </div>
              <ExportButtons
                type={type}
                filter={{
                  status: statusFilter === 'ALL' ? undefined : statusFilter,
                  search: search.trim() || undefined,
                }}
              />
            </div>

            {/* Filter & Search Bar */}
            <div className="filter-bar">
              <div className="filter-row">
                <input
                  className="search-input"
                  style={{ flex: 1, minWidth: 200 }}
                  placeholder={`Search ${label} requests...`}
                  value={search}
                  onChange={(e) => {
                    setPage(0);
                    setSearch(e.target.value);
                  }}
                />
                {search && (
                  <button
                    type="button"
                    className="btn btn-ghost small"
                    onClick={() => {
                      setSearch('');
                      setPage(0);
                    }}
                  >
                    Clear
                  </button>
                )}
              </div>
              <div className="filter-pills">
                {STATUS_FILTERS.map((st) => (
                  <button
                    key={st}
                    type="button"
                    className={`status-pill-btn ${statusFilter === st ? 'active' : ''}`}
                    onClick={() => {
                      setStatusFilter(st);
                      setPage(0);
                    }}
                  >
                    {st === 'ALL' ? 'All statuses' : st}
                  </button>
                ))}
              </div>
            </div>

            {/* Batch Toolbar */}
            {selectedIds.length > 0 && (
              <div className="batch-toolbar">
                <div className="batch-info">
                  <span>{selectedIds.length} item(s) selected</span>
                </div>
                <div className="action-row" style={{ gap: 6 }}>
                  <button
                    type="button"
                    className="btn btn-secondary small"
                    onClick={() => triggerBatchAction('Submit')}
                    disabled={batchBusy}
                  >
                    Submit
                  </button>
                  {isAdmin && (
                    <>
                      <button
                        type="button"
                        className="btn btn-primary small"
                        onClick={() => triggerBatchAction('Approve')}
                        disabled={batchBusy}
                      >
                        Approve
                      </button>
                      <button
                        type="button"
                        className="btn btn-danger small"
                        onClick={() => triggerBatchAction('Reject', true)}
                        disabled={batchBusy}
                      >
                        Reject
                      </button>
                    </>
                  )}
                  <button
                    type="button"
                    className="btn btn-ghost small"
                    onClick={() => triggerBatchAction('Cancel', true)}
                    disabled={batchBusy}
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger small"
                    onClick={() => triggerBatchAction('Delete', true)}
                    disabled={batchBusy}
                  >
                    Delete
                  </button>
                </div>
              </div>
            )}

            {error && <div className="error-box" role="alert">{error}</div>}

            {loading ? (
              <div className="empty-box">Loading {label} requests...</div>
            ) : items.length === 0 ? (
              <div className="empty-box">No requests found matching current filters.</div>
            ) : (
              <div className="request-list">
                <div style={{ padding: '0 8px 6px', display: 'flex', alignItems: 'center', gap: 8 }}>
                  <input
                    type="checkbox"
                    className="select-checkbox"
                    checked={items.length > 0 && selectedIds.length === items.length}
                    onChange={toggleSelectAll}
                    id="select-all"
                  />
                  <label htmlFor="select-all" style={{ fontSize: 13, color: 'var(--muted)', cursor: 'pointer' }}>
                    Select all on this page
                  </label>
                </div>

                {items.map((item) => (
                  <div key={item.id} className="request-card-selectable">
                    <input
                      type="checkbox"
                      className="select-checkbox"
                      checked={selectedIds.includes(item.id)}
                      onChange={() => toggleSelectRow(item.id)}
                      aria-label={`Select request #${item.id}`}
                    />
                    <Link
                      className="request-card"
                      style={{ flex: 1, margin: 0 }}
                      href={`/${type.toLowerCase()}/${item.id}`}
                    >
                      <div>
                        <strong>{item.title}</strong>
                        <p>{item.description}</p>
                        <small className="table-subtitle">
                          #{item.id} · {item.ownerUsername || 'Me'} · {new Date(item.createdAt).toLocaleDateString()}
                        </small>
                      </div>
                      <span className="role-pill">{item.status}</span>
                    </Link>
                  </div>
                ))}
              </div>
            )}

            <div className="pagination">
              <button
                type="button"
                className="btn btn-ghost small"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Previous
              </button>
              <span>
                Page {page + 1} of {Math.max(totalPages, 1)}
              </span>
              <button
                type="button"
                className="btn btn-ghost small"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage(page + 1)}
              >
                Next
              </button>
            </div>
          </div>
        </section>

        {/* Confirmation Modal */}
        <ConfirmModal
          isOpen={confirmState.isOpen}
          onClose={() => setConfirmState((prev) => ({ ...prev, isOpen: false }))}
          onConfirm={confirmState.action}
          title={confirmState.title}
          message={confirmState.message}
          confirmText={confirmState.confirmText}
          confirmDanger={confirmState.confirmDanger}
          busy={batchBusy}
        />
      </main>
    </ProtectedRoute>
  );
}
