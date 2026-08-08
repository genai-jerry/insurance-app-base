package com.insurance.admin.service;

import com.insurance.auth.dto.UserDto;
import com.insurance.auth.repository.UserRepository;
import com.insurance.common.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Normalisation of the email filter term in {@link UserManagementService#getAllUsers(String)}.
 *
 * <p>Covers every row of the normalisation table in the change design (§3.2 of
 * {@code openspec/changes/17-filter-users-by-email/design.md}): which repository method
 * is called, with exactly which argument, and that the filtered query is never reached
 * for the null / empty / blank / over-long cases.
 *
 * <p>Match semantics themselves (case-insensitivity, substring, literal {@code %}) are a
 * property of the database and are asserted against a real Postgres in
 * {@code UserRepositoryEmailFilterTest} — a mock repository could only ever confirm that
 * the right string was handed over, which is what this class checks.
 */
@ExtendWith(MockitoExtension.class)
class UserManagementServiceEmailFilterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserManagementService userManagementService;

    private static User user(long id, String name, String email, User.Role role) {
        return User.builder()
            .id(id)
            .name(name)
            .email(email)
            .hashedPassword("irrelevant")
            .role(role)
            .build();
    }

    private static final User ADMIN = user(1L, "Admin User", "admin@insurance.com", User.Role.ADMIN);
    private static final User AGENT = user(2L, "Agent Smith", "agent@insurance.com", User.Role.AGENT);

    @Nested
    @DisplayName("A blank term means no filter")
    class NoFilter {

        @ParameterizedTest(name = "term = [{0}]")
        @NullSource
        @ValueSource(strings = {"", "   ", "\t", "\n  \t "})
        @DisplayName("absent, empty and whitespace-only terms list every user, ordered by id")
        void blankTermsListEveryUser(String term) {
            when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(ADMIN, AGENT));

            List<UserDto> result = userManagementService.getAllUsers(term);

            assertThat(result).extracting(UserDto::getEmail)
                .containsExactly("admin@insurance.com", "agent@insurance.com");

            // Ordering must match the filtered path's OrderByIdAsc, otherwise the
            // "clearing the filter restores the full list" scenario is untestable.
            ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
            verify(userRepository).findAll(sort.capture());
            assertThat(sort.getValue()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));

            verify(userRepository, never()).findByEmailContainingIgnoreCaseOrderByIdAsc(anyString());
        }
    }

    @Nested
    @DisplayName("A non-blank term is trimmed and passed through untouched")
    class FilteredTerm {

        @Test
        @DisplayName("a full email address reaches the repository verbatim")
        void fullAddressIsPassedThrough() {
            when(userRepository.findByEmailContainingIgnoreCaseOrderByIdAsc("agent@insurance.com"))
                .thenReturn(List.of(AGENT));

            List<UserDto> result = userManagementService.getAllUsers("agent@insurance.com");

            assertThat(result).extracting(UserDto::getEmail).containsExactly("agent@insurance.com");
            verify(userRepository).findByEmailContainingIgnoreCaseOrderByIdAsc("agent@insurance.com");
            verify(userRepository, never()).findAll(any(Sort.class));
        }

        @Test
        @DisplayName("surrounding whitespace is stripped before the query")
        void surroundingWhitespaceIsStripped() {
            when(userRepository.findByEmailContainingIgnoreCaseOrderByIdAsc("agent@insurance.com"))
                .thenReturn(List.of(AGENT));

            List<UserDto> result = userManagementService.getAllUsers("  agent@insurance.com  ");

            assertThat(result).extracting(UserDto::getEmail).containsExactly("agent@insurance.com");
            verify(userRepository).findByEmailContainingIgnoreCaseOrderByIdAsc("agent@insurance.com");
        }

        @Test
        @DisplayName("case is left to the database — the term is not lower-cased in Java")
        void caseIsNotAlteredByTheService() {
            when(userRepository.findByEmailContainingIgnoreCaseOrderByIdAsc("AGENT@Insurance.COM"))
                .thenReturn(List.of(AGENT));

            List<UserDto> result = userManagementService.getAllUsers("AGENT@Insurance.COM");

            assertThat(result).extracting(UserDto::getEmail).containsExactly("agent@insurance.com");
            // IgnoreCase in the derived query name does the folding; if the service also
            // lower-cased the term this assertion would fail, flagging duplicated logic.
            verify(userRepository).findByEmailContainingIgnoreCaseOrderByIdAsc("AGENT@Insurance.COM");
        }

        @Test
        @DisplayName("a partial term is not expanded, anchored or wildcarded by the service")
        void partialTermIsNotRewritten() {
            when(userRepository.findByEmailContainingIgnoreCaseOrderByIdAsc("insurance.com"))
                .thenReturn(List.of(ADMIN, AGENT));

            List<UserDto> result = userManagementService.getAllUsers("insurance.com");

            assertThat(result).hasSize(2);
            verify(userRepository).findByEmailContainingIgnoreCaseOrderByIdAsc("insurance.com");
        }

        @Test
        @DisplayName("a term matching nothing yields an empty list, not an exception")
        void termMatchingNothingYieldsEmptyList() {
            when(userRepository.findByEmailContainingIgnoreCaseOrderByIdAsc("nobody@example.invalid"))
                .thenReturn(List.of());

            assertThat(userManagementService.getAllUsers("nobody@example.invalid")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Hostile terms are data, never query structure")
    class HostileTerms {

        @Test
        @DisplayName("SQL metacharacters are passed to the bound parameter unchanged")
        void sqlMetacharactersArePassedThroughAsData() {
            String injection = "' OR 1=1 --";
            when(userRepository.findByEmailContainingIgnoreCaseOrderByIdAsc(injection))
                .thenReturn(List.of());

            assertThat(userManagementService.getAllUsers(injection)).isEmpty();

            // The service must neither sanitise nor reject: the derived query binds the
            // term as a JDBC parameter, so escaping here would only corrupt real matches.
            verify(userRepository).findByEmailContainingIgnoreCaseOrderByIdAsc(injection);
        }

        @Test
        @DisplayName("a term at the 255-character column length still queries")
        void termAtMaximumLengthStillQueries() {
            String term = "a".repeat(255);
            when(userRepository.findByEmailContainingIgnoreCaseOrderByIdAsc(term)).thenReturn(List.of());

            assertThat(userManagementService.getAllUsers(term)).isEmpty();

            verify(userRepository).findByEmailContainingIgnoreCaseOrderByIdAsc(term);
        }

        @ParameterizedTest(name = "term of {0} characters")
        @ValueSource(ints = {256, 10_000})
        @DisplayName("an over-long term short-circuits to an empty list without touching the database")
        void overLongTermShortCircuits(int length) {
            List<UserDto> result = userManagementService.getAllUsers("a".repeat(length));

            assertThat(result).isEmpty();

            // No email in a VARCHAR(255) column can contain a longer substring, so [] is
            // the same answer the query would give — and no driver limit can turn it into
            // a server error.
            verify(userRepository, never()).findByEmailContainingIgnoreCaseOrderByIdAsc(anyString());
            verify(userRepository, never()).findAll(any(Sort.class));
        }

        @Test
        @DisplayName("an over-long term that trims down to a valid length is queried normally")
        void whitespacePaddingIsTrimmedBeforeTheLengthCheck() {
            String padded = " ".repeat(600) + "agent@insurance.com" + " ".repeat(600);
            when(userRepository.findByEmailContainingIgnoreCaseOrderByIdAsc("agent@insurance.com"))
                .thenReturn(List.of(AGENT));

            assertThat(userManagementService.getAllUsers(padded))
                .extracting(UserDto::getEmail).containsExactly("agent@insurance.com");
        }
    }

    @Nested
    @DisplayName("Filtering leaves no trace")
    class NoTrace {

        @ParameterizedTest(name = "term = [{0}]")
        @NullSource
        @ValueSource(strings = {"", "  ", "agent@insurance.com", "' OR 1=1 --"})
        @DisplayName("listing never writes an audit record and never mutates a user")
        void listingWritesNothing(String term) {
            // Lenient: each parameter exercises only one of the two branches.
            lenient().when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(ADMIN, AGENT));
            lenient().when(userRepository.findByEmailContainingIgnoreCaseOrderByIdAsc(anyString()))
                .thenReturn(List.of());

            userManagementService.getAllUsers(term);

            verifyNoInteractions(auditLogService);
            verifyNoInteractions(passwordEncoder);
            verify(userRepository, never()).save(any(User.class));
            verify(userRepository, never()).delete(any(User.class));
        }
    }

    @Test
    @DisplayName("the listing exposes DTO fields only — never the password hash")
    void listingReturnsDtosNotEntities() {
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(ADMIN));

        List<UserDto> result = userManagementService.getAllUsers(null);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getName()).isEqualTo("Admin User");
            assertThat(dto.getEmail()).isEqualTo("admin@insurance.com");
            assertThat(dto.getRole()).isEqualTo(User.Role.ADMIN);
        });
    }
}
