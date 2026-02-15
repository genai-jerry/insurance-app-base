package com.insurance.rag.dto;

import com.insurance.common.entity.VectorEmbedding;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingDto {

    private Long id;
    private VectorEmbedding.EntityType entityType;
    private Long entityId;
    private String chunkText;
    private Map<String, Object> metadata;
    private Double similarity;
}
