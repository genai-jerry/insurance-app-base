package com.insurance.products.repository;

import com.insurance.common.entity.Product;
import com.insurance.common.entity.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Match semantics of the name predicate in
 * {@link ProductRepository#findByFilters(Long, String, String, String)} against a real
 * Postgres.
 *
 * <p>This is the only layer that can prove what the spec is actually about — that the name
 * match is a case-insensitive substring evaluated over the whole catalogue, that it composes
 * with the category / insurer / plan-type predicates, that it looks at the name and nothing
 * else, and that an injection payload is compared as text rather than executed. A mocked
 * repository can only confirm the right string was handed over; the semantics live in the
 * database.
 *
 * <p>H2 is not an option: {@code @DataJpaTest} builds the persistence unit from every entity
 * in {@code common/entity/}, which uses {@code jsonb}, {@code vector(1536)} and {@code text[]}.
 *
 * <p>Requires a running Docker daemon. Where Docker is unavailable this class is the only one
 * in the change that cannot run; the service and controller tests still cover normalisation,
 * binding and authorization.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProductRepositoryNameFilterTest {

    /**
     * The image docker-compose.yml runs. The init script mirrors the compose mount of
     * db/init into /docker-entrypoint-initdb.d: V4__create_vector_embeddings_table.sql uses
     * vector(1536) but no migration creates the extension, so without it Flyway fails at V4
     * and the context never starts.
     */
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
        .withInitScript("db/init/01-init-pgvector.sql");

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ProductCategory lifeCategory;
    private ProductCategory healthCategory;

    /**
     * V10__seed_initial_data.sql already seeds three products ("SecureLife Term Plan",
     * "FamilyCare Health Plus", "WealthBuilder ULIP") whose names share common words like
     * "Term" and "Life". Fixture names are therefore tagged with a token that appears nowhere
     * in the seed data, so a filtered assertion can be exact about what came back rather than
     * having to enumerate the catalogue.
     */
    @BeforeEach
    void seedFixtures() {
        lifeCategory = persistCategory("Zenith Life Filter Fixtures");
        healthCategory = persistCategory("Zenith Health Filter Fixtures");

        persistProduct("Zenith Term Life Secure", lifeCategory, "LifeGuard Insurance Co.", "TERM", null);
        persistProduct("Zenith Family Term Cover", healthCategory, "HealthFirst Insurance", "TERM", null);
        persistProduct("Zenith Whole Life Plan", lifeCategory, "LifeGuard Insurance Co.", "WHOLE", null);
        // Carries the token "Helvetia" in its insurer and its tags but not in its name, so a
        // filter on that token proves only the name column is consulted.
        persistProduct("Aurora Motor Shield", healthCategory, "Helvetia Assurance", "MOTOR",
            List.of("helvetia-partner", "motor"));

        entityManager.flush();
    }

    private ProductCategory persistCategory(String name) {
        return entityManager.persist(ProductCategory.builder().name(name).build());
    }

    private Product persistProduct(String name, ProductCategory category, String insurer,
                                   String planType, List<String> tags) {
        return entityManager.persist(Product.builder()
            .name(name)
            .category(category)
            .insurer(insurer)
            .planType(planType)
            .tags(tags)
            .build());
    }

    private List<String> namesMatching(String term) {
        return names(productRepository.findByFilters(null, null, null, term));
    }

    private static List<String> names(List<Product> products) {
        return products.stream().map(Product::getName).toList();
    }

    @Test
    @DisplayName("a full product name matches exactly that product")
    void fullNameMatchesThatProduct() {
        assertThat(namesMatching("Zenith Term Life Secure")).containsExactly("Zenith Term Life Secure");
    }

    @Test
    @DisplayName("a partial term matches every product containing it, and no others")
    void partialTermMatchesEveryProductContainingIt() {
        assertThat(namesMatching("Zenith"))
            .containsExactly("Zenith Term Life Secure", "Zenith Family Term Cover", "Zenith Whole Life Plan")
            .doesNotContain("Aurora Motor Shield");
    }

    @Test
    @DisplayName("matching ignores case")
    void matchingIgnoresCase() {
        assertThat(namesMatching("zEnItH tErM lIfE")).containsExactly("Zenith Term Life Secure");
        assertThat(namesMatching("ZENITH WHOLE")).containsExactly("Zenith Whole Life Plan");
    }

    @Test
    @DisplayName("a term matching nothing yields an empty list")
    void termMatchingNothingYieldsEmptyList() {
        assertThat(namesMatching("no product is called this")).isEmpty();
    }

    @Test
    @DisplayName("only the name is matched — a term found only in insurer, plan type or tags returns nothing")
    void onlyTheNameIsMatched() {
        // "Helvetia" is the insurer of, and a tag on, "Aurora Motor Shield"; no product name
        // contains it. searchProducts() would return that product — findByFilters must not.
        assertThat(namesMatching("Helvetia")).isEmpty();
        assertThat(namesMatching("helvetia-partner")).isEmpty();
        assertThat(namesMatching("MOTOR")).containsExactly("Aurora Motor Shield"); // name, not planType
        assertThat(namesMatching("ULIP")).containsExactly("WealthBuilder ULIP");   // seed row, by name
    }

    @Test
    @DisplayName("a name term and a category narrow together")
    void nameAndCategoryNarrowTogether() {
        assertThat(names(productRepository.findByFilters(lifeCategory.getId(), null, null, "Zenith")))
            .containsExactly("Zenith Term Life Secure", "Zenith Whole Life Plan");

        assertThat(names(productRepository.findByFilters(healthCategory.getId(), null, null, "Zenith")))
            .containsExactly("Zenith Family Term Cover");

        // Intersection, not union: a term that matches only in the other category is empty here.
        assertThat(productRepository.findByFilters(healthCategory.getId(), null, null, "Whole")).isEmpty();
    }

    @Test
    @DisplayName("a name term composes with the insurer and plan-type filters too")
    void nameComposesWithInsurerAndPlanType() {
        assertThat(names(productRepository.findByFilters(null, "healthfirst insurance", null, "Zenith")))
            .containsExactly("Zenith Family Term Cover");

        assertThat(names(productRepository.findByFilters(null, null, "term", "Zenith")))
            .containsExactly("Zenith Term Life Secure", "Zenith Family Term Cover");

        assertThat(names(productRepository.findByFilters(
                lifeCategory.getId(), "LifeGuard Insurance Co.", "TERM", "Zenith")))
            .containsExactly("Zenith Term Life Secure");
    }

    @Test
    @DisplayName("a null name skips the predicate, leaving the existing filters exactly as they were")
    void nullNameSkipsThePredicate() {
        assertThat(names(productRepository.findByFilters(null, null, null, null)))
            .hasSize((int) productRepository.count())
            .contains("Zenith Term Life Secure", "Aurora Motor Shield",
                      "SecureLife Term Plan", "FamilyCare Health Plus", "WealthBuilder ULIP");

        assertThat(names(productRepository.findByFilters(lifeCategory.getId(), null, null, null)))
            .containsExactly("Zenith Term Life Secure", "Zenith Whole Life Plan");
    }

    @Test
    @DisplayName("several products sharing a matching name all come back, in a stable repeatable order")
    void severalMatchesComeBackInAStableOrder() {
        // Names are not unique (V3__create_products_tables.sql has no unique constraint), so the
        // duplicate case the spec calls out is representable.
        persistProduct("Zenith Duplicate Name", lifeCategory, "LifeGuard Insurance Co.", "TERM", null);
        persistProduct("Zenith Duplicate Name", healthCategory, "HealthFirst Insurance", "HEALTH", null);
        entityManager.flush();

        List<Product> first = productRepository.findByFilters(null, null, null, "Zenith Duplicate Name");
        List<Product> second = productRepository.findByFilters(null, null, null, "Zenith Duplicate Name");

        assertThat(names(first)).containsExactly("Zenith Duplicate Name", "Zenith Duplicate Name");
        assertThat(first.stream().map(Product::getId).toList()).isSorted();
        assertThat(second.stream().map(Product::getId).toList())
            .isEqualTo(first.stream().map(Product::getId).toList());
    }

    @Test
    @DisplayName("the filter covers the whole catalogue, not just a first page of it")
    void filterCoversTheWholeCatalogue() {
        // A run of products that would sit well past any client-side page boundary, with the
        // matching one last.
        for (int i = 0; i < 30; i++) {
            persistProduct("Filler Product " + i, healthCategory, "Filler Insurance", "FILLER", null);
        }
        persistProduct("Needle Endowment Plan", lifeCategory, "LifeGuard Insurance Co.", "TERM", null);
        entityManager.flush();

        assertThat(namesMatching("Needle")).containsExactly("Needle Endowment Plan");
    }

    @Test
    @DisplayName("SQL metacharacters are matched literally, not executed")
    void sqlMetacharactersAreMatchedLiterally() {
        long before = productRepository.count();

        assertThat(namesMatching("' OR 1=1 --")).isEmpty();

        // If the term were interpolated rather than bound, the tautology would have returned
        // every row instead of none, and the catalogue could have been altered.
        assertThat(productRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("a term whose text really is in a name is found even when it looks like an injection")
    void injectionShapedTextIsStillMatchedWhenItIsGenuinelyPartOfAName() {
        persistProduct("Odd '; DROP TABLE products; -- Plan", lifeCategory, "LifeGuard Insurance Co.",
            "TERM", null);
        entityManager.flush();

        assertThat(namesMatching("'; DROP TABLE products; --"))
            .containsExactly("Odd '; DROP TABLE products; -- Plan");
        assertThat(productRepository.count()).isPositive();
    }

    @Test
    @DisplayName("LIKE wildcards in the term act as wildcards — bound as data, deliberately unescaped")
    void likeWildcardsActAsWildcards() {
        // design.md §7 decision 4: %/_ typed into the filter are LIKE metacharacters. They are
        // still bound as data (they cannot alter the query), but escaping them so they match
        // literally is an explicit non-goal of this change. This test pins that decision so a
        // later change to it is a deliberate one, not an accident.
        assertThat(namesMatching("Zen_th"))
            .containsExactly("Zenith Term Life Secure", "Zenith Family Term Cover", "Zenith Whole Life Plan");

        assertThat(namesMatching("Zenith%Life"))
            .containsExactly("Zenith Term Life Secure", "Zenith Whole Life Plan");

        assertThat(namesMatching("%")).hasSize((int) productRepository.count());
    }

    @Test
    @DisplayName("a term at the column's full length is queried without failure")
    void maximumLengthTermIsQueriedWithoutFailure() {
        assertThat(namesMatching("a".repeat(255))).isEmpty();
    }

    @Test
    @DisplayName("an unfiltered listing is the superset the filter draws from")
    void unfilteredListingIsTheSuperset() {
        List<String> all = names(productRepository.findAll());

        assertThat(all).containsAll(namesMatching("Zenith"));
        assertThat(all).contains("Aurora Motor Shield", "SecureLife Term Plan");
    }
}
