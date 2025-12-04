"""Text chunking service for RAG"""

from typing import List, Dict, Any
from loguru import logger

from app.config import settings


class ChunkingService:
    """Service for splitting documents into chunks"""
    
    def __init__(self, chunk_size: int = None, chunk_overlap: int = None):
        self.chunk_size = chunk_size or settings.CHUNK_SIZE
        self.chunk_overlap = chunk_overlap or settings.CHUNK_OVERLAP
    
    def chunk_text(self, text: str, metadata: Dict[str, Any] = None) -> List[Dict[str, Any]]:
        """
        Split text into overlapping chunks.
        
        Args:
            text: Text to chunk
            metadata: Optional metadata to attach to each chunk
            
        Returns:
            List[Dict[str, Any]]: List of chunks with metadata
        """
        if not text or not text.strip():
            return []
        
        chunks = []
        words = text.split()
        
        # Simple word-based chunking
        start = 0
        chunk_index = 0
        
        while start < len(words):
            end = min(start + self.chunk_size, len(words))
            chunk_words = words[start:end]
            chunk_text = " ".join(chunk_words)
            
            chunk_data = {
                "content": chunk_text,
                "chunk_index": chunk_index,
                "metadata": metadata or {},
            }
            
            chunks.append(chunk_data)
            
            # Move start forward by (chunk_size - overlap) to create overlap
            start += self.chunk_size - self.chunk_overlap
            chunk_index += 1
        
        logger.info(f"Created {len(chunks)} chunks from text of {len(words)} words")
        return chunks
    
    def chunk_by_paragraphs(self, text: str, metadata: Dict[str, Any] = None) -> List[Dict[str, Any]]:
        """
        Split text into chunks by paragraphs, respecting chunk size limits.
        
        Args:
            text: Text to chunk
            metadata: Optional metadata to attach to each chunk
            
        Returns:
            List[Dict[str, Any]]: List of chunks with metadata
        """
        if not text or not text.strip():
            return []
        
        # Split by double newlines (paragraphs)
        paragraphs = [p.strip() for p in text.split("\n\n") if p.strip()]
        
        chunks = []
        current_chunk = []
        current_word_count = 0
        chunk_index = 0
        
        for paragraph in paragraphs:
            para_words = paragraph.split()
            para_word_count = len(para_words)
            
            # If adding this paragraph exceeds chunk size, save current chunk
            if current_word_count + para_word_count > self.chunk_size and current_chunk:
                chunk_text = "\n\n".join(current_chunk)
                chunks.append({
                    "content": chunk_text,
                    "chunk_index": chunk_index,
                    "metadata": metadata or {},
                })
                
                # Start new chunk with overlap (keep last paragraph if small enough)
                if current_word_count > self.chunk_overlap:
                    current_chunk = [current_chunk[-1]] if len(current_chunk) > 1 else []
                    current_word_count = len(current_chunk[0].split()) if current_chunk else 0
                else:
                    current_chunk = []
                    current_word_count = 0
                
                chunk_index += 1
            
            current_chunk.append(paragraph)
            current_word_count += para_word_count
        
        # Add remaining chunk
        if current_chunk:
            chunk_text = "\n\n".join(current_chunk)
            chunks.append({
                "content": chunk_text,
                "chunk_index": chunk_index,
                "metadata": metadata or {},
            })
        
        logger.info(f"Created {len(chunks)} chunks from {len(paragraphs)} paragraphs")
        return chunks

