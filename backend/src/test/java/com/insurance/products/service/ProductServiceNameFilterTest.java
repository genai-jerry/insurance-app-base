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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
 * <p>Covers every row of the normalisation table in §3.2 of
 * {@code openspec/changes/42-filter-products-by-name/design.md}: which arguments reach
 * {@code findByFilters}, that the repository is not touched at all for an oversized term,
 * and that listing — however it is filtered — never writes.
 *
 * <p>Match semantics (case-insensitivity, substring, composition, only-the-name) are a
 * property of the database and are asserted against a real Postgres in
 * {@code ProductRepositoryNameFilterTest}. A mocked repository can only confirm that the
 * right string was handed over, which is exactly what this class checks.
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

    private static final Product TERM_LIFE = product(1L, "Zenith Term Life Secure", LIFE, "LifeGuard", "TERM");
    private static final Product FAMILY_TERM = product(2L, "Zenith Family Term Cover", HEALTH, "HealthFirst", "TERM");

    private static Product product(long id, String name, ProductCategory category,
                                   String insurer, String planType) {
        return Product.builder()
            .id(id)
            .name(name)
            .category(category)
            .insurer(insurer)
            .planType(planType)
            .tags(List.of("tag-" + id))
            .build();
    }

    /** The name argument {@code findByFilters} was called with, for a single invocation. */
    private String capturedName() {
        ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        verify(productRepository).findByFilters(nullable(Long.class), nullable(String.class),
            nullable(String.class), name.capture());
        return name.getValue();
    }

    @Nested
    @DisplayName("A blank term means no name filter")
    class BlankTerms {

        @ParameterizedTest(name = "term = [{0}]")
        @NullSource
        @ValueSource(strings = {"", "   ", "\t", "\n  \t "})
        @DisplayName("absent, empty and whitespace-only terms query with a null name")
        void blankTermsPassNull(String term) {
            when(productRepository.findByFilters(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class)))
                .thenReturn(List.of(TERM_LIFE, FAMILY_TERM));

            List<ProductDto> result = productService.filterProducts(null, null, null, term);

            // A null name skips the predicate, so the whole catalogue comes back — the
            // spec's "whitespace-only term is not an error" and the server half of
            // "clearing the filter restores the full list".
            assertThat(result).extracting(ProductDto::getName)
                .containsExactly("Zenith Term Life Secure", "Zenith Family Term Cover");
            assertThat(capturedName()).isNull();
        }

        @Test
        @DisplayName("a blank name still lets the other filters through untouched")
        void blankNameLeavesTheOtherFiltersAlone() {
            when(productRepository.findByFilters(10L, "LifeGuard", "TERM", null))
                .thenReturn(List.of(TERM_LIFE));

            assertThat(productService.filterProducts(10L, "LifeGuard", "TERM", "   "))
                .extracting(ProductDto::getName).containsExactly("Zenith Term Life Secure");

            verify(productRepository).findByFilters(10L, "LifeGuard", "TERM", null);
        }
    }

    @Nested
    @DisplayName("A non-blank term is trimmed and otherwise passed through untouched")
    class FilteredTerm {

        @Test
        @DisplayName("a full product name reaches the repository verbatim")
        void fullNameIsPassedThrough() {
            when(productRepository.findByFilters(null, null, null, "Zenith Term Life Secure"))
                .thenReturn(List.of(TERM_LIFE));

            assertThat(productService.filterProducts(null, null, null, "Zenith Term Life Secure"))
                .extracting(ProductDto::getName).containsExactly("Zenith Term Life Secure");
            assertThat(capturedName()).isEqualTo("Zenith Term Life Secure");
        }

        @Test
        @DisplayName("surrounding whitespace is stripped before the query")
        void surroundingWhitespaceIsStripped() {
            when(productRepository.findByFilters(null, null, null, "Zenith"))
                .thenReturn(List.of(TERM_LIFE, FAMILY_TERM));

            assertThat(productService.filterProducts(null, null, null, "\t  Zenith \n "))
                .hasSize(2);
            assertThat(capturedName()).isEqualTo("Zenith");
        }

        @Test
        @DisplayName("case is left to the database — the term is not lower-cased in Java")
        void caseIsNotAlteredByTheService() {
            when(productRepository.findByFilters(null, null, null, "zEnItH tErM"))
                .thenReturn(List.of(TERM_LIFE));

            assertThat(productService.filterProducts(null, null, null, "zEnItH tErM")).hasSize(1);

            // LOWER(...) in the JPQL does the folding; folding here as well would be
            // duplicated logic, and this assertion is what flags it if it appears.
            assertThat(capturedName()).isEqualTo("zEnItH tErM");
        }

        @Test
        @DisplayName("a partial term is not expanded, anchored or wildcarded by the service")
        void partialTermIsNotRewritten() {
            when(productRepository.findByFilters(null, null, null, "Term"))
                .thenReturn(List.of(TERM_LIFE, FAMILY_TERM));

            assertThat(productService.filterProducts(null, null, null, "Term")).hasSize(2);

            // The '%' wrapping belongs to the CONCAT in the JPQL; if the service added
            // its own the term would arrive as "%Term%" and match nothing real.
            assertThat(capturedName()).isEqualTo("Term");
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
        @DisplayName("the name term composes with category, insurer and plan type")
        void nameComposesWithTheExistingFilters() {
            when(productRepository.findByFilters(10L, "LifeGuard", "TERM", "Zenith"))
                .thenReturn(List.of(TERM_LIFE));

            assertThat(productService.filterProducts(10L, "LifeGuard", "TERM", "  Zenith  "))
                .extracting(ProductDto::getName).containsExactly("Zenith Term Life Secure");

            // Every supplied filter is forwarded on the one query that ANDs them — the
            // service must not drop or reorder any of them while normalising the name.
            verify(productRepository).findByFilters(10L, "LifeGuard", "TERM", "Zenith");
        }

        @Test
        @DisplayName("results are mapped to DTOs, never entities")
        void resultsAreMappedToDtos() {
            when(productRepository.findByFilters(null, null, null, "Zenith"))
                .thenReturn(List.of(TERM_LIFE));

            assertThat(productService.filterProducts(null, null, null, "Zenith"))
                .singleElement().satisfies(dto -> {
                    assertThat(dto).isInstanceOf(ProductDto.class);
                    assertThat(dto.getId()).isEqualTo(1L);
                    assertThat(dto.getName()).isEqualTo("Zenith Term Life Secure");
                    assertThat(dto.getCategoryId()).isEqualTo(10L);
                    assertThat(dto.getCategoryName()).isEqualTo("Life");
                    assertThat(dto.getInsurer()).isEqualTo("LifeGuard");
                    assertThat(dto.getPlanType()).isEqualTo("TERM");
                });
        }

        @Test
        @DisplayName("the repository's order is preserved, not re-sorted in Java")
        void repositoryOrderIsPreserved() {
            // findByFilters orders by id ascending; the service must hand that order on
            // unchanged, which is what makes the order stable end to end.
            when(productRepository.findByFilters(null, null, null, "Zenith"))
                .thenReturn(List.of(TERM_LIFE, FAMILY_TERM));

            assertThat(productService.filterProducts(null, null, null, "Zenith"))
                .extracting(ProductDto::getId).containsExactly(1L, 2L);
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

            // Neither sanitised nor rejected: the JPQL binds the term as a parameter, so
            // escaping here would only corrupt names that genuinely contain these characters.
            assertThat(capturedName()).isEqualTo(injection);
        }

        @Test
        @DisplayName("LIKE wildcards are not escaped by the service")
        void likeWildcardsArePassedThroughUnescaped() {
            // design.md §7 decision 4: %/_ act as LIKE wildcards, deliberately unescaped.
            when(productRepository.findByFilters(null, null, null, "Zen_th%Plan")).thenReturn(List.of());

            productService.filterProducts(null, null, null, "Zen_th%Plan");

            assertThat(capturedName()).isEqualTo("Zen_th%Plan");
        }

        @Test
        @DisplayName("a term at the 255-character column length still queries")
        void termAtMaximumLengthStillQueries() {
            String term = "a".repeat(255);
            when(productRepository.findByFilters(null, null, null, term)).thenReturn(List.of());

            assertThat(productService.filterProducts(null, null, null, term)).isEmpty();

            assertThat(capturedName()).isEqualTo(term);
        }

        @ParameterizedTest(name = "term of {0} characters")
        @ValueSource(ints = {256, 10_000})
        @DisplayName("an oversized term short-circuits to an empty list without touching the database")
        void oversizedTermShortCircuits(int length) {
            assertThat(productService.filterProducts(null, null, null, "a".repeat(length))).isEmpty();

            // No name in a VARCHAR(255) column can contain a longer substring, so [] is
            // the same answer the query would give — and no driver limit can turn it into
            // a server error.
            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("an oversized term wins over the other filters rather than widening the result")
        void oversizedTermIsEmptyEvenWithOtherFiltersSupplied() {
            // Filters AND together, so an unsatisfiable name predicate empties the
            // intersection whatever categoryId/insurer/planType were asked for.
            assertThat(productService.filterProducts(10L, "LifeGuard", "TERM", "a".repeat(300))).isEmpty();

            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("an oversized term that trims down to a valid length is queried normally")
        void whitespacePaddingIsTrimmedBeforeTheLengthCheck() {
            String padded = " ".repeat(600) + "Zenith" + " ".repeat(600);
            when(productRepository.findByFilters(null, null, null, "Zenith"))
                .thenReturn(List.of(TERM_LIFE, FAMILY_TERM));

            assertThat(productService.filterProducts(null, null, null, padded)).hasSize(2);
            assertThat(capturedName()).isEqualTo("Zenith");
        }
    }

    @Nested
    @DisplayName("Filtering grants no write access")
    class NoWrites {

        @ParameterizedTest(name = "term = [{0}]")
        @NullSource
        @ValueSource(strings = {"", "  ", "Zenith", "' OR 1=1 --", "'; DROP TABLE products; --"})
        @DisplayName("listing never saves or deletes a product, and never touches categories")
        void listingWritesNothing(String term) {
            // Lenient: the oversized/blank branches do not reach the repository at all.
            lenient().when(productRepository.findByFilters(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class)))
                .thenReturn(List.of(TERM_LIFE));

            productService.filterProducts(null, null, null, term);

            verify(productRepository, never()).save(any(Product.class));
            verify(productRepository, never()).delete(any(Product.class));
            verify(productRepository, never()).deleteById(anyLong());
            verifyNoInteractions(productCategoryRepository);
        }
    }
}
