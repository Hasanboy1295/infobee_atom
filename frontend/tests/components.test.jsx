import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AdminPage from '../app/admin/page';
import HomePage from '../app/page';
import SignupPage from '../app/signup/page';
import { AuthProvider } from '../components/AuthProvider';
import { LanguageProvider } from '../components/LanguageProvider';
import { ToastProvider } from '../components/Toast';
import { RequestDetail } from '../components/RequestDetail';
import { RequestList } from '../components/RequestList';
import { ApiError, adminApi, authApi, notificationsApi, requestsApi, saveSession } from '../lib/api';

const routerReplace = vi.hoisted(() => vi.fn());

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: routerReplace }),
  useParams: () => ({ id: '5' }),
}));

vi.mock('next/link', () => ({
  default: ({ children, href, ...props }) => (
    <a href={href} {...props}>{children}</a>
  ),
}));

const user = {
  id: 7,
  username: 'tester',
  fullName: 'Test User',
  role: 'USER',
  enabled: true,
};

const admin = {
  id: 1,
  username: 'admin',
  fullName: 'Admin User',
  role: 'ADMIN',
  enabled: true,
};

const userSession = { user };
const adminSession = { user: admin };

function page(content) {
  return { content, page: 0, size: 10, totalElements: content.length, totalPages: 1 };
}

