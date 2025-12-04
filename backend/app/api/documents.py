"""Document API endpoints."""
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, BackgroundTasks, Query
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from sqlalchemy import func
from typing import List, Optional
from datetime import datetime, timedelta, date
from loguru import logger
import os
from jose import JWTError, jwt

from app.models.database import get_db
from app.models.user import User
from app.models.document import Document, DocumentStatus, DocumentType
from app.schemas.document import (
    DocumentResponse, DocumentUploadResponse, DocumentStatistics, 
    DocumentUpdate, FilingCabinetHierarchicalOverview, CategoryStats,
    DocumentSearchResult
)
from app.api.auth import get_current_user
from app.config import settings
from app.services.document_service import DocumentService
from app.services.embedding_service import EmbeddingService
from app.services.document_analysis_service import DocumentAnalysisService
from app.services.duplicate_detection_service import DuplicateDetectionService
from app.services.pdf_conversion_service import PDFConversionService
from app.services.filing_cabinet_service import FilingCabinetService
from app.services.image_preprocessing_service import ImagePreprocessingService
from app.config import settings

router = APIRouter()


def get_user_from_token_or_header(
    token: Optional[str] = Query(None),
    current_user: Optional[User] = Depends(get_current_user)
) -> User:
    """
    Get user from token in URL query parameter or from Authorization header.
    Used for endpoints that need to work with direct URLs (downloads, previews).
    """
    # If we have a user from header auth, use it
    if current_user:
        return current_user
    
    # Otherwise, try to validate token from URL
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Not authenticated"
        )
    
    try:
        payload = jwt.decode(token, settings.JWT_SECRET, algorithms=[settings.JWT_ALGORITHM])
        user_id: str = payload.get("sub")
        if user_id is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid authentication credentials"
            )
    except JWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication credentials"
        )
    
    # Get user from database
    from app.models.database import SessionLocal
    db = SessionLocal()
    try:
        user = db.query(User).filter(User.id == int(user_id)).first()
        if user is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User not found"
            )
        return user
    finally:
        db.close()


