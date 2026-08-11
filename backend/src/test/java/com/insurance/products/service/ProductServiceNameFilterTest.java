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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
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
 * {@code openspec/changes/42-filter-products-by-name/design.md}): which arguments reach
 * {@code findByFilters}, and that the repository is never touched at all for the oversized
 * case.
 *
 * <p>Match semantics themselves (case-insensitive substring, composition, name-only matching)
 * are a property of the database and are asserted against a real Postgres in
 * {@code ProductRepositoryNameFilterTest} — a mock repository could only ever confirm that the
 * right string was handed over, which is what this class checks.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceNameFilterTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @InjectMocks
    private ProductService productService;

    private static final ProductCategory LIFE = ProductCategory.builder()
        .id(10L).name("Life Insurance").build();

    private static final Product TERM_LIFE = product(1L, "Term Life Secure", "LifeGuard Insurance Co.", "TERM");
    private static final Product WHOLE_LIFE = product(2L, "Whole Life Plan", "LifeGuard Insurance Co.", "WHOLE");

    private static Product product(long id, String name, String insurer, String planType) {
        return Product.builder()
            .id(id)
            .category(LIFE)
            .name(name)
            .insurer(insurer)
            .planType(planType)
            .tags(List.of("life-cover"))
            .build();
    }

    /** The name argument that actually reached the repository for a single filter call. */
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
        @DisplayName("absent, empty and whitespace-only terms pass null through, listing everything")
        void blankTermsPassNull(String term) {
            when(productRepository.findByFilters(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(TERM_LIFE, WHOLE_LIFE));

            List<ProductDto> result = productService.filterProducts(null, null, null, term);

            assertThat(result).extracting(ProductDto::getName)
                .containsExactly("Term Life Secure", "Whole Life Plan");

            // null, not "" — the repository predicate is (:name IS NULL OR ...), so an empty
            // string would still apply a LIKE '%%' rather than skipping the predicate.
            assertThat(capturedName()).isNull();
        }

        @Test
        @DisplayName("a blank name still leaves the existing filters in force")
        void blankNameLeavesOtherFiltersInForce() {
            when(productRepository.findByFilters(10L, "LifeGuard Insurance Co.", "TERM", null))
                .thenReturn(List.of(TERM_LIFE));

            assertThat(productService.filterProducts(10L, "LifeGuard Insurance Co.", "TERM", "   "))
                .extracting(ProductDto::getName).containsExactly("Term Life Secure");

            verify(productRepository).findByFilters(10L, "LifeGuard Insurance Co.", "TERM", null);
        }
    }

    @Nested
    @DisplayName("A non-blank term is trimmed and passed through untouched")
    class FilteredTerm {

        @Test
        @DisplayName("a full product name reaches the repository verbatim")
        void fullNameIsPassedThrough() {
            when(productRepository.findByFilters(null, null, null, "Term Life Secure"))
                .thenReturn(List.of(TERM_LIFE));

            assertThat(productService.filterProducts(null, null, null, "Term Life Secure"))
                .extracting(ProductDto::getName).containsExactly("Term Life Secure");

            assertThat(capturedName()).isEqualTo("Term Life Secure");
        }

        @Test
        @DisplayName("surrounding whitespace is stripped before the query")
        void surroundingWhitespaceIsStripped() {
            when(productRepository.findByFilters(null, null, null, "Term Life"))
                .thenReturn(List.of(TERM_LIFE));

            assertThat(productService.filterProducts(null, null, null, "  Term Life  "))
                .extracting(ProductDto::getName).containsExactly("Term Life Secure");

            // Inner whitespace is part of the term; only the surrounding whitespace goes.
            assertThat(capturedName()).isEqualTo("Term Life");
        }

        @Test
        @DisplayName("case is left to the database — the term is not lower-cased in Java")
        void caseIsNotAlteredByTheService() {
            when(productRepository.findByFilters(null, null, null, "tErM lIfE"))
                .thenReturn(List.of(TERM_LIFE));

            productService.filterProducts(null, null, null, "tErM lIfE");

            // LOWER(...) in the JPQL does the folding; if the service also folded the term this
            // assertion would fail, flagging duplicated logic.
            assertThat(capturedName()).isEqualTo("tErM lIfE");
        }

        @Test
        @DisplayName("a partial term is not expanded, anchored or wildcarded by the service")
        void partialTermIsNotRewritten() {
            when(productRepository.findByFilters(null, null, null, "Life"))
                .thenReturn(List.of(TERM_LIFE, WHOLE_LIFE));

            assertThat(productService.filterProducts(null, null, null, "Life")).hasSize(2);

            // The '%' wrapping belongs to the CONCAT in the JPQL, not to the term.
            assertThat(capturedName()).isEqualTo("Life");
        }

        @Test
        @DisplayName("the category, insurer and plan-type arguments are passed straight through beside it")
        void otherFiltersArePassedThroughUnchanged() {
            when(productRepository.findByFilters(10L, "LifeGuard Insurance Co.", "TERM", "Life"))
                .thenReturn(List.of(TERM_LIFE));

            assertThat(productService.filterProducts(10L, "LifeGuard Insurance Co.", "TERM", "  Life  "))
                .hasSize(1);

            verify(productRepository).findByFilters(10L, "LifeGuard Insurance Co.", "TERM", "Life");
        }

        @Test
        @DisplayName("a term matching nothing yields an empty list, not an exception")
        void termMatchingNothingYieldsEmptyList() {
            when(productRepository.findByFilters(null, null, null, "no such product"))
                .thenReturn(List.of());

            assertThat(productService.filterProducts(null, null, null, "no such product")).isEmpty();
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

            // The service must neither sanitise nor reject: the JPQL binds the term as a named
            // parameter, so escaping here would only corrupt genuine matches.
            assertThat(capturedName()).isEqualTo(injection);
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
        @DisplayName("an over-long term short-circuits to an empty list without touching the database")
        void overLongTermShortCircuits(int length) {
            List<ProductDto> result = productService.filterProducts(null, null, null, "a".repeat(length));

            assertThat(result).isEmpty();

            // No name in a VARCHAR(255) column can contain a longer substring, so [] is the same
            // answer the query would give — and no driver limit can turn it into a server error.
            verify(productRepository, never()).findByFilters(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class));
            verify(productRepository, never()).findAll();
        }

        @Test
        @DisplayName("an over-long term stays empty even when other filters are supplied")
        void overLongTermShortCircuitsRegardlessOfTheOtherFilters() {
            // Filters AND together, so an unsatisfiable name predicate empties the intersection
            // whatever the category, insurer and plan type are.
            assertThat(productService.filterProducts(10L, "LifeGuard Insurance Co.", "TERM",
                "a".repeat(400))).isEmpty();

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
    @DisplayName("Filtering leaves no trace")
    class NoTrace {

        @ParameterizedTest(name = "term = [{0}]")
        @NullSource
        @ValueSource(strings = {"", "  ", "Term Life Secure", "' OR 1=1 --", "aaaaaaaaaa"})
        @DisplayName("listing never creates, modifies or deletes a product")
        void listingWritesNothing(String term) {
            lenient().when(productRepository.findByFilters(nullable(Long.class), nullable(String.class),
                nullable(String.class), nullable(String.class))).thenReturn(List.of(TERM_LIFE));

            productService.filterProducts(null, null, null, term);

            verify(productRepository, never()).save(any(Product.class));
            verify(productRepository, never()).delete(any(Product.class));
            verify(productRepository, never()).deleteById(anyLong());
            // Categories are only ever loaded on the write paths; a read filter must not touch them.
            verifyNoInteractions(productCategoryRepository);
        }
    }

    @Test
    @DisplayName("the filtered listing returns DTOs carrying the category, never the entity")
    void filteredListingReturnsDtos() {
        when(productRepository.findByFilters(null, null, null, "Life"))
            .thenReturn(List.of(TERM_LIFE));

        assertThat(productService.filterProducts(null, null, null, "Life"))
            .singleElement().satisfies(dto -> {
                assertThat(dto.getId()).isEqualTo(1L);
                assertThat(dto.getName()).isEqualTo("Term Life Secure");
                assertThat(dto.getCategoryId()).isEqualTo(10L);
                assertThat(dto.getCategoryName()).isEqualTo("Life Insurance");
                assertThat(dto.getInsurer()).isEqualTo("LifeGuard Insurance Co.");
                assertThat(dto.getPlanType()).isEqualTo("TERM");
                assertThat(dto.getTags()).containsExactly("life-cover");
            });
    }

    @Test
    @DisplayName("the no-filter listing path is untouched by the name parameter")
    void unfilteredListingPathIsUnchanged() {
        // getAllProducts() is what the controller still calls when no parameter at all is
        // supplied; it must keep using findAll(), not the filtered query.
        when(productRepository.findAll()).thenReturn(List.of(TERM_LIFE, WHOLE_LIFE));

        assertThat(productService.getAllProducts()).hasSize(2);

        verify(productRepository, never()).findByFilters(nullable(Long.class), nullable(String.class),
            nullable(String.class), nullable(String.class));
        verify(productRepository, never()).searchProducts(anyString());
    }
}
