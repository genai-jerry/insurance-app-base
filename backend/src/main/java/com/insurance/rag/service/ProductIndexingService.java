package com.insurance.rag.service;

import com.insurance.common.entity.Product;
import com.insurance.common.entity.ProductDocument;
import com.insurance.common.entity.VectorEmbedding;
import com.insurance.products.repository.ProductRepository;
import com.insurance.products.repository.ProductDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexingService {

    private final EmbeddingService embeddingService;
    private final ProductRepository productRepository;
    private final ProductDocumentRepository productDocumentRepository;

    /**
     * Index all products in the vector database
     */
    @Transactional
    public int indexAllProducts() {
        log.info("Starting to index all products");

        List<Product> products = productRepository.findAll();
        int indexed = 0;

        for (Product product : products) {
            try {
                indexProduct(product);
                indexed++;
            } catch (Exception e) {
                log.error("Failed to index product: {}", product.getId(), e);
            }
        }

        log.info("Indexed {} products", indexed);
        return indexed;
    }

    /**
     * Index a single product
     */
    @Transactional
    public void indexProduct(Product product) {
        log.info("Indexing product: {} - {}", product.getId(), product.getName());

        // Delete existing embeddings for this product
        embeddingService.deleteEmbeddings(VectorEmbedding.EntityType.PRODUCT, product.getId());

        // Create text representation of product
        String productText = createProductText(product);

        // Create metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("productId", product.getId());
        metadata.put("productName", product.getName());
        metadata.put("insurer", product.getInsurer());
        metadata.put("planType", product.getPlanType());
        metadata.put("categoryId", product.getCategory().getId());
        metadata.put("categoryName", product.getCategory().getName());

        // Store embedding
        embeddingService.storeEmbedding(
            VectorEmbedding.EntityType.PRODUCT,
            product.getId(),
            productText,
            metadata
        );

        log.info("Indexed product: {}", product.getId());
    }

    /**
     * Index all product documents
     */
    @Transactional
    public int indexAllDocuments() {
        log.info("Starting to index all product documents");

        List<ProductDocument> documents = productDocumentRepository.findAll();
        int indexed = 0;

        for (ProductDocument document : documents) {
            try {
                indexDocument(document);
                indexed++;
            } catch (Exception e) {
                log.error("Failed to index document: {}", document.getId(), e);
            }
        }

        log.info("Indexed {} documents", indexed);
        return indexed;
    }

    /**
     * Index a single product document
     */
    @Transactional
    public void indexDocument(ProductDocument document) {
        log.info("Indexing document: {} - {}", document.getId(), document.getFilename());

        // Delete existing embeddings for this document
        embeddingService.deleteEmbeddings(VectorEmbedding.EntityType.DOC_CHUNK, document.getId());

        // Extract text from document
        String extractedText = extractTextFromDocument(document);

        if (extractedText == null || extractedText.isEmpty()) {
            log.warn("No text extracted from document: {}", document.getId());
            return;
        }

        // Split text into chunks
        List<String> chunks = splitIntoChunks(extractedText, 1000);

        // Create embeddings for each chunk
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", document.getId());
            metadata.put("filename", document.getFilename());
            metadata.put("productId", document.getProduct().getId());
            metadata.put("productName", document.getProduct().getName());
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", chunks.size());

            embeddingService.storeEmbedding(
                VectorEmbedding.EntityType.DOC_CHUNK,
                document.getId(),
                chunk,
                metadata
            );
        }

        log.info("Indexed document {} with {} chunks", document.getId(), chunks.size());
    }

    /**
     * Re-index everything
     */
    @Transactional
    public Map<String, Integer> reindexAll() {
        log.info("Starting full re-indexing");

        int productsIndexed = indexAllProducts();
        int documentsIndexed = indexAllDocuments();

        Map<String, Integer> result = new HashMap<>();
        result.put("productsIndexed", productsIndexed);
        result.put("documentsIndexed", documentsIndexed);

        log.info("Full re-indexing complete: {} products, {} documents", productsIndexed, documentsIndexed);
        return result;
    }

    /**
     * Create searchable text from product
     */
    private String createProductText(Product product) {
        StringBuilder sb = new StringBuilder();

        sb.append("Product: ").append(product.getName()).append("\n");
        sb.append("Insurer: ").append(product.getInsurer()).append("\n");
        sb.append("Category: ").append(product.getCategory().getName()).append("\n");

        if (product.getPlanType() != null) {
            sb.append("Plan Type: ").append(product.getPlanType()).append("\n");
        }

        if (product.getDetailsJson() != null) {
            sb.append("Details: ").append(product.getDetailsJson()).append("\n");
        }

        if (product.getEligibilityJson() != null) {
            sb.append("Eligibility: ").append(product.getEligibilityJson()).append("\n");
        }

        if (product.getTags() != null && !product.getTags().isEmpty()) {
            sb.append("Tags: ").append(String.join(", ", product.getTags())).append("\n");
        }

        return sb.toString();
    }

    /**
     * Extract text from document using Apache Tika
     */
    private String extractTextFromDocument(ProductDocument document) {
        if (document.getStoragePath() == null) {
            return "";
        }

        try {
            File file = new File(document.getStoragePath());
            if (!file.exists()) {
                log.warn("Document file not found: {}", document.getStoragePath());
                return "";
            }

            BodyContentHandler handler = new BodyContentHandler(-1); // No limit
            Metadata metadata = new Metadata();
            FileInputStream inputStream = new FileInputStream(file);
            ParseContext context = new ParseContext();

            Parser parser = new AutoDetectParser();
            parser.parse(inputStream, handler, metadata, context);

            inputStream.close();

            return handler.toString();

        } catch (Exception e) {
            log.error("Failed to extract text from document: {}", document.getId(), e);
            return "";
        }
    }

    /**
     * Split text into chunks
     */
    private List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new java.util.ArrayList<>();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // Try to break at a sentence or word boundary
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('.', end);
                int lastSpace = text.lastIndexOf(' ', end);

                if (lastPeriod > start + chunkSize / 2) {
                    end = lastPeriod + 1;
                } else if (lastSpace > start + chunkSize / 2) {
                    end = lastSpace;
                }
            }

            chunks.add(text.substring(start, end).trim());
            start = end;
        }

        return chunks;
    }
}
