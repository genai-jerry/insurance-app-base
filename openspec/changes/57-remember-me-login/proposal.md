# Remember-me sign-in on the login screen

Requirement issue: [#57](https://github.com/genai-jerry/insurance-app-base/issues/57)

## Why

The login screen offers no say in how long a sign-in lasts. The requirement
asks for a checkbox that lets a user choose to be remembered, so that on their
next visit they reach the app without typing credentials again.

Today that choice is made for everyone, silently, in one direction: the JWT is
written to `localStorage` unconditionally (`frontend/src/store/authStore.ts`)
and is valid for 24 hours (`app.jwt.expiration-ms`, `application.yml`), so
*every* user is auto-logged-in across browser restarts for up to a day, and
*no* user can stay signed in longer than that. A remember-me checkbox gives the
user the choice in both directions: checked, the session survives browser
restarts for an extended period; unchecked, it ends with the browser session.

The issue is titled "remember password". Storing or replaying the actual
password is a credential-handling anti-pattern and is **not** what this change
specifies; the stated desired outcome — "login automatically" on the next
visit — is the industry-standard *remember me* feature, realised by persisting
a revocable session credential (a token), never the password itself. This
reading is recorded as assumption 1 below for the gate-G1 approver.

## What Changes

- The login screen gains a **"Remember me" checkbox**, unchecked by default.
- The login API accepts an optional remember-me flag alongside email and
  password. A request without the flag behaves as "not remembered", so existing
  callers are unaffected at the contract level.
- **Checked:** the session persists across browser restarts. Returning to the
  app within the remember-me validity window signs the user in automatically —
  straight to their role's landing page, no credential entry. The window is
  configurable, defaulting to 30 days (assumption 3).
- **Unchecked:** the session does not outlive the browser session. Closing the
  browser and returning requires signing in again, even if the token would
  still be valid. Within one browser session, behaviour is as today (tab
  reloads and new tabs stay signed in, up to the standard token validity).
- Logging out ends the session either way — a return visit after logout always
  requires credentials.
- An expired or invalid remembered credential is treated as signed out: the
  user lands on the login screen, with no error other than the normal prompt.

Note the unchecked path is an observable **behaviour change**: a user who signs
in without checking the box no longer survives a browser restart, where today
everyone does (within 24h). That tightening is the point of the checkbox — see
assumption 2.

### Assumptions recorded (raised for the approver at gate G1)

1. **"Remember password" means "remember me", not password storage.** The
   desired outcome in the issue is automatic login on the next visit; that is
   delivered by a persistent session token. The password itself is never stored
   client-side or replayed — that is a non-goal on security grounds. ("Next
   installation" in the issue is read as "next visit/browser session".)
2. **Unchecked means session-only.** For the checkbox to mean anything, not
   checking it must stop the current always-persist behaviour: an unchecked
   sign-in ends when the browser closes. Users who want the old
   survive-a-restart behaviour tick the box.
3. **Remember-me window: 30 days, configurable.** The issue names no duration.
   30 days is a conventional default; it ships as configuration (like the
   existing `app.jwt.expiration-ms`) so it can be tuned without a code change.

If any of these readings is wrong, say so on the issue and the spec will be
corrected before planning.

## Capabilities

### New Capabilities

- `user-authentication`: How a user signs in to the app and how long that
  sign-in lasts — the login screen's remember-me choice, session persistence
  across browser sessions, and the boundaries (logout, expiry) that end a
  session. Existing login/registration/password-reset behaviour is not
  re-specified here; only the remember-me requirements are added.

### Modified Capabilities

<!-- None: openspec/specs/ is currently empty, so there is no existing
     capability whose requirements this change alters. -->

## Impact

**Affected surface**

- Frontend — `frontend/src/pages/auth/Login.tsx` (checkbox),
  `frontend/src/store/authStore.ts` (where and how the token is kept:
  persistent vs session-scoped storage), `frontend/src/api/auth.ts` and
  `frontend/src/types/` (login request shape).
- Backend — `backend/src/main/java/com/insurance/auth/` (`LoginRequest` gains
  the optional flag; `AuthService`/`JwtUtil` issue a token whose validity
  depends on it; `application.yml` gains the remember-me duration setting).
- **No database schema change expected.** The issue's template guessed
  "Database" affected, but the app's sessions are stateless JWTs — a
  remember-me realised as a longer-lived token needs no table. Only if the
  design stage instead chooses server-side persistent tokens (e.g. revocable
  rotating tokens) would a Flyway migration appear; that choice is the design
  stage's, and the spec is written mechanism-neutral.
- No change to RBAC, registration, password reset, or any post-login screen.

**Data & privacy**

- The persisted credential is authentication material: it must be a revocable,
  expiring token — never the password, and never anything from which the
  password can be recovered.
- A longer-lived token in browser storage extends the window in which a stolen
  token is useful (the app already accepts `localStorage` exposure for 24h
  tokens today). The design stage should weigh storage location and lifetime;
  the spec fixes only the observable behaviour.
- No new personal data is collected; the remember-me flag itself is not stored
  server-side in this change.

**Discovered while analysing (not fixed here)**

- `authStore.logout()` clears the token client-side only; a JWT remains valid
  server-side until expiry, and there is no revocation list. Remember-me
  lengthens that window after logout. Pre-existing property of the stateless
  JWT design — called out so the design stage weighs it rather than inherits it
  silently.
- Closed issue #3 asked for this same feature; its agent run errored and it was
  closed with nothing implemented. This change supersedes it.

## Non-goals

- Storing, pre-filling, or replaying the user's actual password, client- or
  server-side. Browser/password-manager autofill is the browser's business and
  is neither enabled nor blocked here.
- Server-side session revocation, refresh-token rotation, or a token denylist
  (unless the design stage chooses persistent tokens as its mechanism).
- Changing the standard (non-remembered) token validity, the logout flow,
  registration, or password reset.
- "Keep me signed in forever" — remembered sessions still expire.
