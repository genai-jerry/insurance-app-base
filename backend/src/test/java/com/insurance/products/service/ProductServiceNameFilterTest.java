package com.insurance.products.service;

import com.insurance.common.entity.Product;
import com.insurance.common.entity.ProductCategory;
import com.insurance.products.dto.ProductDto;
import com.insurance.products.repository.ProductCategoryRepository;
import com.insurance.products.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Normalisation of the name filter term in
 * {@link ProductService#filterProducts(Long, String, String, String)}.
 *
 * <p>Covers every row of the normalisation table in the change design (§3.2 of
 * {@code openspec/changes/42-filter-products-by-name/design.md}): what reaches
 * {@code findByFilters}, that the trimmed term is passed through otherwise untouched, and
 * that the repository is never consulted at all for an over-long term.
 *
 * <p>Match semantics — substring, case-insensitivity, name-only, composition, stable order —
 * belong to the database and are asserted against a real Postgres in
 * {@code ProductRepositoryNameFilterTest}. A mocked repository can only prove which argument
 * was handed over, which is exactly what this class does.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceNameFilterTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @InjectMocks
    private ProductService productService;

    private static final ProductCategory LIFE = ProductCategory.builder().id(10L).name("Life").build();
    private static final ProductCategory HEALTH = ProductCategory.builder().id(20L).name("Health").build();

    private static final Product TERM_LIFE = product(1L, "Term Life Secure", LIFE, "LifeGuard", "TERM");
    private static final Product FAMILY_HEALTH = product(2L, "Family Health Plus", HEALTH, "HealthFirst", "HEALTH");

    private static Product product(long id, String name, ProductCategory category,
                                   String insurer, String planType) {
        return Product.builder()
            .id(id)
            .name(name)
            .category(category)
            .insurer(insurer)
            .planType(planType)
            .build();
    }

    /**
     * A term from each branch the listing has: no filter, a plain match, an injection
     * payload, and one long enough to short-circuit before the repository.
     */
    static Stream<String> writeFreeTerms() {
        return Stream.of("", "  ", "Term Life", "' OR 1=1 --", "a".repeat(300));
    }

    /** The name term the single {@code findByFilters} call so far actually bound. */
    private String boundTerm() {
        ArgumentCaptor<String> term = ArgumentCaptor.forClass(String.class);
        verify(productRepository).findByFilters(nullable(Long.class), nullable(String.class),
            nullable(String.class), term.capture());
        return term.getValue();
    }

    /** The term the service bound, for a call made with only a name filter. */
    private String capturedTerm(String name) {
        productService.filterProducts(null, null, null, name);
        return boundTerm();
    }

    @Nested
    @DisplayName("A blank term means no name filter")
    class NoNameFilter {

        @ParameterizedTest(name = "term = [{0}]")
        @NullSource
        @ValueSource(strings = {"", "   ", "\t", "\n  \t "})
        @DisplayName("absent, empty and whitespace-only terms bind null, leaving the predicate skipped")
        void blankTermsBindNull(String term) {
            when(productRepository.findByFilters(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class)))
                .thenReturn(List.of(TERM_LIFE, FAMILY_HEALTH));

            List<ProductDto> result = productService.filterProducts(null, null, null, term);

            // Spec: "a term of only whitespace is not an error" — every product comes back.
            assertThat(result).extracting(ProductDto::getName)
                .containsExactly("Term Life Secure", "Family Health Plus");
            assertThat(boundTerm()).isNull();
        }

        @Test
        @DisplayName("a blank name still lets the other filters through unchanged")
        void blankNameDoesNotDisturbTheOtherFilters() {
            when(productRepository.findByFilters(10L, "LifeGuard", "TERM", null))
                .thenReturn(List.of(TERM_LIFE));

            List<ProductDto> result = productService.filterProducts(10L, "LifeGuard", "TERM", "  ");

            // Spec: "existing callers are unaffected" — a request carrying no usable name term
            // reaches the repository exactly as it did before this change.
            assertThat(result).extracting(ProductDto::getName).containsExactly("Term Life Secure");
            verify(productRepository).findByFilters(10L, "LifeGuard", "TERM", null);
        }
    }

    @Nested
    @DisplayName("A non-blank term is trimmed and otherwise passed through untouched")
    class FilteredTerm {

        @Test
        @DisplayName("a full product name reaches the repository verbatim")
        void fullNameIsPassedThrough() {
            when(productRepository.findByFilters(null, null, null, "Term Life Secure"))
                .thenReturn(List.of(TERM_LIFE));

            List<ProductDto> result = productService.filterProducts(null, null, null, "Term Life Secure");

            assertThat(result).extracting(ProductDto::getName).containsExactly("Term Life Secure");
            verify(productRepository).findByFilters(null, null, null, "Term Life Secure");
        }

        @Test
        @DisplayName("surrounding whitespace is stripped before the query")
        void surroundingWhitespaceIsStripped() {
            assertThat(capturedTerm("  Term Life  ")).isEqualTo("Term Life");
        }

        @Test
        @DisplayName("interior whitespace is preserved — only the ends are trimmed")
        void interiorWhitespaceIsPreserved() {
            assertThat(capturedTerm("\tTerm  Life\n")).isEqualTo("Term  Life");
        }

        @Test
        @DisplayName("case is left to the database — the term is not lower-cased in Java")
        void caseIsNotAlteredByTheService() {
            // LOWER(...) in the JPQL does the folding; folding here as well would be
            // duplicated logic that silently breaks the day the query changes.
            assertThat(capturedTerm("tErM lIfE")).isEqualTo("tErM lIfE");
        }

        @Test
        @DisplayName("a partial term is not anchored, wildcarded or otherwise rewritten")
        void partialTermIsNotRewritten() {
            // The '%' wrapping lives in the query's CONCAT; a service that added its own
            // would produce '%%Life%%' and quietly change the match.
            assertThat(capturedTerm("Life")).isEqualTo("Life");
        }

        @Test
        @DisplayName("a term matching nothing yields an empty list, not an exception")
        void termMatchingNothingYieldsEmptyList() {
            when(productRepository.findByFilters(null, null, null, "no product is called this"))
                .thenReturn(List.of());

            assertThat(productService.filterProducts(null, null, null, "no product is called this"))
                .isEmpty();
        }

        @Test
        @DisplayName("name and category are handed over together, so they can narrow together")
        void nameAndCategoryAreBothForwarded() {
            when(productRepository.findByFilters(10L, null, null, "Life")).thenReturn(List.of(TERM_LIFE));

            List<ProductDto> result = productService.filterProducts(10L, null, null, "  Life  ");

            assertThat(result).extracting(ProductDto::getCategoryId).containsExactly(10L);
            verify(productRepository).findByFilters(10L, null, null, "Life");
        }
    }

    @Nested
    @DisplayName("Hostile terms are data, never query structure")
    class HostileTerms {

        @Test
        @DisplayName("SQL metacharacters are passed to the bound parameter unchanged")
        void sqlMetacharactersArePassedThroughAsData() {
            String injection = "' OR 1=1 --";
            when(productRepository.findByFilters(null, null, null, injection)).thenReturn(List.of());

            assertThat(productService.filterProducts(null, null, null, injection)).isEmpty();

            // The service must neither sanitise nor reject: the JPQL binds :name as a JDBC
            // parameter, so escaping here would only corrupt names that genuinely contain
            // these characters.
            verify(productRepository).findByFilters(null, null, null, injection);
        }

        @Test
        @DisplayName("LIKE wildcards are left alone — deliberately unescaped, per design §7 decision 4")
        void likeWildcardsAreNotEscaped() {
            assertThat(capturedTerm("50%_cover")).isEqualTo("50%_cover");
        }

        @Test
        @DisplayName("a term at the 255-character column length still queries")
        void termAtMaximumLengthStillQueries() {
            String term = "a".repeat(255);
            when(productRepository.findByFilters(null, null, null, term)).thenReturn(List.of());

            assertThat(productService.filterProducts(null, null, null, term)).isEmpty();
            verify(productRepository).findByFilters(null, null, null, term);
        }

        @ParameterizedTest(name = "term of {0} characters")
        @ValueSource(ints = {256, 10_000})
        @DisplayName("an over-long term short-circuits to an empty list without touching the database")
        void overLongTermShortCircuits(int length) {
            List<ProductDto> result = productService.filterProducts(null, null, null, "a".repeat(length));

            assertThat(result).isEmpty();

            // No name in a VARCHAR(255) column can contain a longer substring, so [] is the
            // same answer the query would give — and no driver limit can turn it into a 500.
            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("an over-long term wins over the other filters — the intersection is empty either way")
        void overLongTermShortCircuitsEvenWithOtherFiltersSupplied() {
            assertThat(productService.filterProducts(10L, "LifeGuard", "TERM", "a".repeat(300))).isEmpty();

            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("an over-long term that trims down to a valid length is queried normally")
        void whitespacePaddingIsTrimmedBeforeTheLengthCheck() {
            String padded = " ".repeat(600) + "Term Life Secure" + " ".repeat(600);
            when(productRepository.findByFilters(null, null, null, "Term Life Secure"))
                .thenReturn(List.of(TERM_LIFE));

            assertThat(productService.filterProducts(null, null, null, padded))
                .extracting(ProductDto::getName).containsExactly("Term Life Secure");
        }
    }

    @Nested
    @DisplayName("Filtering leaves the catalogue untouched")
    class NoWrites {

        @ParameterizedTest(name = "term = [{0}]")
        @NullSource
        @MethodSource("com.insurance.products.service.ProductServiceNameFilterTest#writeFreeTerms")
        @DisplayName("no filtered listing creates, updates or deletes a product")
        void filteringWritesNothing(String term) {
            // Lenient: the over-long parameter short-circuits and never reaches the repository.
            lenient().when(productRepository.findByFilters(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class)))
                .thenReturn(List.of(TERM_LIFE, FAMILY_HEALTH));

            productService.filterProducts(null, null, null, term);

            verify(productRepository, never()).save(any(Product.class));
            verify(productRepository, never()).saveAll(any());
            verify(productRepository, never()).delete(any(Product.class));
            verify(productRepository, never()).deleteById(anyLong());
            // Categories are only read when a product is created or moved; a listing must
            // not touch that repository at all.
            verifyNoInteractions(productCategoryRepository);
        }
    }

    @Test
    @DisplayName("the listing maps entities to DTOs — controllers never see the entity")
    void listingReturnsDtosNotEntities() {
        when(productRepository.findByFilters(null, null, null, "Term Life"))
            .thenReturn(List.of(TERM_LIFE));

        List<ProductDto> result = productService.filterProducts(null, null, null, "Term Life");

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getName()).isEqualTo("Term Life Secure");
            assertThat(dto.getCategoryId()).isEqualTo(10L);
            assertThat(dto.getCategoryName()).isEqualTo("Life");
            assertThat(dto.getInsurer()).isEqualTo("LifeGuard");
            assertThat(dto.getPlanType()).isEqualTo("TERM");
        });
    }

    @Test
    @DisplayName("the repository's order is preserved — the service does not re-sort")
    void repositoryOrderIsPreserved() {
        // ORDER BY p.id ASC in the query is what makes the "stable, repeatable order"
        // scenario hold; a stream that re-sorted or used an unordered collector would
        // discard it.
        when(productRepository.findByFilters(null, null, null, "e"))
            .thenReturn(List.of(TERM_LIFE, FAMILY_HEALTH));

        assertThat(productService.filterProducts(null, null, null, "e"))
            .extracting(ProductDto::getId).containsExactly(1L, 2L);
    }
}
