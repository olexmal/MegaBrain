-- Create vector storage schema for similarity search
-- This migration creates the necessary tables for storing code chunks with their vector embeddings

-- Create the main vectors table
CREATE TABLE code_chunks_vectors (
    id VARCHAR(255) PRIMARY KEY,
    content TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_name VARCHAR(500) NOT NULL,
    source_file VARCHAR(1000) NOT NULL,
    start_line INTEGER NOT NULL,
    end_line INTEGER NOT NULL,
    start_byte INTEGER NOT NULL,
    end_byte INTEGER NOT NULL,
    embedding vector(384) NOT NULL, -- 384 dimensions for all-MiniLM-L6-v2
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_code_chunks_vectors_language ON code_chunks_vectors(language);
CREATE INDEX idx_code_chunks_vectors_entity_type ON code_chunks_vectors(entity_type);
CREATE INDEX idx_code_chunks_vectors_source_file ON code_chunks_vectors(source_file);

-- Create a partial index for faster entity name searches within specific languages
CREATE INDEX idx_code_chunks_vectors_entity_name_java ON code_chunks_vectors(entity_name)
    WHERE language = 'java';

-- Add comments for documentation
COMMENT ON TABLE code_chunks_vectors IS 'Stores code chunks with their vector embeddings for similarity search';
COMMENT ON COLUMN code_chunks_vectors.id IS 'Unique identifier for the code chunk';
COMMENT ON COLUMN code_chunks_vectors.embedding IS '384-dimensional vector embedding using all-MiniLM-L6-v2 model';
COMMENT ON COLUMN code_chunks_vectors.created_at IS 'Timestamp when the chunk was first stored';
COMMENT ON COLUMN code_chunks_vectors.updated_at IS 'Timestamp when the chunk was last updated';

-- Create a function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_code_chunks_vectors_updated_at
    BEFORE UPDATE ON code_chunks_vectors
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();