-- Migration: Add duplicate detection fields
-- Date: 2024-12-04
-- Description: Adds fields for detecting and tracking duplicate documents

-- Add duplicate detection columns
ALTER TABLE documents 
ADD COLUMN IF NOT EXISTS file_hash VARCHAR(64),
ADD COLUMN IF NOT EXISTS is_duplicate BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS duplicate_of_id INTEGER REFERENCES documents(id),
ADD COLUMN IF NOT EXISTS similarity_score FLOAT;

-- Create indexes for efficient duplicate detection
CREATE INDEX IF NOT EXISTS idx_documents_file_hash ON documents(file_hash);
CREATE INDEX IF NOT EXISTS idx_documents_is_duplicate ON documents(is_duplicate);
CREATE INDEX IF NOT EXISTS idx_documents_duplicate_of_id ON documents(duplicate_of_id);

-- Add comments for documentation
COMMENT ON COLUMN documents.file_hash IS 'SHA256 hash of file content for exact duplicate detection';
COMMENT ON COLUMN documents.is_duplicate IS 'Boolean flag indicating if this document is a duplicate';
COMMENT ON COLUMN documents.duplicate_of_id IS 'ID of the original document if this is a duplicate';
COMMENT ON COLUMN documents.similarity_score IS 'Similarity score with original document (0.0-1.0)';

