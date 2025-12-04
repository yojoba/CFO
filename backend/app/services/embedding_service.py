"""Embedding service for RAG."""
from typing import List
from openai import AsyncOpenAI
from loguru import logger
from sqlalchemy.orm import Session

from app.config import settings
from app.models.document import DocumentChunk


class EmbeddingService:
    """Service for creating and managing embeddings."""
    
    def __init__(self):
        self.client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)
        self.model = settings.OPENAI_EMBEDDING_MODEL
    
    async def create_embedding(self, text: str) -> List[float]:
        """Create embedding vector for text.
        
        Args:
            text: Text to embed
            
        Returns:
            Embedding vector
        """
        try:
            response = await self.client.embeddings.create(
                model=self.model,
                input=text
            )
            return response.data[0].embedding
        except Exception as e:
            logger.error(f"Error creating embedding: {e}")
            raise
    
    async def create_embeddings(
        self,
        document_id: int,
        chunks: List[str],
        db: Session
    ) -> List[DocumentChunk]:
        """Create embeddings for all chunks and save to database.
        
        Args:
            document_id: ID of parent document
            chunks: List of text chunks
            db: Database session
            
        Returns:
            List of created DocumentChunk objects
        """
        chunk_objects = []
        
        for idx, chunk_text in enumerate(chunks):
            if not chunk_text.strip():
                continue
            
            try:
                # Create embedding
                embedding = await self.create_embedding(chunk_text)
                
                # Create chunk object
                chunk = DocumentChunk(
                    document_id=document_id,
                    content=chunk_text,
                    chunk_index=idx,
                    embedding=embedding
                )
                db.add(chunk)
                chunk_objects.append(chunk)
                
                logger.debug(f"Created embedding for chunk {idx} of document {document_id}")
                
            except Exception as e:
                logger.error(f"Error creating embedding for chunk {idx}: {e}")
                continue
        
        db.commit()
        logger.info(f"Created {len(chunk_objects)} embeddings for document {document_id}")
        
        return chunk_objects
