'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { adminApi, ApiError } from '@/lib/api';
import { useAuth } from '@/components/AuthProvider';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { NotificationBell } from '@/components/NotificationBell';
import { DashboardStats } from '@/components/DashboardStats';
import { useLanguage } from '@/components/LanguageProvider';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';

const emptyUser = { username: '', password: '', fullName: '', role: 'USER' };

function AdminContent() {
  const { user, logout } = useAuth();
  const { t } = useLanguage();
  const [users, setUsers] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [roles, setRoles] = useState([]);
  const [menus, setMenus] = useState([]);
  const [codeGroups, setCodeGroups] = useState([]);
  const [codes, setCodes] = useState([]);
  const [selectedGroup, setSelectedGroup] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [editingUser, setEditingUser] = useState(null);
  const [userForm, setUserForm] = useState(emptyUser);
  const [departmentForm, setDepartmentForm] = useState('');
  const [roleForm, setRoleForm] = useState('');
  const [menuForm, setMenuForm] = useState({ label: '', path: '' });
  const [codeGroupForm, setCodeGroupForm] = useState({ code: '', name: '' });
  const [codeForm, setCodeForm] = useState({ code: '', name: '' });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [usersPage, departmentsPage, rolesPage, menusPage] = await Promise.all([
        adminApi.users({ page, size: 10, search }),
        adminApi.departments({ page: 0, size: 100, search }),
        adminApi.roles({ page: 0, size: 100, search }),
        adminApi.menus({ page: 0, size: 100, search }),
      ]);
      setUsers(usersPage.content);
      setTotalPages(usersPage.totalPages);
      setDepartments(departmentsPage.content);
      setRoles(rolesPage.content);
      setMenus(menusPage.content);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : t('activity.noActivity'));
    } finally {
      setLoading(false);
    }
  }, [page, search, t]);

  useEffect(() => { void load(); }, [load]);

  const loadCodeGroups = useCallback(async () => {
    try {
      const groupsPage = await adminApi.codeGroups({ page: 0, size: 100 });
      setCodeGroups(groupsPage.content || []);
    } catch { /* code groups are optional; ignore load errors */ }
  }, []);

  useEffect(() => { void loadCodeGroups(); }, [loadCodeGroups]);

  const loadCodes = useCallback(async (groupCode) => {
    if (!groupCode) { setCodes([]); return; }
    try {
      setCodes(await adminApi.codesByGroup(groupCode));
    } catch { setCodes([]); }
  }, []);

  useEffect(() => { void loadCodes(selectedGroup); }, [loadCodes, selectedGroup]);

  function showError(caught) {
    setError(caught instanceof ApiError ? caught.message : t('activity.noActivity'));
    setMessage(null);
  }

  async function saveUser(event) {
    event.preventDefault();
    setSaving(true); setError(null);
    try {
      if (editingUser === null) await adminApi.createUser(userForm);
      else await adminApi.updateUser(editingUser, userForm);
      setUserForm(emptyUser); setEditingUser(null); setMessage(t('common.success')); await load();
    } catch (caught) { showError(caught); } finally { setSaving(false); }
  }

  async function removeUser(id) {
    if (!window.confirm(t('admin.confirmDelete'))) return;
    try { await adminApi.deleteUser(id); setMessage(t('common.success')); await load(); } catch (caught) { showError(caught); }
  }

  async function toggleUser(item) {
    try { await adminApi.setUserEnabled(item.id, !item.enabled); setMessage(t('common.success')); await load(); } catch (caught) { showError(caught); }
  }

  async function saveSimple(kind) {
    const value = kind === 'department' ? departmentForm : roleForm;
    if (!value.trim()) return;
    try {
      if (kind === 'department') await adminApi.createDepartment(value);
      else await adminApi.createRole(value);
      if (kind === 'department') setDepartmentForm(''); else setRoleForm('');
      setMessage(t('common.success')); await load();
    } catch (caught) { showError(caught); }
  }

  async function saveMenu(event) {
    event.preventDefault();
    try { await adminApi.createMenu(menuForm.label, menuForm.path); setMenuForm({ label: '', path: '' }); setMessage(t('common.success')); await load(); } catch (caught) { showError(caught); }
  }

  async function removeSimple(kind, id) {
    if (!window.confirm(t('admin.confirmDelete'))) return;
    try {
      if (kind === 'department') await adminApi.deleteDepartment(id);
      if (kind === 'role') await adminApi.deleteRole(id);
      if (kind === 'menu') await adminApi.deleteMenu(id);
      setMessage(t('common.success')); await load();
    } catch (caught) { showError(caught); }
  }

  async function editSimple(kind, item) {
    if (kind === 'menu' && 'label' in item) {
      const label = window.prompt(t('admin.label'), item.label);
      const path = window.prompt(t('admin.path'), item.path);
      if (label === null || path === null || !label.trim() || !path.trim()) return;
      try { await adminApi.updateMenu(item.id, label, path); setMessage(t('common.success')); await load(); } catch (caught) { showError(caught); }
      return;
    }
    if (!('name' in item)) return;
    const name = window.prompt(`${kind === 'department' ? t('admin.departments') : t('admin.roles')}`, item.name);
    if (name === null || !name.trim()) return;
    try {
      if (kind === 'department') await adminApi.updateDepartment(item.id, name);
      else await adminApi.updateRole(item.id, name);
      setMessage(t('common.success')); await load();
    } catch (caught) { showError(caught); }
  }

  async function createCodeGroup(event) {
    event.preventDefault();
    try {
      await adminApi.createCodeGroup(codeGroupForm.code.trim(), codeGroupForm.name.trim());
      setCodeGroupForm({ code: '', name: '' });
      setMessage(t('common.success')); await loadCodeGroups();
    } catch (caught) { showError(caught); }
  }  async function removeCodeGroup(item) {
    if (!window.confirm(t('admin.confirmDelete'))) return;
    try { await adminApi.deleteCodeGroup(item.id); if (selectedGroup === item.code) setSelectedGroup(''); setMessage(t('common.success')); await loadCodeGroups(); } catch (caught) { showError(caught); }
  }

  async function createCode(event) {
    event.preventDefault();
    if (!selectedGroup) { setError('Select a code group first.'); return; }
    const group = codeGroups.find((entry) => entry.groupCode === selectedGroup);
    if (!group) { setError('Selected group not found.'); return; }
    try {
      await adminApi.createCode({
        groupId: group.id,
        codeValue: codeForm.code.trim(),
        codeLabel: codeForm.name.trim(),
      });
      setCodeForm({ code: '', name: '' });
      setMessage(t('common.success')); await loadCodes(selectedGroup);
    } catch (caught) { showError(caught); }
  }

  async function toggleCode(item) {
    try { await adminApi.setCodeEnabled(item.id, !item.enabled); await loadCodes(selectedGroup); } catch (caught) { showError(caught); }
  }

  async function removeCode(item) {
    if (!window.confirm(t('admin.confirmDelete'))) return;
    try { await adminApi.deleteCode(item.id); await loadCodes(selectedGroup); } catch (caught) { showError(caught); }
  }

  return (
    <main className="dashboard-shell">
      <header className="dashboard-topbar">
        <div className="brand-wrap"><div className="brand-mark">A</div><div><div className="brand-name">{t('common.brand')}</div><div className="brand-subtitle">{t('admin.controlPanel')}</div></div></div>
        <nav className="dashboard-nav">
          <Link href="/user">{t('common.workspace')}</Link>
          <Link href="/atom">ATOM</Link>
          <Link href="/cpsr">CPSR</Link>
          <Link href="/admin" className="nav-link active">{t('common.admin')}</Link>
          <Link href="/admin/activity">{t('nav.auditLog')}</Link>
          <Link href="/profile">{t('common.profile')}</Link>
          <LanguageSwitcher />
          <NotificationBell />
          <button className="link-button" onClick={logout}>{t('common.signOut')}</button>
        </nav>
      </header>
      <section className="summary-row">
        <div className="summary-card accent welcome-card"><span>{t('admin.signedInAs')}</span><strong>{user?.username}</strong><small>ADMIN</small></div>
      </section>
      <DashboardStats showUsers />
      <section className="panel">
        <div className="panel-header compact"><div><p className="panel-eyebrow">{t('admin.administration')}</p><h2>{t('admin.manageAccounts')}</h2></div><input className="search-input" value={search} onChange={(event) => { setPage(0); setSearch(event.target.value); }} placeholder={t('admin.searchPlaceholder')} /></div>
        {error && <div className="error-box" role="alert">{error}</div>}
        {message && <div className="success-box" role="status">{message}</div>}
        <div className="two-col">
          <form className="stack-form" onSubmit={saveUser}>
            <h3>{editingUser === null ? t('admin.createUser') : t('admin.editUser')}</h3>
            <input required placeholder={t('home.username')} value={userForm.username} onChange={(event) => setUserForm({ ...userForm, username: event.target.value })} />
            <input required type="password" minLength={8} placeholder={t('admin.passwordNeverShown')} value={userForm.password} onChange={(event) => setUserForm({ ...userForm, password: event.target.value })} />
            <input required placeholder={t('signup.fullName')} value={userForm.fullName} onChange={(event) => setUserForm({ ...userForm, fullName: event.target.value })} />
            <select value={userForm.role} onChange={(event) => setUserForm({ ...userForm, role: event.target.value })}><option value="USER">USER</option><option value="ADMIN">ADMIN</option></select>
            <button className="btn btn-primary" disabled={saving}>{saving ? t('common.loading') : editingUser === null ? t('admin.createUser') : t('admin.saveUser')}</button>
            {editingUser !== null && <button type="button" className="btn btn-ghost" onClick={() => { setEditingUser(null); setUserForm(emptyUser); }}>{t('admin.cancelEdit')}</button>}
          </form>
          <div>
            {loading ? <div className="empty-box">{t('admin.loadingUsers')}</div> : users.length === 0 ? <div className="empty-box">{t('admin.noUsers')}</div> : <div className="table-wrap"><table className="data-table"><thead><tr><th>{t('admin.userHeader')}</th><th>{t('admin.roleHeader')}</th><th>{t('common.status')}</th><th>{t('common.actions')}</th></tr></thead><tbody>{users.map((item) => <tr key={item.id}><td>{item.username}<small className="table-subtitle">{item.fullName}</small></td><td><span className="role-pill">{item.role}</span></td><td>{item.enabled ? t('common.enabled') : t('common.disabled')}</td><td><div className="action-row"><button className="btn btn-ghost small" onClick={() => { setEditingUser(item.id); setUserForm({ username: item.username, password: '', fullName: item.fullName, role: item.role }); }}>{t('common.edit')}</button><button className="btn btn-secondary small" onClick={() => void toggleUser(item)}>{item.enabled ? t('common.disabled') : t('common.enabled')}</button><button className="btn btn-danger small" onClick={() => void removeUser(item.id)}>{t('common.delete')}</button></div></td></tr>)}</tbody></table></div>}
            <div className="pagination"><button className="btn btn-ghost small" disabled={page === 0} onClick={() => setPage(page - 1)}>{t('common.previous')}</button><span>{t('common.page')} {page + 1} {t('common.of')} {Math.max(totalPages, 1)}</span><button className="btn btn-ghost small" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>{t('common.next')}</button></div>
          </div>
        </div>
      </section>
      <section className="module-grid">
        <div className="module-box"><h3>{t('admin.departments')}</h3><div className="mini-form"><input value={departmentForm} onChange={(event) => setDepartmentForm(event.target.value)} placeholder={t('admin.departmentName')} /><button className="btn btn-secondary" onClick={() => void saveSimple('department')}>{t('common.add')}</button></div><ul className="tiny-list">{departments.map((item) => <li key={item.id}>{item.name}<span className="icon-actions"><button className="icon-button" onClick={() => void editSimple('department', item)}>{t('common.edit')}</button><button className="icon-button" onClick={() => void removeSimple('department', item.id)}>{t('common.delete')}</button></span></li>)}</ul></div>
        <div className="module-box"><h3>{t('admin.roles')}</h3><div className="mini-form"><input value={roleForm} onChange={(event) => setRoleForm(event.target.value)} placeholder={t('admin.roleName')} /><button className="btn btn-secondary" onClick={() => void saveSimple('role')}>{t('common.add')}</button></div><ul className="tiny-list">{roles.map((item) => <li key={item.id}>{item.name}<span className="icon-actions"><button className="icon-button" onClick={() => void editSimple('role', item)}>{t('common.edit')}</button><button className="icon-button" onClick={() => void removeSimple('role', item.id)}>{t('common.delete')}</button></span></li>)}</ul></div>
        <div className="module-box"><h3>{t('admin.menus')}</h3><form className="mini-form" onSubmit={saveMenu}><input required value={menuForm.label} onChange={(event) => setMenuForm({ ...menuForm, label: event.target.value })} placeholder={t('admin.label')} /><input required value={menuForm.path} onChange={(event) => setMenuForm({ ...menuForm, path: event.target.value })} placeholder="/path" /><button className="btn btn-secondary">{t('common.add')}</button></form><ul className="tiny-list">{menus.map((item) => <li key={item.id}>{item.label} ({item.path})<span className="icon-actions"><button className="icon-button" onClick={() => void editSimple('menu', item)}>{t('common.edit')}</button><button className="icon-button" onClick={() => void removeSimple('menu', item.id)}>{t('common.delete')}</button></span></li>)}</ul></div>
      </section>
      <section className="panel">
        <div className="panel-header compact"><div><p className="panel-eyebrow">{t('admin.refData')}</p><h2>{t('admin.codeGroups')}</h2></div></div>
        <div className="two-col">
          <div>
            <form className="mini-form menu-form three-col" onSubmit={createCodeGroup}>
              <input required maxLength={50} placeholder="Group code" value={codeGroupForm.code} onChange={(event) => setCodeGroupForm({ ...codeGroupForm, code: event.target.value })} />
              <input required maxLength={100} placeholder={t('admin.roleName')} value={codeGroupForm.name} onChange={(event) => setCodeGroupForm({ ...codeGroupForm, name: event.target.value })} />
              <button className="btn btn-secondary small">{t('common.add')}</button>
            </form>
            <ul className="tiny-list">
              {codeGroups.map((group) => (
                <li key={group.id}>
                  <strong>{group.groupCode}</strong>
                  {group.groupName ? ` · ${group.groupName}` : ''}
                  <span className="icon-actions">
                    <button type="button" className="icon-button" onClick={() => setSelectedGroup(group.groupCode)}>View codes</button>
                    <button type="button" className="icon-button" onClick={() => void removeCodeGroup(group)}>{t('common.delete')}</button>
                  </span>
                </li>
              ))}
              {codeGroups.length === 0 && <li>{t('common.noData')}</li>}
            </ul>
          </div>
          <div>
            <form className="mini-form menu-form three-col" onSubmit={createCode}>
              <input required maxLength={50} placeholder={`New code in ${selectedGroup || '(select a group)'}`} value={codeForm.code} onChange={(event) => setCodeForm({ ...codeForm, code: event.target.value })} disabled={!selectedGroup} />
              <input required maxLength={100} placeholder={t('admin.label')} value={codeForm.name} onChange={(event) => setCodeForm({ ...codeForm, name: event.target.value })} disabled={!selectedGroup} />
              <button className="btn btn-secondary small" disabled={!selectedGroup}>{t('common.add')}</button>
            </form>
            {!selectedGroup ? <div className="empty-box small-pad">{t('admin.noData')}</div>
              : codes.length === 0 ? <div className="empty-box small-pad">{t('common.noData')}</div>
                : (
                  <ul className="tiny-list">
                    {codes.map((codeItem) => (
                      <li key={codeItem.id} style={{ opacity: codeItem.enabled ? 1 : 0.5 }}>
                        <strong>{codeItem.codeValue}</strong> · {codeItem.codeLabel}
                        <span className="icon-actions">
                          <button type="button" className="icon-button" onClick={() => void toggleCode(codeItem)}>{codeItem.enabled ? t('common.disabled') : t('common.enabled')}</button>
                          <button type="button" className="icon-button" onClick={() => void removeCode(codeItem)}>{t('common.delete')}</button>
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
          </div>
        </div>
      </section>
    </main>
  );
}

export default function AdminPage() {
  return <ProtectedRoute adminOnly><AdminContent /></ProtectedRoute>;
}
