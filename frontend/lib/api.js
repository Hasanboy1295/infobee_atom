const API_BASE_URL = (process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const SESSION_KEY = 'atom-session';
const CSRF_COOKIE_NAME = process.env.NEXT_PUBLIC_CSRF_COOKIE_NAME || 'ATOM_CSRF';
const CSRF_HEADER_NAME = 'X-CSRF-TOKEN';

export class ApiError extends Error {
  constructor(message, status, validationErrors = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.validationErrors = validationErrors;
  }
}

function isRecord(value) {
  return typeof value === 'object' && value !== null;
}

function isUser(value) {
  return isRecord(value) && typeof value.id === 'number' && typeof value.username === 'string'
    && typeof value.fullName === 'string' && typeof value.role === 'string' && typeof value.enabled === 'boolean';
}

function validationErrors(value) {
  if (!isRecord(value)) return undefined;
  const result = {};
  for (const [field, message] of Object.entries(value)) if (typeof message === 'string') result[field] = message;
  return Object.keys(result).length > 0 ? result : undefined;
}

function readCookie(name) {
  if (typeof document === 'undefined') return null;
  const prefix = `${encodeURIComponent(name)}=`;
  const cookie = document.cookie.split('; ').find((entry) => entry.startsWith(prefix));
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : null;
}

export function saveSession(session) {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(SESSION_KEY, JSON.stringify({ user: session.user }));
  } catch {
    // Storage may be disabled; the cookie session still works for this page lifetime.
  }
}

export function loadSession() {
  if (typeof window === 'undefined') return null;
  try {
    const raw = window.localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!isRecord(parsed) || !isUser(parsed.user)) return null;
    return { user: parsed.user };
  } catch {
    return null;
  }
}

export function clearSession() {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.removeItem(SESSION_KEY);
  } catch {
    // Ignore unavailable storage.
  }
}

function expireSession() {
  clearSession();
  if (typeof window !== 'undefined') window.dispatchEvent(new Event('atom-auth-expired'));
}

async function parseBody(response) {
  const contentType = response.headers.get('content-type') || '';
  if (response.status === 204 || !contentType.includes('application/json')) return null;
  try { return await response.json(); } catch { return null; }
}

export async function apiRequest(path, options = {}) {
  const { body, skipAuth, headers: providedHeaders, ...init } = options;
  const headers = new Headers(providedHeaders);
  if (body !== undefined) headers.set('Content-Type', 'application/json');
  const method = (init.method || 'GET').toUpperCase();
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    const csrfToken = readCookie(CSRF_COOKIE_NAME);
    if (csrfToken) headers.set(CSRF_HEADER_NAME, csrfToken);
  }
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    credentials: 'include',
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const payload = await parseBody(response);
  if (!response.ok) {
    if (response.status === 401 && !skipAuth) expireSession();
    const errorPayload = isRecord(payload) ? {
      status: typeof payload.status === 'number' ? payload.status : undefined,
      error: typeof payload.error === 'string' ? payload.error : undefined,
      message: typeof payload.message === 'string' ? payload.message : undefined,
      validationErrors: validationErrors(payload.validationErrors),
    } : {};
    throw new ApiError(
      errorPayload.message || errorPayload.error || `Request failed with status ${response.status}`,
      response.status, errorPayload.validationErrors || {},
    );
  }
  return payload;
}

async function apiUpload(path, formData) {
  const headers = new Headers();
  const csrfToken = readCookie(CSRF_COOKIE_NAME);
  if (csrfToken) headers.set(CSRF_HEADER_NAME, csrfToken);
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers,
    credentials: 'include',
    body: formData,
  });
  const payload = await parseBody(response);
  if (!response.ok) throw new ApiError(`Upload failed with status ${response.status}`, response.status);
  return payload;
}

export const authApi = {
  login: (username, password) =>
    apiRequest('/api/auth/login', { method: 'POST', body: { username, password }, skipAuth: true }),
  me: () => apiRequest('/api/auth/me'),
  signup: (username, password, fullName) =>
    apiRequest('/api/auth/signup', { method: 'POST', body: { username, password, fullName }, skipAuth: true }),
  logout: () => apiRequest('/api/auth/logout', { method: 'POST' }),
};

function query(params) {
  const values = Object.entries(params).filter(([, value]) => value !== undefined);
  return values.length ? `?${new URLSearchParams(values.map(([key, value]) => [key, String(value)]))}` : '';
}