async def process_document_async(
    document_id: int,
    file_path: str,
    mime_type: Optional[str],
    db: Session
):
    """Process document in background with enhanced analysis and filing cabinet organization."""
    try:
        doc_service = DocumentService()
        embedding_service = EmbeddingService()
        analysis_service = DocumentAnalysisService()
        duplicate_service = DuplicateDetectionService()
        pdf_conversion_service = PDFConversionService()
        filing_cabinet_service = FilingCabinetService()
        preprocessing_service = ImagePreprocessingService()
        
        # Update status to processing
        document = db.query(Document).filter(Document.id == document_id).first()
        if not document:
            logger.error(f"Document {document_id} not found")
            return
            
        document.status = DocumentStatus.PROCESSING
        db.commit()
        
        # Step 1: Calculate file hash for duplicate detection
        file_hash = duplicate_service.calculate_file_hash(file_path)
        if file_hash:
            document.file_hash = file_hash
            db.commit()
        
        # Step 2: Preprocess image (auto-crop, deskew, enhance) if it's an image
        preprocessed_path = file_path
        if mime_type and mime_type.startswith('image/'):
            logger.info(f"Preprocessing image for document {document_id}")
            import tempfile
            with tempfile.NamedTemporaryFile(suffix='.jpg', delete=False) as temp_preprocessed:
                temp_preprocessed_path = temp_preprocessed.name
            
            success, preprocessed_path = preprocessing_service.preprocess_for_ocr(
                file_path,
                temp_preprocessed_path
            )
            
            if success:
                logger.info(f"✓ Image preprocessing successful for document {document_id}")
            else:
                logger.warning(f"Image preprocessing failed, using original")
                preprocessed_path = file_path
        
        # Step 3: Perform complete document analysis (OCR + AI) on preprocessed image
        logger.info(f"Starting intelligent analysis for document {document_id}")
        analysis_result = await analysis_service.analyze_document(preprocessed_path, mime_type)
        
        # Step 4: Update document with analysis results
        db_fields = analysis_service.prepare_database_fields(analysis_result)
        
        # Update all fields
        for field, value in db_fields.items():
            setattr(document, field, value)
        
        db.commit()
        db.refresh(document)
        
        importance_str = f"{document.importance_score:.1f}" if document.importance_score else "N/A"
        logger.info(f"Document {document_id} metadata updated: "
                   f"type={document.document_type}, "
                   f"importance={importance_str}")
        
        # Step 5: Determine storage year for filing cabinet
        storage_year = filing_cabinet_service.determine_storage_year(
            document.document_date,
            document.created_at
        )
        document.storage_year = storage_year
        db.commit()
        logger.info(f"Document {document_id} storage year: {storage_year}")
        
        # Step 6: Convert to searchable PDF with OCR layer using preprocessed image
        logger.info(f"Converting document {document_id} to searchable PDF")
        import tempfile
        with tempfile.NamedTemporaryFile(suffix='.pdf', delete=False) as temp_ocr:
            temp_ocr_path = temp_ocr.name
        
        try:
            # Use preprocessed image for PDF creation (better quality)
            success, ocr_pdf_path, error = await pdf_conversion_service.ensure_searchable_pdf(
                preprocessed_path,
                temp_ocr_path,
                languages=['fra', 'deu', 'eng']
            )
            
            if success:
                logger.info(f"Successfully created searchable PDF for document {document_id}")
                
                # Step 7: Organize files in filing cabinet structure (3-level hierarchy)
                # Use original file (not preprocessed) for storage, but preprocessed was used for OCR/PDF
                category = analysis_result.get('metadata', {}).get('category', 'General')
                organized_original_path, organized_ocr_path = await filing_cabinet_service.store_document(
                    original_file_path=file_path,
                    ocr_pdf_path=ocr_pdf_path,
                    year=storage_year,
                    document_type=document.document_type,
                    original_filename=document.original_filename,
                    category=category
                )
                
                # Update category in database
                document.category = category
                
                # Update paths in database
                document.file_path = organized_original_path
                document.ocr_pdf_path = organized_ocr_path
                db.commit()
                
                logger.info(f"Document {document_id} organized in filing cabinet: "
                          f"{storage_year}/{category}/{document.document_type.value}")
                
                # Extract text from searchable PDF for better RAG quality
                extracted_text = pdf_conversion_service.extract_text_from_searchable_pdf(organized_ocr_path)
                if extracted_text:
                    document.extracted_text = extracted_text
                    db.commit()
                    logger.info(f"Extracted text from searchable PDF for document {document_id}")
            else:
                logger.warning(f"PDF conversion failed for document {document_id}: {error}")
                # Continue with original file
                extracted_text = analysis_result.get('extracted_text', '')
        finally:
            # Clean up temp files
            if os.path.exists(temp_ocr_path):
                os.remove(temp_ocr_path)
            # Clean up preprocessed image if it was created
            if preprocessed_path != file_path and os.path.exists(preprocessed_path):
                os.remove(preprocessed_path)
        
        # Step 8: Generate chunks and embeddings for RAG
        extracted_text = document.extracted_text or analysis_result.get('extracted_text', '')
        if extracted_text:
            chunks = doc_service.create_chunks(extracted_text)
            await embedding_service.create_embeddings(document_id, chunks, db)
            logger.info(f"Created {len(chunks)} embeddings for document {document_id}")
        
        # Step 9: Detect duplicates - BLOCK if exact duplicate found
        try:
            is_duplicate, original_id, similarity, method = await duplicate_service.detect_duplicate(
                document_id=document_id,
                file_path=document.file_path,
                user_id=document.user_id,
                db=db,
                extracted_text=extracted_text,
                metadata=analysis_result.get('metadata', {})
            )
            
            if is_duplicate and similarity >= 0.95:  # Very high similarity = exact duplicate
                # Delete the duplicate and keep only the original
                logger.warning(f"🚫 EXACT DUPLICATE detected for document {document_id}: "
                             f"original={original_id}, similarity={similarity:.2f}, method={method}")
                logger.warning(f"🗑️ Rejecting duplicate and keeping original document {original_id}")
                
                # Delete files
                filing_cabinet_service.delete_document_files(document)
                
                # Delete from database
                db.delete(document)
                db.commit()
                
                logger.info(f"✅ Duplicate document {document_id} removed, original {original_id} kept")
                return  # Exit processing
            elif is_duplicate:
                # Mark as potential duplicate but keep it
                document.is_duplicate = True
                document.duplicate_of_id = original_id
                document.similarity_score = similarity
                db.commit()
                logger.warning(f"⚠️ Potential duplicate detected for document {document_id}: "
                             f"original={original_id}, similarity={similarity:.2f}, method={method}")
            else:
                logger.info(f"✅ No duplicate found for document {document_id}")
        except Exception as e:
            # Don't fail the whole process if duplicate detection fails
            logger.error(f"Error in duplicate detection for document {document_id}: {e}")
            # Rollback any failed transaction
            db.rollback()
            # Refresh document state
            db.refresh(document)
        
        # Step 10: Mark as completed
        document.status = DocumentStatus.COMPLETED
        db.commit()
        
        category_str = document.category or 'General'
        logger.info(f"✅ Document {document_id} processed successfully and filed in {storage_year}/{category_str}/{document.document_type.value}")
        
    except Exception as e:
        logger.error(f"❌ Error processing document {document_id}: {e}")
        import traceback
        logger.error(traceback.format_exc())
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
    background_tasks.add_task(process_document_async, document.id, file_path, file.content_type, db)
    
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


