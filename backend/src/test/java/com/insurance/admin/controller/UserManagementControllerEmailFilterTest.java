package com.insurance.admin.controller;

import com.insurance.admin.service.UserManagementService;
import com.insurance.auth.config.CustomUserDetailsService;
import com.insurance.auth.config.JwtAuthenticationFilter;
import com.insurance.auth.config.SecurityConfig;
import com.insurance.auth.dto.UserDto;
import com.insurance.common.entity.User;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Binding and authorization for {@code GET /api/admin/users?email=}.
 *
 * <p>Two things are asserted here and nowhere else: that the raw query parameter reaches
 * the service untouched (no bean validation intercepting it, no rewriting), and that a
 * non-admin caller is refused without the service ever being consulted — so nothing can
 * leak about whether the term matched.
 *
 * <p>{@link SecurityConfig} is imported explicitly: it is a plain {@code @Configuration}
 * and {@code @WebMvcTest}'s type filters do not pick it up, so without the import there
 * would be no {@code @EnableMethodSecurity} and every authorization assertion below would
 * pass vacuously.
 */
@WebMvcTest(UserManagementController.class)
@Import(SecurityConfig.class)
class UserManagementControllerEmailFilterTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserManagementService userManagementService;

    /** Required by {@link SecurityConfig}'s {@code DaoAuthenticationProvider}. */
    @MockBean
    private CustomUserDetailsService userDetailsService;

    /** Required by {@link SecurityConfig}'s filter chain. */
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final UserDto ADMIN_DTO = UserDto.builder()
        .id(1L).name("Admin User").email("admin@insurance.com")
        .role(User.Role.ADMIN).createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
        .build();

    private static final UserDto AGENT_DTO = UserDto.builder()
        .id(2L).name("Agent Smith").email("agent@insurance.com")
        .role(User.Role.AGENT).createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
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

    @Nested
    @DisplayName("An admin's term reaches the service unchanged")
    class ParameterBinding {

        /**
         * Performs the request against a literal URI. {@code get(String)} would treat its
         * argument as a template and re-encode it, so the percent-escapes below would
         * arrive doubly encoded rather than as the characters they stand for.
         */
        private String capturedFilter(String queryString) throws Exception {
            mvc.perform(get(URI.create("/api/admin/users" + queryString))).andExpect(status().isOk());

            ArgumentCaptor<String> filter = ArgumentCaptor.forClass(String.class);
            verify(userManagementService).getAllUsers(filter.capture());
            return filter.getValue();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("an absent parameter binds to null")
        void absentParameterBindsToNull() throws Exception {
            when(userManagementService.getAllUsers(nullable(String.class)))
                .thenReturn(List.of(ADMIN_DTO, AGENT_DTO));

            assertThat(capturedFilter("")).isNull();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("an empty parameter binds to the empty string, not null")
        void emptyParameterBindsToEmptyString() throws Exception {
            when(userManagementService.getAllUsers(nullable(String.class)))
                .thenReturn(List.of(ADMIN_DTO, AGENT_DTO));

            assertThat(capturedFilter("?email=")).isEmpty();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("surrounding whitespace survives binding — trimming is the service's job")
        void whitespaceIsNotStrippedByBinding() throws Exception {
            when(userManagementService.getAllUsers(nullable(String.class))).thenReturn(List.of(AGENT_DTO));

            // %20 rather than '+': the controller must not depend on either encoding.
            assertThat(capturedFilter("?email=%20%20agent@insurance.com%20%20"))
                .isEqualTo("  agent@insurance.com  ");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("SQL metacharacters are not rejected, escaped or rewritten at the edge")
        void sqlMetacharactersReachTheServiceVerbatim() throws Exception {
            when(userManagementService.getAllUsers(nullable(String.class))).thenReturn(List.of());

            assertThat(capturedFilter("?email=%27%20OR%201%3D1%20--")).isEqualTo("' OR 1=1 --");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("an oversized term is accepted, not answered with a server error")
        void oversizedTermIsAcceptedNotRejected() throws Exception {
            when(userManagementService.getAllUsers(nullable(String.class))).thenReturn(List.of());

            String term = "a".repeat(4000);

            // The absence of @Size/@Validated on the parameter is load-bearing: no
            // @ControllerAdvice covers com.insurance.admin, so a ConstraintViolationException
            // would surface as HTTP 500 and break the spec's "never a server error".
            assertThat(capturedFilter("?email=" + term)).isEqualTo(term);
        }
    }

    @Nested
    @DisplayName("The response an admin gets")
    class AdminResponses {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a matching filter returns 200 and the matching accounts only")
        void matchingFilterReturnsMatchingAccounts() throws Exception {
            when(userManagementService.getAllUsers("agent@insurance.com")).thenReturn(List.of(AGENT_DTO));

            mvc.perform(get("/api/admin/users").param("email", "agent@insurance.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("agent@insurance.com"))
                .andExpect(jsonPath("$[0].role").value("AGENT"))
                // Entities are never returned from controllers: no password hash may appear.
                .andExpect(jsonPath("$[0].hashedPassword").doesNotExist());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("a term matching nothing is a successful empty array, not a 404")
        void noMatchIsAnEmptyArray() throws Exception {
            when(userManagementService.getAllUsers("nobody@example.invalid")).thenReturn(List.of());

            mvc.perform(get("/api/admin/users").param("email", "nobody@example.invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Filtering does not widen access to user data")
    class Authorization {

        @Test
        @WithMockUser(roles = "AGENT")
        @DisplayName("an AGENT filtering the user list is forbidden and learns nothing")
        void agentIsForbidden() throws Exception {
            MvcResult result = mvc.perform(get("/api/admin/users").param("email", "agent@insurance.com"))
                .andExpect(status().isForbidden())
                .andReturn();

            assertResponseDisclosesNoUserData(result);

            // The refusal happens before any lookup, so the response cannot differ
            // between a term that matches and one that does not.
            verifyNoInteractions(userManagementService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("an unauthenticated caller filtering the user list is refused and learns nothing")
        void anonymousIsRefused() throws Exception {
            MvcResult result = mvc.perform(get("/api/admin/users").param("email", "agent@insurance.com"))
                .andReturn();

            // 401 or 403: SecurityConfig registers no authenticationEntryPoint, so Spring
            // Security's default Http403ForbiddenEntryPoint answers anonymous requests with
            // 403 today. The spec's substance — refused, nothing disclosed — holds either
            // way; see §3.5 of the change design for why epic #17 does not change this
            // estate-wide behaviour. Tighten this to 401 if an entry point is ever added.
            assertThat(result.getResponse().getStatus()).isIn(401, 403);

            assertResponseDisclosesNoUserData(result);
            verifyNoInteractions(userManagementService);
        }

        private void assertResponseDisclosesNoUserData(MvcResult result) throws Exception {
            String body = result.getResponse().getContentAsString();
            assertThat(body).doesNotContain("agent@insurance.com", "admin@insurance.com");
        }
    }
}
