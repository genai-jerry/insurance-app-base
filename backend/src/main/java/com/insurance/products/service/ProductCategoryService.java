package com.insurance.products.service;

import com.insurance.common.entity.ProductCategory;
import com.insurance.products.dto.ProductCategoryDto;
import com.insurance.products.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;

    @Transactional(readOnly = true)
    public List<ProductCategoryDto> getAllCategories() {
        return productCategoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductCategoryDto getCategoryById(Long id) {
        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product category not found with id: " + id));
        return toDto(category);
    }

    @Transactional
    public ProductCategoryDto createCategory(ProductCategoryDto dto) {
        if (productCategoryRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Product category with name '" + dto.getName() + "' already exists");
        }

        ProductCategory category = ProductCategory.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();

        ProductCategory savedCategory = productCategoryRepository.save(category);
        log.info("Created product category: {}", savedCategory.getName());
        return toDto(savedCategory);
    }

    @Transactional
    public ProductCategoryDto updateCategory(Long id, ProductCategoryDto dto) {
        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product category not found with id: " + id));

        // Check if name is being changed and if new name already exists
        if (dto.getName() != null && !dto.getName().equals(category.getName())) {
            if (productCategoryRepository.existsByName(dto.getName())) {
                throw new RuntimeException("Product category with name '" + dto.getName() + "' already exists");
            }
            category.setName(dto.getName());
        }

        if (dto.getDescription() != null) {
            category.setDescription(dto.getDescription());
        }

        ProductCategory updatedCategory = productCategoryRepository.save(category);
        log.info("Updated product category: {}", updatedCategory.getId());
        return toDto(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!productCategoryRepository.existsById(id)) {
            throw new RuntimeException("Product category not found with id: " + id);
        }
        productCategoryRepository.deleteById(id);
        log.info("Deleted product category: {}", id);
    }

    private ProductCategoryDto toDto(ProductCategory category) {
        return ProductCategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
