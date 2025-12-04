"""PDF conversion service for creating searchable PDFs with OCR layer."""
import os
import tempfile
import shutil
from pathlib import Path
from typing import Optional, Tuple
from loguru import logger
from PIL import Image
import ocrmypdf
from ocrmypdf.exceptions import PriorOcrFoundError

from app.config import settings


class PDFConversionService:
    """Service for converting documents to searchable PDFs with OCR layer."""
    
    def __init__(self):
        self.quality = getattr(settings, 'OCR_PDF_QUALITY', 'high')
        # Map quality settings to OCRmyPDF parameters
        self.quality_settings = {
            'low': {'optimize': 1, 'jpeg_quality': 60, 'png_quality': 60},
            'medium': {'optimize': 2, 'jpeg_quality': 75, 'png_quality': 75},
            'high': {'optimize': 3, 'jpeg_quality': 90, 'png_quality': 90}
        }
    
    async def ensure_searchable_pdf(
        self,
        input_path: str,
        output_path: str,
        languages: Optional[list] = None
    ) -> Tuple[bool, str, Optional[str]]:
        """
        Ensure document is a searchable PDF with OCR layer.
        
        Args:
            input_path: Path to input file (image or PDF)
            output_path: Path for output searchable PDF
            languages: List of language codes (e.g., ['fra', 'deu', 'eng'])
            
        Returns:
            Tuple of (success, output_path, error_message)
        """
        if languages is None:
            languages = ['fra', 'deu', 'eng']  # Swiss default
        
        file_extension = Path(input_path).suffix.lower()
        
        try:
            # Ensure output directory exists
            os.makedirs(os.path.dirname(output_path), exist_ok=True)
            
            if file_extension == '.pdf':
                # Add OCR layer to existing PDF
                return await self.add_ocr_layer_to_pdf(input_path, output_path, languages)
            elif file_extension in ['.jpg', '.jpeg', '.png', '.tiff', '.tif', '.bmp']:
                # Convert image to searchable PDF
                return await self.convert_image_to_searchable_pdf(input_path, output_path, languages)
            else:
                error_msg = f"Unsupported file type: {file_extension}"
                logger.warning(error_msg)
                return False, input_path, error_msg
                
        except Exception as e:
            error_msg = f"Error creating searchable PDF: {e}"
            logger.error(error_msg)
            return False, input_path, error_msg
    
    async def convert_image_to_searchable_pdf(
        self,
        image_path: str,
        output_pdf_path: str,
        languages: list
    ) -> Tuple[bool, str, Optional[str]]:
        """
        Convert image to searchable PDF with OCR layer.
        
        Args:
            image_path: Path to image file
            output_pdf_path: Path for output PDF
            languages: List of language codes
            
        Returns:
            Tuple of (success, output_path, error_message)
        """
        try:
            logger.info(f"Converting image to searchable PDF: {image_path}")
            
            # Create temporary PDF from image
            with tempfile.NamedTemporaryFile(suffix='.pdf', delete=False) as temp_pdf:
                temp_pdf_path = temp_pdf.name
            
            try:
                # Convert image to PDF using Pillow
                image = Image.open(image_path)
                
                # Convert to RGB if necessary
                if image.mode in ('RGBA', 'LA', 'P'):
                    rgb_image = Image.new('RGB', image.size, (255, 255, 255))
                    if image.mode == 'P':
                        image = image.convert('RGBA')
                    rgb_image.paste(image, mask=image.split()[-1] if image.mode in ('RGBA', 'LA') else None)
                    image = rgb_image
                elif image.mode != 'RGB':
                    image = image.convert('RGB')
                
                # Save as PDF
                image.save(temp_pdf_path, 'PDF', resolution=300.0)
                
                # Now add OCR layer
                return await self.add_ocr_layer_to_pdf(temp_pdf_path, output_pdf_path, languages)
                
            finally:
                # Clean up temp file
                if os.path.exists(temp_pdf_path):
                    os.remove(temp_pdf_path)
                    
        except Exception as e:
            error_msg = f"Error converting image to PDF: {e}"
            logger.error(error_msg)
            return False, image_path, error_msg
    
    async def add_ocr_layer_to_pdf(
        self,
        input_pdf_path: str,
        output_pdf_path: str,
        languages: list
    ) -> Tuple[bool, str, Optional[str]]:
        """
        Add OCR text layer to PDF.
        
        Args:
            input_pdf_path: Path to input PDF
            output_pdf_path: Path for output PDF with OCR
            languages: List of language codes
            
        Returns:
            Tuple of (success, output_path, error_message)
        """
        try:
            logger.info(f"Adding OCR layer to PDF: {input_pdf_path}")
            
            # Prepare OCRmyPDF language parameter
            # OCRmyPDF uses '+' to separate languages: 'fra+deu+eng'
            lang_string = '+'.join(languages)
            
            # Get quality settings
            quality_params = self.quality_settings.get(self.quality, self.quality_settings['high'])
            
            # OCRmyPDF options
            ocrmypdf_options = {
                'language': lang_string,
                'output_type': 'pdfa',  # PDF/A format for long-term archival
                'optimize': quality_params['optimize'],
                'jpeg_quality': quality_params['jpeg_quality'],
                'png_quality': quality_params['png_quality'],
                'deskew': True,  # Straighten pages
                'clean': True,  # Clean up artifacts
                'remove_background': False,  # Keep original appearance
                'force_ocr': False,  # Don't re-OCR if text already present
                'skip_text': False,  # Add OCR even if text present
                'redo_ocr': False,  # Don't redo existing OCR
                'rotate_pages': True,  # Auto-rotate pages
                'rotate_pages_threshold': 14.0,  # Rotation confidence threshold
            }
            
            # Run OCRmyPDF
            ocrmypdf.ocr(
                input_pdf_path,
                output_pdf_path,
                **ocrmypdf_options
            )
            
            logger.info(f"Successfully created searchable PDF: {output_pdf_path}")
            return True, output_pdf_path, None
            
        except PriorOcrFoundError:
            # PDF already has OCR text, just copy it
            logger.info(f"PDF already has OCR layer, copying: {input_pdf_path}")
            shutil.copy2(input_pdf_path, output_pdf_path)
            return True, output_pdf_path, None
            
        except Exception as e:
            error_msg = f"Error adding OCR layer to PDF: {e}"
            logger.error(error_msg)
            # If OCR fails, copy original file
            try:
                shutil.copy2(input_pdf_path, output_pdf_path)
                return False, output_pdf_path, error_msg
            except:
                return False, input_pdf_path, error_msg
    
    def extract_text_from_searchable_pdf(self, pdf_path: str) -> str:
        """
        Extract text from searchable PDF.
        
        Args:
            pdf_path: Path to PDF file
            
        Returns:
            Extracted text
        """
        try:
            import pdfplumber
            
            text_parts = []
            with pdfplumber.open(pdf_path) as pdf:
                for page in pdf.pages:
                    text = page.extract_text()
                    if text:
                        text_parts.append(text)
            
            return "\n\n".join(text_parts)
            
        except Exception as e:
            logger.error(f"Error extracting text from PDF: {e}")
            # Fallback to PyPDF2
            try:
                import PyPDF2
                text_parts = []
                with open(pdf_path, 'rb') as file:
                    pdf_reader = PyPDF2.PdfReader(file)
                    for page_num in range(len(pdf_reader.pages)):
                        page = pdf_reader.pages[page_num]
                        text = page.extract_text()
                        if text:
                            text_parts.append(text)
                return "\n\n".join(text_parts)
            except Exception as e2:
                logger.error(f"Fallback extraction also failed: {e2}")
                return ""

