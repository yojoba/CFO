"""
Migration script to reorganize existing documents into filing cabinet structure.

This script:
1. Scans existing documents in the database
2. Creates searchable PDFs with OCR for documents that don't have them
3. Reorganizes files into year/type hierarchical structure
4. Updates database records with new paths and storage years

Usage:
    python scripts/migrate_existing_documents.py [--dry-run] [--limit N]
"""
import os
import sys
import asyncio
import argparse
from pathlib import Path
from datetime import datetime

# Add parent directory to path to import app modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from sqlalchemy.orm import Session
from loguru import logger

from app.database import SessionLocal
from app.models.document import Document
from app.services.pdf_conversion_service import PDFConversionService
from app.services.filing_cabinet_service import FilingCabinetService


async def migrate_document(
    document: Document,
    db: Session,
    pdf_service: PDFConversionService,
    filing_service: FilingCabinetService,
    dry_run: bool = False
) -> bool:
    """
    Migrate a single document to filing cabinet structure.
    
    Args:
        document: Document to migrate
        db: Database session
        pdf_service: PDF conversion service
        filing_service: Filing cabinet service
        dry_run: If True, only simulate the migration
        
    Returns:
        True if migration successful, False otherwise
    """
    try:
        logger.info(f"Migrating document {document.id}: {document.original_filename}")
        
        # Check if document file exists
        if not os.path.exists(document.file_path):
            logger.error(f"Document file not found: {document.file_path}")
            return False
        
        # Determine storage year
        storage_year = filing_service.determine_storage_year(
            document.document_date,
            document.created_at
        )
        
        logger.info(f"  Storage year: {storage_year}")
        logger.info(f"  Document type: {document.document_type.value}")
        
        # Check if already has OCR PDF
        has_ocr_pdf = document.ocr_pdf_path and os.path.exists(document.ocr_pdf_path)
        
        # Create OCR PDF if needed
        if not has_ocr_pdf:
            logger.info(f"  Creating searchable PDF...")
            import tempfile
            
            with tempfile.NamedTemporaryFile(suffix='.pdf', delete=False) as temp_ocr:
                temp_ocr_path = temp_ocr.name
            
            try:
                success, ocr_pdf_path, error = await pdf_service.ensure_searchable_pdf(
                    document.file_path,
                    temp_ocr_path,
                    languages=['fra', 'deu', 'eng']
                )
                
                if not success:
                    logger.warning(f"  OCR PDF creation failed: {error}")
                    ocr_pdf_path = document.file_path  # Use original
                else:
                    logger.info(f"  ✓ Created searchable PDF")
            except Exception as e:
                logger.error(f"  Error creating OCR PDF: {e}")
                ocr_pdf_path = document.file_path
        else:
            logger.info(f"  Already has OCR PDF")
            ocr_pdf_path = document.ocr_pdf_path
        
        # Generate organized paths
        organized_original_path = filing_service.get_organized_path(
            storage_year,
            document.document_type,
            document.original_filename,
            is_ocr_pdf=False
        )
        
        organized_ocr_path = filing_service.get_organized_path(
            storage_year,
            document.document_type,
            document.original_filename,
            is_ocr_pdf=True
        )
        
        logger.info(f"  New original path: {organized_original_path}")
        logger.info(f"  New OCR path: {organized_ocr_path}")
        
        if not dry_run:
            # Ensure directory exists
            filing_service.ensure_directory_structure(storage_year, document.document_type)
            
            # Move files to organized structure
            import shutil
            
            # Move original file
            if document.file_path != organized_original_path:
                shutil.move(document.file_path, organized_original_path)
                logger.info(f"  ✓ Moved original file")
            
            # Move or create OCR PDF
            if ocr_pdf_path != organized_ocr_path:
                if os.path.exists(ocr_pdf_path):
                    shutil.move(ocr_pdf_path, organized_ocr_path)
                    logger.info(f"  ✓ Moved OCR PDF")
            
            # Update database
            document.file_path = organized_original_path
            document.ocr_pdf_path = organized_ocr_path
            document.storage_year = storage_year
            db.commit()
            
            logger.info(f"  ✓ Database updated")
        else:
            logger.info(f"  [DRY RUN] Would move files and update database")
        
        return True
        
    except Exception as e:
        logger.error(f"Error migrating document {document.id}: {e}")
        import traceback
        logger.error(traceback.format_exc())
        return False


async def main():
    parser = argparse.ArgumentParser(description='Migrate existing documents to filing cabinet structure')
    parser.add_argument('--dry-run', action='store_true', help='Simulate migration without making changes')
    parser.add_argument('--limit', type=int, help='Limit number of documents to migrate')
    parser.add_argument('--user-id', type=int, help='Migrate documents for specific user only')
    args = parser.parse_args()
    
    logger.info("=" * 80)
    logger.info("FILING CABINET MIGRATION SCRIPT")
    logger.info("=" * 80)
    
    if args.dry_run:
        logger.warning("DRY RUN MODE - No changes will be made")
    
    # Initialize services
    pdf_service = PDFConversionService()
    filing_service = FilingCabinetService()
    
    # Get database session
    db = SessionLocal()
    
    try:
        # Query documents that need migration
        query = db.query(Document).filter(
            (Document.storage_year.is_(None)) | 
            (Document.ocr_pdf_path.is_(None))
        )
        
        if args.user_id:
            query = query.filter(Document.user_id == args.user_id)
            logger.info(f"Filtering for user ID: {args.user_id}")
        
        if args.limit:
            query = query.limit(args.limit)
            logger.info(f"Limiting to {args.limit} documents")
        
        documents = query.all()
        
        total = len(documents)
        logger.info(f"\nFound {total} documents to migrate")
        
        if total == 0:
            logger.info("No documents need migration. Exiting.")
            return
        
        # Confirm before proceeding
        if not args.dry_run:
            response = input(f"\nProceed with migration of {total} documents? (yes/no): ")
            if response.lower() != 'yes':
                logger.info("Migration cancelled by user")
                return
        
        # Migrate each document
        success_count = 0
        fail_count = 0
        
        for i, document in enumerate(documents, 1):
            logger.info(f"\n[{i}/{total}] Processing document {document.id}")
            
            success = await migrate_document(
                document,
                db,
                pdf_service,
                filing_service,
                dry_run=args.dry_run
            )
            
            if success:
                success_count += 1
            else:
                fail_count += 1
        
        # Summary
        logger.info("\n" + "=" * 80)
        logger.info("MIGRATION SUMMARY")
        logger.info("=" * 80)
        logger.info(f"Total documents: {total}")
        logger.info(f"Successfully migrated: {success_count}")
        logger.info(f"Failed: {fail_count}")
        
        if args.dry_run:
            logger.info("\nThis was a DRY RUN. No changes were made.")
            logger.info("Run without --dry-run to perform actual migration.")
        else:
            logger.info("\n✓ Migration completed!")
        
    except Exception as e:
        logger.error(f"Migration failed: {e}")
        import traceback
        logger.error(traceback.format_exc())
    finally:
        db.close()


if __name__ == "__main__":
    asyncio.run(main())

