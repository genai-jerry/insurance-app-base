# Tasks — Filter the admin user list by email

Epic: [#17](https://github.com/genai-jerry/insurance-app-base/issues/17) ·
Change folder: `openspec/changes/17-filter-users-by-email/` ·
Spec: `specs/admin-user-management/spec.md`

All four tasks land in this repository. `.factory/profile.json` records
`estate_role` as a standalone full-stack monorepo with no sibling repos, so the
ordering below is the intra-repo one the estate shape implies: the backend
contract first, its consumers after. There is no schema change in this epic (the
filter reads the existing `users.email` column), so the chain starts at the
service that owns the contract.

Each task is one PR. Every intermediate merge is releasable: task 1 adds an
**optional** query parameter, so the existing frontend keeps working untouched
between task 1 and task 3.

## Order and dependencies

```
T1 #20 (backend: filter + contract)
 ├── T2 #21 (backend tests)        ← parallel with T3 once T1 is merged
 └── T3 #22 (frontend: filter UI)
      └── T4 #23 (frontend test harness + component tests)
```

---

## T1 — Backend: accept an optional email filter on the user listing

- [ ] **Sub-issue:** [#20](https://github.com/genai-jerry/insurance-app-base/issues/20)
- [ ] **Repo:** insurance-app-base (backend)
- [ ] **Depends on:** nothing
- [ ] **Serves scenarios:** Full email address matches one account · Partial term
      matches every account containing it · Matching ignores case · Surrounding
      whitespace is ignored · Clearing the filter restores the full list · A term
      matching nothing yields an explicit empty result (server half: returns an
      empty list) · The filter covers every account, not just the displayed rows
      · A term containing SQL metacharacters is matched literally · An oversized
      term is handled without failure · The filter term is not recorded as PII ·
      Filtering leaves no trace of the term

**What:** `GET /api/admin/users` takes an optional email filter term and returns
exactly the accounts whose email contains that term, compared
case-insensitively, evaluated in the data layer over all accounts. A term that
is absent, empty, or whitespace-only means no filter. Whitespace around the term
is ignored. An over-long term returns an empty list or a validation error —
never a server error. The term must never be interpolated into a query string,
and must not be written to logs or the audit trail. The endpoint's ADMIN-only
authorization is unchanged.

**Touches:** `backend/src/main/java/com/insurance/auth/repository/UserRepository.java`,
`backend/src/main/java/com/insurance/admin/service/UserManagementService.java`,
`backend/src/main/java/com/insurance/admin/controller/UserManagementController.java`,
`backend/API_ENDPOINTS.md` (document the new query parameter on the existing
`GET /api/admin/users` row).

**Not this task:** pagination of any kind (an explicit non-goal — the endpoint
returns a full list today and continues to), escaping `%`/`_` as literals, any
filter field other than email.

---

## T2 — Backend: tests for the filter, its access control and its hostile inputs

- [ ] **Sub-issue:** [#21](https://github.com/genai-jerry/insurance-app-base/issues/21)
- [ ] **Repo:** insurance-app-base (backend)
- [ ] **Depends on:** T1 (#20)
- [ ] **Serves scenarios:** all T1 scenarios, plus An agent cannot read the user
      list by filtering it · An unauthenticated caller cannot read the user list
      by filtering it

**What:** the repo has no `backend/src/test` tree at all today
(`.factory/profile.json` → `qa_notes`), so this task establishes it and covers
the filter's behaviour: match semantics (full, partial, case, whitespace, absent
term, no match), that a term with SQL metacharacters is matched literally rather
than altering the query, that an oversized term does not produce a server error,
that an AGENT and an unauthenticated caller are refused and learn nothing about
whether the term matched, and that filtering writes no audit record.
`spring-boot-starter-test` is already a declared dependency.

**Touches:** new `backend/src/test/java/com/insurance/admin/...`.

**Not this task:** a wider backend test suite for pre-existing untested code.
`gotchas` in the profile notes the Lombok/JDK 21.0.5 compile failure — if it
appears, use the Docker build path and judge only this delta.

---

## T3 — Frontend: email filter control on `/admin/users`

- [ ] **Sub-issue:** [#22](https://github.com/genai-jerry/insurance-app-base/issues/22)
- [ ] **Repo:** insurance-app-base (frontend)
- [ ] **Depends on:** T1 (#20)
- [ ] **Serves scenarios:** Full email address matches one account · Partial term
      matches every account containing it · Matching ignores case · Surrounding
      whitespace is ignored · Clearing the filter restores the full list · A term
      matching nothing yields an explicit empty result · The filter covers every
      account, not just the displayed rows

**What:** an email filter input on the user list, sending the term to the
listing endpoint added in T1 so the filter is evaluated server-side over every
account rather than over the rendered rows. Clearing the input restores the
unfiltered list. A term matching nothing shows a message stating that no users
match the entered email, instead of a bare empty table.

**Touches:** `frontend/src/api/admin.ts` (the `getAllUsers` params),
`frontend/src/pages/admin/UserManagement.tsx`, `frontend/src/types/` if the
request shape is typed there.

**Not this task:** the page's non-functional pagination. The proposal records
that the frontend sends `page`/`size`, the backend ignores both, and the table
renders every row regardless — pre-existing, out of scope here, and the filter
must not be built as if working pagination existed. Filtering client-side over
an already-fetched page would violate the "covers every account" scenario.

---

## T4 — Frontend: test harness and component tests for the filter

- [ ] **Sub-issue:** [#23](https://github.com/genai-jerry/insurance-app-base/issues/23)
- [ ] **Repo:** insurance-app-base (frontend)
- [ ] **Depends on:** T3 (#22)
- [ ] **Serves scenarios:** Clearing the filter restores the full list · A term
      matching nothing yields an explicit empty result · Surrounding whitespace
      is ignored · The filter covers every account, not just the displayed rows
      (asserted as: the component asks the server, rather than filtering locally)

**What:** Vitest is declared but unused and the repo has no jsdom environment,
no `@testing-library/react`, and no test setup file, so this task adds the
harness and the first component tests: entering a term drives a filtered
request, clearing restores the unfiltered list, a no-match result renders the
explicit empty state, and filtering is not performed client-side.

**Touches:** `frontend/package.json`, Vitest/Vite test config and a setup file,
new colocated `frontend/src/pages/admin/UserManagement.test.tsx`.

**Not this task:** backfilling tests for other pages, or an e2e harness.

---

## Scenario coverage map

Every scenario in `specs/admin-user-management/spec.md` is covered by at least
one task.

| Requirement | Scenario | Tasks |
|---|---|---|
| Filter the user list by email | Full email address matches one account | T1, T2, T3 |
| Filter the user list by email | Partial term matches every account containing it | T1, T2, T3 |
| Filter the user list by email | Matching ignores case | T1, T2, T3 |
| Filter the user list by email | Surrounding whitespace is ignored | T1, T2, T3, T4 |
| Filter the user list by email | Clearing the filter restores the full list | T1, T2, T3, T4 |
| Filter the user list by email | A term matching nothing yields an explicit empty result | T1, T2 (empty list), T3, T4 (empty state) |
| Filter the user list by email | The filter covers every account, not just the displayed rows | T1, T2, T3, T4 |
| Filtering does not widen access to user data | An agent cannot read the user list by filtering it | T1 (authorization unchanged), T2 |
| Filtering does not widen access to user data | An unauthenticated caller cannot read the user list by filtering it | T1 (authorization unchanged), T2 |
| The filter term is treated as data, never as query structure | A term containing SQL metacharacters is matched literally | T1, T2 |
| The filter term is treated as data, never as query structure | An oversized term is handled without failure | T1, T2 |
| The filter term is not recorded as PII | Filtering leaves no trace of the term | T1, T2 |

## Planning notes

- Four tasks, well inside the ~10 limit — no split into sequential changes is
  needed.
- The spec's match semantics (case-insensitive substring) were the one open
  assumption at gate G1; the approver accepted the spec as written, so these
  tasks plan to that behaviour.
- Match semantics deliberately mirror the existing lead search in
  `LeadRepository`, which is the repo's only search-style filter today. HOW to
  express that is the Architect's call in `design.md`; this plan only fixes WHAT.
