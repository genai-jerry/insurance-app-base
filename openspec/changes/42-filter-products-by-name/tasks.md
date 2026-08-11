# Tasks — Filter the manage-products list by product name

Epic: [#42](https://github.com/genai-jerry/insurance-app-base/issues/42) ·
Change folder: `openspec/changes/42-filter-products-by-name/` ·
Spec: `specs/admin-product-management/spec.md`

All four tasks land in this repository. `.factory/profile.json` records
`estate_role` as a standalone full-stack monorepo with no sibling repos, so the
ordering below is the intra-repo one the estate shape implies: the backend
contract first, its consumers after. There is no schema change in this epic (the
filter reads the existing `products.name` column, per the proposal), so the
chain starts at the service that owns the listing contract.

Each task is one PR. Every intermediate merge is releasable: task 1 adds an
**optional** name filter to the listing, so the existing frontend, the
agent-facing product browser and every other caller keep working untouched
between task 1 and task 3.

This epic is the product-list twin of the shipped user-email filter
(epic #17). One consequence for the test tasks: #17 already established both
the backend test tree (`backend/src/test/...`) and the frontend Vitest +
React Testing Library harness (`UserManagement.test.tsx`), so T2 and T4 here
**extend** existing harnesses rather than bootstrap them.

## Order and dependencies

```
T1 #46 (backend: name filter + contract)
 ├── T2 #47 (backend tests)        ← parallel with T3 once T1 is merged
 └── T3 #48 (frontend: filter UI)
      └── T4 #49 (frontend component tests)
```

---

## T1 — Backend: accept an optional name filter on the product listing

- [x] **Sub-issue:** [#46](https://github.com/genai-jerry/insurance-app-base/issues/46)
- [x] **Repo:** insurance-app-base (backend)
- [x] **Depends on:** nothing
- [x] **Serves scenarios:** Full product name matches the product · Partial term
      matches every product containing it · Matching ignores case · Surrounding
      whitespace is ignored · Several products may share a matching name
      (including the stable, repeatable order) · Clearing the filter restores
      the full list (server half: no term ⇒ unfiltered list) · A term matching
      nothing yields an explicit empty result (server half: returns an empty
      list) · The filter covers the whole catalogue, not just the displayed rows
      · Only the name is matched · Name and category narrow together · Existing
      callers are unaffected · The agent product browser is unchanged · An
      unauthenticated caller cannot read the catalogue by filtering it · An
      authenticated agent may still list products · Filtering grants no write
      access · A term containing SQL metacharacters is matched literally · An
      oversized term is handled without failure · A term of only whitespace is
      not an error

**What:** the product listing consumed by the admin product screen accepts an
optional product-name filter term and returns exactly the products whose name
contains the term, compared case-insensitively, evaluated in the data layer
over the whole catalogue, in a stable deterministic order. An absent, empty, or
whitespace-only term means no filter; surrounding whitespace is trimmed. The
name term composes with the listing's existing optional `categoryId`, `insurer`
and `planType` filters — every supplied filter narrows — and a request with no
name term returns exactly what it returns today. The term is applied as a bound
value (never interpolated into query structure); an oversized term yields an
empty list or a validation error, never a server error. Authorization is
unchanged: any authenticated caller may list, unauthenticated callers are
rejected without learning whether the term matched.

The proposal's "Discovered while analysing" section flags that the current
`GET /api/products` filter combination is all-or-nothing and that
`GET /api/products/search` matches more than name. Which surface carries the
name filter, and how composition is restructured, is a `design.md` decision —
this task implements whatever that document settles, keeping both scenarios
"Existing callers are unaffected" and "The agent product browser is unchanged"
true.

**Touches:** `backend/src/main/java/com/insurance/products/repository/ProductRepository.java`,
`backend/src/main/java/com/insurance/products/service/ProductService.java`,
`backend/src/main/java/com/insurance/products/controller/ProductController.java`,
`backend/API_ENDPOINTS.md` (document the new query parameter on the affected
endpoint row).

**Not this task:** pagination (an explicit non-goal; the listing returns a full
list today and continues to), escaping `%`/`_` so they match literally,
filtering by insurer/plan type/tags/category beyond composing with the existing
parameters, any behaviour change to `GET /api/products/search`, the category
list filter (issue #41), or any frontend change.

---

## T2 — Backend: tests for the name filter, its composition and its hostile inputs

- [ ] **Sub-issue:** [#47](https://github.com/genai-jerry/insurance-app-base/issues/47)
- [ ] **Repo:** insurance-app-base (backend)
- [ ] **Depends on:** T1 (#46)
- [ ] **Serves scenarios:** all T1 scenarios, verified — in particular the
      composition pair (Name and category narrow together · Existing callers
      are unaffected), the access-control trio (An unauthenticated caller
      cannot read the catalogue by filtering it · An authenticated agent may
      still list products · Filtering grants no write access) and the
      term-as-data trio (SQL metacharacters matched literally · Oversized term
      handled without failure · Whitespace-only term is not an error)

**What:** backend tests covering the filter's behaviour: match semantics (full
name, partial, case-insensitive, whitespace-trimmed, absent/empty/whitespace
term ⇒ unfiltered, no match ⇒ empty list, multiple matches all returned in a
stable order, a term matching only insurer/plan type/tags returns nothing),
composition with the existing category/insurer/plan-type filters, that a
no-name-term request returns exactly the pre-change result, that a term with
SQL metacharacters is matched literally rather than altering the query, that an
oversized term does not produce a server error, that an unauthenticated caller
is refused and learns nothing about whether the term matched, that an
AGENT-role caller can still list, and that filtering performs no write. The
backend test tree exists (established by epic #17's #21) —
follow its patterns and add alongside.

**Touches:** new test classes under
`backend/src/test/java/com/insurance/products/...`.

**Not this task:** a wider backend suite for pre-existing untested product
code. `gotchas` in the profile notes the Lombok/JDK 21.0.5 compile failure — if
it appears, use the Docker build path and judge only this delta.

---

## T3 — Frontend: product-name filter control on `/admin/products`

- [ ] **Sub-issue:** [#48](https://github.com/genai-jerry/insurance-app-base/issues/48)
- [ ] **Repo:** insurance-app-base (frontend)
- [ ] **Depends on:** T1 (#46)
- [ ] **Serves scenarios:** Full product name matches the product · Partial term
      matches every product containing it · Matching ignores case · Surrounding
      whitespace is ignored · Several products may share a matching name ·
      Clearing the filter restores the full list · A term matching nothing
      yields an explicit empty result (UI half: the screen states that no
      products match the entered name) · The filter covers the whole catalogue,
      not just the displayed rows · The agent product browser is unchanged (by
      not touching it)

**What:** a name filter input on the Manage Products page, following the same
standards as the shipped user email filter on `/admin/users`: always-visible
text box above the table, term sent to the listing contract added in T1 so
matching happens server-side over the whole catalogue rather than over the
rendered rows. Clearing the input restores the unfiltered list. A term matching
nothing shows a message stating that no products match the entered name,
instead of a bare empty table. The agent-facing product browser and the
category management screen are not touched.

**Touches:** `frontend/src/api/products.ts` (listing call params),
`frontend/src/pages/admin/ProductManagement.tsx`, `frontend/src/types/` if the
request shape is typed there.

**Not this task:** pagination (the proposal records that `productsApi.getAll`
sends a `size` the backend ignores — pre-existing, out of scope, and the filter
must not be built as if working pagination existed; filtering client-side over
an already-fetched slice would violate the whole-catalogue scenario), any
change to `ProductBrowser` or `CategoryManagement`, filter fields other than
name.

---

## T4 — Frontend: component tests for the product-name filter

- [ ] **Sub-issue:** [#49](https://github.com/genai-jerry/insurance-app-base/issues/49)
- [ ] **Repo:** insurance-app-base (frontend)
- [ ] **Depends on:** T3 (#48)
- [ ] **Serves scenarios:** the T3 scenarios, verified at the component level —
      filtering narrows the table, clearing restores it, the no-match message
      names the entered term's situation explicitly, and the term is passed to
      the listing call (whole-catalogue behaviour) rather than filtered locally

**What:** Vitest + React Testing Library component tests for the Manage
Products filter, colocated with the page, following the harness and patterns
established by epic #17's `UserManagement.test.tsx`: entering a term issues a
listing request carrying that term, the rows rendered are the rows returned,
clearing the input re-requests the unfiltered list, and a no-match response
renders the explicit empty-state message. API calls mocked; no live backend.

**Touches:** new `frontend/src/pages/admin/ProductManagement.test.tsx` (plus
any small test-util reuse).

**Not this task:** e2e/browser tests, tests for unrelated pre-existing
behaviour of the page, backend tests (T2).

---

## Scenario coverage map

Every spec scenario is served by at least one implementing task and verified by
at least one test task:

| Spec scenario | Implemented | Verified |
|---|---|---|
| Full product name matches the product | T1, T3 | T2, T4 |
| Partial term matches every product containing it | T1, T3 | T2, T4 |
| Matching ignores case | T1 | T2 |
| Surrounding whitespace is ignored | T1 | T2 |
| Several products may share a matching name (stable order) | T1 | T2 |
| Clearing the filter restores the full list | T1, T3 | T2, T4 |
| A term matching nothing yields an explicit empty result | T1, T3 | T2, T4 |
| The filter covers the whole catalogue | T1, T3 | T2, T4 |
| Only the name is matched | T1 | T2 |
| Name and category narrow together | T1 | T2 |
| Existing callers are unaffected | T1 | T2 |
| The agent product browser is unchanged | T1, T3 (no touch) | T2 |
| Unauthenticated caller cannot read by filtering | T1 | T2 |
| An authenticated agent may still list products | T1 | T2 |
| Filtering grants no write access | T1 | T2 |
| SQL metacharacters matched literally | T1 | T2 |
| Oversized term handled without failure | T1 | T2 |
| Whitespace-only term is not an error | T1 | T2 |
