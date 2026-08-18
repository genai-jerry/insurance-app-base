import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { Login } from './Login';

vi.mock('../../store/authStore', () => ({
  useAuthStore: Object.assign(
    vi.fn(() => ({ login: vi.fn() })),
    { getState: vi.fn(() => ({ user: null })) }
  ),
}));

describe('Login', () => {
  it('marks the email and password fields as required', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    expect(screen.getByLabelText(/email/i)).toBeRequired();
    expect(screen.getByLabelText(/passkey/i)).toBeRequired();
  });

  it('labels the identifier field as Email / User Name', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    expect(screen.getByLabelText('Email / User Name *')).toBeInTheDocument();
  });

  it('labels the password field as Passkey', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

    expect(screen.getByLabelText('Passkey *')).toBeInTheDocument();
  });
});
