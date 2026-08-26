# Design: Category "Type" field

**Epic:** genai-jerry/insurance-app-base#116
**Change folder:** `openspec/changes/116-category-type-field/`
**Repos affected:** insurance-app-base only (standalone monorepo per `.factory/profile.json` — this is the single copy of the contract; there are no sibling repos to mirror it into).

Grounded against the code at `factory/116-design` (branched from `main`). Every
file named below was read before this design was written.

## 1. Decisions

1. **Storage:** one new nullable `type VARCHAR(100)` column on
   `product_categories`, added by forward-only Flyway migration
   `V14__add_type_to_product_categories.sql`. No index — filtering/sorting by
   type is an explicit non-goal. `VARCHAR(100)` matches the precedent of
   `products.plan_type VARCHAR(100)` (V3), the closest analogous
   free-text-classifier column in the schema.
2. **No new endpoint for type suggestions.** The proposal left this open; the
   decision is **derive suggestions client-side**. The Manage Categories page
   already fetches every category (`categoriesApi.getAll` → TanStack Query key
   `['categories']`); the distinct set of `type` values is a `Set` over that
   already-loaded list. A `/types` endpoint would be a second source of truth
   for data the page already holds, plus an extra round-trip on dialog open.
3. **Blank normalisation lives in the service**, not the controller or the
   frontend: `ProductCategoryService` trims the incoming type and stores
   blank/whitespace-only as `null`. The frontend sends what the user typed;
   the API is the enforcement point, so the spec's normalisation scenarios
   hold for any client.
4. **Update semantics — null vs blank.** The existing `updateCategory` treats
   `null` name/description as "not provided, leave unchanged". Type follows
   the same convention: `type == null` in an update leaves the stored type
   unchanged; a non-null type is trimmed, and a blank result **clears** it
   (stored `null`). This satisfies the "Type cleared" scenario (the edit
   dialog always submits the field, empty string when cleared) without
   breaking the endpoint's established partial-update behaviour.
5. **Bounded input instead of a 500.** The spec's non-goal says no validation
   beyond trimming, but an unbounded string against `VARCHAR(100)` turns into
   a `DataIntegrityViolationException` → HTTP 500. Guard structurally:
   `@Size(max = 100)` on the DTO field (the controller already applies
   `@Valid`, so oversized input becomes a 400) and `inputProps.maxLength` on
   the UI field. This is integrity protection, not value validation — any
   content up to 100 chars is accepted.
6. **Authorization is deliberately unchanged.** The three `products/`
   controllers carry **no** `@PreAuthorize` today (flagged in
   `.factory/profile.json` `review_checklist`): any authenticated user can
   mutate categories. The spec's scenarios say "authenticated user"
   accordingly. Adding role checks here would change the behaviour of
   existing endpoints and is out of scope for this change — **flagged for a
   separate decision**, not silently fixed and not silently ignored.
7. **Frontend suggest-or-free-entry control:** MUI `Autocomplete` with
   `freeSolo` (already available via the installed `@mui/material` 5 — no new
   dependency), wired to react-hook-form via `Controller` (the library's own
   API for controlled inputs; `register()` cannot bind Autocomplete). Inline
   rules stay unnecessary — the field has no required rule, matching the
   "Saving without a type" scenario.

## 2. API contract

All endpoints exist today; this change is **additive** — one optional field on
two DTOs. No path, method, status code, or auth behaviour changes. No breaking
change.

```
Contract: category-type (v1, additive)

ProductCategoryDto (response of all /api/products/categories endpoints;
                    request body of POST and PUT):
  {
    "id": 3,
    "name": "Travel",
    "description": "Travel insurance",
    "type": "Individual" | null,        // NEW, optional, <= 100 chars after trim
    "createdAt": "...", "updatedAt": "..."
  }

ProductDto (response of GET /api/products, GET /api/products/{id},
            GET /api/products/category/{categoryId}, POST/PUT /api/products):
  {
    "id": 7,
    "categoryId": 3,
    "categoryName": "Travel",
    "categoryType": "Individual" | null, // NEW, read-only, = category.type
    ... unchanged fields ...
  }

Semantics:
- POST /api/products/categories: "type" absent, null, "" or whitespace-only
  → stored as no type; otherwise stored trimmed. 201 as today.
- PUT /api/products/categories/{id}: "type" null/absent → unchanged;
  "" or whitespace-only → cleared; otherwise stored trimmed. 200 as today.
- "type" longer than 100 chars after trim → 400 (bean validation), new.
- "categoryType" is derived from the product's category; it is never
  writable through product endpoints.
- Auth: unchanged — authenticated user (JWT); no role restriction (see
  Decision 6).
```

## 3. Data: migration plan