export const adminApi = {
  users: (params) => apiRequest(`/api/admin/users${query({ ...params, sort: 'id,asc' })}`),
  departments: (params) => apiRequest(`/api/admin/departments${query({ ...params, sort: 'id,asc' })}`),
  roles: (params) => apiRequest(`/api/admin/roles${query({ ...params, sort: 'id,asc' })}`),
  menus: (params) => apiRequest(`/api/admin/menus${query({ ...params, sort: 'id,asc' })}`),
  createUser: (body) => apiRequest('/api/admin/users', { method: 'POST', body }),
  updateUser: (id, body) => apiRequest(`/api/admin/users/${id}`, { method: 'PUT', body }),
  deleteUser: (id) => apiRequest(`/api/admin/users/${id}`, { method: 'DELETE' }),
  setUserEnabled: (id, enabled) => apiRequest(`/api/admin/users/${id}/enabled?enabled=${enabled}`, { method: 'PATCH' }),
  createDepartment: (name) => apiRequest('/api/admin/departments', { method: 'POST', body: { name } }),
  updateDepartment: (id, name) => apiRequest(`/api/admin/departments/${id}`, { method: 'PUT', body: { name } }),
  deleteDepartment: (id) => apiRequest(`/api/admin/departments/${id}`, { method: 'DELETE' }),
  createRole: (name) => apiRequest('/api/admin/roles', { method: 'POST', body: { name } }),
  updateRole: (id, name) => apiRequest(`/api/admin/roles/${id}`, { method: 'PUT', body: { name } }),
  deleteRole: (id) => apiRequest(`/api/admin/roles/${id}`, { method: 'DELETE' }),
  createMenu: (label, path) => apiRequest('/api/admin/menus', { method: 'POST', body: { label, path } }),
  updateMenu: (id, label, path) => apiRequest(`/api/admin/menus/${id}`, { method: 'PUT', body: { label, path } }),
  deleteMenu: (id) => apiRequest(`/api/admin/menus/${id}`, { method: 'DELETE' }),
  codeGroups: (params) => apiRequest(`/api/admin/code-groups${query({ ...params, sort: 'id,asc' })}`),
  createCodeGroup: (groupCode, groupName) =>
    apiRequest('/api/admin/code-groups', { method: 'POST', body: { groupCode, groupName } }),
  deleteCodeGroup: (id) => apiRequest(`/api/admin/code-groups/${id}`, { method: 'DELETE' }),
  codesByGroup: (groupCode) => apiRequest(`/api/admin/codes/by-group/${encodeURIComponent(groupCode)}`),
  createCode: (body) => apiRequest('/api/admin/codes', { method: 'POST', body }),
  updateCode: (id, body) => apiRequest(`/api/admin/codes/${id}`, { method: 'PUT', body }),
  deleteCode: (id) => apiRequest(`/api/admin/codes/${id}`, { method: 'DELETE' }),
  setCodeEnabled: (id, enabled) => apiRequest(`/api/admin/codes/${id}/enabled?enabled=${enabled}`, { method: 'PATCH' }),
};

export const statsApi = {
  dashboard: () => apiRequest('/api/stats/dashboard'),
};

export const notificationsApi = {
  list: (params = {}) => apiRequest(`/api/notifications${query(params)}`),
  summary: () => apiRequest('/api/notifications/summary'),
  markRead: (id) => apiRequest(`/api/notifications/${id}/read`, { method: 'POST' }),
  markAllRead: () => apiRequest('/api/notifications/read-all', { method: 'POST' }),
};

export const activityLogsApi = {
  list: (params = {}) => apiRequest(`/api/activity-logs${query({ size: 50, ...params })}`),
};

export const exportApi = {
  atom: (params = {}) => apiRequest(`/api/export/atom${query(params)}`),
  cpsr: (params = {}) => apiRequest(`/api/export/cpsr${query(params)}`),
  atomUrl: (params = {}) => `${API_BASE_URL}/api/export/atom${query(params)}`,
  cpsrUrl: (params = {}) => `${API_BASE_URL}/api/export/cpsr${query(params)}`,
};

export const toxicityApi = {
  search: (params = {}) => apiRequest(`/api/toxicity-records${query(params)}`),
  lookup: (params = {}) => apiRequest(`/api/toxicity-records/lookup${query(params)}`),
};

export const batchApi = {
  transitionAtom: (ids, action, note) =>
    apiRequest('/api/batch/atom/transition', { method: 'POST', body: { ids, action, note } }),
  transitionCpsr: (ids, action, note) =>
    apiRequest('/api/batch/cpsr/transition', { method: 'POST', body: { ids, action, note } }),
  deleteAtom: (ids) =>
    apiRequest('/api/batch/atom/delete', { method: 'POST', body: ids }),
  deleteCpsr: (ids) =>
    apiRequest('/api/batch/cpsr/delete', { method: 'POST', body: ids }),
};

function requestPath(type) {
  return type === 'ATOM' ? '/api/atom-requests' : '/api/cpsr-requests';
}

