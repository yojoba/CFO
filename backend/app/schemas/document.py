"""Document schemas."""
from pydantic import BaseModel
from datetime import datetime
from typing import Optional
from app.models.document import DocumentType, DocumentStatus


class DocumentCreate(BaseModel):
    """Schema for document creation."""
    filename: str
    document_type: Optional[DocumentType] = DocumentType.OTHER


class DocumentResponse(BaseModel):
    """Schema for document response."""
    id: int
    filename: str
    original_filename: str
    file_size: Optional[int]
    document_type: DocumentType
    status: DocumentStatus
    created_at: datetime
    extracted_text: Optional[str]
    
    class Config:
        from_attributes = True


class DocumentUploadResponse(BaseModel):
    """Schema for document upload response."""
    message: str
    document: DocumentResponse
