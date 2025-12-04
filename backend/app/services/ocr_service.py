"""OCR service with Google Cloud Vision API and local fallback."""
import os
from pathlib import Path
from typing import Dict, Any, Optional
from loguru import logger
from PIL import Image
import pytesseract
import io

try:
    from google.cloud import vision
    from google.oauth2 import service_account
    GOOGLE_VISION_AVAILABLE = True
except ImportError:
    GOOGLE_VISION_AVAILABLE = False
    logger.warning("google-cloud-vision not installed, will use local OCR only")

from app.config import settings


class OCRService:
    """Service for OCR with cloud and local fallback."""
    
    def __init__(self):
        has_api_key = hasattr(settings, 'GOOGLE_CLOUD_VISION_API_KEY') and settings.GOOGLE_CLOUD_VISION_API_KEY
        has_credentials = hasattr(settings, 'GOOGLE_CLOUD_VISION_CREDENTIALS') and settings.GOOGLE_CLOUD_VISION_CREDENTIALS
        
        self.use_cloud = GOOGLE_VISION_AVAILABLE and (has_api_key or has_credentials)
        self.fallback_to_local = getattr(settings, 'OCR_FALLBACK_TO_LOCAL', True)
        
        if self.use_cloud:
            try:
                # Initialize Google Cloud Vision client
                if has_credentials:
                    # Use service account credentials (recommended for production)
                    credentials = service_account.Credentials.from_service_account_file(
                        settings.GOOGLE_CLOUD_VISION_CREDENTIALS
                    )
                    self.vision_client = vision.ImageAnnotatorClient(credentials=credentials)
                    logger.info("Google Cloud Vision API initialized with service account")
                elif has_api_key:
                    # Use API key (simpler but less secure)
                    import os
                    os.environ['GOOGLE_API_KEY'] = settings.GOOGLE_CLOUD_VISION_API_KEY
                    self.vision_client = vision.ImageAnnotatorClient()
                    logger.info("Google Cloud Vision API initialized with API key")
                else:
                    # Use default credentials (from environment)
                    self.vision_client = vision.ImageAnnotatorClient()
                    logger.info("Google Cloud Vision API initialized with default credentials")
            except Exception as e:
                logger.warning(f"Failed to initialize Google Cloud Vision: {e}")
                self.use_cloud = False
        else:
            self.vision_client = None
            logger.info("Using local OCR only (Tesseract)")
    
    async def extract_text_from_image(
        self, 
        file_path: str,
        languages: Optional[list] = None
    ) -> Dict[str, Any]:
        """
        Extract text from image using OCR.
        
        Args:
            file_path: Path to image file
            languages: List of language codes (e.g., ['fr', 'de', 'en'])
            
        Returns:
            Dict with 'text', 'confidence', and 'method' keys
        """
        if languages is None:
            languages = ['fr', 'de', 'en']  # Swiss default
        
        # Try cloud OCR first if available
        if self.use_cloud:
            try:
                result = await self._extract_with_google_vision(file_path)
                if result['text']:
                    logger.info(f"Successfully extracted text using Google Vision API")
                    return result
                else:
                    logger.warning("Google Vision returned empty text")
            except Exception as e:
                logger.error(f"Google Vision API error: {e}")
                if not self.fallback_to_local:
                    raise
        
        # Fallback to local OCR
        if self.fallback_to_local:
            logger.info("Using local Tesseract OCR")
            return await self._extract_with_tesseract(file_path, languages)
        else:
            raise Exception("OCR failed and fallback is disabled")
    
    async def _extract_with_google_vision(self, file_path: str) -> Dict[str, Any]:
        """
        Extract text using Google Cloud Vision API.
        
        Args:
            file_path: Path to image file
            
        Returns:
            Dict with extracted text and metadata
        """
        with open(file_path, 'rb') as image_file:
            content = image_file.read()
        
        image = vision.Image(content=content)
        
        # Perform text detection
        response = self.vision_client.document_text_detection(image=image)
        
        if response.error.message:
            raise Exception(f"Google Vision API error: {response.error.message}")
        
        # Extract full text
        text = response.full_text_annotation.text if response.full_text_annotation else ""
        
        # Calculate average confidence
        confidence = 0.0
        if response.text_annotations:
            # First annotation is the full text, rest are individual words
            if len(response.text_annotations) > 1:
                confidences = [
                    annotation.confidence 
                    for annotation in response.text_annotations[1:] 
                    if hasattr(annotation, 'confidence')
                ]
                if confidences:
                    confidence = sum(confidences) / len(confidences)
        
        return {
            'text': text,
            'confidence': confidence,
            'method': 'google_vision',
            'pages': len(response.full_text_annotation.pages) if response.full_text_annotation else 1
        }
    
    async def _extract_with_tesseract(
        self, 
        file_path: str, 
        languages: list
    ) -> Dict[str, Any]:
        """
        Extract text using local Tesseract OCR.
        
        Args:
            file_path: Path to image file
            languages: List of language codes
            
        Returns:
            Dict with extracted text and metadata
        """
        try:
            image = Image.open(file_path)
            
            # Convert language codes to Tesseract format
            lang_map = {'fr': 'fra', 'de': 'deu', 'en': 'eng'}
            tesseract_langs = '+'.join([lang_map.get(lang, lang) for lang in languages])
            
            # Extract text
            text = pytesseract.image_to_string(image, lang=tesseract_langs)
            
            # Get confidence data
            data = pytesseract.image_to_data(image, lang=tesseract_langs, output_type=pytesseract.Output.DICT)
            confidences = [int(conf) for conf in data['conf'] if conf != '-1']
            avg_confidence = sum(confidences) / len(confidences) / 100.0 if confidences else 0.0
            
            return {
                'text': text,
                'confidence': avg_confidence,
                'method': 'tesseract',
                'pages': 1
            }
        except Exception as e:
            logger.error(f"Tesseract OCR error: {e}")
            raise
    
    async def extract_text_from_pdf(self, file_path: str) -> Dict[str, Any]:
        """
        Extract text from PDF using Google Cloud Vision API.
        For PDFs, we convert to images first or use native PDF support.
        
        Args:
            file_path: Path to PDF file
            
        Returns:
            Dict with extracted text and metadata
        """
        # For now, we'll use the existing PyPDF2 extraction
        # In production, you might want to convert PDF pages to images
        # and use OCR on each page for better accuracy
        import PyPDF2
        
        text_parts = []
        with open(file_path, 'rb') as file:
            pdf_reader = PyPDF2.PdfReader(file)
            
            for page_num in range(len(pdf_reader.pages)):
                page = pdf_reader.pages[page_num]
                text = page.extract_text()
                if text:
                    text_parts.append(text)
        
        full_text = "\n\n".join(text_parts)
        
        return {
            'text': full_text,
            'confidence': 1.0 if full_text else 0.0,  # Native PDF text is high confidence
            'method': 'pypdf2',
            'pages': len(pdf_reader.pages) if 'pdf_reader' in locals() else 1
        }