const request = {
  id: 5,
  type: 'CPSR',
  ownerId: user.id,
  ownerUsername: user.username,
  title: 'Safety review',
  description: 'Review this submission',
  status: 'DRAFT',
  evaluationResult: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

function renderWithAuth(element, session) {
  localStorage.setItem('atom-lang', 'en');
  saveSession(session);
  vi.spyOn(authApi, 'me').mockResolvedValue(session.user);
  render(
    <LanguageProvider>
      <ToastProvider>
        <AuthProvider>{element}</AuthProvider>
      </ToastProvider>
    </LanguageProvider>
  );
}

function renderWithProviders(element) {
  localStorage.setItem('atom-lang', 'en');
  render(
    <LanguageProvider>
      <ToastProvider>{element}</ToastProvider>
    </LanguageProvider>
  );
}

describe('login UI', () => {
  beforeEach(() => {
    localStorage.clear();
    routerReplace.mockReset();
    vi.spyOn(notificationsApi, 'summary').mockResolvedValue({ unreadCount: 0 });
    vi.spyOn(notificationsApi, 'list').mockResolvedValue(page([]));
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('logs in and redirects an administrator to the admin page', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({ user: admin });
    renderWithProviders(
      <AuthProvider><HomePage /></AuthProvider>
    );

    fireEvent.change(screen.getByPlaceholderText('Enter password'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => expect(routerReplace).toHaveBeenCalledWith('/admin'));
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('shows an authentication error without redirecting', async () => {
    vi.spyOn(authApi, 'login').mockRejectedValue(new ApiError('Invalid credentials', 401));
    renderWithProviders(
      <AuthProvider><HomePage /></AuthProvider>
    );
    routerReplace.mockReset();
    fireEvent.change(screen.getByPlaceholderText('Enter password'), { target: { value: 'password' } });
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect((await screen.findByRole('alert')).textContent).toContain(
      'Invalid username or password, or this account is disabled.',
    );
    expect(routerReplace).not.toHaveBeenCalled();
  });
});

describe('signup UI', () => {
  beforeEach(() => {
    localStorage.clear();
    routerReplace.mockReset();
    vi.spyOn(notificationsApi, 'summary').mockResolvedValue({ unreadCount: 0 });
    vi.spyOn(notificationsApi, 'list').mockResolvedValue(page([]));
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('renders modern signup page and creates a new account', async () => {
    const signupSpy = vi.spyOn(authApi, 'signup').mockResolvedValue({ id: 10, username: 'newuser', fullName: 'New User' });
    renderWithProviders(
      <SignupPage />
    );

    expect(screen.getByText('Create your account')).toBeTruthy();
    expect(screen.getByText('ATOM Kinetics Yield Engine')).toBeTruthy();

fireEvent.change(screen.getByPlaceholderText('e.g. Kim Minsoo'), { target: { value: 'New User' } });
fireEvent.change(screen.getByPlaceholderText('e.g. user1234'), { target: { value: 'newuser' } });
    const passwordInputs = screen.getAllByPlaceholderText('Enter password');
    fireEvent.change(passwordInputs[0], { target: { value: 'Secret123!' } });
    fireEvent.change(passwordInputs[1], { target: { value: 'Secret123!' } });

    fireEvent.click(screen.getByRole('button', { name: /Create account/i }));

    await waitFor(() => expect(signupSpy).toHaveBeenCalledWith('newuser', 'Secret123!', 'New User'));
  });
});

describe('admin authorization and CRUD UI', () => {
  beforeEach(() => {
    localStorage.clear();
    routerReplace.mockReset();
    vi.spyOn(notificationsApi, 'summary').mockResolvedValue({ unreadCount: 0 });
    vi.spyOn(notificationsApi, 'list').mockResolvedValue(page([]));
    vi.spyOn(requestsApi, 'list').mockResolvedValue(page([]));
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('redirects a non-admin user away from the admin page', async () => {
    renderWithAuth(<AdminPage />, userSession);

    await waitFor(() => expect(routerReplace).toHaveBeenCalledWith('/user'));
    expect(screen.queryByText('Manage accounts')).toBeNull();
  });

  it('loads users and creates a user through the mocked API boundary', async () => {
    const existingUser = { ...user, id: 8, username: 'existing' };
    const createdUser = { ...user, id: 9, username: 'new-user', fullName: 'New User' };
    const users = vi.spyOn(adminApi, 'users').mockResolvedValue(page([existingUser]));
    vi.spyOn(adminApi, 'departments').mockResolvedValue(page([]));
    vi.spyOn(adminApi, 'roles').mockResolvedValue(page([]));
    vi.spyOn(adminApi, 'menus').mockResolvedValue(page([]));
    vi.spyOn(adminApi, 'codeGroups').mockResolvedValue(page([]));
    const createUser = vi.spyOn(adminApi, 'createUser').mockResolvedValue(createdUser);
    renderWithAuth(<AdminPage />, adminSession);

    expect(await screen.findByText('existing')).toBeTruthy();
    expect(users).toHaveBeenCalledWith({ page: 0, size: 10, search: '' });

    fireEvent.change(screen.getByPlaceholderText('Username'), { target: { value: 'new-user' } });
    fireEvent.change(screen.getByPlaceholderText('Password (never displayed)'), {
      target: { value: 'new-password' },
    });
    fireEvent.change(screen.getByPlaceholderText('Full name'), { target: { value: 'New User' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create user' }));

    await waitFor(() => expect(createUser).toHaveBeenCalledWith({
      username: 'new-user',
      password: 'new-password',
      fullName: 'New User',
      role: 'USER',
    }));
    expect((await screen.findByRole('status')).textContent).toContain('Success');
  });
});

describe('request workflow UI', () => {
  beforeEach(() => {
    localStorage.clear();
    routerReplace.mockReset();
    vi.spyOn(notificationsApi, 'summary').mockResolvedValue({ unreadCount: 0 });
    vi.spyOn(notificationsApi, 'list').mockResolvedValue(page([]));
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('loads ATOM requests and creates a request', async () => {
    const listedRequest = {
      ...request,
      id: 11,
      type: 'ATOM',
      title: 'Existing ATOM request',
    };
    const list = vi.spyOn(requestsApi, 'list').mockResolvedValue(page([listedRequest]));
    const create = vi.spyOn(requestsApi, 'create').mockResolvedValue({
      ...listedRequest,
      id: 12,
      title: 'New ATOM request',
    });
    renderWithAuth(<RequestList type="ATOM" />, userSession);

    expect(await screen.findByText('Existing ATOM request')).toBeTruthy();
    expect(list).toHaveBeenCalledWith('ATOM', expect.objectContaining({ page: 0, size: 10 }));

    fireEvent.change(screen.getByPlaceholderText('e.g. Synthesis Optimization v2'), { target: { value: 'New ATOM request' } });
    fireEvent.change(screen.getByPlaceholderText('Detailed objectives, constraints, or formulation notes...'), { target: { value: 'A description' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create ATOM request' }));

    await waitFor(() => expect(create).toHaveBeenCalledWith('ATOM', 'New ATOM request', 'A description'));
    expect(await screen.findByText('Existing ATOM request')).toBeTruthy();
  });

  it('shows comments and submits a status transition on a request', async () => {
    const comment = {
      id: 1,
      authorId: user.id,
      authorUsername: user.username,
      body: 'Please review',
      createdAt: '2026-01-01T00:00:00Z',
    };
    const history = {
      id: 1,
      requestType: 'CPSR',
      requestId: request.id,
      actorId: user.id,
      actorUsername: user.username,
      fromStatus: null,
      toStatus: 'DRAFT',
      note: null,
      createdAt: '2026-01-01T00:00:00Z',
    };
    vi.spyOn(requestsApi, 'get').mockResolvedValue(request);
    vi.spyOn(requestsApi, 'comments').mockResolvedValue([comment]);
    vi.spyOn(requestsApi, 'history').mockResolvedValue([history]);
    const transition = vi.spyOn(requestsApi, 'transition').mockResolvedValue({
      ...request,
      status: 'SUBMITTED',
    });
    renderWithAuth(<RequestDetail type="CPSR" id={request.id} />, userSession);

    expect(await screen.findByText('Please review')).toBeTruthy();
    expect(screen.getByText('NEW → DRAFT')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => expect(transition).toHaveBeenCalledWith('CPSR', request.id, 'submit', ''));
  });

  it('filters requests by search and status pill', async () => {
    const list = vi.spyOn(requestsApi, 'list').mockResolvedValue(page([]));
    renderWithAuth(<RequestList type="ATOM" />, userSession);

    expect(await screen.findByText('All statuses')).toBeTruthy();
    const draftBtn = screen.getByRole('button', { name: 'DRAFT' });
    fireEvent.click(draftBtn);
    await waitFor(() => expect(list).toHaveBeenLastCalledWith('ATOM', expect.objectContaining({ status: 'DRAFT' })));

    const searchInput = screen.getByPlaceholderText('Search ATOM requests...');
    fireEvent.change(searchInput, { target: { value: 'query' } });
    await waitFor(() => expect(list).toHaveBeenLastCalledWith('ATOM', expect.objectContaining({ search: 'query' })));
  });
});
