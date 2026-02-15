package com.insurance.rag.repository;

import com.insurance.common.entity.VectorEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VectorEmbeddingRepository extends JpaRepository<VectorEmbedding, Long> {

    @Query("SELECT ve FROM VectorEmbedding ve WHERE ve.entityType = :entityType AND ve.entityId = :entityId")
    List<VectorEmbedding> findByEntityTypeAndEntityId(
        @Param("entityType") VectorEmbedding.EntityType entityType,
        @Param("entityId") Long entityId
    );

    @Query("SELECT ve FROM VectorEmbedding ve WHERE ve.entityType = :entityType")
    List<VectorEmbedding> findByEntityType(@Param("entityType") VectorEmbedding.EntityType entityType);

    /**
     * Find similar embeddings using cosine distance
     * Note: This uses native query for pgvector support
     */
    @Query(value = "SELECT * FROM vector_embeddings " +
           "ORDER BY embedding <=> CAST(:queryEmbedding AS vector) " +
           "LIMIT :limit", nativeQuery = true)
    List<VectorEmbedding> findSimilarByEmbedding(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("limit") int limit
    );

    /**
     * Find similar embeddings of a specific entity type
     */
    @Query(value = "SELECT * FROM vector_embeddings " +
           "WHERE entity_type = :entityType " +
           "ORDER BY embedding <=> CAST(:queryEmbedding AS vector) " +
           "LIMIT :limit", nativeQuery = true)
    List<VectorEmbedding> findSimilarByEmbeddingAndEntityType(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("entityType") String entityType,
        @Param("limit") int limit
    );

    void deleteByEntityTypeAndEntityId(VectorEmbedding.EntityType entityType, Long entityId);
}
