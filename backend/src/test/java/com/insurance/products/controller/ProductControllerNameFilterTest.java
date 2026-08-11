package com.insurance.products.controller;

import com.insurance.auth.config.CustomUserDetailsService;
import com.insurance.auth.config.JwtAuthenticationFilter;
import com.insurance.auth.config.SecurityConfig;
import com.insurance.products.dto.ProductDto;
import com.insurance.products.service.ProductService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Binding, branching and authorization for {@code GET /api/products?name=}.
 *
 * <p>Three things are asserted here and nowhere else: that the raw query parameter reaches
 * the service untouched (no bean validation intercepting it, no rewriting at the edge),
 * that a request carrying no name term still takes the pre-change {@code getAllProducts()}
 * branch — which is what makes "existing callers are unaffected" true by construction —
 * and that an unauthenticated caller is refused before the service is ever consulted, so
 * nothing can leak about whether the term matched.
 *
 * <p>{@link SecurityConfig} is imported explicitly: it is a plain {@code @Configuration}
 * and {@code @WebMvcTest}'s type filters do not pick it up, so without the import there
 * would be no filter chain at all and every authorization assertion below would pass
 * vacuously.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerNameFilterTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ProductService productService;

    /** Required by {@link SecurityConfig}'s {@code DaoAuthenticationProvider}. */
    @MockBean
    private CustomUserDetailsService userDetailsService;

    /** Required by {@link SecurityConfig}'s filter chain. */
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final ProductDto TERM_LIFE = ProductDto.builder()
        .id(1L).categoryId(10L).categoryName("Life").name("Term Life Secure")
        .insurer("LifeGuard").planType("TERM")
        .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
        .build();

    private static final ProductDto FAMILY_HEALTH = ProductDto.builder()
        .id(2L).categoryId(20L).categoryName("Health").name("Family Health Plus")
        .insurer("HealthFirst").planType("HEALTH")
        .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
        .build();

    @BeforeEach
    void passJwtFilterThrough() throws Exception {
        // A bare Mockito mock of a filter swallows the request: OncePerRequestFilter.doFilter
        // is stubbed to do nothing, so the chain stops and every request would return an
        // empty 200. Authentication itself comes from @WithMockUser / @WithAnonymousUser.
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                .doFilter(invocation.getArgument(0, ServletRequest.class),
                          invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private void stubFilterProducts(List<ProductDto> result) {
        when(productService.filterProducts(nullable(Long.class), nullable(String.class),
            nullable(String.class), nullable(String.class))).thenReturn(result);
    }

    /** The name term {@code filterProducts} was called with, for a single request. */
    private String capturedName() {
        ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        verify(productService).filterProducts(nullable(Long.class), nullable(String.class),
            nullable(String.class), name.capture());
        return name.getValue();
    }

    @Nested
    @DisplayName("The term reaches the service unchanged")
    class ParameterBinding {

        /**
         * Performs the request against a literal URI. {@code get(String)} would treat its
         * argument as a template and re-encode it, so the percent-escapes below would
         * arrive doubly encoded rather than as the characters they stand for.
         */
        private String capturedFilter(String queryString) throws Exception {
            stubFilterProducts(List.of());

            mvc.perform(get(URI.create("/api/products" + queryString)).with(agent()))
                .andExpect(status().isOk());

            return capturedName();
        }

        @Test
        @DisplayName("a term binds verbatim")
        void termBindsVerbatim() throws Exception {
            assertThat(capturedFilter("?name=Term%20Life")).isEqualTo("Term Life");
        }

        @Test
        @DisplayName("an empty parameter binds to the empty string, not null — and still filters")
        void emptyParameterBindsToEmptyString() throws Exception {
            // Non-null, so the controller takes the filter branch; the service normalises the
            // blank away. This is the "?name=" the UI sends the moment the box is cleared
            // before the query key reverts.
            assertThat(capturedFilter("?name=")).isEmpty();
        }

        @Test
        @DisplayName("surrounding whitespace survives binding — trimming is the service's job")
        void whitespaceIsNotStrippedByBinding() throws Exception {
            // %20 rather than '+': the controller must not depend on either encoding.
            assertThat(capturedFilter("?name=%20%20Term%20Life%20%20")).isEqualTo("  Term Life  ");
        }

        @Test
        @DisplayName("SQL metacharacters are not rejected, escaped or rewritten at the edge")
        void sqlMetacharactersReachTheServiceVerbatim() throws Exception {
            assertThat(capturedFilter("?name=%27%20OR%201%3D1%20--")).isEqualTo("' OR 1=1 --");
        }

        @Test
        @DisplayName("LIKE wildcards reach the service unescaped")
        void likeWildcardsReachTheServiceUnescaped() throws Exception {
            assertThat(capturedFilter("?name=50%25_cover")).isEqualTo("50%_cover");
        }

        @Test
        @DisplayName("an oversized term is accepted, not answered with a server error")
        void oversizedTermIsAcceptedNotRejected() throws Exception {
            String term = "a".repeat(4000);

            // The absence of @Size/@Validated on the parameter is load-bearing: no
            // @ControllerAdvice covers com.insurance.products, so a ConstraintViolationException
            // would surface as HTTP 500 and break the spec's "never a server error".
            assertThat(capturedFilter("?name=" + term)).isEqualTo(term);
        }
    }

    @Nested
    @DisplayName("Which branch a request takes")
    class Branching {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a request with no parameters at all takes the untouched getAllProducts path")
        void noParametersTakesTheUnfilteredPath() throws Exception {
            when(productService.getAllProducts()).thenReturn(List.of(TERM_LIFE, FAMILY_HEALTH));

            mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

            // "Existing callers are unaffected" is true by construction only while this
            // branch survives: a no-name request must execute today's exact code path.
            verify(productService).getAllProducts();
            verify(productService, never()).filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("the pre-existing filters still route to filterProducts with a null name")
        void preExistingFiltersStillRouteToFilterProducts() throws Exception {
            stubFilterProducts(List.of(TERM_LIFE));

            mvc.perform(get("/api/products").param("categoryId", "10")
                    .param("insurer", "LifeGuard").param("planType", "TERM"))
                .andExpect(status().isOk());

            verify(productService).filterProducts(10L, "LifeGuard", "TERM", null);
        }

        @Test
        @WithMockUser(roles = "AGENT")
        @DisplayName("the agent browser's category-only request is unchanged")
        void agentBrowserRequestIsUnchanged() throws Exception {
            // ProductBrowser.tsx calls this endpoint with categoryId and nothing else.
            stubFilterProducts(List.of(TERM_LIFE));

            mvc.perform(get("/api/products").param("categoryId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Term Life Secure"));

            verify(productService).filterProducts(10L, null, null, null);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a name term composes with a category on the way through")
        void nameComposesWithCategory() throws Exception {
            stubFilterProducts(List.of(TERM_LIFE));

            mvc.perform(get("/api/products").param("categoryId", "10").param("name", "Life"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].categoryId").value(10));

            verify(productService).filterProducts(10L, null, null, "Life");
        }
    }

    @Nested
    @DisplayName("The response a filtered request gets")
    class Responses {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a matching filter returns 200 and the matching products only")
        void matchingFilterReturnsMatchingProducts() throws Exception {
            stubFilterProducts(List.of(TERM_LIFE));

            mvc.perform(get("/api/products").param("name", "Term Life"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Term Life Secure"))
                .andExpect(jsonPath("$[0].categoryName").value("Life"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a term matching nothing is a successful empty array, not a 404")
        void noMatchIsAnEmptyArray() throws Exception {
            stubFilterProducts(List.of());

            mvc.perform(get("/api/products").param("name", "no product is called this"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("an oversized term answers 200 with an empty array, never a 5xx")
        void oversizedTermAnswersEmptyNotServerError() throws Exception {
            // Mirrors the service's short-circuit: design §7 decision 3 chose [] over 400.
            stubFilterProducts(List.of());

            mvc.perform(get("/api/products").param("name", "a".repeat(4000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("the response order is the service's order, unshuffled by serialisation")
        void responseOrderIsTheServiceOrder() throws Exception {
            stubFilterProducts(List.of(TERM_LIFE, FAMILY_HEALTH));

            mvc.perform(get("/api/products").param("name", "e"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
        }
    }

    @Nested
    @DisplayName("Filtering does not change who may read the catalogue")
    class Authorization {

        @Test
        @WithMockUser(roles = "AGENT")
        @DisplayName("an authenticated AGENT may still list products")
        void agentMayStillList() throws Exception {
            // Pins the "do not add @PreAuthorize to this controller" decision: the agent
            // product browser reads this endpoint and must keep working.
            stubFilterProducts(List.of(TERM_LIFE));

            mvc.perform(get("/api/products").param("name", "Term Life"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("an unauthenticated caller filtering the catalogue is refused and learns nothing")
        void anonymousIsRefused() throws Exception {
            MvcResult result = mvc.perform(get("/api/products").param("name", "Term Life")).andReturn();

            // 401 or 403: SecurityConfig registers no authenticationEntryPoint, so Spring
            // Security's default Http403ForbiddenEntryPoint answers anonymous requests with
            // 403 today. The spec's substance — refused, nothing disclosed — holds either
            // way; tighten this to 401 if an entry point is ever added.
            assertThat(result.getResponse().getStatus()).isIn(401, 403);
            assertThat(result.getResponse().getContentAsString()).doesNotContain("Term Life Secure");

            // The refusal happens before any lookup, so the response cannot differ between a
            // term that matches and one that does not.
            verifyNoInteractions(productService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("a matching and a non-matching term are refused identically")
        void anonymousLearnsNothingFromComparingTwoTerms() throws Exception {
            MvcResult matching = mvc.perform(
                get("/api/products").param("name", "Term Life")).andReturn();
            MvcResult notMatching = mvc.perform(
                get("/api/products").param("name", "no product is called this")).andReturn();

            assertThat(matching.getResponse().getStatus())
                .isEqualTo(notMatching.getResponse().getStatus());
            assertThat(matching.getResponse().getContentAsString())
                .isEqualTo(notMatching.getResponse().getContentAsString());

            verifyNoInteractions(productService);
        }
    }

    @Nested
    @DisplayName("Filtering grants no write access")
    class NoWrites {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a filtered listing invokes no create, update or delete on the service")
        void filteredListingInvokesNoWrite() throws Exception {
            stubFilterProducts(List.of(TERM_LIFE));

            mvc.perform(get("/api/products").param("name", "Term Life")).andExpect(status().isOk());

            verify(productService, never()).createProduct(any());
            verify(productService, never()).updateProduct(any(), any());
            verify(productService, never()).deleteProduct(any());
        }

        @Test
        @WithMockUser(roles = "AGENT")
        @DisplayName("a name term cannot be smuggled into a write via another verb")
        void writeVerbsAreNotReachableThroughTheFilter() throws Exception {
            // GET is the only mapping on the collection path; the filter cannot be turned
            // into a mutation by changing the method.
            mvc.perform(delete("/api/products").param("name", "Term Life"))
                .andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(productService);
        }
    }

    /**
     * An AGENT-authenticated request, for the binding tests — the least-privileged caller
     * the endpoint accepts, so binding is never accidentally asserted under admin-only
     * conditions.
     */
    private static RequestPostProcessor agent() {
        return user("agent@insurance.com").roles("AGENT");
    }
}
