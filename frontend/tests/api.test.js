import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiRequest, authApi, clearSession, loadSession, saveSession } from '../lib/api';

const session = {
  user: { id: 7, username: 'tester', fullName: 'Test User', role: 'USER', enabled: true },
};

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } });
}

describe('API client', () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    localStorage.clear();
    document.cookie = 'ATOM_CSRF=csrf-value; path=/';
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
    document.cookie = 'ATOM_CSRF=; Max-Age=0; path=/';
  });

  it('logs in without exposing a token and includes credentials', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(session));
    await expect(authApi.login('tester', 'password')).resolves.toEqual(session);
    const call = fetchMock.mock.calls[0];
    expect(call?.[1]?.credentials).toBe('include');
    expect(new Headers(call?.[1]?.headers).has('Authorization')).toBe(false);
    expect(JSON.parse(localStorage.getItem('atom-session') || '{}')).not.toHaveProperty('token');
  });

  it('sends the CSRF header for state-changing requests and no bearer token', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ ok: true }));
    await apiRequest('/api/protected', { method: 'POST', body: { value: 1 } });
    const request = fetchMock.mock.calls[0]?.[1];
    expect(new Headers(request?.headers).get('X-CSRF-TOKEN')).toBe('csrf-value');
    expect(new Headers(request?.headers).has('Authorization')).toBe(false);
    expect(request?.credentials).toBe('include');
  });

  it('does not add CSRF headers to safe requests', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ ok: true }));
    await apiRequest('/api/protected');
    expect(new Headers(fetchMock.mock.calls[0]?.[1]?.headers).has('X-CSRF-TOKEN')).toBe(false);
  });

  it('clears safe client metadata and dispatches expiry on an authenticated 401', async () => {
    saveSession(session);
    let expiryEvents = 0;
    const onExpired = () => { expiryEvents += 1; };
    window.addEventListener('atom-auth-expired', onExpired);
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: 'Expired' }, 401));
    await expect(apiRequest('/api/protected')).rejects.toMatchObject({ status: 401, message: 'Expired' });
    expect(loadSession()).toBeNull();
    expect(expiryEvents).toBe(1);
    window.removeEventListener('atom-auth-expired', onExpired);
  });

  it('normalizes JSON validation errors into ApiError', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({
      status: 400, error: 'Bad Request', message: 'Validation failed',
      validationErrors: { username: 'Already in use', password: 'Too short', ignored: 42 },
    }, 400));
    const error = await apiRequest('/api/auth/signup', { method: 'POST', skipAuth: true }).catch((value) => value);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toEqual(expect.objectContaining({
      name: 'ApiError', status: 400, message: 'Validation failed',
      validationErrors: { username: 'Already in use', password: 'Too short' },
    }));
  });

  it('loads only safe user metadata and never a JWT', () => {
    saveSession(session);
    expect(loadSession()).toEqual(session);
    expect(localStorage.getItem('atom-session')).not.toContain('token');
    clearSession();
    expect(loadSession()).toBeNull();
  });
});
