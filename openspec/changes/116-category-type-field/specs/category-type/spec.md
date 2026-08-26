## Purpose

Lets admins classify product categories with an optional "Type" value drawn from a dynamic set (existing values suggested, new values creatable on the fly), and surfaces that classification wherever categories and products are shown.

## ADDED Requirements

### Requirement: Category type is stored and served with category data

The system SHALL persist an optional textual type on each product category and SHALL include it in every API response that returns category data (list, get-by-id, create, update responses of the category endpoints).

#### Scenario: Category created with a type

- **WHEN** an authenticated user creates a category with name "Travel", a description, and type "Individual"
- **THEN** the category is saved with type "Individual" and the create response and subsequent category reads include `type: "Individual"`

#### Scenario: Category created without a type

- **WHEN** an authenticated user creates a category providing only name and description
- **THEN** the request succeeds and the category is returned with no type value (null/absent)

#### Scenario: Type updated on an existing category

- **WHEN** an authenticated user updates a category and changes its type from "Individual" to "Group"
- **THEN** subsequent reads of that category return type "Group"

#### Scenario: Type cleared on an existing category

- **WHEN** an authenticated user updates a category that has a type and submits an empty type
- **THEN** the category is saved with no type and reads return no type value

#### Scenario: Blank type is normalised to no type

- **WHEN** a category is created or updated with a type consisting only of whitespace
- **THEN** the stored category has no type (whitespace-only input is treated the same as omitting the field, and surrounding whitespace is trimmed from non-blank values)

#### Scenario: Existing categories are unaffected by the field's introduction

- **WHEN** the schema change introducing the type field is applied to a database with existing categories
- **THEN** all existing categories remain valid with no type value and all category endpoints keep working

### Requirement: Admin can manage a category's type on the Manage Categories page

The admin Manage Categories page SHALL display each category's type in the list and SHALL let the user set, change, or clear the type in the create and edit dialogs. The type input SHALL offer the type values already in use across categories as suggestions while also accepting a value typed freely, and SHALL NOT require a value.

#### Scenario: Type column in the category table

- **WHEN** an admin opens the Manage Categories page
- **THEN** the category table shows a Type column, with each category's type displayed and categories without a type showing an empty value

#### Scenario: Choosing an existing type value

- **WHEN** an admin opens the create or edit category dialog and focuses the Type field, and categories with types "Individual" and "Group" already exist
- **THEN** "Individual" and "Group" are offered as selectable suggestions

#### Scenario: Creating a new type value on the fly

- **WHEN** an admin types a value not offered in the suggestions, e.g. "Commercial", into the Type field and saves the category
- **THEN** the category is saved with type "Commercial", and "Commercial" is offered as a suggestion the next time the dialog is opened

#### Scenario: Saving without a type

- **WHEN** an admin submits the create or edit dialog leaving the Type field empty
- **THEN** the form submits successfully with no validation error on the Type field

#### Scenario: Editing pre-fills the current type

- **WHEN** an admin opens the edit dialog for a category that has a type
- **THEN** the Type field is pre-filled with that category's current type

### Requirement: Category type is shown where a product's category is displayed

Product API responses SHALL include the type of the product's category alongside the category name, and the pages that display a product's category — the admin Manage Products table and the agent Product Browser — SHALL display that type. Products whose category has no type SHALL display without a type, unchanged from today.

#### Scenario: Product API exposes the category's type

- **WHEN** a product belongs to a category whose type is "Group" and the product is fetched via the product endpoints (list or get-by-id)
- **THEN** the product payload includes the category's type "Group" in addition to the category name

#### Scenario: Manage Products table shows the category type

- **WHEN** an admin views the Manage Products table and a listed product's category has type "Group"
- **THEN** that product's row shows "Group" alongside the category name

#### Scenario: Product Browser shows the category type

- **WHEN** an agent views the Product Browser and a product card's category has type "Individual"
- **THEN** the card shows "Individual" alongside the category name

#### Scenario: Product whose category has no type

- **WHEN** a product's category has no type and the product is displayed on the Manage Products table or Product Browser
- **THEN** the category name is shown as it is today, with no placeholder or empty label for the missing type
