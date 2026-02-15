package com.insurance.products.controller;

import com.insurance.products.dto.ProductDocumentDto;
import com.insurance.products.service.ProductDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Documents", description = "Product document management APIs")
public class ProductDocumentController {

    private final ProductDocumentService productDocumentService;

    @GetMapping("/{productId}/documents")
    @Operation(summary = "Get all documents for a product")
    public ResponseEntity<List<ProductDocumentDto>> getProductDocuments(@PathVariable Long productId) {
        List<ProductDocumentDto> documents = productDocumentService.getDocumentsByProduct(productId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/categories/{categoryId}/documents")
    @Operation(summary = "Get all documents for a category")
    public ResponseEntity<List<ProductDocumentDto>> getCategoryDocuments(@PathVariable Long categoryId) {
        List<ProductDocumentDto> documents = productDocumentService.getDocumentsByCategory(categoryId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "Get document by ID")
    public ResponseEntity<ProductDocumentDto> getDocumentById(@PathVariable Long id) {
        ProductDocumentDto document = productDocumentService.getDocumentById(id);
        return ResponseEntity.ok(document);
    }

    @PostMapping("/{productId}/documents")
    @Operation(summary = "Upload a document for a product")
    public ResponseEntity<ProductDocumentDto> uploadProductDocument(
            @Parameter(description = "Product ID")
            @PathVariable Long productId,
            @Parameter(description = "Category ID")
            @RequestParam Long categoryId,
            @Parameter(description = "Document file to upload")
            @RequestParam("file") MultipartFile file) throws IOException {

        ProductDocumentDto document = productDocumentService.uploadDocument(productId, categoryId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }

    @PostMapping("/categories/{categoryId}/documents")
    @Operation(summary = "Upload a document for a category (without product association)")
    public ResponseEntity<ProductDocumentDto> uploadCategoryDocument(
            @Parameter(description = "Category ID")
            @PathVariable Long categoryId,
            @Parameter(description = "Document file to upload")
            @RequestParam("file") MultipartFile file) throws IOException {

        ProductDocumentDto document = productDocumentService.uploadDocument(null, categoryId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }

    @GetMapping("/documents/{id}/download")
    @Operation(summary = "Download a document by ID")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) throws IOException {
        ProductDocumentDto documentDto = productDocumentService.getDocumentById(id);
        InputStream inputStream = productDocumentService.downloadDocument(id);

        InputStreamResource resource = new InputStreamResource(inputStream);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + documentDto.getFilename() + "\"");

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (documentDto.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(documentDto.getContentType());
            } catch (Exception e) {
                log.warn("Invalid content type: {}", documentDto.getContentType());
            }
        }

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(documentDto.getFileSize())
                .contentType(mediaType)
                .body(resource);
    }

    @DeleteMapping("/documents/{id}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) throws IOException {
        productDocumentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
