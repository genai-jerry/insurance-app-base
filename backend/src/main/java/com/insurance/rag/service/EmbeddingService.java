package com.insurance.rag.service;

import com.insurance.common.entity.VectorEmbedding;
import com.insurance.rag.repository.VectorEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final VectorEmbeddingRepository vectorEmbeddingRepository;

    /**
     * Generate embedding for a text using Spring AI
     */
    public List<Double> generateEmbedding(String text) {
        log.debug("Generating embedding for text of length: {}", text.length());

        EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
        EmbeddingResponse response = embeddingModel.call(request);

        if (response.getResults().isEmpty()) {
            throw new RuntimeException("Failed to generate embedding");
        }

        // Convert float[] to List<Double>
        float[] embedding = response.getResults().get(0).getOutput();
        List<Double> embeddingList = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            embeddingList.add((double) value);
        }
        return embeddingList;
    }

    /**
     * Store embedding in vector database
     */
    @Transactional
    public VectorEmbedding storeEmbedding(
        VectorEmbedding.EntityType entityType,
        Long entityId,
        String chunkText,
        Map<String, Object> metadata
    ) {
        List<Double> embedding = generateEmbedding(chunkText);
        String embeddingString = formatEmbeddingForPostgres(embedding);

        VectorEmbedding vectorEmbedding = VectorEmbedding.builder()
            .entityType(entityType)
            .entityId(entityId)
            .chunkText(chunkText)
            .embedding(embeddingString)
            .metadataJson(metadata)
            .build();

        VectorEmbedding saved = vectorEmbeddingRepository.save(vectorEmbedding);
        log.info("Stored embedding for {} with id {}", entityType, entityId);

        return saved;
    }

    /**
     * Find similar content using vector similarity search
     */
    @Transactional(readOnly = true)
    public List<VectorEmbedding> findSimilar(String queryText, int limit) {
        List<Double> queryEmbedding = generateEmbedding(queryText);
        String embeddingString = formatEmbeddingForPostgres(queryEmbedding);

        return vectorEmbeddingRepository.findSimilarByEmbedding(embeddingString, limit);
    }

    /**
     * Find similar content of a specific entity type
     */
    @Transactional(readOnly = true)
    public List<VectorEmbedding> findSimilarByType(
        String queryText,
        VectorEmbedding.EntityType entityType,
        int limit
    ) {
        List<Double> queryEmbedding = generateEmbedding(queryText);
        String embeddingString = formatEmbeddingForPostgres(queryEmbedding);

        return vectorEmbeddingRepository.findSimilarByEmbeddingAndEntityType(
            embeddingString,
            entityType.name(),
            limit
        );
    }

    /**
     * Delete embeddings for an entity
     */
    @Transactional
    public void deleteEmbeddings(VectorEmbedding.EntityType entityType, Long entityId) {
        vectorEmbeddingRepository.deleteByEntityTypeAndEntityId(entityType, entityId);
        log.info("Deleted embeddings for {} with id {}", entityType, entityId);
    }

    /**
     * Format embedding as PostgreSQL vector format: [0.1, 0.2, 0.3, ...]
     */
    private String formatEmbeddingForPostgres(List<Double> embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
