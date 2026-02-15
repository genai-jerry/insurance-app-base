-- Vector embeddings table for RAG
CREATE TABLE vector_embeddings (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL CHECK (entity_type IN ('PRODUCT', 'DOC_CHUNK')),
    entity_id BIGINT NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(1536),
    metadata_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vector_embeddings_entity ON vector_embeddings(entity_type, entity_id);
CREATE INDEX idx_vector_embeddings_ivfflat ON vector_embeddings
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
