# Filter the admin user list by email

Requirement issue: [#17](https://github.com/genai-jerry/insurance-app-base/issues/17)

## Why

`/admin/users` renders every user account in one flat table with no way to
narrow it. An administrator who knows the email of the account they need —
the normal case when responding to a support request or a role change — has
to scan the whole list by eye, and that list grows with every agent onboarded.
Email is the account's natural identifier here (it is the login credential and
is already unique per user), so filtering by it is the shortest path from
"I need this account" to "I am acting on this account".

## What Changes

- The admin user list gains an email filter: an administrator supplies an
  email (whole or partial) and the list shows only the users whose email
  matches it.
- Matching is **case-insensitive substring (contains)** matching, with
  surrounding whitespace trimmed — consistent with the existing lead search
  on `/leads`, which is the only search-style filter this product ships today
  and therefore the behaviour an admin already expects here.
- The filter is evaluated over the **complete** set of user accounts, not only
  the rows the table happens to have rendered.
- Clearing the filter restores the unfiltered list; a filter matching nothing
  shows an explicit empty state rather than a blank table.
- Access to the user list is unchanged: ADMIN only, before and after filtering.

### Assumptions recorded (raised for the approver at gate G1)

The issue says "filtering user by a supplied email" without pinning down match
semantics. This proposal assumes **partial, case-insensitive** matching rather
than exact-equality matching, because a filter box that only responds to a
perfectly typed full address is of little practical use and because it matches
the existing lead search in this product. If the intent was exact match only,
say so on the issue and the spec will be narrowed before planning.

## Capabilities

### New Capabilities
- `admin-user-management`: How administrators view, locate and manage the
  platform's user accounts (agents and admins) from the admin console. This
  change establishes the capability with its email-filter requirements; the
  existing list/create/edit/delete behaviours are not re-specified here.

### Modified Capabilities
<!-- None: openspec/specs/ is currently empty, so there is no existing
     capability whose requirements this change alters. -->

## Impact

**Affected surface**

- Backend (`backend/src/main/java/com/insurance/admin/`) — the admin user
  listing endpoint accepts an optional email filter and applies it in the data
  layer, so the filter covers all accounts rather than a client-side slice.
- Frontend (`frontend/src/pages/admin/UserManagement.tsx` and
  `frontend/src/api/admin.ts`) — a filter input on the user list, wired to the
  listing request.
- No database schema change: the filter reads the existing `users.email`
  column, so no new Flyway migration is required.
- No change to authentication, authorization, or any agent-facing surface.

**Data & privacy**

- The filter exposes no user attribute that the list does not already display.
  The listing endpoint remains `ADMIN`-only, so the filter cannot be used as an
  account-enumeration oracle by agents or anonymous callers.
- Email addresses are PII. The filter term is user-supplied PII and must not be
  written into application logs or audit records; listing users is a read-only
  operation and stays unaudited, as it is today.

**Discovered while analysing (not fixed here)**

The admin user list has non-functional pagination: the frontend sends `page`
and `size` to `GET /api/admin/users`, the backend ignores both and returns the
full list, and the table renders every row regardless of the selected page.
This is a pre-existing defect, independent of filtering. It is called out so
the design stage does not accidentally build the filter on a pagination
contract that does not exist; fixing it belongs in its own issue.

## Non-goals

- Filtering or searching by name, role, creation date, or any field other than
  email.
- Fixing the non-functional pagination described above, or introducing
  server-side pagination for the user list.
- Sorting, column selection, bulk actions, or exporting the user list.
- Treating SQL wildcard characters (`%`, `_`) typed into the filter as literal
  characters. The filter must be injection-safe (see the spec), but escaping
  wildcards so they match literally is out of scope.
- Any change to the existing lead search on `/leads`.
- Adding phone number to users, or admin-side user creation — those are
  separate requirement issues (#10, #12, #15, #16) touching the same screen.
