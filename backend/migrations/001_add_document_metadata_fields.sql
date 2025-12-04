-- Migration: Add document metadata fields for intelligent document processing
-- Date: 2024-12-04
-- Description: Adds fields for OCR, AI analysis, importance scoring, and deadline tracking

-- Add new columns to documents table
ALTER TABLE documents 
ADD COLUMN IF NOT EXISTS importance_score FLOAT,
ADD COLUMN IF NOT EXISTS document_date DATE,
ADD COLUMN IF NOT EXISTS deadline DATE,
ADD COLUMN IF NOT EXISTS extracted_amount NUMERIC(10, 2),
ADD COLUMN IF NOT EXISTS currency VARCHAR DEFAULT 'CHF',
ADD COLUMN IF NOT EXISTS keywords TEXT,
ADD COLUMN IF NOT EXISTS classification_confidence FLOAT;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_documents_importance_score ON documents(importance_score DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_documents_deadline ON documents(deadline ASC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_documents_document_date ON documents(document_date DESC NULLS LAST);

-- Add comments for documentation
COMMENT ON COLUMN documents.importance_score IS 'Calculated importance score (0-100) based on multiple factors';
COMMENT ON COLUMN documents.document_date IS 'Date extracted from document content';
COMMENT ON COLUMN documents.deadline IS 'Deadline date if applicable (payment, response, etc.)';
COMMENT ON COLUMN documents.extracted_amount IS 'Main amount extracted from document (invoices, receipts)';
COMMENT ON COLUMN documents.currency IS 'Currency of the extracted amount';
COMMENT ON COLUMN documents.keywords IS 'JSON array of important keywords extracted from document';
COMMENT ON COLUMN documents.classification_confidence IS 'AI classification confidence level (0.0-1.0)';

