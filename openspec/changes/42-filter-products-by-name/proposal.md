# Filter the manage-products list by product name

Requirement issue: [#42](https://github.com/genai-jerry/insurance-app-base/issues/42)

## Why

`/admin/products` (the "Manage Products" screen) renders every product in one
flat table with no way to narrow it and no pagination — the whole catalogue is
on screen at once. An administrator who wants to edit or delete a specific
product has to find it by eye, and the list grows with every product added.
The product name is the identifier an admin actually knows and the first column
of the table, so filtering by it is the shortest path from "I need this product"
to "I am acting on this product".

The issue asks for this to "follow similar standards as the filtering supported
in User Management page". That filter already ships (issue #17): a single
always-visible text box above the table, case-insensitive substring matching
applied over the complete data set with the term trimmed, an explicit empty
state when nothing matches. This change gives the product list the same
behaviour, keyed on name.

## What Changes

- The manage-products list gains a product-name filter: an administrator
  supplies a name (whole or partial) and the list shows only the products whose
  name matches it.
- Matching is **case-insensitive substring (contains)** matching, with
  surrounding whitespace trimmed — the same semantics as the shipped user email
  filter and the existing lead search on `/leads`.
- The filter is evaluated over the **complete** set of products, not only the
  rows the table happens to have rendered.
- Clearing the filter restores the unfiltered list; a filter matching nothing
  shows an explicit empty state rather than a blank table.
- Product names are not unique, so a term may legitimately match several
  products; all of them are shown.
- Callers that supply no name filter — including the agent-facing product
  browser and every existing product listing caller — behave exactly as they do
  today.
- Access to the product list is unchanged by this change.

### Assumptions recorded (raised for the approver at gate G1)

1. **Partial, case-insensitive matching**, not exact equality. The issue says
   "search and filter list of products by name" and points at the user email
   filter, which is substring-based; a box that only responds to a perfectly
   typed full product name would be of little practical use.
2. **The admin screen only.** The requirement names the product management
   page. The agent-facing product browser (`/products`) is left alone, as is
   the category management screen — category-name filtering is issue #41.
3. **Name only.** Not insurer, plan type, tag or category. The earlier,
   now-closed issue #32 asked for name *and* category on this screen; #42
   re-scopes that to name, and this proposal follows #42.

If any of these readings is wrong, say so on the issue and the spec will be
narrowed before planning.

## Capabilities

### New Capabilities

- `admin-product-management`: How administrators view, locate and manage the
  insurance product catalogue from the admin console. This change establishes
  the capability with its name-filter requirements; the existing
  list/create/edit/delete behaviours are not re-specified here.

### Modified Capabilities

<!-- None: openspec/specs/ is currently empty, so there is no existing
     capability whose requirements this change alters. -->

## Impact

**Affected surface**

- Frontend (`frontend/src/pages/admin/ProductManagement.tsx`, and
  `frontend/src/api/products.ts` if the filter is applied server-side) — a
  filter input above the product table, wired to the product listing.
- Backend (`backend/src/main/java/com/insurance/products/`) — expected to
  change, because the filter must cover the whole catalogue rather than a
  client-side slice of an already-fetched page. The listing surface
  (`GET /api/products`) already takes optional `categoryId`, `insurer` and
  `planType` parameters; a name filter has to compose with them rather than
  replace them. See the note below before choosing between extending that
  endpoint and reusing `GET /api/products/search`.
- No database schema change: the filter reads the existing
  `products.name VARCHAR(255)` column (`V3__create_products_tables.sql`), so no
  new Flyway migration is required.
- No change to authentication, authorization, the RAG/embedding pipeline, or
  any agent-facing screen.

**Data & privacy**

- Product names are catalogue data, not personal data; the filter exposes no
  attribute the list does not already display, and no lead or user data is
  involved.
- Retrieving the product list is authenticated (any signed-in role) today and
  stays exactly that: the filter must neither widen nor narrow who can read the
  catalogue.

**Discovered while analysing (not fixed here)**

- `GET /api/products` currently applies its filters all-or-nothing: if
  `categoryId`, `insurer` and `planType` are all absent it returns every
  product, and `insurer`/`planType` are matched by case-insensitive *equality*
  while the separate `GET /api/products/search?q=` matches name, insurer **and**
  plan type by substring. Neither is a drop-in "name contains" filter, and the
  search endpoint's wider match would silently return products whose *insurer*
  contains the term. The design stage should settle this explicitly rather than
  assume.
- The manage-products table has no pagination at all (unlike the user list) and
  `productsApi.getAll` accepts a `size` parameter the backend ignores. That is
  pre-existing and independent of filtering; it is called out so the design does
  not build the filter on a pagination contract that does not exist.

## Non-goals

- Filtering or searching the product list by insurer, plan type, tags,
  category, or any field other than name.
- Filtering the category list by category name — that is issue #41, a separate
  requirement in this same release.
- Adding a filter to the agent-facing product browser (`/products`) or to any
  other screen.
- Introducing pagination, sorting, column selection, bulk actions or export on
  the product list, or fixing the ignored `size` parameter described above.
- Fuzzy, phonetic, token-order-insensitive or relevance-ranked matching, and
  ranking of results in general: results keep a stable, deterministic order.
- Treating SQL wildcard characters (`%`, `_`) typed into the filter as literal
  characters. The filter must be injection-safe (see the spec), but escaping
  wildcards so they match literally is out of scope.
- Any change to the existing `GET /api/products/search` behaviour, the lead
  search on `/leads`, or the shipped user email filter on `/admin/users`.
