package com.insurance.auth.repository;

import com.insurance.common.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Match semantics of {@link UserRepository#findByEmailContainingIgnoreCaseOrderByIdAsc(String)}
 * against a real Postgres.
 *
 * <p>This is the only layer that can prove what the spec is actually about — that matching
 * is a case-insensitive substring, that {@code %} is a literal, and that an injection
 * payload is compared as text. A mocked repository can only confirm the right string was
 * handed over; the semantics live in the database.
 *
 * <p>H2 is not an option: {@code @DataJpaTest} builds the persistence unit from every
 * entity in {@code common/entity/}, which uses {@code jsonb}, {@code vector(1536)} and
 * {@code text[]}.
 *
 * <p>Requires a running Docker daemon. Where Docker is unavailable this class is the only
 * one in the change that cannot run; the service and controller tests still cover
 * normalisation, binding, authorization and the audit/PII assertions.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryEmailFilterTest {

    /**
     * The image docker-compose.yml runs. The init script mirrors the compose mount of
     * db/init into /docker-entrypoint-initdb.d: V4__create_vector_embeddings_table.sql
     * uses vector(1536) but no migration creates the extension, so without it Flyway
     * fails at V4 and the context never starts.
     */
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
        .withInitScript("db/init/01-init-pgvector.sql");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * V10__seed_initial_data.sql already seeds admin@insurance.com and agent@insurance.com —
     * the exact addresses the spec scenarios name. This adds a third account outside that
     * domain so "contains insurance.com" has something to exclude, and a fourth whose
     * address contains a literal wildcard.
     */
    @BeforeEach
    void seedExtraFixtures() {
        persist("Outsider", "someone@other.example", User.Role.AGENT);
        persist("Wildcard Wendy", "we_ndy%test@other.example", User.Role.AGENT);
        entityManager.flush();
    }

    private void persist(String name, String email, User.Role role) {
        entityManager.persist(User.builder()
            .name(name)
            .email(email)
            .hashedPassword("irrelevant-for-a-read-only-query")
            .role(role)
            .build());
    }

    private List<String> emailsMatching(String term) {
        return userRepository.findByEmailContainingIgnoreCaseOrderByIdAsc(term)
            .stream().map(User::getEmail).toList();
    }

    @Test
    @DisplayName("a full email address matches exactly that account")
    void fullAddressMatchesOneAccount() {
        assertThat(emailsMatching("agent@insurance.com")).containsExactly("agent@insurance.com");
    }

    @Test
    @DisplayName("a partial term matches every account containing it, and no others")
    void partialTermMatchesEveryAccountContainingIt() {
        assertThat(emailsMatching("insurance.com"))
            .containsExactly("admin@insurance.com", "agent@insurance.com")
            .doesNotContain("someone@other.example");
    }

    @Test
    @DisplayName("matching ignores case")
    void matchingIgnoresCase() {
        assertThat(emailsMatching("AGENT@Insurance.COM")).containsExactly("agent@insurance.com");
        assertThat(emailsMatching("INSURANCE.COM"))
            .containsExactly("admin@insurance.com", "agent@insurance.com");
    }

    @Test
    @DisplayName("a term matching nothing yields an empty list")
    void termMatchingNothingYieldsEmptyList() {
        assertThat(emailsMatching("nobody@example.invalid")).isEmpty();
    }

    @Test
    @DisplayName("the filter covers every account, not just a first page of them")
    void filterCoversEveryAccount() {
        // A run of accounts that would sit well past any client-side page boundary,
        // with the matching one last.
        for (int i = 0; i < 30; i++) {
            persist("Filler " + i, "filler" + i + "@padding.example", User.Role.AGENT);
        }
        persist("Zoe Last", "zoe@needle.example", User.Role.AGENT);
        entityManager.flush();

        assertThat(emailsMatching("needle.example")).containsExactly("zoe@needle.example");
    }

    @Test
    @DisplayName("results are ordered by id ascending, so clearing the filter restores the same order")
    void resultsAreOrderedByIdAscending() {
        List<Long> filteredIds = userRepository.findByEmailContainingIgnoreCaseOrderByIdAsc("example")
            .stream().map(User::getId).toList();

        assertThat(filteredIds).isSorted();
    }

    @Test
    @DisplayName("SQL metacharacters are matched literally, not executed")
    void sqlMetacharactersAreMatchedLiterally() {
        long before = userRepository.count();

        assertThat(emailsMatching("' OR 1=1 --")).isEmpty();

        // If the term were interpolated rather than bound, the tautology would have
        // returned every row instead of none.
        assertThat(userRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("LIKE wildcards in the term are matched literally")
    void likeWildcardsAreMatchedLiterally() {
        // '%' would match everything if it reached the LIKE pattern unescaped; Spring Data's
        // default EscapeCharacter neutralises it, so only the account that literally
        // contains '%' comes back.
        assertThat(emailsMatching("%")).containsExactly("we_ndy%test@other.example");

        // Same for '_', which would otherwise match any single character.
        assertThat(emailsMatching("we_ndy")).containsExactly("we_ndy%test@other.example");
        assertThat(emailsMatching("weXndy")).isEmpty();
    }

    @Test
    @DisplayName("a term at the column's full length is queried without failure")
    void maximumLengthTermIsHandled() {
        assertThat(emailsMatching("a".repeat(255))).isEmpty();
    }

    @Test
    @DisplayName("an unfiltered listing returns every account the filter can reach")
    void unfilteredListingIsTheSuperset() {
        List<String> all = userRepository.findAll().stream().map(User::getEmail).toList();

        assertThat(all).containsAll(emailsMatching("insurance.com"));
        assertThat(all).contains("admin@insurance.com", "agent@insurance.com", "someone@other.example");
    }
}