export const requestsApi = {
  list: (type, paramsOrPage = 0, size = 10) => {
    const params = typeof paramsOrPage === 'object' && paramsOrPage !== null
      ? { sort: 'createdAt,desc', ...paramsOrPage }
      : { page: paramsOrPage, size, sort: 'createdAt,desc' };
    return apiRequest(`${requestPath(type)}${query(params)}`);
  },
  get: (type, id) => apiRequest(`${requestPath(type)}/${id}`),
  create: (type, title, description) => apiRequest(requestPath(type), { method: 'POST', body: { title, description } }),
  update: (type, id, title, description) => apiRequest(`${requestPath(type)}/${id}`, { method: 'PUT', body: { title, description } }),
  remove: (type, id) => apiRequest(`${requestPath(type)}/${id}`, { method: 'DELETE' }),
  transition: (type, id, action, note) => apiRequest(`${requestPath(type)}/${id}/${action}`, { method: 'POST', body: note ? { note } : undefined }),
  comments: (type, id) => apiRequest(`${requestPath(type)}/${id}/comments`),
  addComment: (type, id, body) => apiRequest(`${requestPath(type)}/${id}/comments`, { method: 'POST', body: { body } }),
  history: (type, id) => apiRequest(`${requestPath(type)}/${id}/history`),
};

export const filesApi = {
  list: (type, requestId) => apiRequest(`/api/files/${type.toLowerCase()}/${requestId}`),
  upload: (type, requestId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiUpload(`/api/files/${type.toLowerCase()}/${requestId}`, formData);
  },
  downloadUrl: (storedFilename) => `${API_BASE_URL}/api/files/download/${encodeURIComponent(storedFilename)}`,
  remove: (id) => apiRequest(`/api/files/${id}`, { method: 'DELETE' }),
};

export const predictionsApi = {
  list: (atomRequestId) => apiRequest(`/api/atom-requests/${atomRequestId}/predictions`),
  get: (atomRequestId, predictionId) => apiRequest(`/api/atom-requests/${atomRequestId}/predictions/${predictionId}`),
  create: (atomRequestId, inputConditions) =>
    apiRequest(`/api/atom-requests/${atomRequestId}/predictions`, {
      method: 'POST',
      body: { inputConditions },
    }),
  submit: (atomRequestId, predictionId) =>
    apiRequest(`/api/atom-requests/${atomRequestId}/predictions/${predictionId}/submit`, { method: 'POST' }),
  cancel: (atomRequestId, predictionId) =>
    apiRequest(`/api/atom-requests/${atomRequestId}/predictions/${predictionId}/cancel`, { method: 'POST' }),
  result: (atomRequestId, predictionId) =>
    apiRequest(`/api/atom-requests/${atomRequestId}/predictions/${predictionId}/download`),
};

export const evaluationsApi = {
  substances: (cpsrRequestId) => apiRequest(`/api/cpsr-requests/${cpsrRequestId}/substances`),
  addSubstance: (cpsrRequestId, body) =>
    apiRequest(`/api/cpsr-requests/${cpsrRequestId}/substances`, { method: 'POST', body }),
  updateSubstance: (cpsrRequestId, substanceId, body) =>
    apiRequest(`/api/cpsr-requests/${cpsrRequestId}/substances/${substanceId}`, { method: 'PUT', body }),
  deleteSubstance: (cpsrRequestId, substanceId) =>
    apiRequest(`/api/cpsr-requests/${cpsrRequestId}/substances/${substanceId}`, { method: 'DELETE' }),
  evaluations: (cpsrRequestId) => apiRequest(`/api/cpsr-requests/${cpsrRequestId}/evaluations`),
  createEvaluation: (cpsrRequestId, body) =>
    apiRequest(`/api/cpsr-requests/${cpsrRequestId}/evaluations`, { method: 'POST', body }),
  updateEvaluation: (cpsrRequestId, evalId, body) =>
    apiRequest(`/api/cpsr-requests/${cpsrRequestId}/evaluations/${evalId}`, { method: 'PUT', body }),
  evaluationAction: (cpsrRequestId, evalId, action) =>
    apiRequest(`/api/cpsr-requests/${cpsrRequestId}/evaluations/${evalId}/${action}`, { method: 'POST' }),
  calculateSedMos: (cpsrRequestId, body) =>
    apiRequest(`/api/cpsr-requests/${cpsrRequestId}/evaluations/calculate`, { method: 'POST', body }),
};

export const llmApi = {
  list: (cpsrRequestId) => apiRequest(`/api/cpsr-requests/${cpsrRequestId}/llm`),
  submit: (cpsrRequestId, body) => apiRequest(`/api/cpsr-requests/${cpsrRequestId}/llm`, { method: 'POST', body }),
  get: (cpsrRequestId, inferenceId) => apiRequest(`/api/cpsr-requests/${cpsrRequestId}/llm/${inferenceId}`),
};

export { API_BASE_URL };

