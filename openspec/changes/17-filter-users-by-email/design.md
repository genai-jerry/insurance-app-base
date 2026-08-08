# Design — Filter the admin user list by email

Epic: [#17](https://github.com/genai-jerry/insurance-app-base/issues/17) ·
Change folder: `openspec/changes/17-filter-users-by-email/` ·
Spec: `specs/admin-user-management/spec.md` · Plan: `tasks.md`

**Repos affected:** `insurance-app-base` only. `.factory/profile.json` records
`estate_role` as a standalone full-stack monorepo with no sibling repos, so the
contract below has no second copy to keep in sync (see
[§ API contract](#api-contract)).

**No database migration.** The filter reads the existing `users.email` column
(`VARCHAR(255) NOT NULL UNIQUE`, `V1__create_users_table.sql`). Flyway's head in
this repo is `V13__add_phone_to_users.sql` and stays there. (Note for anyone
reading `.factory/profile.json` or `CLAUDE.md`: both record an older head — V12
and V11 respectively — and are stale. Nothing in this epic needs a migration.)

---

## 1. Reuse survey — what already exists

Everything this change needs has a precedent in the repo. Nothing new is
invented; four existing files are extended and one config block is corrected.

| Need | Existing precedent | Decision |
|---|---|---|
| Case-insensitive substring search in a JPA repository | `LeadRepository.searchLeads` — `LOWER(x) LIKE LOWER(CONCAT('%', :term, '%'))` | Same semantics, but expressed as a **derived query** (see §3.1) — `UserRepository` is entirely derived-query style today (`findByEmail`, `findByResetToken`) |
| Optional search query parameter on a list endpoint | `LeadController.getAllLeads(… @RequestParam(required = false) String search …)` | Same shape: `@RequestParam(required = false) String email` |
| Normalisation of a blank search term | `LeadService.getAllLeads` — `search != null && !search.isEmpty()` | Same idea, hardened to trim-then-check (the spec requires whitespace trimming, which `LeadService` does not do) |
| Search text field on a list page | `frontend/src/pages/agent/LeadList.tsx` — MUI `TextField` in a `Paper`, `value`/`onChange` state, term folded into the TanStack Query `queryKey` | Same pattern, with one deliberate divergence (§4.2 — the loading branch) |
| Axios list client with optional params | `frontend/src/api/leads.ts` / `adminApi.getAllUsers` already accepts a `params` object | Extend the existing `params` type with `email?: string` |

**Files extended (no new production files):**

- `backend/src/main/java/com/insurance/auth/repository/UserRepository.java`
- `backend/src/main/java/com/insurance/admin/service/UserManagementService.java`
- `backend/src/main/java/com/insurance/admin/controller/UserManagementController.java`
- `backend/src/main/resources/application.yml` (logging levels — §3.4)
- `backend/API_ENDPOINTS.md`
- `frontend/src/api/admin.ts`
- `frontend/src/pages/admin/UserManagement.tsx`

No new module, no new DTO, no new endpoint, no new entity.

---

## API contract

This is the canonical contract for the change. There are no sibling repos in
this estate, so it exists in exactly one place; if the estate later gains a
consumer, this block is the snippet to replicate byte-for-byte.

```
GET /api/admin/users
Authorization: Bearer <jwt>            (required)
Authority:     ROLE_ADMIN              (unchanged — @PreAuthorize("hasRole('ADMIN')"))

Query parameters
  email   string   optional   Case-insensitive substring ("contains") filter on the
                              user's email address. Leading/trailing whitespace is
                              trimmed. Absent, empty, or whitespace-only means
                              "no filter". `%` and `_` are matched literally.

200 OK
  [ { "id": 1,
      "name": "Admin User",
      "email": "admin@insurance.com",
      "role": "ADMIN",                       // "ADMIN" | "AGENT"
      "createdAt": "2026-01-01T00:00:00" } ]
  Ordered by id ascending. An empty array is a valid, successful response.

403 Forbidden   caller is authenticated but not ROLE_ADMIN,
                or caller is unauthenticated (see §3.5 — current entry-point
                behaviour; body carries no user data either way)

Not a breaking change: `email` is optional and additive. Callers that omit it —
including the frontend as it exists before task T3 — see the response they see
today, with the sole difference that ordering is now deterministic (§3.1).
```

---

## 2. Task-to-design map

Every task in `tasks.md` is implementable from the section named here alone.

| Task | Design sections |
|---|---|
| T1 #20 — backend filter + contract | §3.1–§3.5, § API contract, §6 |
| T2 #21 — backend tests | §5.1, §6 |
| T3 #22 — frontend filter control | §4.1–§4.3, § API contract, §6 |
| T4 #23 — frontend test harness + tests | §5.2, §6 |

---

## 3. Backend design (T1)

### 3.1 Repository — `UserRepository`

Add one derived query:

```java
List<User> findByEmailContainingIgnoreCaseOrderByIdAsc(String email);
```

**Why a derived query rather than an `@Query` mirroring `LeadRepository`:**

1. `UserRepository` contains only derived queries today; an `@Query` here would
   be the odd one out.
2. There is no hand-written JPQL, so there is no string to interpolate a term
   into. The spec requirement *"the filter term is treated as data, never as
   query structure"* is satisfied **structurally**, not by discipline. Spring
   Data binds the term as a JDBC parameter.
3. Spring Data JPA applies its default `EscapeCharacter` (`\`) to `Containing`
   parameters, so a term containing `%` or `_` is matched **literally**. The
   proposal lists literal-wildcard matching as a non-goal — meaning it is not
   required, not that it is forbidden — so getting it free is a bonus. Note for
   the reviewer: this is a deliberate, favourable divergence from
   `LeadRepository.searchLeads`, whose hand-written `LIKE` leaves `%` active.

**`OrderByIdAsc` and the unfiltered path.** The spec scenario *"Clearing the
filter restores the full list, exactly as before any filter was applied"* is
only testable if both paths order identically. `findAll()` today returns rows in
whatever order Postgres yields. The service therefore uses
`findAll(Sort.by(Sort.Direction.ASC, "id"))` on the unfiltered path so both
branches agree. This is the one behavioural change to existing unfiltered
output, and it strictly increases determinism.

**Index note (no action).** `idx_users_email` is a plain B-tree and cannot serve
a leading-wildcard `LIKE`; the filter is a sequential scan. At this table's size
(users, not leads) that is correct and cheap. A trigram index is not proposed —
it would need a migration and a `pg_trgm` extension for no measurable gain.

### 3.2 Service — `UserManagementService`

Replace `getAllUsers()` with `getAllUsers(String emailFilter)`. The controller
is its only caller, so no overload is kept.

```java
/** users.email is VARCHAR(255); no stored email can contain a longer substring. */
private static final int MAX_EMAIL_FILTER_LENGTH = 255;

@Transactional(readOnly = true)
public List<UserDto> getAllUsers(String emailFilter) {
    String term = emailFilter == null ? null : emailFilter.trim();

    List<User> users;
    if (term == null || term.isEmpty()) {
        users = userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    } else if (term.length() > MAX_EMAIL_FILTER_LENGTH) {
        users = List.of();                       // see below
    } else {
        users = userRepository.findByEmailContainingIgnoreCaseOrderByIdAsc(term);
    }

    return users.stream().map(this::mapToDto).collect(Collectors.toList());
}
```

Normalisation rules, stated exhaustively so T2 can assert each one:

| Input `email` | Term after normalisation | Repository call | Result |
|---|---|---|---|
| absent (`null`) | `null` | `findAll(Sort id ASC)` | all users |
| `""` | `""` | `findAll(Sort id ASC)` | all users |
| `"   "` | `""` | `findAll(Sort id ASC)` | all users |
| `"  agent@insurance.com  "` | `"agent@insurance.com"` | `findByEmailContainingIgnoreCaseOrderByIdAsc` | matching users |
| `"AGENT@Insurance.COM"` | unchanged | same | matches `agent@insurance.com` |
| `"' OR 1=1 --"` | unchanged | same | bound parameter → no match, 200 |
| 256+ characters | unchanged | **none** | `[]`, 200 |

**The over-long short-circuit preserves semantics, it does not special-case
them.** No email in a `VARCHAR(255)` column can contain a substring longer than
255 characters, so `[]` is the same answer the query would give — the guard just
avoids the round trip and makes the spec's *"oversized term is handled without
failure"* scenario deterministic and independent of driver limits.

**Audit and logging.** `getAllUsers` must not call `auditLogService` (it does
not today, and listing stays read-only), and must not add any `log.*` statement
mentioning the term — see §3.4. The `@Transactional(readOnly = true)` annotation
stays, which also guarantees the scenario's *"no user account is created,
modified or deleted"*.

### 3.3 Controller — `UserManagementController`

```java
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
@Operation(summary = "Get all users",
           description = "Optionally filtered by a case-insensitive substring of the email address.")
public ResponseEntity<List<UserDto>> getAllUsers(
        @Parameter(description = "Case-insensitive substring filter on email; blank means no filter")
        @RequestParam(required = false) String email) {
    return ResponseEntity.ok(userManagementService.getAllUsers(email));
}
```

**Do not add `@Size`, `@NotBlank` or `@Validated` to this parameter.** This is
the single most important implementation constraint in T1, and it is not
obvious:

`LeadExceptionHandler` is declared `@RestControllerAdvice(basePackages =
"com.insurance.leads")`. **No `@ControllerAdvice` covers `com.insurance.admin`.**
A bean-validation failure on a `@RequestParam` raises
`ConstraintViolationException`, which with no advice to catch it surfaces as
**HTTP 500** — directly violating the spec requirement that any term yields
"a result set or a validation error — never a server error". Length is therefore
handled in the service (§3.2), where it cannot throw. Introducing an admin-wide
exception handler is out of scope for this epic (§7).

`API_ENDPOINTS.md` line 179 is updated in the same PR:

```
| GET | `/api/admin/users` | ADMIN | Get all users; optional `?email=` case-insensitive substring filter |
```

### 3.4 PII in logs — a required `application.yml` change

The spec requires that applying a filter *"SHALL NOT write the supplied term
into application logs"*. Reading the current config, **it would**:

```yaml
# backend/src/main/resources/application.yml (current)
logging:
  level:
    org.springframework.web: DEBUG
    org.springframework.web.servlet: TRACE
    org.springframework.web.servlet.mvc.method.annotation: TRACE
    org.springframework.web.servlet.handler: TRACE
```

`DispatcherServlet.logRequest` emits the request line **including the query
string** at DEBUG. With these levels, `GET /api/admin/users?email=jane@acme.com`
writes `jane@acme.com` — an email address, PII — into the application log on
every keystroke-driven request.

**T1 therefore lowers these to `INFO`, env-overridable for local debugging:**

```yaml
logging:
  level:
    root: ${LOG_LEVEL:INFO}
    com.insurance: ${APP_LOG_LEVEL:DEBUG}
    org.springframework.security: ${SECURITY_LOG_LEVEL:INFO}
    org.springframework.web: ${WEB_LOG_LEVEL:INFO}
    org.springframework.web.servlet: ${WEB_LOG_LEVEL:INFO}
    org.springframework.web.servlet.mvc.method.annotation: ${WEB_LOG_LEVEL:INFO}
    org.springframework.web.servlet.handler: ${WEB_LOG_LEVEL:INFO}
    org.springframework.boot.autoconfigure: ${BOOT_LOG_LEVEL:INFO}
    org.springframework.context.annotation: ${BOOT_LOG_LEVEL:INFO}
```

Notes for the implementer and reviewer:

- `org.springframework.security` is dropped to INFO as well for volume, not for
  PII: `FilterChainProxy` logs the request URI via `UrlUtils.buildRequestUrl`,
  which passes a `null` query string — Spring Security does **not** leak the
  term. Keep it at DEBUG if you prefer; it does not affect the requirement.
- `com.insurance: DEBUG` stays. Our own code simply must not log the term.
- `spring.jpa.show-sql` is already `false` and Hibernate parameter-binding
  loggers are not enabled, so the bound value never reaches the log from JPA.
  Do not enable them.
- Side benefit, not scope creep: the pre-existing `/api/leads?search=` endpoint
  leaks lead names, phone numbers and emails through the same channel today.
  This change closes that too.

**Residual, flagged rather than fixed (see §7):** `frontend/nginx.conf` proxies
`/api/` and inherits nginx's default `combined` access-log format, whose
`$request` field contains the query string. That is a web-server access log, not
an application log, so it sits outside the spec's wording — but it is a real
second copy of the term. Mitigation if the approver wants it in scope: a
`log_format` without `$request`, or `access_log off` on the `/api/` location.

### 3.5 Authorization — unchanged, with one honest caveat

`@PreAuthorize("hasRole('ADMIN')")` already guards the endpoint and is not
touched. An authenticated `AGENT` gets **403** with no body, satisfying
*"rejected as forbidden … no indication of whether the term matched"*.

For the **unauthenticated** scenario the spec says "rejected as unauthorized".
`SecurityConfig` configures no `authenticationEntryPoint`, and with no
`formLogin`/`httpBasic` (Boot's `SecurityAutoConfiguration` backs off because a
custom `SecurityFilterChain` bean exists) Spring Security's default is
`Http403ForbiddenEntryPoint` — so an anonymous request currently receives **403,
not 401**.

**Decision: do not change this in epic #17.** The `authenticationEntryPoint` is
estate-wide; flipping it would change the response of all 70+ endpoints and
would newly trigger the frontend's global 401 interceptor
(`frontend/src/api/client.ts` clears the token and hard-redirects to `/login`),
which is a behaviour change no task here scopes or tests. The spec's substance —
*rejected, and nothing disclosed* — is met either way.

T2 therefore asserts **"status is 401 or 403, and the body contains no user
data"** and pins the exact code to whatever the running code returns, with a
comment pointing here. Raised for the G2 approver in §7.

---

## 4. Frontend design (T3)

### 4.1 API client — `frontend/src/api/admin.ts`

```ts
getAllUsers: async (params?: {
  page?: number;
  size?: number;
  email?: string;
}): Promise<User[]> => {
  const response = await apiClient.get<User[]>('/admin/users', { params });
  return response.data;
},
```

Axios omits `undefined` params, so `email: undefined` sends no `email` key at
all — matching the contract's "absent means no filter". `frontend/src/types/`
needs no change: the response type is still `User[]`, and `User` already carries
`email`.

### 4.2 Page — `frontend/src/pages/admin/UserManagement.tsx`

State and query:

```tsx
const [emailFilter, setEmailFilter] = useState('');
const appliedFilter = emailFilter.trim();

const { data, isLoading, error } = useQuery({
  queryKey: ['users', page, rowsPerPage, appliedFilter],
  queryFn: () =>
    adminApi.getAllUsers({
      page,
      size: rowsPerPage,
      email: appliedFilter || undefined,
    }),
  placeholderData: (previousData) => previousData,
});
```

- The term is **trimmed client-side before it enters the query key**, so
  `"a "` and `"a"` are one cache entry and a whitespace-only entry issues no
  filtered request. The server trims again (§3.2) — it does not trust this.
- `placeholderData: (previousData) => previousData` (TanStack Query v5's
  `keepPreviousData` idiom) keeps the previous rows on screen while the next
  request is in flight, so the table does not flash empty between keystrokes.
- No debounce, matching `LeadList`. One request per keystroke against an admin
  user table is acceptable and TanStack Query dedupes/caches.
- Filter changes reset `page` to 0, mirroring `LeadList`. This is cosmetic here
  (§4.3) but keeps the two pages consistent.

**Required restructure — the filter input must never unmount.** The page today
does:

```tsx
if (isLoading) return <LoadingSpinner message="Loading users..." />;
if (error)     return <ErrorAlert error={error as Error} />;
```

Each new `queryKey` is a cold cache entry, so a naive implementation would make
`isLoading` true on the first keystroke, replace the whole page with a spinner,
**unmount the TextField and drop keyboard focus after one character**. (This bug
already exists in `LeadList`; do not copy it.)

Render the heading and the filter `Paper` unconditionally, and put the loading
and error branches *inside* the results area:

```tsx
return (
  <Box>
    <Typography variant="h4" gutterBottom>User Management</Typography>

    <Paper sx={{ mb: 2, p: 2 }}>
      <TextField
        label="Filter by email"
        variant="outlined"
        size="small"
        value={emailFilter}
        onChange={(e) => { setEmailFilter(e.target.value); setPage(0); }}
        sx={{ minWidth: 300 }}
      />
    </Paper>

    {error ? (
      <ErrorAlert error={error as Error} />
    ) : isLoading ? (
      <LoadingSpinner message="Loading users..." />
    ) : (
      <TableContainer component={Paper}> … </TableContainer>
    )}

    {/* Edit dialog unchanged */}
  </Box>
);
```

Empty state, inside `<TableBody>` (the table has 6 columns):

```tsx
{data && data.length === 0 && (
  <TableRow>
    <TableCell colSpan={6} align="center">
      {appliedFilter
        ? `No users match the entered email.`
        : 'No users found.'}
    </TableCell>
  </TableRow>
)}
```

The spec requires an explicit message for the filtered case; the unfiltered
message is included so the branch is never silently blank.

### 4.3 What T3 must **not** do

- **Do not filter client-side.** The spec scenario *"the filter covers every
  account, not just the displayed rows"* requires the term to reach the server.
  `data.filter(u => u.email.includes(...))` fails this the moment the list is
  genuinely paged.
- **Do not fix pagination.** `page`/`size` are sent, the backend ignores them,
  and `TablePagination` renders `count={data?.length || 0}` over rows that are
  all already displayed. This is a pre-existing defect recorded in
  `proposal.md`, out of scope here (§7). Keep sending the params — the contract
  tolerates unknown params and removing them widens the diff for no benefit.

---

## 5. Test design

### 5.1 Backend (T2) — establishing `backend/src/test`

The repo has no `src/test` tree. `spring-boot-starter-test`, `spring-security-test`
and `h2` are already declared test-scoped dependencies.

**H2 is not usable for a JPA slice here.** `@DataJpaTest` builds the persistence
unit from every entity in `common/entity/`, and those use Postgres-only column
definitions — `jsonb` (`Lead`, `Product`, `AuditLog`, `VoiceSession`,
`LeadActivity`), `vector(1536)` (`VectorEmbedding`), `text[]` (`Product`).
Hibernate DDL generation against H2 fails on all three. Do not spend time
fighting this.

Three layers, in the order an implementer should build them:

**(a) Service unit tests — Mockito, no Spring context.**
`UserManagementServiceTest` with `@Mock UserRepository`, `@Mock PasswordEncoder`,
`@Mock AuditLogService`, `@InjectMocks UserManagementService`. Assert every row
of the §3.2 normalisation table: which repository method is called, with exactly
what argument, and that `findByEmailContaining…` is *never* called for the
null/empty/blank/over-long cases. Assert `verifyNoInteractions(auditLogService)`
for the PII/audit scenario.

**(b) Controller slice — `@WebMvcTest(UserManagementController.class)`.**
`SecurityConfig` is a plain `@Configuration` and is not picked up by
`@WebMvcTest`'s type filters, so import it explicitly to get both the filter
chain and `@EnableMethodSecurity`:

```java
@WebMvcTest(UserManagementController.class)
@Import(SecurityConfig.class)
class UserManagementControllerEmailFilterTest {
    @Autowired MockMvc mvc;
    @MockBean UserManagementService userManagementService;
    @MockBean CustomUserDetailsService userDetailsService;   // SecurityConfig dependency
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter; // SecurityConfig dependency
    …
}
```

Covers: `@WithMockUser(roles = "ADMIN")` → 200 and the raw `email` param reaches
the service unchanged (captured with `ArgumentCaptor`); absent param → `null`
reaches the service; `?email=` → `""` reaches the service;
`@WithMockUser(roles = "AGENT")` → 403 with an empty body and the service never
invoked; no authentication → rejected per §3.5 with no user data.

**(c) Repository/integration test — Testcontainers, real Postgres.**
This is the only layer that can prove the *match semantics* the spec is actually
about, so it is worth the two test-scoped dependencies:

```xml
<dependency><groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-testcontainers</artifactId><scope>test</scope></dependency>
<dependency><groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId><scope>test</scope></dependency>
<dependency><groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId><scope>test</scope></dependency>
```

Versions come from the Spring Boot 3.2.2 parent's dependency management — do not
pin them.

```java
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryEmailFilterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withInitScript("db/init/01-init-pgvector.sql");
    …
}
```

Why each piece:

- `pgvector/pgvector:pg16` is exactly the image `docker-compose.yml` runs.
- `V4__create_vector_embeddings_table.sql` uses `vector(1536)` but **no migration
  creates the extension** — `docker-compose.yml` mounts
  `backend/src/main/resources/db/init/` into `/docker-entrypoint-initdb.d`.
  `.withInitScript("db/init/01-init-pgvector.sql")` reproduces that from the
  classpath. Without it, Flyway fails at V4 and the test cannot boot.
- `ddl-auto=none` + `replace = NONE` lets the real Flyway migrations build the
  schema (`@DataJpaTest` includes `FlywayAutoConfiguration`), instead of
  `@DataJpaTest`'s default `create-drop` fighting them.
- `V10__seed_initial_data.sql` seeds `admin@insurance.com` and
  `agent@insurance.com` — the exact addresses the spec scenarios name. Insert
  any extra fixtures (e.g. `someone@other.example`) via `TestEntityManager`.

Scenarios this layer covers: full-address match, partial `insurance.com` match,
`AGENT@Insurance.COM` case-insensitivity, a term matching nothing, and — worth
an explicit test since §3.1 gives it for free — `%` matched literally.

Add `backend/src/test/resources/application.properties` with
`spring.ai.openai.api-key=test`; `spring.ai.openai.api-key` is the one
placeholder in `application.yml` with **no default** (line 53). Every other
property the slices touch (`app.jwt.secret`, `app.frontend.url`) already has one.

**If Docker is unavailable** in the QA environment, layer (c) is the only test
that cannot run; (a) and (b) still cover normalisation, authorization, binding
and the audit/PII assertions. Report that honestly rather than deleting the
class. Separately: `.factory/profile.json` → `gotchas` records that
`mvn test` can fail on Lombok + OpenJDK 21.0.5 Temurin with `TypeTag :: UNKNOWN`;
that signature is environmental — use the Docker build path and judge only this
delta.

### 5.2 Frontend (T4) — establishing the Vitest harness

Vitest is declared but has never run. Add as devDependencies: `jsdom`,
`@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`.

`vite.config.ts` gains a `test` block (the file needs
`/// <reference types="vitest" />` at the top for the config to typecheck):

```ts
test: {
  globals: true,
  environment: 'jsdom',
  setupFiles: './src/test/setup.ts',
},
```

`src/test/setup.ts` is one line: `import '@testing-library/jest-dom/vitest';`

Set `"test": "vitest run"` in `package.json` scripts (currently `"vitest"`, which
starts an interactive watcher and would hang CI). The profile's test command
already passes `--passWithNoTests`; leave that harmless once real specs exist.

`UserManagement.test.tsx`, colocated in `pages/admin/`, mocks the API module and
renders inside a `QueryClientProvider` (with `retry: false`):

```tsx
vi.mock('../../api/admin', () => ({ adminApi: { getAllUsers: vi.fn(), … } }));
```

Four assertions, one per scenario T4 serves:

1. Typing `insurance.com` calls `adminApi.getAllUsers` with
   `expect.objectContaining({ email: 'insurance.com' })` — **this is the
   "filtering is server-side, not client-side" assertion**, and it is the most
   important one in the file.
2. Typing `  agent@insurance.com  ` sends the trimmed term.
3. Clearing the input issues a request with `email: undefined` and re-renders
   the full list.
4. A mocked `[]` response while a filter is applied renders the text
   "No users match the entered email."

`npm run lint` runs with `--max-warnings 0`; note that the repo currently has
**no ESLint configuration file at all** (`eslint .` under ESLint 8 needs
`.eslintrc*`), so that command does not work today. That is pre-existing and out
of scope — do not add an ESLint config as part of T4; just do not be surprised
by it.

---

## 6. Failure modes and edge cases, per spec scenario

| Spec scenario | Handled by | Failure mode guarded |
|---|---|---|
| Full email address matches one account | §3.1 derived query | — |
| Partial term matches every account containing it | §3.1 `Containing` | Someone "optimising" to `findByEmail` (exact) — the derived name must stay `Containing` |
| Matching ignores case | §3.1 `IgnoreCase` | Postgres `LIKE` is case-sensitive; omitting `IgnoreCase` silently breaks this |
| Surrounding whitespace is ignored | §3.2 trim, §4.2 client trim | A trailing space from copy/paste yielding zero results |
| Clearing the filter restores the full list | §3.2 blank→`findAll`, §4.2 `undefined` param | Axios sending `email=` for an empty string — avoided by `|| undefined`; and non-deterministic ordering — avoided by §3.1 |
| A term matching nothing yields an explicit empty result | §3.2 (`[]`, 200), §4.2 empty-state row | Returning 404, or rendering a bare empty table |
| The filter covers every account, not just displayed rows | §3.1 data-layer filter, §4.3 | Client-side `Array.filter` over a fetched page |
| An agent cannot read the user list by filtering it | §3.5 unchanged `@PreAuthorize` | Relaxing to `hasAnyRole('ADMIN','AGENT')` while "improving" the endpoint |
| An unauthenticated caller cannot read the user list | §3.5 (403 today; caveat flagged) | — |
| SQL metacharacters matched literally | §3.1 bound parameter + escape | Hand-built JPQL/native SQL string concatenation |
| Oversized term handled without failure | §3.2 length guard, §3.3 no bean validation | `@Size` + `@Validated` → `ConstraintViolationException` → **500**, because no advice covers `com.insurance.admin` |
| Filter term is not recorded as PII | §3.4 logging levels, §3.2 no audit call | `DispatcherServlet` TRACE logging the query string |

---

## 7. Rollout, rollback, and decisions flagged for gate G2

**Order** (as `tasks.md` sets it): T1 → then T2 and T3 in parallel → T4.
There is no `staging` branch in this repo (`branches.staging: null`), so task PRs
base on `main` and each merge is a deploy.

**Every intermediate state is releasable.** T1 is purely additive: an optional
query parameter plus a logging-level change. The existing frontend, which never
sends `email`, behaves identically after T1 — the only observable difference is
that the unfiltered list is now ordered by id.

**Rollback** is a plain revert of the offending PR; there is no migration, no
data backfill and no feature flag to unwind. Reverting T3 alone leaves T1's
unused parameter in place, which is harmless. Post-deploy check is the standard
`curl -fsS http://localhost:8080/actuator/health`; nothing in this change touches
`db/migration/`, so the profile's "red health check after a migration" note does
not apply.

**Three decisions the G2 approver should look at explicitly:**

1. **`application.yml` logging levels are lowered (§3.4).** This is the only
   change in the epic that touches shared configuration. Without it the spec's
   PII requirement cannot be met, because Spring's `DispatcherServlet` logs the
   query string at DEBUG. It also silently fixes the same leak on
   `/api/leads?search=`. Say so if you would rather keep the verbose levels and
   solve PII another way.
2. **Unauthenticated callers get 403, not 401 (§3.5).** The spec says
   "unauthorized". Correcting it means adding an estate-wide
   `authenticationEntryPoint`, changing every endpoint's anonymous response and
   activating the frontend's global 401 logout redirect. This design keeps the
   current behaviour and has T2 assert *refused + nothing disclosed*. If you want
   the true 401, it should be its own issue.
3. **T2 adds Testcontainers (§5.1c).** Three test-scoped dependencies and a
   Docker requirement for one test class. The alternative — H2 — is genuinely
   unavailable here because the entity model uses `jsonb`, `vector(1536)` and
   `text[]`; without Testcontainers the match semantics at the heart of the spec
   would only ever be asserted as "we called the repository with the right
   string".

**Pre-existing defects observed and deliberately not fixed** (each deserves its
own issue): non-functional pagination on `/admin/users`; no `@ControllerAdvice`
covering `com.insurance.admin`, so its `RuntimeException`s surface as 500;
`UserManagementController.extractUserId` returns a hard-coded `1L` placeholder,
so every admin audit entry is attributed to user 1 — untouched here because
listing writes no audit entry; nginx access logs record query strings (§3.4);
the keystroke focus-loss bug in `LeadList.tsx` (§4.2), avoided in
`UserManagement.tsx` rather than fixed at source; and `npm run lint` cannot run
because no ESLint config file exists.
