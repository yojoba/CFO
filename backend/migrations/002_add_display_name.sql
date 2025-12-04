-- Migration: Add display_name field for human-readable document titles
-- Date: 2024-12-04
-- Description: Adds display_name column for AI-generated descriptive titles

-- Add display_name column
ALTER TABLE documents 
ADD COLUMN IF NOT EXISTS display_name VARCHAR(100);

-- Create index for search
CREATE INDEX IF NOT EXISTS idx_documents_display_name ON documents(display_name);

-- Add comment for documentation
COMMENT ON COLUMN documents.display_name IS 'AI-generated human-readable document title (e.g., "Facture Électricité Nov 2024")';

