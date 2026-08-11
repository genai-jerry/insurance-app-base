package com.insurance.products.controller;

import com.insurance.auth.config.CustomUserDetailsService;
import com.insurance.auth.config.JwtAuthenticationFilter;
import com.insurance.auth.config.SecurityConfig;
import com.insurance.products.dto.CreateProductRequest;
import com.insurance.products.dto.ProductDto;
import com.insurance.products.dto.UpdateProductRequest;
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
import static org.mockito.ArgumentMatchers.anyLong;
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
 * Binding and authorization for {@code GET /api/products?name=}.
 *
 * <p>Three things are asserted here and nowhere else: that the raw query parameter reaches the
 * service untouched (no bean validation intercepting it, no rewriting), that a request without
 * a name still takes the pre-change {@code getAllProducts()} branch, and that an unauthenticated
 * caller is refused without the service ever being consulted — so nothing can leak about whether
 * the term matched.
 *
 * <p>{@link SecurityConfig} is imported explicitly: it is a plain {@code @Configuration} and
 * {@code @WebMvcTest}'s type filters do not pick it up, so without the import there would be no
 * filter chain at all and every authorization assertion below would pass vacuously.
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
        .id(1L).categoryId(10L).categoryName("Life Insurance")
        .name("Term Life Secure").insurer("LifeGuard Insurance Co.").planType("TERM")
        .tags(List.of("life-cover"))
        .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
        .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
        .build();

    private static final ProductDto FAMILY_HEALTH = ProductDto.builder()
        .id(2L).categoryId(20L).categoryName("Health Insurance")
        .name("FamilyCare Health Plus").insurer("HealthFirst Insurance").planType("HEALTH")
        .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
        .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
        .build();

    @BeforeEach
    void passJwtFilterThrough() throws Exception {
        // A bare Mockito mock of a filter swallows the request: OncePerRequestFilter.doFilter is
        // stubbed to do nothing, so the chain stops and every request would return an empty 200.
        // Authentication itself comes from @WithMockUser / @WithAnonymousUser.
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                .doFilter(invocation.getArgument(0, ServletRequest.class),
                          invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Nested
    @DisplayName("The term reaches the service unchanged")
    class ParameterBinding {

        /**
         * Performs the request against a literal URI. {@code get(String)} would treat its
         * argument as a template and re-encode it, so the percent-escapes below would arrive
         * doubly encoded rather than as the characters they stand for.
         */
        private String capturedName(String queryString) throws Exception {
            mvc.perform(get(URI.create("/api/products" + queryString))).andExpect(status().isOk());

            ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
            verify(productService).filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), name.capture());
            return name.getValue();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("an empty parameter binds to the empty string and still takes the filter branch")
        void emptyParameterBindsToEmptyString() throws Exception {
            when(productService.filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class))).thenReturn(List.of(TERM_LIFE, FAMILY_HEALTH));

            assertThat(capturedName("?name=")).isEmpty();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("surrounding whitespace survives binding — trimming is the service's job")
        void whitespaceIsNotStrippedByBinding() throws Exception {
            when(productService.filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class))).thenReturn(List.of(TERM_LIFE));

            // %20 rather than '+': the controller must not depend on either encoding.
            assertThat(capturedName("?name=%20%20Term%20Life%20%20")).isEqualTo("  Term Life  ");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("SQL metacharacters are not rejected, escaped or rewritten at the edge")
        void sqlMetacharactersReachTheServiceVerbatim() throws Exception {
            when(productService.filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class))).thenReturn(List.of());

            assertThat(capturedName("?name=%27%20OR%201%3D1%20--")).isEqualTo("' OR 1=1 --");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("an oversized term is accepted, not answered with a server error")
        void oversizedTermIsAcceptedNotRejected() throws Exception {
            // The service short-circuits an over-long term to an empty list; the controller's job
            // is only to let it through.
            when(productService.filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class))).thenReturn(List.of());

            String term = "a".repeat(4000);

            // The absence of @Size/@Validated on the parameter is load-bearing: no
            // @ControllerAdvice covers com.insurance.products, so a ConstraintViolationException
            // would surface as HTTP 500 and break the spec's "never a server error".
            assertThat(capturedName("?name=" + term)).isEqualTo(term);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("the name composes with the existing filters, each bound to its own parameter")
        void nameComposesWithTheExistingFilters() throws Exception {
            when(productService.filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class))).thenReturn(List.of(TERM_LIFE));

            mvc.perform(get("/api/products")
                    .param("categoryId", "10")
                    .param("insurer", "LifeGuard Insurance Co.")
                    .param("planType", "TERM")
                    .param("name", "Term Life"))
                .andExpect(status().isOk());

            verify(productService).filterProducts(10L, "LifeGuard Insurance Co.", "TERM", "Term Life");
        }
    }

    @Nested
    @DisplayName("Existing callers are unaffected")
    class ExistingCallers {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a request with no parameters still takes the unfiltered branch")
        void noParametersStillListsEverything() throws Exception {
            when(productService.getAllProducts()).thenReturn(List.of(TERM_LIFE, FAMILY_HEALTH));

            mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

            verify(productService).getAllProducts();
            verify(productService, never()).filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class));
        }

        @Test
        @WithMockUser(roles = "AGENT")
        @DisplayName("the agent product browser's category-only request passes a null name")
        void categoryOnlyRequestPassesNullName() throws Exception {
            // ProductBrowser.tsx calls GET /api/products?categoryId=... and nothing else; the new
            // parameter must arrive as null so the predicate is skipped entirely.
            when(productService.filterProducts(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class))).thenReturn(List.of(TERM_LIFE));

            mvc.perform(get("/api/products").param("categoryId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

            verify(productService).filterProducts(10L, null, null, null);
        }
    }

    @Nested
    @DisplayName("The response an authorised caller gets")
    class Responses {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a matching filter returns 200 and the matching products only")
        void matchingFilterReturnsMatchingProducts() throws Exception {
            when(productService.filterProducts(null, null, null, "Term Life"))
                .thenReturn(List.of(TERM_LIFE));

            mvc.perform(get("/api/products").param("name", "Term Life"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Term Life Secure"))
                .andExpect(jsonPath("$[0].categoryName").value("Life Insurance"))
                // Entities are never returned from controllers: the DTO has no such property.
                .andExpect(jsonPath("$[0].category").doesNotExist());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a term matching nothing is a successful empty array, not a 404")
        void noMatchIsAnEmptyArray() throws Exception {
            when(productService.filterProducts(null, null, null, "no such product"))
                .thenReturn(List.of());

            mvc.perform(get("/api/products").param("name", "no such product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Filtering does not change who may read the catalogue")
    class Authorization {

        @Test
        @WithMockUser(roles = "AGENT")
        @DisplayName("an authenticated AGENT may still list products, filtered or not")
        void agentMayStillList() throws Exception {
            // Pins the design decision not to add @PreAuthorize("hasRole('ADMIN')") here: the
            // agent-facing product browser calls this same endpoint.
            when(productService.getAllProducts()).thenReturn(List.of(TERM_LIFE, FAMILY_HEALTH));
            when(productService.filterProducts(null, null, null, "Term Life")).thenReturn(List.of(TERM_LIFE));

            mvc.perform(get("/api/products")).andExpect(status().isOk());
            mvc.perform(get("/api/products").param("name", "Term Life"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("an unauthenticated caller filtering the catalogue is refused and learns nothing")
        void anonymousIsRefused() throws Exception {
            MvcResult matching = mvc.perform(get("/api/products").param("name", "Term Life")).andReturn();
            MvcResult notMatching = mvc.perform(get("/api/products").param("name", "no such product"))
                .andReturn();

            // 401 or 403: SecurityConfig registers no authenticationEntryPoint, so Spring
            // Security's default Http403ForbiddenEntryPoint answers anonymous requests with 403
            // today. The spec's substance — refused, nothing disclosed — holds either way.
            assertThat(matching.getResponse().getStatus()).isIn(401, 403);
            assertThat(notMatching.getResponse().getStatus()).isIn(401, 403);

            assertThat(matching.getResponse().getContentAsString())
                .doesNotContain("Term Life Secure", "FamilyCare Health Plus");

            // The refusal happens before any lookup, so a term that matches and one that does not
            // are indistinguishable from outside.
            assertThat(notMatching.getResponse().getStatus())
                .isEqualTo(matching.getResponse().getStatus());
            assertThat(notMatching.getResponse().getContentAsString())
                .isEqualTo(matching.getResponse().getContentAsString());

            verifyNoInteractions(productService);
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("filtering grants no write access — no create, update or delete is reached")
    void filteringPerformsNoWrite() throws Exception {
        when(productService.filterProducts(nullable(Long.class), nullable(String.class),
            nullable(String.class), nullable(String.class))).thenReturn(List.of(TERM_LIFE));

        mvc.perform(get("/api/products").param("name", "Term Life")).andExpect(status().isOk());

        verify(productService, never()).createProduct(any(CreateProductRequest.class));
        verify(productService, never()).updateProduct(anyLong(), any(UpdateProductRequest.class));
        verify(productService, never()).deleteProduct(anyLong());
    }
}
