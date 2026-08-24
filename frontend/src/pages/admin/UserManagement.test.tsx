import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { UserManagement } from './UserManagement';
import { adminApi } from '../../api/admin';
import { User } from '../../types';

vi.mock('../../api/admin', () => ({
  adminApi: {
    getAllUsers: vi.fn(),
    updateUser: vi.fn(),
    deleteUser: vi.fn(),
    createUser: vi.fn(),
  },
}));

const getAllUsers = vi.mocked(adminApi.getAllUsers);
const createUser = vi.mocked(adminApi.createUser);

const ADMIN: User = {
  id: 1,
  name: 'Admin User',
  email: 'admin@insurance.com',
  role: 'ADMIN',
  createdAt: '2026-01-01T00:00:00',
};

const AGENT: User = {
  id: 2,
  name: 'Agent User',
  email: 'agent@insurance.com',
  role: 'AGENT',
  createdAt: '2026-01-02T00:00:00',
};

const OUTSIDER: User = {
  id: 3,
  name: 'Someone Else',
  email: 'someone@other.example',
  role: 'AGENT',
  createdAt: '2026-01-03T00:00:00',
};

const ALL_USERS = [ADMIN, AGENT, OUTSIDER];

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <UserManagement />
    </QueryClientProvider>
  );
};

const filterInput = () => screen.getByLabelText('Filter by email');

/**
 * Every keystroke starts a request, and each one settles between the
 * keystrokes user-event awaits internally. Without this outer act() those
 * resolutions land outside an act scope and React floods stderr with
 * "not wrapped in act" warnings even though the assertions pass.
 */
const typeFilter = (user: UserEvent, text: string) =>
  act(async () => {
    await user.type(filterInput(), text);
  });

const clearFilter = (user: UserEvent) =>
  act(async () => {
    await user.clear(filterInput());
  });

/** The params of the most recent listing request. */
const lastCallParams = () =>
  getAllUsers.mock.calls[getAllUsers.mock.calls.length - 1][0];

