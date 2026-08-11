## Purpose

Describes how an administrator views and locates products in the insurance
product catalogue from the admin console, so that an admin acting on a specific
product can find it without scanning the entire catalogue.

## ADDED Requirements

### Requirement: Filter the product list by product name

The admin product list SHALL accept an optional product-name filter term. When
a term is supplied, the list SHALL contain exactly those products whose name
contains the term, compared case-insensitively, and no others. When no term is
supplied the list SHALL be unfiltered.

The term SHALL have leading and trailing whitespace removed before matching, and
a term that is empty after trimming SHALL be treated as no filter.

#### Scenario: Full product name matches the product

- **WHEN** an administrator filters the product list by `Term Life Secure` and a
  product with that name exists
- **THEN** the list contains that product
- **AND** the list contains no product whose name does not contain `Term Life Secure`

#### Scenario: Partial term matches every product containing it

- **WHEN** an administrator filters by `Life` and several products have `Life`
  somewhere in their names
- **THEN** the list contains all of those products
- **AND** the list contains no product whose name does not contain `Life`

#### Scenario: Matching ignores case

- **WHEN** an administrator filters by `tErM lIfE` and a product named
  `Term Life Secure` exists
- **THEN** the list contains that product

#### Scenario: Surrounding whitespace is ignored

- **WHEN** an administrator filters by `  Term Life  `
- **THEN** the list contains the same products as filtering by `Term Life`

#### Scenario: Several products may share a matching name

- **WHEN** an administrator filters by a term and more than one product's name
  contains it
- **THEN** the list contains every one of those products
- **AND** the products are shown in a stable, repeatable order — the same term
  applied twice to an unchanged catalogue yields the same list in the same order

#### Scenario: Clearing the filter restores the full list

- **WHEN** an administrator clears a previously applied name filter
- **THEN** the list contains every product, exactly as before any filter was applied

#### Scenario: A term matching nothing yields an explicit empty result

- **WHEN** an administrator filters by a name term that no product's name contains
- **THEN** the list contains no products
- **AND** the screen states that no products match the entered name, rather than
  showing an unexplained empty table

#### Scenario: The filter covers the whole catalogue, not just the displayed rows

- **WHEN** an administrator filters by a term that matches a product which was
  not visible on screen before the filter was applied
- **THEN** the list contains that product

#### Scenario: Only the name is matched

- **WHEN** an administrator filters by a term that appears in some product's
  insurer, plan type or tags but in no product's name
- **THEN** the list contains no products

### Requirement: The name filter composes with the listing's existing filters

The product listing SHALL continue to honour its existing optional category,
insurer and plan-type filters. When a name term is supplied together with any of
them, the list SHALL contain exactly the products that satisfy every supplied
filter. When no name term is supplied, the listing SHALL return exactly what it
returns today.

#### Scenario: Name and category narrow together

- **WHEN** a caller requests the product list with a name term and a category
- **THEN** the list contains exactly the products in that category whose name
  contains the term
- **AND** the list contains no product from another category

#### Scenario: Existing callers are unaffected

- **WHEN** a caller requests the product list without a name term — with no
  filters, or with only the category, insurer or plan-type filters
- **THEN** the list contains exactly the products it contains today for that
  request

#### Scenario: The agent product browser is unchanged

- **WHEN** an agent opens the product browser and browses products, with or
  without choosing a category
- **THEN** the products shown are exactly those shown today

### Requirement: Filtering does not change who may read the product catalogue

Retrieving the product list SHALL remain available to exactly the callers who
can retrieve it today — any authenticated user — whether or not a name filter is
supplied. A filtered request from an unauthenticated caller SHALL be rejected and
SHALL disclose nothing about whether any product matches the term.

#### Scenario: An unauthenticated caller cannot read the catalogue by filtering it

- **WHEN** an unauthenticated caller requests the product list with a name filter
- **THEN** the request is rejected as unauthorized
- **AND** the response contains no product data and no indication of whether the
  term matched a product

#### Scenario: An authenticated agent may still list products

- **WHEN** a caller authenticated as an AGENT requests the product list
- **THEN** the request succeeds exactly as it does today

#### Scenario: Filtering grants no write access

- **WHEN** any caller supplies a name filter
- **THEN** no product is created, modified or deleted as a result

### Requirement: The filter term is treated as data, never as query structure

The name filter term SHALL be applied as a bound value so that no term can alter
the meaning of the underlying query, and any term SHALL either return a result
set or a validation error — never a server error or an unhandled failure.

#### Scenario: A term containing SQL metacharacters is matched literally

- **WHEN** an administrator filters by `' OR 1=1 --`
- **THEN** the list contains only products whose name actually contains that
  text — in practice none
- **AND** the request completes successfully without a server error

#### Scenario: An oversized term is handled without failure

- **WHEN** an administrator filters by a term far longer than any stored product
  name
- **THEN** the list contains no products
- **AND** the request completes without a server error

#### Scenario: A term of only whitespace is not an error

- **WHEN** an administrator filters by a term consisting solely of whitespace
- **THEN** the list contains every product, as if no filter had been applied
- **AND** the request completes without a server error
