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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Binding, branching and authorization for {@code GET /api/products?name=}.
 *
 * <p>Three things are asserted here and nowhere else: that the raw query parameter reaches
 * the service untouched (no bean validation intercepting it, no rewriting at the edge);
 * that a request carrying no {@code name} still takes the pre-change code path, which is
 * what makes "existing callers are unaffected" true by construction; and that the endpoint's
 * authorization is exactly what it was — any authenticated role may list, anonymous callers
 * are refused before any lookup happens.
 *
 * <p>{@link SecurityConfig} is imported explicitly: it is a plain {@code @Configuration} and
 * {@code @WebMvcTest}'s type filters do not pick it up, so without the import there would be
 * no filter chain at all and every authorization assertion below would pass vacuously.
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
        .id(1L).categoryId(10L).categoryName("Life").name("Zenith Term Life Secure")
        .insurer("LifeGuard Insurance Co.").planType("TERM").tags(List.of("term"))
        .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0)).updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
        .build();

    private static final ProductDto FAMILY_TERM = ProductDto.builder()
        .id(2L).categoryId(20L).categoryName("Health").name("Zenith Family Term Cover")
        .insurer("HealthFirst Insurance").planType("TERM").tags(List.of("family"))
        .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0)).updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
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

    /** The name argument {@code filterProducts} was called with, for a single invocation. */
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
        private String capturedFilterFor(String queryString) throws Exception {
            mvc.perform(get(URI.create("/api/products" + queryString))).andExpect(status().isOk());
            return capturedName();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("an empty parameter binds to the empty string, not null")
        void emptyParameterBindsToEmptyString() throws Exception {
            when(productService.filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class)))
                .thenReturn(List.of(TERM_LIFE, FAMILY_TERM));

            // Present-but-empty still takes the filter branch; normalising it back to
            // "no filter" is the service's job, and the whole catalogue comes back.
            assertThat(capturedFilterFor("?name=")).isEmpty();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("surrounding whitespace survives binding — trimming is the service's job")
        void whitespaceIsNotStrippedByBinding() throws Exception {
            when(productService.filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class))).thenReturn(List.of(TERM_LIFE));

            // %20 rather than '+': the controller must not depend on either encoding.
            assertThat(capturedFilterFor("?name=%20%20Zenith%20Term%20Life%20Secure%20%20"))
                .isEqualTo("  Zenith Term Life Secure  ");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a whitespace-only term is accepted, not rejected")
        void whitespaceOnlyTermIsAccepted() throws Exception {
            when(productService.filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class)))
                .thenReturn(List.of(TERM_LIFE, FAMILY_TERM));

            assertThat(capturedFilterFor("?name=%20%20%20")).isEqualTo("   ");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("SQL metacharacters are not rejected, escaped or rewritten at the edge")
        void sqlMetacharactersReachTheServiceVerbatim() throws Exception {
            when(productService.filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class))).thenReturn(List.of());

            assertThat(capturedFilterFor("?name=%27%20OR%201%3D1%20--")).isEqualTo("' OR 1=1 --");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a statement-terminating payload is bound as a term, not as a second statement")
        void statementTerminatingPayloadReachesTheServiceVerbatim() throws Exception {
            when(productService.filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class))).thenReturn(List.of());

            assertThat(capturedFilterFor("?name=%27%3B%20DROP%20TABLE%20products%3B%20--"))
                .isEqualTo("'; DROP TABLE products; --");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("LIKE wildcards reach the service unescaped")
        void likeWildcardsReachTheServiceUnescaped() throws Exception {
            when(productService.filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class))).thenReturn(List.of());

            assertThat(capturedFilterFor("?name=Zen_th%25Plan")).isEqualTo("Zen_th%Plan");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("the name term is passed alongside the existing filters, none of them dropped")
        void nameIsPassedAlongsideTheExistingFilters() throws Exception {
            when(productService.filterProducts(10L, "LifeGuard Insurance Co.", "TERM", "Zenith"))
                .thenReturn(List.of(TERM_LIFE));

            mvc.perform(get("/api/products")
                    .param("categoryId", "10")
                    .param("insurer", "LifeGuard Insurance Co.")
                    .param("planType", "TERM")
                    .param("name", "Zenith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Zenith Term Life Secure"));

            verify(productService).filterProducts(10L, "LifeGuard Insurance Co.", "TERM", "Zenith");
        }
    }

    @Nested
    @DisplayName("The branch that keeps existing callers unaffected")
    class ExistingCallers {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a request with no parameters at all takes the unfiltered path")
        void noParametersTakesTheUnfilteredPath() throws Exception {
            when(productService.getAllProducts()).thenReturn(List.of(TERM_LIFE, FAMILY_TERM));

            mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

            // Byte for byte the pre-change code path: filterProducts is not consulted, so
            // nothing about the name filter can alter what today's callers receive.
            verify(productService).getAllProducts();
            verify(productService, never()).filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class));
        }

        @Test
        @WithMockUser(roles = "AGENT")
        @DisplayName("the agent product browser's category-only request is unchanged")
        void categoryOnlyRequestPassesANullName() throws Exception {
            // ProductBrowser.tsx sends categoryId and nothing else; it must keep reaching
            // filterProducts with a null name rather than acquiring a filter it never asked for.
            when(productService.filterProducts(10L, null, null, null)).thenReturn(List.of(TERM_LIFE));

            mvc.perform(get("/api/products").param("categoryId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

            assertThat(capturedName()).isNull();
        }
    }

    @Nested
    @DisplayName("The response a listing caller gets")
    class Responses {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a matching term returns 200 and the matching products only")
        void matchingTermReturnsMatchingProducts() throws Exception {
            when(productService.filterProducts(null, null, null, "Zenith Term"))
                .thenReturn(List.of(TERM_LIFE));

            mvc.perform(get("/api/products").param("name", "Zenith Term"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Zenith Term Life Secure"))
                .andExpect(jsonPath("$[0].categoryName").value("Life"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a term matching nothing is a successful empty array, not a 404")
        void noMatchIsAnEmptyArray() throws Exception {
            when(productService.filterProducts(null, null, null, "no product is called this"))
                .thenReturn(List.of());

            mvc.perform(get("/api/products").param("name", "no product is called this"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("an oversized term is answered 200 [], never a server error")
        void oversizedTermIsNotAServerError() throws Exception {
            String term = "a".repeat(4000);
            when(productService.filterProducts(null, null, null, term)).thenReturn(List.of());

            // The absence of @Size/@Validated on the parameter is load-bearing: no
            // @ControllerAdvice covers com.insurance.products, so a ConstraintViolationException
            // would surface as HTTP 500 and break the spec's "never a server error".
            mvc.perform(get(URI.create("/api/products?name=" + term)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

            assertThat(capturedName()).isEqualTo(term);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("several matches are all returned, in the order the service supplied")
        void severalMatchesAreAllReturnedInOrder() throws Exception {
            when(productService.filterProducts(null, null, null, "Zenith"))
                .thenReturn(List.of(TERM_LIFE, FAMILY_TERM));

            mvc.perform(get("/api/products").param("name", "Zenith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
        }
    }

    @Nested
    @DisplayName("Filtering does not change who may read the catalogue")
    class Authorization {

        @Test
        @WithMockUser(roles = "AGENT")
        @DisplayName("an authenticated AGENT may still list products with a name term")
        void agentMayStillList() throws Exception {
            when(productService.filterProducts(null, null, null, "Zenith"))
                .thenReturn(List.of(TERM_LIFE, FAMILY_TERM));

            // Pins design.md §3.3's "do not add @PreAuthorize": the catalogue is readable by
            // any signed-in role, and the agent product browser depends on that.
            mvc.perform(get("/api/products").param("name", "Zenith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("an unauthenticated caller filtering the catalogue is refused and learns nothing")
        void anonymousIsRefused() throws Exception {
            MvcResult result = mvc.perform(get("/api/products").param("name", "Zenith")).andReturn();

            // 401 or 403: SecurityConfig registers no authenticationEntryPoint, so Spring
            // Security's default Http403ForbiddenEntryPoint answers anonymous requests with
            // 403 today. The spec's substance — refused, nothing disclosed — holds either way.
            assertThat(result.getResponse().getStatus()).isIn(401, 403);
            assertThat(result.getResponse().getContentAsString()).doesNotContain("Zenith");

            // The refusal happens before any lookup, so the response cannot differ between
            // a term that matches and one that does not.
            verifyNoInteractions(productService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("a matching and a non-matching term are refused indistinguishably")
        void anonymousRefusalIsIndistinguishable() throws Exception {
            MvcResult matching = mvc.perform(get("/api/products").param("name", "Zenith")).andReturn();
            MvcResult missing = mvc.perform(get("/api/products").param("name", "nothing-matches-this"))
                .andReturn();

            assertThat(matching.getResponse().getStatus()).isEqualTo(missing.getResponse().getStatus());
            assertThat(matching.getResponse().getContentAsString())
                .isEqualTo(missing.getResponse().getContentAsString());

            verifyNoInteractions(productService);
        }
    }

    @Nested
    @DisplayName("Filtering grants no write access")
    class NoWrites {

        @Test
        @WithMockUser(roles = "AGENT")
        @DisplayName("a filtered listing invokes no create, update or delete")
        void filteredListingInvokesNoWrite() throws Exception {
            when(productService.filterProducts(null, null, null, "Zenith"))
                .thenReturn(List.of(TERM_LIFE));

            mvc.perform(get("/api/products").param("name", "Zenith")).andExpect(status().isOk());

            verify(productService, never()).createProduct(any());
            verify(productService, never()).updateProduct(any(), any());
            verify(productService, never()).deleteProduct(any());
        }
    }
}
