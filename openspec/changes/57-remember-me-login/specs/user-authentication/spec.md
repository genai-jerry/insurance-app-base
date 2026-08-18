## Purpose

Describes how a user signs in to the app and how long that sign-in lasts: the
login screen's remember-me choice, whether a session persists across browser
sessions, and the boundaries — logout and expiry — that end it.

## ADDED Requirements

### Requirement: The login screen offers a remember-me choice

The login screen SHALL present a "Remember me" checkbox alongside the email and
password fields. The checkbox SHALL be unchecked by default, and its state
SHALL be carried with the sign-in request when the user submits the form.

The login API SHALL accept the remember-me choice as an optional flag with the
credentials. A sign-in request that omits the flag SHALL be treated exactly as
"not remembered", so existing callers of the login endpoint are unaffected.

#### Scenario: The checkbox is present and defaults to unchecked

- **WHEN** a user opens the login screen
- **THEN** a "Remember me" checkbox is visible with the credential fields
- **AND** it is unchecked

#### Scenario: A login request without the flag is not remembered

- **WHEN** a client calls the login API with valid credentials and no
  remember-me flag
- **THEN** the sign-in succeeds
- **AND** the resulting session behaves as a non-remembered session

#### Scenario: The choice does not affect credential checking

- **WHEN** a user submits invalid credentials with the checkbox checked
- **THEN** the sign-in fails exactly as it would unchecked
- **AND** no session is created and nothing is persisted

### Requirement: A remembered session persists across browser sessions

When a user signs in with "Remember me" checked, the session SHALL persist
across browser restarts: returning to the app within the remember-me validity
window SHALL sign the user in automatically, without credential entry, landing
them where a freshly signed-in user of their role lands.

The remember-me validity window SHALL be configurable via application
configuration, with a default of 30 days. Automatic sign-in SHALL rely only on
a revocable, expiring session credential — never on the password (see the
password-persistence requirement).

#### Scenario: Returning after a browser restart, still within the window

- **WHEN** a user signed in with "Remember me" checked, closed the browser, and
  returns to the app within the remember-me validity window
- **THEN** they reach the app signed in as themselves, without entering
  credentials
- **AND** an ADMIN lands on the admin dashboard and an AGENT on the agent
  dashboard, as after a fresh sign-in

#### Scenario: The remembered session eventually expires

- **WHEN** a user signed in with "Remember me" checked and returns after the
  remember-me validity window has elapsed
- **THEN** they are presented with the login screen and must sign in again
- **AND** no error beyond the normal prompt to sign in is shown

#### Scenario: The window is configuration, not code

- **WHEN** an operator changes the configured remember-me validity duration and
  restarts the backend
- **THEN** sessions remembered from then on use the new duration
- **AND** no code change is required

### Requirement: A non-remembered session does not outlive the browser session

When a user signs in with "Remember me" unchecked (or a client omits the flag),
the session SHALL last at most the browser session: after the browser is closed
and reopened, the user SHALL be required to sign in again, even if the session
credential would otherwise still be valid.

Within the same browser session, behaviour SHALL be unchanged from today: the
user stays signed in across page reloads and new tabs, up to the standard
(non-remembered) credential validity, which this change does not alter.

#### Scenario: Closing the browser ends a non-remembered session

- **WHEN** a user signed in with the checkbox unchecked, closed the browser,
  and returns to the app
- **THEN** they are presented with the login screen and must sign in again

#### Scenario: A page reload does not end a non-remembered session

- **WHEN** a user signed in with the checkbox unchecked reloads the app in the
  same browser session
- **THEN** they remain signed in without re-entering credentials

### Requirement: Logout ends the session regardless of the remember-me choice

Logging out SHALL end the session and remove every persisted session credential
for that browser, whether or not the session was remembered. A return visit
after logout SHALL always require credentials.

#### Scenario: Logout forgets a remembered session

- **WHEN** a user signed in with "Remember me" checked logs out, then returns
  to the app — before or after a browser restart
- **THEN** they are presented with the login screen and must sign in again

### Requirement: The password is never persisted

The system SHALL NOT store the user's password — or any value from which it can
be recovered — in browser storage, cookies, or anywhere client-side, regardless
of the remember-me choice. Automatic sign-in SHALL be achieved only via a
revocable, expiring session credential issued by the backend.

A stored session credential that is invalid, expired, or rejected by the
backend SHALL be treated as being signed out: the credential is discarded and
the user is presented with the login screen, with no error beyond the normal
prompt.

#### Scenario: Browser storage holds no password after a remembered sign-in

- **WHEN** a user signs in with "Remember me" checked
- **THEN** nothing stored client-side contains the password or any value from
  which it can be recovered

#### Scenario: A rejected stored credential falls back to the login screen

- **WHEN** a user returns to the app and their stored session credential is
  rejected by the backend (invalid or expired)
- **THEN** the stored credential is discarded
- **AND** they are presented with the login screen and can sign in normally
