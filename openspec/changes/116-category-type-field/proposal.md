# Proposal: Category "Type" field

**Issue:** genai-jerry/insurance-app-base#116

## Why

Product categories today carry only a name and a description, so there is no way to group or characterise categories (e.g. mark which kind of insurance line a category belongs to). The requester wants a "Type" attribute stored per category and visible where categories are shown, so admins can classify categories and agents can see that classification when browsing products.

## What Changes

- Add an optional **Type** attribute to product categories, persisted in the database (new column on `product_categories` via a new Flyway migration).
- Type behaves as a **dynamic enumeration**: when setting it, the user chooses from the type values already in use across categories, but can also enter a brand-new value on the fly. There is no separately managed list of allowed types.
- Type is **optional**: a category may have no type, and existing categories start with no type.
- The admin **Manage Categories** page shows Type as a table column and lets admins set/change/clear it in the create and edit dialogs.
- Pages that display a product's category also display that category's type: the admin **Manage Products** table and the agent **Product Browser** cards.
- Category and product API payloads (`ProductCategoryDto`, `ProductDto`) expose the type so the frontend can render it.

## Capabilities

### New Capabilities

- `category-type`: storing an optional, dynamically-enumerated type on product categories; setting it through the admin category UI; displaying it on the category list and wherever a product's category is shown.

### Modified Capabilities

_None — `openspec/specs/` is empty; there are no existing specs to modify._

## Impact

- **Database:** new nullable column on `product_categories`; new migration `V14__...` (forward-only; current head is V13).
- **Backend:** `common/entity/ProductCategory`, `products/dto/ProductCategoryDto`, `products/dto/ProductDto`, `products/service/ProductCategoryService`, `products/service/ProductService` (category type surfaced on product DTOs). Existing `/api/products/categories` CRUD endpoints carry the new field; whether a dedicated endpoint for distinct type values is needed is a design-stage decision (the values are derivable from the category list the UI already fetches).
- **Frontend:** `pages/admin/CategoryManagement.tsx` (table column + dialog input with suggestions and free entry), `pages/admin/ProductManagement.tsx` and `pages/agent/ProductBrowser.tsx` (display only), `types/index.ts`.
- **Data/privacy:** none — category type is non-personal catalog metadata.
- **Affected repositories:** insurance-app-base only (standalone monorepo; no cross-repo contract).

## Non-goals

- No filtering, sorting or grouping of products or categories by type (display only).
- No managed "category types" admin screen or lookup table lifecycle (create/rename/delete of types as their own objects).
- No backfill of types for the existing seeded categories (Life, Health, Auto, Home Insurance) — they start empty.
- No validation of type values beyond trimming whitespace and treating empty as "no type".
- No changes to RAG indexing, prospectus generation, or any consumer of categories beyond the pages named above.
