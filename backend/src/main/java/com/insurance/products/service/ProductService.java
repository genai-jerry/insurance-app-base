package com.insurance.products.service;

import com.insurance.common.entity.Product;
import com.insurance.common.entity.ProductCategory;
import com.insurance.products.dto.CreateProductRequest;
import com.insurance.products.dto.ProductDto;
import com.insurance.products.dto.UpdateProductRequest;
import com.insurance.products.repository.ProductCategoryRepository;
import com.insurance.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    /** products.name is VARCHAR(255); no stored name can contain a longer substring. */
    private static final int MAX_NAME_FILTER_LENGTH = 255;

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return toDto(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDto> searchProducts(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllProducts();
        }
        return productRepository.searchProducts(searchTerm)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Lists products narrowed by every supplied filter. The name filter is a
     * case-insensitive substring match on the product name only; a term that is null,
     * empty or whitespace-only means "no name filter", and surrounding whitespace is
     * trimmed before matching.
     */
    @Transactional(readOnly = true)
    public List<ProductDto> filterProducts(Long categoryId, String insurer, String planType, String name) {
        String term = name == null ? null : name.trim();

        if (term != null && term.isEmpty()) {
            term = null;
        }

        if (term != null && term.length() > MAX_NAME_FILTER_LENGTH) {
            // No name in a VARCHAR(255) column can contain a longer substring, so an
            // empty result is the same answer the query would give — this just avoids
            // the round trip and keeps oversized terms failure-free.
            return List.of();
        }

        return productRepository.findByFilters(categoryId, insurer, planType, term)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        ProductCategory category = productCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Product category not found with id: " + request.getCategoryId()));

        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .insurer(request.getInsurer())
                .planType(request.getPlanType())
                .detailsJson(request.getDetailsJson())
                .eligibilityJson(request.getEligibilityJson())
                .tags(request.getTags())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Created product: {} ({})", savedProduct.getName(), savedProduct.getId());
        return toDto(savedProduct);
    }

    @Transactional
    public ProductDto updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (request.getCategoryId() != null) {
            ProductCategory category = productCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Product category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getName() != null) {
            product.setName(request.getName());
        }

        if (request.getInsurer() != null) {
            product.setInsurer(request.getInsurer());
        }

        if (request.getPlanType() != null) {
            product.setPlanType(request.getPlanType());
        }

        if (request.getDetailsJson() != null) {
            product.setDetailsJson(request.getDetailsJson());
        }

        if (request.getEligibilityJson() != null) {
            product.setEligibilityJson(request.getEligibilityJson());
        }

        if (request.getTags() != null) {
            product.setTags(request.getTags());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Updated product: {}", updatedProduct.getId());
        return toDto(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        log.info("Deleted product: {}", id);
    }

    private ProductDto toDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .insurer(product.getInsurer())
                .planType(product.getPlanType())
                .detailsJson(product.getDetailsJson())
                .eligibilityJson(product.getEligibilityJson())
                .tags(product.getTags())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
