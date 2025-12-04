"""Document analysis orchestration service."""
from typing import Dict, Any, Optional
from pathlib import Path
from loguru import logger
import json

from app.services.ocr_service import OCRService
from app.agents.document_agent import DocumentAgent
from app.models.document import DocumentType


class DocumentAnalysisService:
    """Service to orchestrate document analysis pipeline."""
    
    def __init__(self):
        self.ocr_service = OCRService()
        self.document_agent = DocumentAgent()
    
    async def analyze_document(
        self, 
        file_path: str,
        mime_type: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Complete document analysis pipeline: OCR → AI Analysis → Metadata extraction.
        
        Args:
            file_path: Path to document file
            mime_type: MIME type of the file
            
        Returns:
            Dict with all extracted data and metadata
        """
        try:
            logger.info(f"Starting analysis for document: {file_path}")
            
            # Step 1: Extract text using OCR
            ocr_result = await self._extract_text(file_path, mime_type)
            extracted_text = ocr_result['text']
            ocr_confidence = ocr_result['confidence']
            ocr_method = ocr_result['method']
            
            if not extracted_text or not extracted_text.strip():
                logger.warning(f"No text extracted from document: {file_path}")
                return {
                    'extracted_text': '',
                    'ocr_confidence': 0.0,
                    'ocr_method': ocr_method,
                    'metadata': self._get_empty_metadata()
                }
            
            logger.info(f"Extracted {len(extracted_text)} characters using {ocr_method} (confidence: {ocr_confidence:.2f})")
            
            # Step 2: Analyze with DocumentAgent
            analysis_result = await self.document_agent.analyze_document(
                extracted_text=extracted_text,
                ocr_confidence=ocr_confidence
            )
            
            # Step 3: Prepare final result
            result = {
                'extracted_text': extracted_text,
                'ocr_confidence': ocr_confidence,
                'ocr_method': ocr_method,
                'metadata': analysis_result
            }
            
            logger.info(f"Document analysis complete: type={analysis_result.get('document_type')}, "
                       f"importance={analysis_result.get('importance_score'):.1f}")
            
            return result
            
        except Exception as e:
            logger.error(f"Error in document analysis pipeline: {e}")
            return {
                'extracted_text': '',
                'ocr_confidence': 0.0,
                'ocr_method': 'error',
                'metadata': self._get_empty_metadata()
            }
    
    async def _extract_text(self, file_path: str, mime_type: Optional[str]) -> Dict[str, Any]:
        """
        Extract text from document based on file type.
        
        Args:
            file_path: Path to document
            mime_type: MIME type of document
            
        Returns:
            Dict with extracted text and metadata
        """
        file_extension = Path(file_path).suffix.lower()
        
        # Determine if it's a PDF or image
        is_pdf = file_extension == '.pdf' or (mime_type and 'pdf' in mime_type.lower())
        is_image = file_extension in ['.jpg', '.jpeg', '.png', '.tiff', '.bmp', '.gif'] or \
                   (mime_type and 'image' in mime_type.lower())
        
        if is_pdf:
            # For PDFs, use the PDF extraction method
            return await self.ocr_service.extract_text_from_pdf(file_path)
        elif is_image:
            # For images, use OCR
            return await self.ocr_service.extract_text_from_image(file_path)
        else:
            logger.warning(f"Unsupported file type: {file_extension}")
            return {
                'text': '',
                'confidence': 0.0,
                'method': 'unsupported',
                'pages': 0
            }
    
    def prepare_database_fields(self, analysis_result: Dict[str, Any]) -> Dict[str, Any]:
        """
        Prepare fields for database storage from analysis result.
        
        Args:
            analysis_result: Result from analyze_document
            
        Returns:
            Dict with fields ready for database update
        """
        metadata = analysis_result.get('metadata', {})
        
        # Map document type string to enum
        doc_type_str = metadata.get('document_type', 'other')
        doc_type_map = {
            'invoice': DocumentType.INVOICE,
            'letter': DocumentType.LETTER,
            'contract': DocumentType.CONTRACT,
            'receipt': DocumentType.RECEIPT,
            'other': DocumentType.OTHER
        }
        document_type = doc_type_map.get(doc_type_str, DocumentType.OTHER)
        
        # Prepare keywords as JSON string
        keywords = metadata.get('keywords', [])
        keywords_json = json.dumps(keywords) if keywords else None
        
        # Prepare extracted_data as JSON string (store full metadata)
        extracted_data = json.dumps({
            'importance_factors': metadata.get('importance_factors', {}),
            'summary': metadata.get('summary', ''),
            'ocr_method': analysis_result.get('ocr_method'),
            'ocr_confidence': analysis_result.get('ocr_confidence')
        })
        
        return {
            'extracted_text': analysis_result.get('extracted_text', ''),
            'extracted_data': extracted_data,
            'document_type': document_type,
            'display_name': metadata.get('display_name', ''),
            'importance_score': metadata.get('importance_score'),
            'document_date': metadata.get('document_date'),
            'deadline': metadata.get('deadline'),
            'extracted_amount': metadata.get('amount'),
            'currency': metadata.get('currency'),
            'keywords': keywords_json,
            'classification_confidence': metadata.get('confidence')
        }
    
    def _get_empty_metadata(self) -> Dict[str, Any]:
        """
        Get empty metadata structure.
        
        Returns:
            Empty metadata dict
        """
        return {
            'document_type': 'other',
            'document_date': None,
            'deadline': None,
            'amount': None,
            'currency': 'CHF',
            'keywords': [],
            'importance_factors': {
                'has_deadline': False,
                'is_urgent': False,
                'has_high_amount': False,
                'requires_action': False
            },
            'confidence': 0.0,
            'summary': '',
            'importance_score': 0.0
        }

