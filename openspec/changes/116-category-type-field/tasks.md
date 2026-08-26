# Tasks: Category "Type" field

**Epic:** genai-jerry/insurance-app-base#116
**Change folder:** `openspec/changes/116-category-type-field/`
**Repos affected:** insurance-app-base only (standalone monorepo — no cross-repo merge order).

Ordered so every intermediate merge is releasable: the schema/data change lands
first behind a nullable column, the product API addition is additive, and each
frontend task only consumes fields already merged. Each task is one PR,
≤ half a day. Sub-issue numbers are the GitHub mirror of this checklist.

## 1. Persist category type — migration, entity, category API (#118)

- **Repo:** insurance-app-base (backend)
- **Depends on:** —
- **What:** Add the optional type to storage and the category endpoints: new
  forward-only Flyway migration `V14__` adding a nullable column to
  `product_categories`; `common/entity/ProductCategory` field;
  `products/dto/ProductCategoryDto` field carried through the existing
  category CRUD endpoints; create/update paths in
  `products/service/ProductCategoryService` trim whitespace and store
  blank/whitespace-only input as no type.
- **Spec scenarios served** (`specs/category-type/spec.md`, "Category type is
  stored and served with category data"):
  - Category created with a type
  - Category created without a type
  - Type updated on an existing category
  - Type cleared on an existing category
  - Blank type is normalised to no type
  - Existing categories are unaffected by the field's introduction

## 2. Expose category type on product API payloads (#119)

- **Repo:** insurance-app-base (backend)
- **Depends on:** Task 1 (#118)
- **What:** Include the product's category type alongside the category name in
  product payloads: `products/dto/ProductDto` and the mapping in
  `products/service/ProductService`, covering product list and get-by-id.
- **Spec scenarios served** ("Category type is shown where a product's
  category is displayed"):
  - Product API exposes the category's type

## 3. Manage Categories UI — Type column and free-entry type input (#120)

- **Repo:** insurance-app-base (frontend)
- **Depends on:** Task 1 (#118)
- **What:** Surface and edit the type on the admin Manage Categories page
  (`pages/admin/CategoryManagement.tsx`, plus the category type in
  `types/index.ts`): a Type column in the table (empty when unset); a Type
  input in the create and edit dialogs that suggests the type values already
  in use across categories while accepting freely typed new values, requires
  no value, and pre-fills the current type when editing.
- **Spec scenarios served** ("Admin can manage a category's type on the
  Manage Categories page"):
  - Type column in the category table
  - Choosing an existing type value
  - Creating a new type value on the fly
  - Saving without a type
  - Editing pre-fills the current type

## 4. Show category type on Manage Products and Product Browser (#121)

- **Repo:** insurance-app-base (frontend)
- **Depends on:** Task 2 (#119)
- **What:** Display-only: show the category's type alongside the category name
  in the admin Manage Products table (`pages/admin/ProductManagement.tsx`) and
  on agent Product Browser cards (`pages/agent/ProductBrowser.tsx`), with the
  product-side type added to `types/index.ts`. Products whose category has no
  type render exactly as today — no placeholder.
- **Spec scenarios served** ("Category type is shown where a product's
  category is displayed"):
  - Manage Products table shows the category type
  - Product Browser shows the category type
  - Product whose category has no type

## Scenario coverage

Every scenario in `specs/category-type/spec.md` is served by exactly one task:
Requirement 1 (storage/API) → Task 1; Requirement 2 (Manage Categories UI) →
Task 3; Requirement 3 (product surfaces) → Task 2 (API scenario) and Task 4
(the three display scenarios).
