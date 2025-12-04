"""Virtual filing cabinet service for hierarchical document organization."""
import os
import uuid
import shutil
from pathlib import Path
from typing import Dict, List, Tuple, Optional
from datetime import datetime, date
from sqlalchemy.orm import Session
from sqlalchemy import func, distinct
from loguru import logger

from app.models.document import Document, DocumentType
from app.config import settings


class FilingCabinetService:
    """Service for organizing documents in a hierarchical filing cabinet structure."""
    
    def __init__(self):
        self.root_dir = Path(getattr(settings, 'FILING_CABINET_ROOT', settings.UPLOAD_DIR))
        self.root_dir.mkdir(parents=True, exist_ok=True)
    
    def get_organized_path(
        self,
        year: int,
        document_type: DocumentType,
        original_filename: str,
        is_ocr_pdf: bool = False,
        category: str = "General"
    ) -> str:
        """
        Generate hierarchical path for document storage.
        
        Format: /uploads/{year}/{category}/{type}/{uuid}_{original-name}.pdf
        
        Args:
            year: Storage year
            document_type: Type of document
            original_filename: Original filename
            is_ocr_pdf: Whether this is the OCR'd PDF (adds _ocr suffix)
            category: Thematic category (Impots, Assurance, etc.)
            
        Returns:
            Full path for storing document
        """
        # Create year directory
        year_dir = self.root_dir / str(year)
        
        # Create category subdirectory
        category_dir = year_dir / category
        
        # Create type subdirectory
        type_dir = category_dir / document_type.value
        
        # Generate unique filename
        file_extension = Path(original_filename).suffix
        stem = Path(original_filename).stem
        unique_id = str(uuid.uuid4())[:8]
        
        # Sanitize filename (remove special characters)
        safe_stem = "".join(c for c in stem if c.isalnum() or c in (' ', '-', '_'))[:50]
        
        if is_ocr_pdf:
            # Always use .pdf extension for OCR'd files
            filename = f"{unique_id}_{safe_stem}_ocr.pdf"
        else:
            filename = f"{unique_id}_{safe_stem}{file_extension}"
        
        full_path = type_dir / filename
        
        return str(full_path)
    
    def ensure_directory_structure(
        self, 
        year: int, 
        document_type: DocumentType,
        category: str = "General"
    ) -> Path:
        """
        Ensure year/category/type directory structure exists.
        
        Args:
            year: Storage year
            document_type: Type of document
            category: Thematic category
            
        Returns:
            Path to the created directory
        """
        year_dir = self.root_dir / str(year)
        category_dir = year_dir / category
        type_dir = category_dir / document_type.value
        
        type_dir.mkdir(parents=True, exist_ok=True)
        
        logger.debug(f"Ensured directory structure: {type_dir}")
        return type_dir
    
    async def store_document(
        self,
        original_file_path: str,
        ocr_pdf_path: str,
        year: int,
        document_type: DocumentType,
        original_filename: str,
        category: str = "General"
    ) -> Tuple[str, str]:
        """
        Store both original and OCR'd document in filing cabinet structure.
        
        Args:
            original_file_path: Path to original uploaded file
            ocr_pdf_path: Path to OCR'd PDF
            year: Storage year
            document_type: Type of document
            original_filename: Original filename
            category: Thematic category
            
        Returns:
            Tuple of (organized_original_path, organized_ocr_path)
        """
        try:
            # Ensure directory exists
            self.ensure_directory_structure(year, document_type, category)
            
            # Generate organized paths
            organized_original_path = self.get_organized_path(
                year, document_type, original_filename, is_ocr_pdf=False, category=category
            )
            organized_ocr_path = self.get_organized_path(
                year, document_type, original_filename, is_ocr_pdf=True, category=category
            )
            
            # Move/copy original file
            if os.path.exists(original_file_path):
                shutil.move(original_file_path, organized_original_path)
                logger.info(f"Moved original file to: {organized_original_path}")
            
            # Move/copy OCR'd PDF
            if os.path.exists(ocr_pdf_path) and ocr_pdf_path != organized_ocr_path:
                shutil.move(ocr_pdf_path, organized_ocr_path)
                logger.info(f"Moved OCR PDF to: {organized_ocr_path}")
            
            return organized_original_path, organized_ocr_path
            
        except Exception as e:
            logger.error(f"Error storing document in filing cabinet: {e}")
            raise
    
    def get_years_with_documents(self, user_id: int, db: Session) -> List[int]:
        """
        Get list of years that have documents for a user.
        
        Args:
            user_id: User ID
            db: Database session
            
        Returns:
            List of years (sorted descending)
        """
        years = db.query(distinct(Document.storage_year))\
            .filter(Document.user_id == user_id)\
            .filter(Document.storage_year.isnot(None))\
            .order_by(Document.storage_year.desc())\
            .all()
        
        return [year[0] for year in years]
    
    def get_document_stats_by_year(
        self,
        user_id: int,
        year: int,
        db: Session
    ) -> Dict[str, int]:
        """
        Get document counts by type for a specific year.
        
        Args:
            user_id: User ID
            year: Storage year
            db: Database session
            
        Returns:
            Dictionary of {document_type: count}
        """
        stats = db.query(
            Document.document_type,
            func.count(Document.id)
        )\
            .filter(Document.user_id == user_id)\
            .filter(Document.storage_year == year)\
            .group_by(Document.document_type)\
            .all()
        
        return {doc_type.value: count for doc_type, count in stats}
    
    def get_documents_by_year_type(
        self,
        user_id: int,
        year: int,
        document_type: Optional[DocumentType],
        db: Session,
        skip: int = 0,
        limit: int = 100
    ) -> List[Document]:
        """
        Get documents for specific year and optionally type.
        
        Args:
            user_id: User ID
            year: Storage year
            document_type: Optional document type filter
            db: Database session
            skip: Pagination offset
            limit: Pagination limit
            
        Returns:
            List of documents
        """
        query = db.query(Document)\
            .filter(Document.user_id == user_id)\
            .filter(Document.storage_year == year)
        
        if document_type:
            query = query.filter(Document.document_type == document_type)
        
        documents = query\
            .order_by(Document.document_date.desc().nullslast(), Document.created_at.desc())\
            .offset(skip)\
            .limit(limit)\
            .all()
        
        return documents
    
    def get_filing_cabinet_overview(self, user_id: int, db: Session) -> Dict:
        """
        Get complete overview of filing cabinet.
        
        Args:
            user_id: User ID
            db: Database session
            
        Returns:
            Dictionary with years and stats
        """
        years = self.get_years_with_documents(user_id, db)
        
        overview = {
            'years': [],
            'total_documents': 0,
            'total_years': len(years)
        }
        
        for year in years:
            stats = self.get_document_stats_by_year(user_id, year, db)
            year_total = sum(stats.values())
            
            overview['years'].append({
                'year': year,
                'document_counts': stats,
                'total': year_total
            })
            overview['total_documents'] += year_total
        
        return overview
    
    def get_hierarchical_overview(self, user_id: int, db: Session) -> Dict:
        """
        Get hierarchical overview of filing cabinet: Year > Category > Type.
        
        Args:
            user_id: User ID
            db: Database session
            
        Returns:
            Dictionary with hierarchical structure
        """
        years = self.get_years_with_documents(user_id, db)
        
        overview = {
            'years': [],
            'total_documents': 0,
            'total_years': len(years)
        }
        
        for year in years:
            categories_data = self.get_categories_by_year(user_id, year, db)
            year_total = 0
            
            year_data = {
                'year': year,
                'categories': {},
                'total': 0
            }
            
            for category, types_counts in categories_data.items():
                year_data['categories'][category] = types_counts
                year_total += sum(types_counts.values())
            
            year_data['total'] = year_total
            overview['years'].append(year_data)
            overview['total_documents'] += year_total
        
        return overview
    
    def get_categories_by_year(
        self,
        user_id: int,
        year: int,
        db: Session
    ) -> Dict[str, Dict[str, int]]:
        """
        Get categories and document type counts for a specific year.
        
        Args:
            user_id: User ID
            year: Storage year
            db: Database session
            
        Returns:
            Dictionary of {category: {document_type: count}}
        """
        # Query to get category, document_type, and count
        results = db.query(
            Document.category,
            Document.document_type,
            func.count(Document.id)
        )\
            .filter(Document.user_id == user_id)\
            .filter(Document.storage_year == year)\
            .group_by(Document.category, Document.document_type)\
            .all()
        
        # Organize by category > type > count
        categories_data = {}
        
        for category, doc_type, count in results:
            # Normalize category: NULL or 'General' becomes 'Non classé'
            normalized_category = self._normalize_category(category)
            
            if normalized_category not in categories_data:
                categories_data[normalized_category] = {}
            
            categories_data[normalized_category][doc_type.value] = count
        
        return categories_data
    
    def get_documents_by_year_category_type(
        self,
        user_id: int,
        year: int,
        category: str,
        document_type: Optional[DocumentType],
        db: Session,
        skip: int = 0,
        limit: int = 100
    ) -> List[Document]:
        """
        Get documents for specific year, category, and optionally type.
        
        Args:
            user_id: User ID
            year: Storage year
            category: Category name (use "Non classé" for uncategorized)
            document_type: Optional document type filter
            db: Database session
            skip: Pagination offset
            limit: Pagination limit
            
        Returns:
            List of documents
        """
        query = db.query(Document)\
            .filter(Document.user_id == user_id)\
            .filter(Document.storage_year == year)
        
        # Handle "Non classé" category
        if category == "Non classé":
            query = query.filter(
                (Document.category.is_(None)) | 
                (Document.category == 'General')
            )
        else:
            query = query.filter(Document.category == category)
        
        if document_type:
            query = query.filter(Document.document_type == document_type)
        
        documents = query\
            .order_by(Document.document_date.desc().nullslast(), Document.created_at.desc())\
            .offset(skip)\
            .limit(limit)\
            .all()
        
        return documents
    
    def get_all_categories(self, user_id: int, db: Session) -> List[str]:
        """
        Get list of all unique categories for a user (excluding 'Non classé').
        
        Args:
            user_id: User ID
            db: Database session
            
        Returns:
            List of unique category names
        """
        categories = db.query(distinct(Document.category))\
            .filter(Document.user_id == user_id)\
            .filter(Document.category.isnot(None))\
            .filter(Document.category != 'General')\
            .order_by(Document.category)\
            .all()
        
        return [cat[0] for cat in categories if cat[0]]
    
    def _normalize_category(self, category: Optional[str]) -> str:
        """
        Normalize category name for display.
        
        Args:
            category: Category name or None
            
        Returns:
            Normalized category name
        """
        if category is None or category == 'General' or category.strip() == '':
            return "Non classé"
        return category
    
    def determine_storage_year(
        self,
        document_date: Optional[date],
        created_at: datetime
    ) -> int:
        """
        Determine storage year for document.
        
        Uses document_date if available, otherwise falls back to upload date.
        
        Args:
            document_date: Extracted document date
            created_at: Upload timestamp
            
        Returns:
            Year for storage
        """
        year_source = getattr(settings, 'FILING_CABINET_YEAR_SOURCE', 'document_date')
        
        if year_source == 'document_date' and document_date:
            return document_date.year
        elif document_date:
            # Even if source is upload_date, prefer document_date if available
            return document_date.year
        else:
            # Fallback to upload date
            return created_at.year
    
    def delete_document_files(self, document: Document):
        """
        Delete both original and OCR files for a document.
        
        Args:
            document: Document model instance
        """
        files_to_delete = [document.file_path]
        if document.ocr_pdf_path:
            files_to_delete.append(document.ocr_pdf_path)
        
        for file_path in files_to_delete:
            if file_path and os.path.exists(file_path):
                try:
                    os.remove(file_path)
                    logger.info(f"Deleted file: {file_path}")
                except Exception as e:
                    logger.error(f"Error deleting file {file_path}: {e}")

