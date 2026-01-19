-- Add vector indexes for improved similarity search performance
-- This migration creates HNSW indexes on the embedding column for efficient vector similarity search

-- Create HNSW index for cosine similarity search
-- HNSW (Hierarchical Navigable Small World) provides excellent performance for high-dimensional vectors
-- Parameters:
--   m: maximum number of connections per layer (default 16, good balance of speed vs accuracy)
--   ef_construction: size of the dynamic candidate list during index construction (default 64)

CREATE INDEX CONCURRENTLY idx_code_chunks_vectors_embedding_hnsw
    ON code_chunks_vectors
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Alternative IVFFlat index (commented out, but available if HNSW performance is insufficient)
-- IVFFlat is faster to build but generally slower to query than HNSW
-- Uncomment if you need faster index creation and can tolerate slightly lower accuracy/speed
-- CREATE INDEX CONCURRENTLY idx_code_chunks_vectors_embedding_ivfflat
--     ON code_chunks_vectors
--     USING ivfflat (embedding vector_cosine_ops)
--     WITH (lists = 100);  -- Number of lists, should be roughly sqrt(table_size)

-- Add comments for documentation
COMMENT ON INDEX idx_code_chunks_vectors_embedding_hnsw IS
    'HNSW index for efficient cosine similarity search on 384-dimensional embeddings. ' ||
    'm=16 provides good balance of index size and search speed. ' ||
    'ef_construction=64 provides good build time vs search quality tradeoff.';

-- Note: After creating the index, you may want to adjust the ef_search parameter for queries
-- Higher ef_search values improve recall but increase query time
-- Example: SET hnsw.ef_search = 100; (default is 40)