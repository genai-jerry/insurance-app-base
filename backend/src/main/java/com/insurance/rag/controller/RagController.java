package com.insurance.rag.controller;

import com.insurance.rag.dto.ProductRecommendationRequest;
import com.insurance.rag.dto.ProductRecommendationResponse;
import com.insurance.rag.service.EmbeddingService;
import com.insurance.rag.service.ProductIndexingService;
import com.insurance.rag.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Tag(name = "RAG & Recommendations", description = "Endpoints for RAG-based product recommendations")
public class RagController {

    private final RecommendationService recommendationService;
    private final ProductIndexingService productIndexingService;
    private final EmbeddingService embeddingService;

    @PostMapping("/recommend")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get product recommendations using RAG")
    public ResponseEntity<ProductRecommendationResponse> recommendProducts(
        @Valid @RequestBody ProductRecommendationRequest request
    ) {
        ProductRecommendationResponse response = recommendationService.recommendProducts(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Re-index all products and documents in vector store")
    public ResponseEntity<Map<String, Integer>> reindex() {
        Map<String, Integer> result = productIndexingService.reindexAll();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reindex/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Re-index all products")
    public ResponseEntity<Map<String, Integer>> reindexProducts() {
        int count = productIndexingService.indexAllProducts();
        return ResponseEntity.ok(Map.of("productsIndexed", count));
    }

    @PostMapping("/reindex/documents")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Re-index all documents")
    public ResponseEntity<Map<String, Integer>> reindexDocuments() {
        int count = productIndexingService.indexAllDocuments();
        return ResponseEntity.ok(Map.of("documentsIndexed", count));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Search for similar content using vector similarity")
    public ResponseEntity<Map<String, Object>> search(
        @RequestParam String query,
        @RequestParam(defaultValue = "10") int limit
    ) {
        var results = embeddingService.findSimilar(query, limit);
        return ResponseEntity.ok(Map.of(
            "query", query,
            "results", results,
            "count", results.size()
        ));
    }
}
