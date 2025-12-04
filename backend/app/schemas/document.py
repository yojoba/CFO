"""Document schemas."""
from pydantic import BaseModel
from datetime import datetime, date
from typing import Optional, List, Dict
from app.models.document import DocumentType, DocumentStatus


class DocumentCreate(BaseModel):
    """Schema for document creation."""
    filename: str
    document_type: Optional[DocumentType] = DocumentType.OTHER


class DocumentMetadata(BaseModel):
    """Schema for document metadata."""
    importance_score: Optional[float] = None
    document_date: Optional[date] = None
    deadline: Optional[date] = None
    extracted_amount: Optional[float] = None
    currency: Optional[str] = None
    keywords: Optional[List[str]] = None
    classification_confidence: Optional[float] = None


class DocumentResponse(BaseModel):
    """Schema for document response."""
    id: int
    filename: str
    original_filename: str
    display_name: Optional[str] = None
    file_size: Optional[int]
    document_type: DocumentType
    status: DocumentStatus
    created_at: datetime
    extracted_text: Optional[str]
    
    # Filing cabinet fields
    storage_year: Optional[int] = None
    category: Optional[str] = None
    ocr_pdf_path: Optional[str] = None
    
    # New metadata fields
    importance_score: Optional[float] = None
    document_date: Optional[date] = None
    deadline: Optional[date] = None
    extracted_amount: Optional[float] = None
    currency: Optional[str] = None
    keywords: Optional[str] = None  # JSON string in DB
    classification_confidence: Optional[float] = None
    
    # Duplicate detection
    is_duplicate: Optional[bool] = None
    duplicate_of_id: Optional[int] = None
    similarity_score: Optional[float] = None
    
    class Config:
        from_attributes = True


class DocumentUpdate(BaseModel):
    """Schema for updating document metadata."""
    display_name: Optional[str] = None
    document_type: Optional[DocumentType] = None
    importance_score: Optional[float] = None
    document_date: Optional[date] = None
    deadline: Optional[date] = None
    extracted_amount: Optional[float] = None
    currency: Optional[str] = None
    keywords: Optional[str] = None  # JSON string


class DocumentUpdate(BaseModel):
    """Schema for updating document metadata."""
    display_name: Optional[str] = None
    importance_score: Optional[float] = None
    document_date: Optional[date] = None
    deadline: Optional[date] = None
    extracted_amount: Optional[float] = None
    currency: Optional[str] = None
    document_type: Optional[str] = None
    category: Optional[str] = None


class DocumentUploadResponse(BaseModel):
    """Schema for document upload response."""
    message: str
    document: DocumentResponse


class DocumentStatistics(BaseModel):
    """Schema for document statistics."""
    total_documents: int
    documents_by_type: Dict[str, int]
    documents_with_deadline: int
    overdue_documents: int
    upcoming_deadlines: int
    high_importance_documents: int
    average_importance_score: float
    total_amount_extracted: float


class FilingCabinetYear(BaseModel):
    """Schema for filing cabinet year with document counts."""
    year: int
    document_counts: Dict[str, int]
    total: int


class FilingCabinetStats(BaseModel):
    """Schema for complete filing cabinet statistics."""
    years: List[FilingCabinetYear]
    total_documents: int
    total_years: int


class CategoryStats(BaseModel):
    """Schema for category statistics with document type counts."""
    category: str
    document_counts: Dict[str, int]
    total: int


class FilingCabinetHierarchicalYear(BaseModel):
    """Schema for hierarchical filing cabinet year: Year > Category > Type."""
    year: int
    categories: Dict[str, Dict[str, int]]  # {category: {type: count}}
    total: int


class FilingCabinetHierarchicalOverview(BaseModel):
    """Schema for complete hierarchical filing cabinet overview."""
    years: List[FilingCabinetHierarchicalYear]
    total_documents: int
    total_years: int


class DocumentSearchResult(BaseModel):
    """Schema for document search result with hierarchy context."""
    document: DocumentResponse
    year: int
    category: str
    document_type: str