Flyway (the migration tool named in `.factory/profile.json`), directory
`backend/src/main/resources/db/migration/`. Current head is **V13** (trust the
directory, not CLAUDE.md's "V11").

`V14__add_type_to_product_categories.sql`:

```sql
ALTER TABLE product_categories ADD COLUMN type VARCHAR(100);
```

- Nullable, no default, no backfill (non-goal: seeded Life/Health/Auto/Home
  categories start with no type) — this is exactly the "Existing categories
  are unaffected" scenario.
- Purely additive and instant on PostgreSQL (no table rewrite for a nullable
  column without default); safe on a live database.
- `type` is not a reserved word in PostgreSQL column position; no quoting
  needed.

## 4. Modules to touch, per task

### Task 1 (#118) — backend: persist + category API

| File | Change |
|---|---|
| `backend/src/main/resources/db/migration/V14__add_type_to_product_categories.sql` | New migration (above) |
| `backend/src/main/java/com/insurance/common/entity/ProductCategory.java` | Add `@Column(length = 100) private String type;` between `description` and the timestamps. Entities live only in `common/entity/` per conventions — no new entity, extend this one |
| `backend/src/main/java/com/insurance/products/dto/ProductCategoryDto.java` | Add `@Size(max = 100) private String type;` (import `jakarta.validation.constraints.Size`) |
| `backend/src/main/java/com/insurance/products/service/ProductCategoryService.java` | `createCategory`: `.type(normalizeType(dto.getType()))` in the builder. `updateCategory`: `if (dto.getType() != null) { category.setType(normalizeType(dto.getType())); }` — where `normalizeType` is a private helper: trim, return `null` if blank. `toDto`: add `.type(category.getType())`. This module converts via private `toDto()` helpers, **not** MapStruct — follow that (profile convention: use whichever the module already uses) |

No repository change: no new query, so the `CAST(:p AS string)` bytea gotcha
is not in play. No controller change: `ProductCategoryController` already
passes the DTO through with `@Valid`.

### Task 2 (#119) — backend: product payloads

| File | Change |
|---|---|
| `backend/src/main/java/com/insurance/products/dto/ProductDto.java` | Add `private String categoryType;` next to `categoryName` |
| `backend/src/main/java/com/insurance/products/service/ProductService.java` | In the private `toDto()` (line ~159): `.categoryType(product.getCategory().getType())` next to `.categoryName(...)`. `Product.category` is `@ManyToOne(optional = false)` and `toDto` already dereferences it for id/name, so no new null-handling or fetch concern |

Covers every product read path (list, get-by-id, by-category, filter, and the
create/update responses) because they all funnel through the same `toDto()`.

### Task 3 (#120) — frontend: Manage Categories

| File | Change |
|---|---|
| `frontend/src/types/index.ts` | `Category` gains `type?: string` |
| `frontend/src/api/products.ts` | `categoriesApi.create` data type gains `type?: string`; `categoriesApi.update` data type gains `type?: string`. No new axios instance — these already go through the shared `client.ts` |
| `frontend/src/pages/admin/CategoryManagement.tsx` | (a) `CategoryForm` gains `type: string`. (b) Table: `Type` column header between Description and Actions; cell renders `{category.type ?? ''}` — empty when unset, no placeholder. (c) Dialog: an `Autocomplete freeSolo` bound via react-hook-form `Controller` (add `Controller` to the existing react-hook-form import, `Autocomplete` to the MUI import); `options` = sorted unique non-empty `type` values from the already-fetched `categories`; `renderInput` = `TextField` labelled "Type" with `inputProps.maxLength = 100`, no error/required wiring. Bind `inputValue` to the field value and `onInputChange → field.onChange` so freely typed text (not just selections) reaches the form state. (d) `handleEdit`: `setValue('type', category.type ?? '')` — pre-fills current type. (e) `onSubmit` passes `data` through unchanged; the create dialog's `reset()` keeps clearing all fields including type |

Suggestion freshness: the create/update mutations already invalidate
`['categories']`, so a newly typed value ("Commercial") appears in the
suggestion list the next time the dialog opens — exactly the "Creating a new
type value on the fly" scenario, with no extra code.

### Task 4 (#121) — frontend: product surfaces (display only)

| File | Change |
|---|---|
| `frontend/src/types/index.ts` | `Product` gains `categoryType?: string` |
| `frontend/src/pages/admin/ProductManagement.tsx` | Category cell (line ~169) becomes `{product.categoryName}` followed by, **only when `product.categoryType` is truthy**, a `size="small"` outlined `Chip` labelled with the type (Chip is already imported for tags). No type → cell renders the bare name exactly as today |
| `frontend/src/pages/agent/ProductBrowser.tsx` | Next to the existing category `Chip` (line ~77), render a second `size="small"` `variant="outlined"` `Chip` with `product.categoryType`, only when truthy. No type → card unchanged from today |

No new pages, routes, or Sidebar entries — all four tasks modify existing
screens, so the "new frontend page" convention checklist does not apply.

## 5. Failure modes and edge cases, per spec scenario

| Scenario | Handling |
|---|---|
| Category created with a type | `createCategory` builder stores trimmed type; `toDto` returns it in the 201 body and all reads |
| Category created without a type | `normalizeType(null)` → `null`; column nullable; response `type: null` |
| Type updated | Non-null incoming type → `setType(trimmed)`; `@Transactional` flushes; reads return new value |
| Type cleared | Edit dialog submits `type: ""` → `normalizeType` → `null` stored. API-only clients clear the same way (Decision 4) |
| Blank normalised | `"   "` → trim → blank → `null`. Same single helper on both create and update paths — one code path, no drift |
| Existing categories unaffected | Nullable column, no default, no backfill; `existsByName`/CRUD queries untouched; Hibernate maps the new field from the migrated column with no `ddl-auto` interference |
| Type column in table | Renders `category.type ?? ''`; TanStack Query refetch on invalidation keeps it current |
| Choosing an existing type | `Autocomplete` options from loaded categories; duplicates collapsed by `Set`, blanks excluded |
| New type on the fly | `freeSolo` accepts arbitrary text; mutation → invalidate → next dialog open suggests it |
| Saving without a type | No required rule, no error state; empty string sent; backend normalises |
| Editing pre-fills | `setValue('type', category.type ?? '')` mirrors the existing name/description pre-fill |
| Product API exposes type | Single `toDto()` funnel in `ProductService`; category is non-optional on `Product`, so no NPE path |
| Manage Products shows type | Conditional Chip; `categoryType` absent/null → nothing rendered |
| Product Browser shows type | Same conditional pattern on the card |
| Product whose category has no type | Both surfaces render the bare category name — the falsy check is the whole mechanism, no placeholder branch to get wrong |
| Oversized type (edge, no spec scenario) | > 100 chars: 400 from `@Size` instead of a 500 from the DB; UI prevents it with `maxLength` |

Test-layer notes (for the implementers; the profile's `qa_notes` govern):
service normalisation cases fit `ProductCategoryService` Mockito tests;
DTO-passthrough and 400-on-oversize fit `@WebMvcTest`; the migration +
column mapping is only proven by the Testcontainers repository tests, which
need Docker — on a no-Docker machine judge against the profile's
74-pass/2-container-error baseline and run the frontend suite separately.
Frontend: extend colocated Vitest per the `ProductManagement.test.tsx`
mock-the-api pattern; note `ProductManagement.test.tsx`'s existing 8 tests
must stay green after the Task 4 cell change.

## 6. Rollout order and rollback

Order (matches `tasks.md` dependencies; every intermediate merge releasable):

1. **#118** — migration + entity + category API. Frontend ignores unknown
   JSON fields; nothing consumes `type` yet. Releasable alone.
2. **#119** and **#120** — independent of each other, both depend only on
   #118. `categoryType` is additive on product payloads; the Manage
   Categories UI consumes only #118's field. Either order, releasable alone.
3. **#121** — needs #119's `categoryType` in product payloads. Releasable.

Frontend tasks degrade safely even if deployed against an older backend
(fields simply `undefined` → empty cell / no chip), so no deploy coupling
beyond the merge order above. This repo has **no staging branch and no CI**
(profile: `branches.staging: null`, no workflows exist) — task PRs base on
`main`, and implementers must run the test commands themselves; nothing else
will.

Rollback: reverting any task's code PR is safe in any order — the fields are
optional end to end. The V14 migration is forward-only per repo convention
and is **not** reverted with code: a nullable, unindexed, unreferenced column
is inert under the previous application version. If a rollback of #118's code
is ever needed, leave the column in place; a compensating drop migration is
only for the (unlikely) case the whole change is abandoned after production
apply.

## 7. Reuse audit (what this design extends instead of duplicating)

- `ProductCategory` entity + `product_categories` table — extended, not
  paralleled (no type lookup table; non-goal).
- `ProductCategoryService.toDto()` / `ProductService.toDto()` private-helper
  mapping style — followed; MapStruct not introduced into this module.
- Existing category CRUD endpoints — reused verbatim; zero new endpoints.
- `['categories']` TanStack Query cache + invalidation — reused as the
  suggestion source and freshness mechanism.
- MUI `Chip` (already used for tags/category on both product surfaces) and
  `Autocomplete` (in the installed MUI package) — no new dependency.
- Migration numbering: `V14` continues the existing Flyway chain.
