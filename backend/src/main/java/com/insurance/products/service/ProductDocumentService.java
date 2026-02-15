package com.insurance.products.service;

import com.insurance.common.entity.Product;
import com.insurance.common.entity.ProductCategory;
import com.insurance.common.entity.ProductDocument;
import com.insurance.products.dto.ProductDocumentDto;
import com.insurance.products.repository.ProductCategoryRepository;
import com.insurance.products.repository.ProductDocumentRepository;
import com.insurance.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductDocumentService {

    private final ProductDocumentRepository productDocumentRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final DocumentStorageService storageService;
    private final Tika tika = new Tika();

    @Transactional(readOnly = true)
    public List<ProductDocumentDto> getDocumentsByProduct(Long productId) {
        return productDocumentRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDocumentDto> getDocumentsByCategory(Long categoryId) {
        return productDocumentRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDocumentDto getDocumentById(Long id) {
        ProductDocument document = productDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product document not found with id: " + id));
        return toDto(document);
    }

    @Transactional
    public ProductDocumentDto uploadDocument(Long productId, Long categoryId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        // Validate category exists
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Product category not found with id: " + categoryId));

        // Validate product if provided
        Product product = null;
        if (productId != null) {
            product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        }

        // Determine storage directory
        String directory = "products/category_" + categoryId;
        if (productId != null) {
            directory = "products/product_" + productId;
        }

        // Store file
        String storagePath = storageService.storeFile(file, directory);
        String storageUrl = storageService.getFileUrl(storagePath);

        // Extract text content
        String extractedText = extractText(file);

        // Create document entity
        ProductDocument document = ProductDocument.builder()
                .product(product)
                .category(category)
                .filename(file.getOriginalFilename())
                .storagePath(storagePath)
                .storageUrl(storageUrl)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .extractedText(extractedText)
                .build();

        ProductDocument savedDocument = productDocumentRepository.save(document);
        log.info("Uploaded document: {} for product: {} category: {}",
                savedDocument.getFilename(), productId, categoryId);

        return toDto(savedDocument);
    }

    @Transactional
    public void deleteDocument(Long id) throws IOException {
        ProductDocument document = productDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product document not found with id: " + id));

        // Delete file from storage
        try {
            storageService.deleteFile(document.getStoragePath());
        } catch (IOException e) {
            log.error("Failed to delete file from storage: {}", document.getStoragePath(), e);
            // Continue with database deletion even if file deletion fails
        }

        // Delete from database
        productDocumentRepository.delete(document);
        log.info("Deleted document: {}", id);
    }

    public InputStream downloadDocument(Long id) throws IOException {
        ProductDocument document = productDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product document not found with id: " + id));

        return storageService.getFile(document.getStoragePath());
    }

    private String extractText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String text = tika.parseToString(inputStream);

            // Limit extracted text to reasonable size (e.g., 100KB)
            if (text.length() > 100000) {
                text = text.substring(0, 100000) + "... [truncated]";
            }

            log.debug("Extracted {} characters from {}", text.length(), file.getOriginalFilename());
            return text;
        } catch (IOException | TikaException e) {
            log.warn("Failed to extract text from {}: {}", file.getOriginalFilename(), e.getMessage());
            return null;
        }
    }

    private ProductDocumentDto toDto(ProductDocument document) {
        return ProductDocumentDto.builder()
                .id(document.getId())
                .productId(document.getProduct() != null ? document.getProduct().getId() : null)
                .categoryId(document.getCategory().getId())
                .filename(document.getFilename())
                .storagePath(document.getStoragePath())
                .storageUrl(document.getStorageUrl())
                .fileSize(document.getFileSize())
                .contentType(document.getContentType())
                .extractedText(document.getExtractedText())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
