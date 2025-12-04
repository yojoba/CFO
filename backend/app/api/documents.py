"""Document API endpoints."""
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, BackgroundTasks
from sqlalchemy.orm import Session
from typing import List
from loguru import logger

from app.models.database import get_db
from app.models.user import User
from app.models.document import Document, DocumentStatus, DocumentType
from app.schemas.document import DocumentResponse, DocumentUploadResponse
from app.api.auth import get_current_user
from app.services.document_service import DocumentService
from app.services.embedding_service import EmbeddingService
from app.config import settings

router = APIRouter()


async def process_document_async(
    document_id: int,
    file_path: str,
    db: Session
):
    """Process document in background."""
    try:
        doc_service = DocumentService()
        embedding_service = EmbeddingService()
        
        # Extract text
        extracted_text = await doc_service.extract_text(file_path)
        
        # Update document with extracted text
        document = db.query(Document).filter(Document.id == document_id).first()
        if document:
            document.extracted_text = extracted_text
            document.status = DocumentStatus.PROCESSING
            db.commit()
        
        # Generate chunks and embeddings
        chunks = doc_service.create_chunks(extracted_text)
        await embedding_service.create_embeddings(document_id, chunks, db)
        
        # Mark as completed
        if document:
            document.status = DocumentStatus.COMPLETED
            db.commit()
        
        logger.info(f"Document {document_id} processed successfully")
        
    except Exception as e:
        logger.error(f"Error processing document {document_id}: {e}")
        document = db.query(Document).filter(Document.id == document_id).first()
        if document:
            document.status = DocumentStatus.FAILED
            db.commit()


@router.post("/upload", response_model=DocumentUploadResponse)
async def upload_document(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    document_type: DocumentType = DocumentType.OTHER,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Upload a document (PDF or image)."""
    # Validate file
    if not file.filename:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No file provided"
        )
    
    # Check file size
    content = await file.read()
    file_size = len(content)
    max_size = settings.MAX_UPLOAD_SIZE_MB * 1024 * 1024
    
    if file_size > max_size:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"File size exceeds maximum allowed size of {settings.MAX_UPLOAD_SIZE_MB}MB"
        )
    
    # Save file
    doc_service = DocumentService()
    file_path, saved_filename = await doc_service.save_file(file.filename, content)
    
    # Create document record
    document = Document(
        filename=saved_filename,
        original_filename=file.filename,
        file_path=file_path,
        file_size=file_size,
        mime_type=file.content_type,
        document_type=document_type,
        status=DocumentStatus.PENDING,
        user_id=current_user.id
    )
    db.add(document)
    db.commit()
    db.refresh(document)
    
    # Process document in background
    background_tasks.add_task(process_document_async, document.id, file_path, db)
    
    return DocumentUploadResponse(
        message="Document uploaded successfully and is being processed",
        document=DocumentResponse.from_orm(document)
    )


@router.get("/", response_model=List[DocumentResponse])
def get_documents(
    skip: int = 0,
    limit: int = 100,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get list of user's documents."""
    documents = db.query(Document)\
        .filter(Document.user_id == current_user.id)\
        .order_by(Document.created_at.desc())\
        .offset(skip)\
        .limit(limit)\
        .all()
    
    return [DocumentResponse.from_orm(doc) for doc in documents]


@router.get("/{document_id}", response_model=DocumentResponse)
def get_document(
    document_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get document details."""
    document = db.query(Document)\
        .filter(Document.id == document_id, Document.user_id == current_user.id)\
        .first()
    
    if not document:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document not found"
        )
    
    return DocumentResponse.from_orm(document)


@router.delete("/{document_id}")
def delete_document(
    document_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Delete a document."""
    document = db.query(Document)\
        .filter(Document.id == document_id, Document.user_id == current_user.id)\
        .first()
    
    if not document:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document not found"
        )
    
    # Delete file
    doc_service = DocumentService()
    doc_service.delete_file(document.file_path)
    
    # Delete from database
    db.delete(document)
    db.commit()
    
    return {"message": "Document deleted successfully"}
