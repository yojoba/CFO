"""Document processing service."""
import os
import uuid
from pathlib import Path
from typing import Tuple, List
import PyPDF2
import pytesseract
from PIL import Image
from loguru import logger

from app.config import settings


class DocumentService:
    """Service for document upload and processing."""
    
    def __init__(self):
        self.upload_dir = Path(settings.UPLOAD_DIR)
        self.upload_dir.mkdir(parents=True, exist_ok=True)
    
    async def save_file(self, filename: str, content: bytes) -> Tuple[str, str]:
        """Save uploaded file to disk.
        
        Args:
            filename: Original filename
            content: File content as bytes
            
        Returns:
            Tuple of (file_path, saved_filename)
        """
        # Generate unique filename
        file_extension = Path(filename).suffix
        saved_filename = f"{uuid.uuid4()}{file_extension}"
        file_path = self.upload_dir / saved_filename
        
        # Write file
        with open(file_path, "wb") as f:
            f.write(content)
        
        logger.info(f"Saved file: {saved_filename}")
        return str(file_path), saved_filename
    
    def delete_file(self, file_path: str):
        """Delete a file from disk.
        
        Args:
            file_path: Path to file
        """
        try:
            if os.path.exists(file_path):
                os.remove(file_path)
                logger.info(f"Deleted file: {file_path}")
        except Exception as e:
            logger.error(f"Error deleting file {file_path}: {e}")
    
    async def extract_text(self, file_path: str) -> str:
        """Extract text from PDF or image file.
        
        Args:
            file_path: Path to file
            
        Returns:
            Extracted text
        """
        file_extension = Path(file_path).suffix.lower()
        
        try:
            if file_extension == '.pdf':
                return await self._extract_text_from_pdf(file_path)
            elif file_extension in ['.jpg', '.jpeg', '.png', '.tiff', '.bmp']:
                return await self._extract_text_from_image(file_path)
            else:
                logger.warning(f"Unsupported file type: {file_extension}")
                return ""
        except Exception as e:
            logger.error(f"Error extracting text from {file_path}: {e}")
            return ""
    
    async def _extract_text_from_pdf(self, file_path: str) -> str:
        """Extract text from PDF file.
        
        Args:
            file_path: Path to PDF file
            
        Returns:
            Extracted text
        """
        text_parts = []
        
        with open(file_path, 'rb') as file:
            pdf_reader = PyPDF2.PdfReader(file)
            
            for page_num in range(len(pdf_reader.pages)):
                page = pdf_reader.pages[page_num]
                text = page.extract_text()
                if text:
                    text_parts.append(text)
        
        return "\n\n".join(text_parts)
    
    async def _extract_text_from_image(self, file_path: str) -> str:
        """Extract text from image using OCR.
        
        Args:
            file_path: Path to image file
            
        Returns:
            Extracted text
        """
        image = Image.open(file_path)
        
        # Use French and German language support (common in Switzerland)
        text = pytesseract.image_to_string(image, lang='fra+deu+eng')
        
        return text
    
    def create_chunks(self, text: str, chunk_size: int = None, overlap: int = None) -> List[str]:
        """Split text into chunks for RAG.
        
        Args:
            text: Full text to split
            chunk_size: Size of each chunk in characters (default from settings)
            overlap: Overlap between chunks (default from settings)
            
        Returns:
            List of text chunks
        """
        if chunk_size is None:
            chunk_size = settings.CHUNK_SIZE
        if overlap is None:
            overlap = settings.CHUNK_OVERLAP
        
        if not text:
            return []
        
        chunks = []
        start = 0
        text_length = len(text)
        
        while start < text_length:
            end = start + chunk_size
            chunk = text[start:end]
            
            # Try to break at sentence or word boundary
            if end < text_length:
                # Look for sentence end
                last_period = chunk.rfind('.')
                last_newline = chunk.rfind('\n')
                break_point = max(last_period, last_newline)
                
                if break_point > chunk_size * 0.5:  # At least halfway through
                    chunk = chunk[:break_point + 1]
                    end = start + break_point + 1
            
            chunks.append(chunk.strip())
            start = end - overlap
        
        return chunks