@router.get("/urgent", response_model=List[DocumentResponse])
def get_urgent_documents(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get urgent documents (deadline < 7 days or importance score > 80)."""
    seven_days_from_now = date.today() + timedelta(days=7)
    
    # Documents with upcoming deadlines OR high importance score
    documents = db.query(Document)\
        .filter(Document.user_id == current_user.id)\
        .filter(
            (Document.deadline <= seven_days_from_now) |
            (Document.importance_score > 80)
        )\
        .order_by(Document.importance_score.desc().nullslast(), Document.deadline.asc().nullslast())\
        .all()
    
    return [DocumentResponse.from_orm(doc) for doc in documents]


@router.get("/by-importance", response_model=List[DocumentResponse])
def get_documents_by_importance(
    skip: int = 0,
    limit: int = 100,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get documents sorted by importance score (highest first)."""
    documents = db.query(Document)\
        .filter(Document.user_id == current_user.id)\
        .order_by(Document.importance_score.desc().nullslast(), Document.created_at.desc())\
        .offset(skip)\
        .limit(limit)\
        .all()
    
    return [DocumentResponse.from_orm(doc) for doc in documents]


@router.get("/by-deadline", response_model=List[DocumentResponse])
def get_documents_by_deadline(
    skip: int = 0,
    limit: int = 100,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get documents sorted by deadline (closest first)."""
    documents = db.query(Document)\
        .filter(Document.user_id == current_user.id)\
        .filter(Document.deadline.isnot(None))\
        .order_by(Document.deadline.asc())\
        .offset(skip)\
        .limit(limit)\
        .all()
    
    return [DocumentResponse.from_orm(doc) for doc in documents]


@router.get("/duplicates", response_model=List[DocumentResponse])
def get_duplicate_documents(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get all potential duplicate documents (similarity < 95%)."""
    documents = db.query(Document)\
        .filter(Document.user_id == current_user.id)\
        .filter(Document.is_duplicate == True)\
        .order_by(Document.similarity_score.desc())\
        .all()
    
    return [DocumentResponse.from_orm(doc) for doc in documents]


@router.delete("/duplicates/cleanup")
def cleanup_duplicates(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Delete all duplicate documents, keeping only originals."""
    duplicates = db.query(Document)\
        .filter(Document.user_id == current_user.id)\
        .filter(Document.is_duplicate == True)\
        .all()
    
    filing_cabinet_service = FilingCabinetService()
    deleted_count = 0
    
    for doc in duplicates:
        try:
            # Delete files
            filing_cabinet_service.delete_document_files(doc)
            # Delete from database
            db.delete(doc)
            deleted_count += 1
        except Exception as e:
            logger.error(f"Error deleting duplicate {doc.id}: {e}")
    
    db.commit()
    
    return {
        "message": f"{deleted_count} duplicate(s) deleted successfully",
        "deleted_count": deleted_count
    }


@router.get("/statistics", response_model=DocumentStatistics)
def get_document_statistics(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get statistics about user's documents."""
    from sqlalchemy import func
    
    # Total documents
    total_documents = db.query(func.count(Document.id))\
        .filter(Document.user_id == current_user.id)\
        .scalar()
    
    # Documents by type
    type_counts = db.query(Document.document_type, func.count(Document.id))\
        .filter(Document.user_id == current_user.id)\
        .group_by(Document.document_type)\
        .all()
    
    documents_by_type = {doc_type.value: count for doc_type, count in type_counts}
    
    # Documents with deadlines
    documents_with_deadline = db.query(func.count(Document.id))\
        .filter(Document.user_id == current_user.id)\
        .filter(Document.deadline.isnot(None))\
        .scalar()
    
    # Overdue documents
    overdue_documents = db.query(func.count(Document.id))\
        .filter(Document.user_id == current_user.id)\
        .filter(Document.deadline < date.today())\
        .scalar()
    
    # Upcoming deadlines (next 7 days)
    seven_days_from_now = date.today() + timedelta(days=7)
    upcoming_deadlines = db.query(func.count(Document.id))\
        .filter(Document.user_id == current_user.id)\
        .filter(Document.deadline >= date.today())\
        .filter(Document.deadline <= seven_days_from_now)\
        .scalar()
    
    # High importance documents (score > 80)
    high_importance = db.query(func.count(Document.id))\
        .filter(Document.user_id == current_user.id)\
        .filter(Document.importance_score > 80)\
        .scalar()
    
    # Average importance score
    avg_importance = db.query(func.avg(Document.importance_score))\
        .filter(Document.user_id == current_user.id)\
        .filter(Document.importance_score.isnot(None))\
        .scalar()
    
    # Total amount from invoices/receipts
    total_amount = db.query(func.sum(Document.extracted_amount))\
        .filter(Document.user_id == current_user.id)\
        .filter(Document.extracted_amount.isnot(None))\
        .scalar()
    
    return DocumentStatistics(
        total_documents=total_documents or 0,
        documents_by_type=documents_by_type,
        documents_with_deadline=documents_with_deadline or 0,
        overdue_documents=overdue_documents or 0,
        upcoming_deadlines=upcoming_deadlines or 0,
        high_importance_documents=high_importance or 0,
        average_importance_score=float(avg_importance) if avg_importance else 0.0,
        total_amount_extracted=float(total_amount) if total_amount else 0.0
    )


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


@router.patch("/{document_id}", response_model=DocumentResponse)
def update_document(
    document_id: int,
    document_update: DocumentUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Update document metadata."""
    document = db.query(Document)\
        .filter(Document.id == document_id, Document.user_id == current_user.id)\
        .first()
    
    if not document:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document not found"
        )
    
    # Update only provided fields
    update_data = document_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(document, field, value)
    
    db.commit()
    db.refresh(document)
    
    logger.info(f"Document {document_id} updated by user {current_user.id}")
    
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
    
    # Delete files using filing cabinet service
    filing_cabinet_service = FilingCabinetService()
    filing_cabinet_service.delete_document_files(document)
    
    # Delete from database
    db.delete(document)
    db.commit()
    
    return {"message": "Document deleted successfully"}


# ============================================================================
# FILING CABINET ENDPOINTS
# ============================================================================

@router.get("/filing-cabinet/years", response_model=List[int])
def get_filing_cabinet_years(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get list of years with documents in filing cabinet."""
    filing_cabinet_service = FilingCabinetService()
    years = filing_cabinet_service.get_years_with_documents(current_user.id, db)
    return years


@router.get("/filing-cabinet/overview")
def get_filing_cabinet_overview(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get complete filing cabinet overview with all years and stats."""
    filing_cabinet_service = FilingCabinetService()
    overview = filing_cabinet_service.get_filing_cabinet_overview(current_user.id, db)
    return overview


@router.get("/filing-cabinet/hierarchical-overview", response_model=FilingCabinetHierarchicalOverview)
def get_hierarchical_filing_cabinet_overview(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get hierarchical filing cabinet overview: Year > Category > Type."""
    filing_cabinet_service = FilingCabinetService()
    overview = filing_cabinet_service.get_hierarchical_overview(current_user.id, db)
    return overview


@router.get("/filing-cabinet/{year}")
def get_year_stats(
    year: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get document statistics for a specific year."""
    filing_cabinet_service = FilingCabinetService()
    stats = filing_cabinet_service.get_document_stats_by_year(current_user.id, year, db)
    
    return {
        "year": year,
        "document_counts": stats,
        "total": sum(stats.values())
    }


@router.get("/filing-cabinet/{year}/{document_type}", response_model=List[DocumentResponse])
def get_documents_by_year_type(
    year: int,
    document_type: str,
    skip: int = 0,
    limit: int = 100,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get documents for specific year and type."""
    try:
        doc_type = DocumentType(document_type)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid document type: {document_type}"
        )
    
    filing_cabinet_service = FilingCabinetService()
    documents = filing_cabinet_service.get_documents_by_year_type(
        current_user.id,
        year,
        doc_type,
        db,
        skip,
        limit
    )
    
    return [DocumentResponse.from_orm(doc) for doc in documents]


@router.get("/filing-cabinet/{year}/categories")
def get_year_categories(
    year: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get categories and document counts for a specific year."""
    filing_cabinet_service = FilingCabinetService()
    categories = filing_cabinet_service.get_categories_by_year(current_user.id, year, db)
    
    return {
        "year": year,
        "categories": categories
    }


@router.get("/filing-cabinet/{year}/{category}/{document_type}", response_model=List[DocumentResponse])
def get_documents_by_year_category_type(
    year: int,
    category: str,
    document_type: str,
    skip: int = 0,
    limit: int = 100,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get documents for specific year, category, and type."""
    try:
        doc_type = DocumentType(document_type)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid document type: {document_type}"
        )
    
    filing_cabinet_service = FilingCabinetService()
    documents = filing_cabinet_service.get_documents_by_year_category_type(
        current_user.id,
        year,
        category,
        doc_type,
        db,
        skip,
        limit
    )
    
    return [DocumentResponse.from_orm(doc) for doc in documents]


@router.get("/categories", response_model=List[str])
def get_all_categories(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get list of all unique categories for the current user."""
    filing_cabinet_service = FilingCabinetService()
    categories = filing_cabinet_service.get_all_categories(current_user.id, db)
    return categories


@router.get("/search", response_model=List[DocumentResponse])
def search_documents(
    q: str = Query(..., description="Search query"),
    skip: int = 0,
    limit: int = 100,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Search documents by text content (display_name, extracted_text, keywords)."""
    if not q or len(q.strip()) < 2:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Search query must be at least 2 characters"
        )
    
    search_term = f"%{q.lower()}%"
    
    # Search in display_name, extracted_text, and original_filename
    documents = db.query(Document)\
        .filter(Document.user_id == current_user.id)\
        .filter(
            (func.lower(Document.display_name).like(search_term)) |
            (func.lower(Document.original_filename).like(search_term)) |
            (func.lower(Document.extracted_text).like(search_term)) |
            (func.lower(Document.keywords).like(search_term))
        )\
        .order_by(Document.importance_score.desc().nullslast(), Document.created_at.desc())\
        .offset(skip)\
        .limit(limit)\
        .all()
    
    return [DocumentResponse.from_orm(doc) for doc in documents]


@router.get("/{document_id}/download/original")
def download_original_document(
    document_id: int,
    token: Optional[str] = Query(None),
    db: Session = Depends(get_db)
):
    """Download original uploaded document."""
    # Get user from token (URL) or header
    user = get_user_from_token_or_header(token, None)
    
    document = db.query(Document)\
        .filter(Document.id == document_id, Document.user_id == user.id)\
        .first()
    
    if not document:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document not found"
        )
    
    if not document.file_path or not os.path.exists(document.file_path):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Original file not found"
        )
    
    return FileResponse(
        path=document.file_path,
        filename=document.original_filename,
        media_type=document.mime_type or 'application/octet-stream',
        headers={"Content-Disposition": f'attachment; filename="{document.original_filename}"'}
    )


@router.get("/{document_id}/download/ocr-pdf")
def download_ocr_pdf(
    document_id: int,
    token: Optional[str] = Query(None),
    db: Session = Depends(get_db)
):
    """Download searchable PDF with OCR layer."""
    # Get user from token (URL) or header
    user = get_user_from_token_or_header(token, None)
    
    document = db.query(Document)\
        .filter(Document.id == document_id, Document.user_id == user.id)\
        .first()
    
    if not document:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document not found"
        )
    
    if not document.ocr_pdf_path or not os.path.exists(document.ocr_pdf_path):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="OCR PDF not found"
        )
    
    # Generate filename for OCR PDF
    filename_base = os.path.splitext(document.original_filename)[0]
    ocr_filename = f"{filename_base}_searchable.pdf"
    
    return FileResponse(
        path=document.ocr_pdf_path,
        filename=ocr_filename,
        media_type='application/pdf',
        headers={"Content-Disposition": f'attachment; filename="{ocr_filename}"'}
    )


@router.get("/{document_id}/preview")
def preview_document(
    document_id: int,
    token: Optional[str] = Query(None),
    db: Session = Depends(get_db)
):
    """Preview document (serves OCR PDF for viewing)."""
    # Get user from token (URL) or header
    user = get_user_from_token_or_header(token, None)
    
    document = db.query(Document)\
        .filter(Document.id == document_id, Document.user_id == user.id)\
        .first()
    
    if not document:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Document not found"
        )
    
    # Prefer OCR PDF for preview, fallback to original if PDF
    preview_path = None
    if document.ocr_pdf_path and os.path.exists(document.ocr_pdf_path):
        preview_path = document.ocr_pdf_path
    elif document.file_path and os.path.exists(document.file_path):
        if document.mime_type == 'application/pdf':
            preview_path = document.file_path
    
    if not preview_path:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Preview not available"
        )
    
    return FileResponse(
        path=preview_path,
        media_type='application/pdf',
        headers={"Content-Disposition": "inline"}
    )