describe('UserManagement email filter', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAllUsers.mockResolvedValue(ALL_USERS);
  });

  it('sends the term to the server rather than filtering the fetched rows', async () => {
    // The scenario "the filter covers every account, not just the displayed
    // rows" is only satisfied if the term reaches the listing endpoint. This
    // is the assertion that fails the moment someone reimplements the filter
    // as a client-side Array.filter over the rows already fetched.
    const user = userEvent.setup();
    getAllUsers.mockResolvedValue([ADMIN, AGENT]);
    renderPage();

    await screen.findByText('admin@insurance.com');
    await typeFilter(user, 'insurance.com');

    await waitFor(() =>
      expect(getAllUsers).toHaveBeenLastCalledWith(
        expect.objectContaining({ email: 'insurance.com' })
      )
    );

    // ...and the rows on screen are exactly what the server returned, in the
    // order it returned them — the component filtered nothing itself.
    const rows = await screen.findAllByRole('row');
    const emails = rows
      .slice(1)
      .map((row) => within(row).getAllByRole('cell')[2].textContent);
    expect(emails).toEqual(['admin@insurance.com', 'agent@insurance.com']);
  });

  it('ignores whitespace surrounding the term', async () => {
    const user = userEvent.setup();
    getAllUsers.mockResolvedValue([AGENT]);
    renderPage();

    await screen.findByText('agent@insurance.com');
    await typeFilter(user, '  agent@insurance.com  ');

    // The field keeps what was typed; the request carries the trimmed term.
    expect(filterInput()).toHaveValue('  agent@insurance.com  ');
    await waitFor(() =>
      expect(getAllUsers).toHaveBeenLastCalledWith(
        expect.objectContaining({ email: 'agent@insurance.com' })
      )
    );
  });

  it('issues no filtered request for a whitespace-only term', async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText('admin@insurance.com');
    await typeFilter(user, '   ');

    await waitFor(() => expect(filterInput()).toHaveValue('   '));
    expect(
      getAllUsers.mock.calls.every(([params]) => params?.email === undefined)
    ).toBe(true);
  });

  it('restores the full list when the filter is cleared', async () => {
    const user = userEvent.setup();
    getAllUsers.mockImplementation(async (params) =>
      params?.email ? [AGENT] : ALL_USERS
    );
    renderPage();

    await screen.findByText('someone@other.example');
    await typeFilter(user, 'agent');

    await waitFor(() =>
      expect(screen.queryByText('someone@other.example')).not.toBeInTheDocument()
    );

    await clearFilter(user);

    // No `email` key at all — an empty string would be a filter on "".
    await waitFor(() => expect(lastCallParams()).not.toHaveProperty('email', ''));
    expect(lastCallParams()?.email).toBeUndefined();
    expect(await screen.findByText('someone@other.example')).toBeInTheDocument();
    expect(screen.getByText('admin@insurance.com')).toBeInTheDocument();
    expect(screen.getByText('agent@insurance.com')).toBeInTheDocument();
  });

  it('renders an explicit empty state when a filtered search matches nothing', async () => {
    const user = userEvent.setup();
    getAllUsers.mockImplementation(async (params) =>
      params?.email ? [] : ALL_USERS
    );
    renderPage();

    await screen.findByText('admin@insurance.com');
    await typeFilter(user, 'nobody@nowhere.example');

    expect(
      await screen.findByText('No users match the entered email.')
    ).toBeInTheDocument();
    expect(screen.queryByText('No users found.')).not.toBeInTheDocument();
  });

  it('keeps the filter field mounted and focused while requests are in flight', async () => {
    // A loading branch that replaced the whole page would unmount the field
    // after the first keystroke and drop keyboard focus.
    const user = userEvent.setup();
    renderPage();

    await screen.findByText('admin@insurance.com');

    await act(async () => {
      await user.click(filterInput());
      await user.keyboard('adm');
    });

    expect(filterInput()).toHaveFocus();
    expect(filterInput()).toHaveValue('adm');
  });
});

describe('UserManagement add user', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAllUsers.mockResolvedValue(ALL_USERS);
  });

  it('opens a create dialog and submits the new user to the server', async () => {
    const user = userEvent.setup();
    createUser.mockResolvedValue({
      id: 4,
      name: 'New Person',
      email: 'new.person@insurance.com',
      role: 'AGENT',
    });
    renderPage();

    await screen.findByText('admin@insurance.com');
    await act(async () => {
      await user.click(screen.getByRole('button', { name: 'Add User' }));
    });

    const dialog = await screen.findByRole('dialog');
    await act(async () => {
      await user.type(within(dialog).getByLabelText('Name'), 'New Person');
      await user.type(
        within(dialog).getByLabelText('Email'),
        'new.person@insurance.com'
      );
      await user.type(within(dialog).getByLabelText('Password'), 'Secret@123');
      await user.click(within(dialog).getByRole('button', { name: 'Create' }));
    });

    await waitFor(() =>
      expect(createUser).toHaveBeenCalledWith(
        {
          name: 'New Person',
          email: 'new.person@insurance.com',
          password: 'Secret@123',
          role: 'AGENT',
        },
        expect.anything()
      )
    );
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    );
  });

  it('requires name, email and password before submitting', async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText('admin@insurance.com');
    await act(async () => {
      await user.click(screen.getByRole('button', { name: 'Add User' }));
    });

    const dialog = await screen.findByRole('dialog');
    await act(async () => {
      await user.click(within(dialog).getByRole('button', { name: 'Create' }));
    });

    expect(await within(dialog).findByText('Name is required')).toBeInTheDocument();
    expect(within(dialog).getByText('Email is required')).toBeInTheDocument();
    expect(within(dialog).getByText('Password is required')).toBeInTheDocument();
    expect(createUser).not.toHaveBeenCalled();
  });
});
