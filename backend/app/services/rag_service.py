"""RAG (Retrieval Augmented Generation) service."""
from typing import List, Dict, Any
from sqlalchemy.orm import Session
from sqlalchemy import text
from loguru import logger

from app.models.document import DocumentChunk, Document
from app.services.embedding_service import EmbeddingService
from app.config import settings


class RAGService:
    """Service for semantic search using RAG."""
    
    def __init__(self):
        self.embedding_service = EmbeddingService()
    
    async def search_documents(
        self,
        query: str,
        user_id: int,
        db: Session,
        max_results: int = None,
        similarity_threshold: float = None
    ) -> List[Dict[str, Any]]:
        """Search documents using semantic similarity.
        
        Args:
            query: Search query
            user_id: User ID to filter documents
            db: Database session
            max_results: Maximum number of results (default from settings)
            similarity_threshold: Minimum similarity score (default from settings)
            
        Returns:
            List of relevant document chunks with metadata
        """
        if max_results is None:
            max_results = settings.MAX_RESULTS
        if similarity_threshold is None:
            similarity_threshold = settings.SIMILARITY_THRESHOLD
        
        try:
            # Create embedding for query
            query_embedding = await self.embedding_service.create_embedding(query)
            
            # Perform vector similarity search
            # Using cosine distance (1 - cosine_similarity)
            # Include filing cabinet metadata for agent context
            sql = text("""
                SELECT 
                    dc.id,
                    dc.content,
                    dc.chunk_index,
                    dc.page_number,
                    dc.document_id,
                    d.filename,
                    d.original_filename,
                    d.display_name,
                    d.document_type,
                    d.storage_year,
                    d.document_date,
                    d.file_path,
                    d.ocr_pdf_path,
                    d.created_at,
                    1 - (dc.embedding <=> :query_embedding) as similarity
                FROM document_chunks dc
                JOIN documents d ON dc.document_id = d.id
                WHERE d.user_id = :user_id
                    AND dc.embedding IS NOT NULL
                    AND 1 - (dc.embedding <=> :query_embedding) >= :threshold
                ORDER BY dc.embedding <=> :query_embedding
                LIMIT :max_results
            """)
            
            results = db.execute(
                sql,
                {
                    "query_embedding": str(query_embedding),
                    "user_id": user_id,
                    "threshold": similarity_threshold,
                    "max_results": max_results
                }
            ).fetchall()
            
            # Format results with filing cabinet metadata
            formatted_results = []
            for row in results:
                formatted_results.append({
                    "chunk_id": row.id,
                    "content": row.content,
                    "chunk_index": row.chunk_index,
                    "page_number": row.page_number,
                    "document_id": row.document_id,
                    "filename": row.filename,
                    "original_filename": row.original_filename,
                    "display_name": row.display_name,
                    "document_type": row.document_type,
                    "storage_year": row.storage_year,
                    "document_date": row.document_date,
                    "file_path": row.file_path,
                    "ocr_pdf_path": row.ocr_pdf_path,
                    "created_at": row.created_at,
                    "similarity": float(row.similarity),
                    "filing_location": f"{row.storage_year}/{row.document_type}" if row.storage_year else None
                })
            
            logger.info(f"Found {len(formatted_results)} relevant chunks for query")
            return formatted_results
            
        except Exception as e:
            logger.error(f"Error in RAG search: {e}")
            return []
    
    def format_context(self, search_results: List[Dict[str, Any]]) -> str:
        """Format search results into context for LLM.
        
        Args:
            search_results: Results from search_documents
            
        Returns:
            Formatted context string
        """
        if not search_results:
            return "Aucun document pertinent trouvé."
        
        context_parts = ["Documents pertinents:\n"]
        
        for idx, result in enumerate(search_results, 1):
            # Include filing cabinet location for agent reference
            doc_name = result.get('display_name') or result['original_filename']
            filing_loc = result.get('filing_location', 'Non classé')
            doc_date = result.get('document_date', '')
            
            context_parts.append(
                f"\n[Document {idx}: {doc_name}]\n"
                f"Classement: {filing_loc}\n"
                f"Date: {doc_date if doc_date else 'Non spécifiée'}\n"
                f"Contenu:\n{result['content']}\n"
            )
        
        return "\n".join(context_parts)
