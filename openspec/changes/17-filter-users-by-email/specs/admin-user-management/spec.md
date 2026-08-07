## Purpose

Describes how an administrator views and locates the platform's user accounts
(agents and administrators) from the admin console, so that an admin acting on
a specific account can find it without scanning the entire user list.

## ADDED Requirements

### Requirement: Filter the user list by email

The admin user list SHALL accept an optional email filter term. When a term is
supplied, the list SHALL contain exactly those user accounts whose email
address contains the term, compared case-insensitively, and no others. When no
term is supplied the list SHALL be unfiltered.

The term SHALL have leading and trailing whitespace removed before matching, and
a term that is empty after trimming SHALL be treated as no filter.

#### Scenario: Full email address matches one account

- **WHEN** an administrator filters the user list by `agent@insurance.com` and an
  account with that email exists
- **THEN** the list contains that account
- **AND** the list contains no account whose email differs from `agent@insurance.com`

#### Scenario: Partial term matches every account containing it

- **WHEN** an administrator filters by `insurance.com` and several accounts have
  emails ending in `@insurance.com`
- **THEN** the list contains all of those accounts
- **AND** the list contains no account whose email does not contain `insurance.com`

#### Scenario: Matching ignores case

- **WHEN** an administrator filters by `AGENT@Insurance.COM` and an account with
  email `agent@insurance.com` exists
- **THEN** the list contains that account

#### Scenario: Surrounding whitespace is ignored

- **WHEN** an administrator filters by `  agent@insurance.com  `
- **THEN** the list contains the same accounts as filtering by `agent@insurance.com`

#### Scenario: Clearing the filter restores the full list

- **WHEN** an administrator clears a previously applied email filter
- **THEN** the list contains every user account, exactly as before any filter was applied

#### Scenario: A term matching nothing yields an explicit empty result

- **WHEN** an administrator filters by an email term that no account's email contains
- **THEN** the list contains no user accounts
- **AND** the screen states that no users match the entered email, rather than
  showing an unexplained empty table

#### Scenario: The filter covers every account, not just the displayed rows

- **WHEN** an administrator filters by a term that matches an account which was
  not visible on screen before the filter was applied
- **THEN** the list contains that account

### Requirement: Filtering does not widen access to user data

Retrieving the user list SHALL remain restricted to administrators whether or
not an email filter is supplied. A filtered request from a caller who is not an
administrator SHALL be rejected and SHALL disclose nothing about whether any
account matches the term.

#### Scenario: An agent cannot read the user list by filtering it

- **WHEN** a caller authenticated as an AGENT requests the user list with an
  email filter
- **THEN** the request is rejected as forbidden
- **AND** the response contains no user account data and no indication of whether
  the term matched an account

#### Scenario: An unauthenticated caller cannot read the user list by filtering it

- **WHEN** an unauthenticated caller requests the user list with an email filter
- **THEN** the request is rejected as unauthorized
- **AND** the response contains no user account data

### Requirement: The filter term is treated as data, never as query structure

The email filter term SHALL be applied as a bound value so that no term can
alter the meaning of the underlying query, and any term SHALL either return a
result set or a validation error — never a server error or an unhandled failure.

#### Scenario: A term containing SQL metacharacters is matched literally

- **WHEN** an administrator filters by `' OR 1=1 --`
- **THEN** the list contains only accounts whose email actually contains that
  text — in practice none
- **AND** the request completes successfully without a server error

#### Scenario: An oversized term is handled without failure

- **WHEN** an administrator filters by a term far longer than any stored email
  address
- **THEN** the list contains no user accounts
- **AND** the request completes without a server error

### Requirement: The filter term is not recorded as PII

Applying an email filter SHALL NOT write the supplied term into application
logs or the audit trail. Listing users is a read-only operation and SHALL NOT
create, modify or delete any user account or audit record.

#### Scenario: Filtering leaves no trace of the term

- **WHEN** an administrator filters the user list by an email term
- **THEN** no audit log entry is created for the filter
- **AND** no user account is created, modified or deleted
