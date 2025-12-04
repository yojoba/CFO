-- Add category field for 3-level hierarchical organization
-- Migration: 005_add_document_category

-- Add category column for thematic organization (Impots, Assurance, Poursuites, etc.)
ALTER TABLE documents 
ADD COLUMN IF NOT EXISTS category VARCHAR;

-- Add comment
COMMENT ON COLUMN documents.category IS 'Thematic category for 3-level filing: year/category/type';

-- Create index for efficient category queries
CREATE INDEX IF NOT EXISTS idx_documents_category 
ON documents(category)
WHERE category IS NOT NULL;

-- Create compound index for 3-level filing cabinet
CREATE INDEX IF NOT EXISTS idx_documents_filing_3level 
ON documents(user_id, storage_year, category, document_type)
WHERE storage_year IS NOT NULL AND category IS NOT NULL;

-- Set default category for existing documents
UPDATE documents 
SET category = 'General'
WHERE category IS NULL;

