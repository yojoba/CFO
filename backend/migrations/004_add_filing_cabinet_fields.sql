-- Add filing cabinet fields to documents table
-- Migration: 004_add_filing_cabinet_fields

-- Add storage_year column for year-based organization
ALTER TABLE documents 
ADD COLUMN IF NOT EXISTS storage_year INTEGER;

-- Add ocr_pdf_path column for searchable PDF with OCR layer
ALTER TABLE documents 
ADD COLUMN IF NOT EXISTS ocr_pdf_path VARCHAR;

-- Add comment to file_path to clarify it stores the original file
COMMENT ON COLUMN documents.file_path IS 'Path to original uploaded file';
COMMENT ON COLUMN documents.ocr_pdf_path IS 'Path to searchable PDF with OCR text layer';
COMMENT ON COLUMN documents.storage_year IS 'Year for filing cabinet organization (from document_date or upload date)';

-- Create index for efficient filing cabinet queries
CREATE INDEX IF NOT EXISTS idx_documents_filing_cabinet 
ON documents(user_id, storage_year, document_type)
WHERE storage_year IS NOT NULL;

-- Create index on storage_year alone for year listing queries
CREATE INDEX IF NOT EXISTS idx_documents_storage_year 
ON documents(storage_year)
WHERE storage_year IS NOT NULL;

-- Update existing documents to set storage_year from document_date or created_at
UPDATE documents 
SET storage_year = COALESCE(
    EXTRACT(YEAR FROM document_date)::INTEGER,
    EXTRACT(YEAR FROM created_at)::INTEGER
)
WHERE storage_year IS NULL;

