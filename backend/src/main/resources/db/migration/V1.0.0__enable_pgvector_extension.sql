-- Enable pgvector extension for vector similarity search
-- This migration enables the pgvector extension which provides vector types and operators

CREATE EXTENSION IF NOT EXISTS vector;

-- Verify the extension is installed and working
DO $$
BEGIN
    -- Check if vector type exists
    IF NOT EXISTS (
        SELECT 1 FROM pg_type WHERE typname = 'vector'
    ) THEN
        RAISE EXCEPTION 'pgvector extension not properly installed';
    END IF;

    -- Log success
    RAISE NOTICE 'pgvector extension enabled successfully';
END $$;