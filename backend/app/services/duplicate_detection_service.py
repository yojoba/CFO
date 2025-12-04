"""Service for duplicate document detection."""
import hashlib
from typing import Optional, Tuple, List, Dict, Any
from sqlalchemy.orm import Session
from sqlalchemy import text, or_, and_
from loguru import logger
from datetime import datetime, timedelta

from app.models.document import Document, DocumentChunk


class DuplicateDetectionService:
    """Service to detect duplicate documents using multiple strategies."""
    
    def __init__(self):
        self.exact_match_threshold = 0.99  # For file hash matching
        self.high_similarity_threshold = 0.95  # Very similar content
        self.moderate_similarity_threshold = 0.85  # Likely duplicate
        self.metadata_match_window_days = 30  # Days to check for metadata matches
    
    def calculate_file_hash(self, file_path: str) -> str:
        """
        Calculate SHA256 hash of file content.
        
        Args:
            file_path: Path to file
            
        Returns:
            SHA256 hash string
        """
        sha256_hash = hashlib.sha256()
        
        try:
            with open(file_path, "rb") as f:
                # Read in 64kb chunks
                for byte_block in iter(lambda: f.read(65536), b""):
                    sha256_hash.update(byte_block)
            
            return sha256_hash.hexdigest()
        except Exception as e:
            logger.error(f"Error calculating file hash: {e}")
            return ""
    
    async def detect_duplicate(
        self,
        document_id: int,
        file_path: str,
        user_id: int,
        db: Session,
        extracted_text: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None
    ) -> Tuple[bool, Optional[int], float, str]:
        """
        Detect if document is a duplicate.
        
        Args:
            document_id: ID of current document
            file_path: Path to document file
            user_id: User ID
            db: Database session
            extracted_text: Extracted text (optional, for content comparison)
            metadata: Document metadata (optional, for metadata comparison)
            
        Returns:
            Tuple of (is_duplicate, original_document_id, similarity_score, detection_method)
        """
        try:
            # Strategy 1: Check exact file hash match
            file_hash = self.calculate_file_hash(file_path)
            if file_hash:
                duplicate = self._check_file_hash_duplicate(document_id, file_hash, user_id, db)
                if duplicate:
                    logger.info(f"Exact duplicate detected via file hash: {duplicate[0].id}")
                    return True, duplicate[0].id, 1.0, "exact_hash"
            
            # Strategy 2: Check content similarity via embeddings
            if extracted_text and len(extracted_text) > 100:
                duplicate = await self._check_content_similarity(
                    document_id, user_id, db
                )
                if duplicate:
                    doc_id, similarity = duplicate
                    logger.info(f"Content duplicate detected: {doc_id} (similarity: {similarity:.2f})")
                    return True, doc_id, similarity, "content_similarity"
            
            # Strategy 3: Check metadata similarity
            if metadata:
                duplicate = self._check_metadata_similarity(
                    document_id, metadata, user_id, db
                )
                if duplicate:
                    doc_id, score = duplicate
                    logger.info(f"Metadata duplicate detected: {doc_id} (score: {score:.2f})")
                    return True, doc_id, score, "metadata_match"
            
            # No duplicate found
            return False, None, 0.0, "none"
            
        except Exception as e:
            logger.error(f"Error in duplicate detection: {e}")
            return False, None, 0.0, "error"
    
    def _check_file_hash_duplicate(
        self,
        current_doc_id: int,
        file_hash: str,
        user_id: int,
        db: Session
    ) -> Optional[List[Document]]:
        """
        Check for exact file hash match.
        
        Returns:
            List of matching documents or None
        """
        if not file_hash:
            return None
        
        duplicates = db.query(Document)\
            .filter(Document.id != current_doc_id)\
            .filter(Document.user_id == user_id)\
            .filter(Document.file_hash == file_hash)\
            .limit(1)\
            .all()
        
        return duplicates if duplicates else None
    
    async def _check_content_similarity(
        self,
        current_doc_id: int,
        user_id: int,
        db: Session
    ) -> Optional[Tuple[int, float]]:
        """
        Check content similarity using vector embeddings.
        
        Returns:
            Tuple of (document_id, similarity_score) or None
        """
        try:
            # Get embeddings for current document
            current_chunks = db.query(DocumentChunk)\
                .filter(DocumentChunk.document_id == current_doc_id)\
                .filter(DocumentChunk.embedding.isnot(None))\
                .all()
            
            if not current_chunks:
                return None
            
            # Use first chunk as representative
            current_embedding = current_chunks[0].embedding
            
            # Convert embedding to proper format for pgvector
            # pgvector needs the embedding as the actual vector type, not a string
            from pgvector.sqlalchemy import Vector
            
            # Find similar documents using vector similarity
            sql = text("""
                SELECT DISTINCT
                    dc.document_id,
                    1 - (dc.embedding <=> CAST(:current_embedding AS vector)) as similarity
                FROM document_chunks dc
                JOIN documents d ON dc.document_id = d.id
                WHERE d.user_id = :user_id
                    AND dc.document_id != :current_doc_id
                    AND dc.embedding IS NOT NULL
                    AND 1 - (dc.embedding <=> CAST(:current_embedding AS vector)) >= :threshold
                ORDER BY similarity DESC
                LIMIT 1
            """)
            
            # Format embedding as pgvector string
            if hasattr(current_embedding, 'tolist'):
                embedding_list = current_embedding.tolist()
            elif isinstance(current_embedding, list):
                embedding_list = current_embedding
            else:
                embedding_list = list(current_embedding)
            
            embedding_str = '[' + ','.join(str(x) for x in embedding_list) + ']'
            
            result = db.execute(
                sql,
                {
                    "current_embedding": embedding_str,
                    "user_id": user_id,
                    "current_doc_id": current_doc_id,
                    "threshold": self.moderate_similarity_threshold
                }
            ).fetchone()
            
            if result:
                return (result[0], result[1])
            
            return None
            
        except Exception as e:
            logger.error(f"Error checking content similarity: {e}")
            # Rollback on error to clean up transaction
            try:
                db.rollback()
            except:
                pass
            return None
    
    def _check_metadata_similarity(
        self,
        current_doc_id: int,
        metadata: Dict[str, Any],
        user_id: int,
        db: Session
    ) -> Optional[Tuple[int, float]]:
        """
        Check metadata similarity (same amount, date, and type).
        
        Returns:
            Tuple of (document_id, similarity_score) or None
        """
        try:
            amount = metadata.get('amount')
            doc_date = metadata.get('document_date')
            doc_type = metadata.get('document_type')
            
            # Need at least amount and type for metadata matching
            if not amount or not doc_type:
                return None
            
            # Search for documents with same amount and type, within date window
            query = db.query(Document)\
                .filter(Document.id != current_doc_id)\
                .filter(Document.user_id == user_id)\
                .filter(Document.extracted_amount == amount)\
                .filter(Document.document_type == doc_type)
            
            # If we have a date, check within window
            if doc_date:
                try:
                    date_obj = datetime.fromisoformat(doc_date).date()
                    date_from = date_obj - timedelta(days=self.metadata_match_window_days)
                    date_to = date_obj + timedelta(days=self.metadata_match_window_days)
                    
                    query = query.filter(
                        or_(
                            and_(
                                Document.document_date >= date_from,
                                Document.document_date <= date_to
                            ),
                            Document.document_date.is_(None)
                        )
                    )
                except (ValueError, TypeError):
                    pass
            
            duplicates = query.limit(1).all()
            
            if duplicates:
                # Calculate similarity score based on matching fields
                score = 0.85  # Base score for amount + type match
                if doc_date and duplicates[0].document_date:
                    score += 0.10  # Bonus for date match
                
                return (duplicates[0].id, score)
            
            return None
            
        except Exception as e:
            logger.error(f"Error checking metadata similarity: {e}")
            # Rollback on error to clean up transaction
            try:
                db.rollback()
            except:
                pass
            return None
    
    def get_duplicate_info(self, document_id: int, db: Session) -> Optional[Dict[str, Any]]:
        """
        Get information about a duplicate document.
        
        Args:
            document_id: Document ID
            db: Database session
            
        Returns:
            Dict with duplicate info or None
        """
        document = db.query(Document).filter(Document.id == document_id).first()
        
        if not document or not document.is_duplicate:
            return None
        
        original = None
        if document.duplicate_of_id:
            original = db.query(Document).filter(Document.id == document.duplicate_of_id).first()
        
        return {
            'is_duplicate': True,
            'duplicate_of_id': document.duplicate_of_id,
            'similarity_score': document.similarity_score,
            'original_filename': original.original_filename if original else None,
            'original_display_name': original.display_name if original else None,
            'detection_method': 'See logs'
        }

