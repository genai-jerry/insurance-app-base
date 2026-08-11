# Design — Filter the manage-products list by product name

Epic: [#42](https://github.com/genai-jerry/insurance-app-base/issues/42) ·
Change folder: `openspec/changes/42-filter-products-by-name/` ·
Spec: `specs/admin-product-management/spec.md` · Tasks: `tasks.md`

Single-repo epic (`.factory/profile.json` `estate_role`: standalone full-stack
monorepo). There is no sibling repo, so the API contract below is the whole
contract — no cross-repo snippet to keep in sync.

This change is the product-list twin of the shipped user email filter
(epic #17, `openspec/changes/17-filter-users-by-email/design.md`). Where a
decision here matches #17, that is deliberate — the issue asks for "similar
standards as the filtering supported in User Management page" — and where it
diverges, the divergence is stated and justified.

## 1. Reuse survey — what already exists

| Asset | Where | How this change uses it |
|---|---|---|
| Listing endpoint with optional filters | `ProductController.getProducts` (`GET /api/products`, `backend/.../products/controller/ProductController.java:28-47`) | **Extended** with an optional `name` param — this is the surface the admin page already calls |
| Composable filter query | `ProductRepository.findByFilters` (`backend/.../products/repository/ProductRepository.java:27-33`) | **Extended** with one more `(:name IS NULL OR …)` predicate + `ORDER BY` — same null-means-skip idiom it already uses |
| Filter service path | `ProductService.filterProducts` (`backend/.../products/service/ProductService.java:60-66`) | **Extended** to normalise the term (trim, blank→null, oversized→empty) before binding |
| Term-normalisation pattern | `UserManagementService.getAllUsers` (`backend/.../admin/service/UserManagementService.java:34-53`) | Copied: trim → blank means no filter → oversized short-circuits to empty |
| Stable-order repository idiom | `UserRepository.findByEmailContainingIgnoreCaseOrderByIdAsc` | Copied as `ORDER BY p.id ASC` on the filtered query |
| Filter UI pattern | `frontend/src/pages/admin/UserManagement.tsx:36-118` | Copied: controlled `TextField` above the table, trimmed term in the query key, unconditional render so the input never unmounts, explicit no-match row |
| Frontend API client | `productsApi.getAll` (`frontend/src/api/products.ts:10-18`) | **Extended** with `name?: string` in its params object |
| Backend test harness | `backend/src/test/java/com/insurance/{auth,admin}/…EmailFilterTest.java` (from #17) | Mirrored: Testcontainers `pgvector/pg16` repository test, Mockito service test, `@WebMvcTest` + `SecurityConfig` controller test |
| Frontend test harness | `frontend/src/pages/admin/UserManagement.test.tsx` + `frontend/src/test/setup.ts` | Mirrored: Vitest + RTL, `vi.mock` of the API module, same render/act helpers |

Nothing new is invented: every piece is an extension of a named existing file.

### What is deliberately NOT reused

**`GET /api/products/search?q=` does not carry this filter.** Its
`searchProducts` JPQL matches name **or insurer or plan type** by substring
(`ProductRepository.java:21-25`). Routing the admin filter through it would
violate the spec scenario *Only the name is matched* (a term appearing in an
insurer's name would return that insurer's products), and narrowing the
endpoint's matching would change behaviour for its existing callers — an
explicit non-goal. The search endpoint is untouched.

## 2. API contract

One endpoint changes, backward-compatibly. No other endpoint, DTO, status code
or auth rule changes.

```
GET /api/products
Auth:   any authenticated user (JWT) — unchanged; unauthenticated → 401/403
        with no body content revealing match information (SecurityConfig
        anyRequest().authenticated(), unchanged)

Query parameters (all optional, all composable — every supplied one narrows):
  categoryId : long    (existing) equality on category
  insurer    : string  (existing) case-insensitive equality
  planType   : string  (existing) case-insensitive equality
  name       : string  (NEW) case-insensitive SUBSTRING match on product name;
                       trimmed before matching; absent / empty / whitespace-only
                       ⇒ no name filter

Response: 200, JSON array of ProductDto (unchanged shape):
  [{ id, categoryId, categoryName, name, insurer, planType,
     detailsJson, eligibilityJson, tags, createdAt, updatedAt }]

Semantics:
  - name matches by contains, case-insensitive; % and _ are treated as SQL LIKE
    metacharacters bound as data (injection-safe); escaping them to match
    literally is out of scope per the proposal's non-goals
  - a name term longer than 255 chars (products.name is VARCHAR(255),
    V3__create_products_tables.sql) returns 200 [] — same answer the query
    would give, without the round trip (mirrors #17)
  - when a name term is applied, results are ordered by id ascending (stable,
    repeatable); requests without a name term return exactly today's result
  - no request with a name term performs any write
```

**Not a breaking change:** the parameter is optional and every existing request
returns what it returns today (see §3.3 for the one controller branch that
guarantees this).

## 3. Backend design (T1)

### 3.1 Repository — `ProductRepository`

Extend the existing `findByFilters` JPQL — same `(:param IS NULL OR …)` idiom
already proven in this exact query — with a name predicate and a deterministic
order:

```java
@Query("SELECT p FROM Product p WHERE " +
       "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
       "(:insurer IS NULL OR LOWER(p.insurer) = LOWER(:insurer)) AND " +
       "(:planType IS NULL OR LOWER(p.planType) = LOWER(:planType)) AND " +
       "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
       "ORDER BY p.id ASC")
List<Product> findByFilters(@Param("categoryId") Long categoryId,
                            @Param("insurer") String insurer,
                            @Param("planType") String planType,
                            @Param("name") String name);
```

Decisions:

- **One query, not a new derived method.** Composition (*Name and category
  narrow together*) requires the predicates in a single query; a separate
  `findByNameContainingIgnoreCase…` could not compose with categoryId/insurer/
  planType without N combinatorial methods.
- **`LIKE` with `CONCAT` + bound `:name`** is the same construct
  `searchProducts` already uses against this Postgres; the term is a bound
  value, never concatenated into query structure (spec: *term is data*).
- **`ORDER BY p.id ASC`** satisfies *Several products may share a matching
  name (stable, repeatable order)*. This adds a deterministic order to the
  three existing-filter combinations too. That preserves the *containment*
  the "Existing callers are unaffected" scenario specifies (the scenario says
  "contains exactly the products", not "in the same order" — today's order is
  unspecified heap order) and is the same id-ascending choice #17 made.
  Flagged for gate G2 as the only observable side effect on existing callers.
- **No change to `findAll()`/`getAllProducts`** — the no-filter path is not
  touched at all (§3.3).

No migration: the filter reads `products.name VARCHAR(255) NOT NULL`
(`V3__create_products_tables.sql`); Flyway head stays V12.

### 3.2 Service — `ProductService`

Change `filterProducts` in place (its only caller is `ProductController`):

```java
/** products.name is VARCHAR(255); no stored name can contain a longer substring. */
private static final int MAX_NAME_FILTER_LENGTH = 255;

@Transactional(readOnly = true)
public List<ProductDto> filterProducts(Long categoryId, String insurer,
                                       String planType, String name) {
    String term = name == null ? null : name.trim();
    if (term != null && term.isEmpty()) {
        term = null;                       // blank ⇒ no name filter
    }
    if (term != null && term.length() > MAX_NAME_FILTER_LENGTH) {
        return List.of();                  // cannot match; same answer, no round trip
    }
    return productRepository.findByFilters(categoryId, insurer, planType, term)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
}
```

Normalisation table (the contract T2's service test asserts row by row):

| Incoming `name` | Passed to repository | Result |
|---|---|---|
| `null` | `null` | existing behaviour, no name predicate |
| `""` / `"   "` | `null` | unfiltered by name (spec: *whitespace-only term is not an error*) |
| `"  Term Life  "` | `"Term Life"` | trimmed (spec: *surrounding whitespace is ignored*) |
| length > 255 after trim | — (repository not called) | `[]` (spec: *oversized term handled without failure*) |
| anything else | trimmed term | bound into `LIKE` |

The oversized short-circuit's interaction with composition is still correct:
filters AND together, and an impossible name predicate makes the intersection
empty regardless of the other parameters.

Unlike #17 §3.4, **no logging/PII change is needed**: product names are
catalogue data, not personal data (proposal, *Data & privacy*), and this
method logs nothing today. Do not add logging of the term anyway — it is
user-typed input.

### 3.3 Controller — `ProductController`

Add the parameter and extend the existing branch condition:

```java
@GetMapping
@Operation(summary = "Get all products or filter by parameters")
public ResponseEntity<List<ProductDto>> getProducts(
        @Parameter(description = "Category ID to filter by")
        @RequestParam(required = false) Long categoryId,
        @Parameter(description = "Insurer name to filter by")
        @RequestParam(required = false) String insurer,
        @Parameter(description = "Plan type to filter by")
        @RequestParam(required = false) String planType,
        @Parameter(description = "Case-insensitive substring filter on product name; blank means no filter")
        @RequestParam(required = false) String name) {

    List<ProductDto> products;
    if (categoryId != null || insurer != null || planType != null || name != null) {
        products = productService.filterProducts(categoryId, insurer, planType, name);
    } else {
        products = productService.getAllProducts();
    }
    return ResponseEntity.ok(products);
}
```

Keeping the existing branch (rather than routing everything through
`filterProducts`) makes *Existing callers are unaffected* true by
construction: a request without `name` executes byte-for-byte the same code
path it executes today. The agent product browser
(`frontend/src/pages/agent/ProductBrowser.tsx:31-36`) calls this endpoint with
only `categoryId` and is therefore untouched (*The agent product browser is
unchanged*).

Note the resulting edge: `?name=%20` (whitespace-only) takes the filter branch,
normalises to `null`, and `findByFilters(null,null,null,null)` returns the full
catalogue — every product, as the spec requires, ordered by id. That ordering
difference versus the no-param path is covered by the G2 flag in §3.1.

Authorization is unchanged: no `@PreAuthorize` exists on this controller today;
access is `anyRequest().authenticated()` from `SecurityConfig` (any signed-in
role — the agent browser depends on that), and the spec requires exactly that
to stay true. Do not add a role annotation.

### 3.4 Documentation

`backend/API_ENDPOINTS.md` line 51 (`GET /api/products` row): change the
description to name the optional query parameters, e.g.
"Get all products; optional `categoryId`, `insurer`, `planType`, `name`
(case-insensitive substring) filters".

## 4. Frontend design (T3)

### 4.1 API client — `frontend/src/api/products.ts`

Add `name` to the existing params object (the ignored `size` stays as-is —
pre-existing, out of scope):

```ts
getAll: async (params?: {
  categoryId?: number;
  size?: number;
  name?: string;
}): Promise<Product[]> => { …unchanged body… }
```

No change in `frontend/src/types/` is needed: the request shape is typed
inline here (as today), and `Product` (`frontend/src/types/index.ts:90`)
already mirrors `ProductDto`, which is unchanged.

### 4.2 Page — `frontend/src/pages/admin/ProductManagement.tsx`

Mirror `UserManagement.tsx` exactly, adapted to this page's simpler state (no
pagination — the page has none today and adding it is a non-goal):

1. **State:** `const [nameFilter, setNameFilter] = useState('');` and
   `const appliedFilter = nameFilter.trim();` — trimmed before it enters the
   query key so `"a "` and `"a"` share one cache entry and a whitespace-only
   term issues no filtered request (same comment-documented reasoning as
   `UserManagement.tsx:40-43`; the server trims again and does not trust this).
2. **Query:**
   ```ts
   const { data: products, isLoading, error } = useQuery({
     queryKey: ['products', appliedFilter],
     queryFn: () => productsApi.getAll({ name: appliedFilter || undefined }),
     placeholderData: (previousData) => previousData,
   });
   ```
   `placeholderData` keeps the previous rows on screen between keystrokes
   instead of flashing the spinner (as in `UserManagement.tsx:60`).
   The key extension is safe for the mutations: `invalidateQueries({ queryKey:
   ['products'] })` prefix-matches every `['products', *]` entry, so
   create/update/delete still refresh the filtered view, and `ProductBrowser`'s
   `['products', selectedCategory]` keys are unrelated numbers/undefined.
3. **Restructure the early returns.** The page currently returns
   `<LoadingSpinner/>` / `<ErrorAlert/>` *instead of* the whole page
   (`ProductManagement.tsx:111-117`). Keep the header + filter box rendered
   unconditionally and move the loading/error branches to wrap only the table
   — same structure as `UserManagement.tsx:120-124` — otherwise the filter
   input unmounts on the first keystroke's refetch and loses focus.
4. **Filter box** between the header row and the table:
   ```tsx
   <Paper sx={{ mb: 2, p: 2 }}>
     <TextField
       label="Filter by name"
       variant="outlined"
       size="small"
       value={nameFilter}
       onChange={(e) => setNameFilter(e.target.value)}
       sx={{ minWidth: 300 }}
     />
   </Paper>
   ```
   (No `setPage(0)` — this page has no pagination.)
5. **Empty state** in the table body (today an empty result renders a bare
   table):
   ```tsx
   {products && products.length === 0 && (
     <TableRow>
       <TableCell colSpan={6} align="center">
         {appliedFilter
           ? 'No products match the entered name.'
           : 'No products found.'}
       </TableCell>
     </TableRow>
   )}
   ```
6. **Clearing** the input returns `appliedFilter` to `''`, so the query key
   reverts and the unfiltered list is re-served (from cache or refetch) —
   spec: *Clearing the filter restores the full list*.

Filtering is server-side via the T1 parameter — never a client-side
`Array.filter` over fetched rows. The proposal records that `getAll`'s `size`
is ignored by the backend and the full list is returned today; the filter must
not be built on the fiction of pagination (*The filter covers the whole
catalogue*).

### 4.3 What T3 must not do

- Touch `ProductBrowser.tsx`, `CategoryManagement.tsx`, or any agent page.
- Add pagination, sorting, debouncing beyond what TanStack Query's keystroke
  refetch does today for #17, or filter fields other than name.
- Change `frontend/src/api/products.ts` beyond the one params addition.

## 5. Test design

### 5.1 Backend (T2) — extend the tree #17 established

Three classes under `backend/src/test/java/com/insurance/products/`, each
mirroring its #17 counterpart's harness (named in parentheses):

**`repository/ProductRepositoryNameFilterTest`**
(mirrors `UserRepositoryEmailFilterTest`: `@DataJpaTest(properties =
"spring.jpa.hibernate.ddl-auto=none")` + `@AutoConfigureTestDatabase(replace =
NONE)` + Testcontainers `pgvector/pgvector:pg16` with the
`db/init/01-init-pgvector.sql` init script — H2 cannot build this persistence
unit, see that class's javadoc). Seeds a category + products via
`TestEntityManager` and asserts the semantics only a real database can prove:

- full-name and partial-term contains matching; case-insensitivity
- `%`, `_` and `' OR 1=1 --` bound as data (injection literal; no error)
- a term present only in `insurer`/`planType`/`tags` matches nothing
  (*Only the name is matched*)
- composition: name + categoryId returns exactly the intersection; name +
  insurer/planType likewise (*Name and category narrow together*)
- `findByFilters(null,null,null,null)` returns every product
- duplicate-prone names: several matches all returned, ordered by id, twice
  in a row identically (*stable, repeatable order*)

**`service/ProductServiceNameFilterTest`**
(mirrors `UserManagementServiceEmailFilterTest`: Mockito, repository mocked,
`ArgumentCaptor`). Asserts every row of the §3.2 normalisation table — which
arguments reach `findByFilters`, that the repository is *not* called for the
oversized case, that blank and null both pass `null`, and that no write method
(`save`/`delete*`) is ever invoked (*Filtering grants no write access*).

**`controller/ProductControllerNameFilterTest`**
(mirrors `UserManagementControllerEmailFilterTest`: `@WebMvcTest(ProductController.class)`
+ `@Import(SecurityConfig.class)` with the JWT filter/user-details mocked).
Asserts:

- unauthenticated `GET /api/products?name=x` → 401/403, empty of product data
  and of any match indication (*unauthenticated caller cannot read by
  filtering*), service never invoked
- authenticated AGENT-role request succeeds (*an authenticated agent may still
  list products*) — this pins the "do not add `@PreAuthorize`" decision
- `?name=` present → `filterProducts(…, name)` called with the raw param;
  no params → `getAllProducts()` called (*existing callers unaffected*, the
  branch itself)
- oversized term → 200 `[]`, not 5xx

Profile `gotchas` apply: if local `mvn test` hits the Lombok/JDK 21.0.5
`TypeTag :: UNKNOWN` failure, it is environmental — use the Docker path and
judge the delta only. The repository class needs a Docker daemon (as #17's
does); where unavailable, the other two classes still cover normalisation,
binding, and authorization.

### 5.2 Frontend (T4) — extend the Vitest harness

New `frontend/src/pages/admin/ProductManagement.test.tsx`, colocated, following
`UserManagement.test.tsx` patterns: `vi.mock('../../api/products')` (both
`productsApi` and `categoriesApi` — the page queries categories for its
dialog), `QueryClientProvider` with `retry: false`, `userEvent` typing with the
same act-settling helper. Asserts:

- typing a term calls `productsApi.getAll` with `{ name: term }` and renders
  exactly the returned rows (server-side filtering, not local)
- a whitespace-padded term is sent trimmed; a whitespace-only term issues no
  filtered request (`name: undefined`)
- clearing the input re-requests/serves the unfiltered list and all rows return
- a `[]` response with a term shows "No products match the entered name."; a
  `[]` response without a term shows "No products found."
- the filter input keeps focus across a refetch (guards the §4.2-3 restructure)

API mocked throughout; no live backend; e2e is out of scope.

## 6. Failure modes and edge cases, per spec scenario

| Scenario | Mechanism | Failure mode guarded |
|---|---|---|
| Full name / partial term / case-insensitive | `LOWER(name) LIKE LOWER('%'∥term∥'%')` in Postgres | collation surprises — proven against real pgvector/pg16 container, not H2 |
| Surrounding whitespace ignored | trim in service (and pre-trim in UI query key) | double-trim is idempotent; server does not trust client |
| Several matches, stable order | `ORDER BY p.id ASC` | heap-order nondeterminism between identical requests |
| Clearing restores full list | UI: key reverts to `['products','']` → unfiltered call; server: null term skips predicate | stale cache — prefix invalidation on mutations |
| No match → explicit empty state | 200 `[]` + conditional table row | bare empty table (today's behaviour) misread as "no products exist" |
| Whole catalogue, not displayed rows | filter is a query param; no client-side `Array.filter` | filtering an already-fetched slice if pagination is ever added later |
| Only the name is matched | predicate references `p.name` only; `/search` deliberately not reused | insurer/planType bleed-through via `searchProducts` |
| Name + category narrow together | single AND-composed JPQL | combinatorial derived-method drift |
| Existing callers unaffected | `name == null` requests take today's exact code path | behavioural drift on the agent browser; order change flagged §3.1 |
| Agent product browser unchanged | no frontend change outside `ProductManagement.tsx` + params type | accidental shared-component edits |
| Unauthenticated caller rejected | `anyRequest().authenticated()`, unchanged; controller test pins it | new endpoint param accidentally widening access (it cannot — same endpoint) |
| Authenticated agent may still list | no `@PreAuthorize` added; test pins it | over-zealous ADMIN-only annotation breaking the agent browser |
| Filtering grants no write access | `@Transactional(readOnly = true)`; service test verifies no `save` | — |
| SQL metacharacters literal | JPQL named parameter binding | injection / query-structure alteration |
| Oversized term without failure | 255-length short-circuit → `[]` | pathological `LIKE` input reaching the DB; 5xx on huge params |
| Whitespace-only term not an error | blank→null normalisation | `LIKE '%%'` oddities / accidental validation error |

## 7. Rollout, rollback, and decisions flagged for gate G2

**Rollout order** (from `tasks.md`; every intermediate merge releasable):

1. **T1 #46** — backend param. Optional ⇒ no caller changes behaviour.
2. **T2 #47** and **T3 #48** in parallel — tests exercise the merged T1;
   the UI starts sending `name`.
3. **T4 #49** — component tests for the merged T3.

**Rollback:** each PR reverts cleanly in reverse order. No migration, no
schema change, no config change, no new dependency (Testcontainers/pgvector
image and all frontend test deps already entered via #17). Reverting T1 after
T3 would leave the UI sending a `name` param Spring ignores (unknown request
params are dropped) — degraded (filter stops filtering) but not broken;
revert T3 first regardless.

**Decisions the G2 approver should consciously accept:**

1. **The name filter rides `GET /api/products`, not `/search`** — settling the
   question the proposal and `tasks.md` left open, for the *Only the name is
   matched* reason in §1.
2. **`findByFilters` gains `ORDER BY p.id ASC`**, which also gives the three
   existing filter params a deterministic order where today's is unspecified.
   Containment is untouched; this is the only observable change for existing
   callers.
3. **Oversized terms (>255 chars) return 200 `[]`**, not 400 — the spec allows
   either ("a result set or a validation error"); `[]` mirrors #17.
4. **No wildcard escaping**: `%`/`_` typed into the filter act as LIKE
   wildcards (bound safely as data). Explicitly a proposal non-goal; noted so
   nobody files it as a bug later.
