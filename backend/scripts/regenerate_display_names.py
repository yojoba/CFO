"""Script to regenerate display names for existing documents."""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import asyncio
from sqlalchemy.orm import Session
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.models.document import Document, DocumentStatus
from app.agents.document_agent import DocumentAgent
from app.config import settings
from loguru import logger

# Create session
engine = create_engine(settings.DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


async def regenerate_display_names():
    """Regenerate display names for documents that don't have one."""
    db: Session = SessionLocal()
    agent = DocumentAgent()
    
    try:
        # Get documents without display_name
        documents = db.query(Document)\
            .filter(Document.display_name == None)\
            .filter(Document.status == DocumentStatus.COMPLETED)\
            .filter(Document.extracted_text != None)\
            .all()
        
        logger.info(f"Found {len(documents)} documents without display_name")
        
        for doc in documents:
            try:
                logger.info(f"Regenerating display_name for document {doc.id}: {doc.original_filename}")
                
                # Analyze with agent
                metadata = await agent.analyze_document(
                    extracted_text=doc.extracted_text[:4000],
                    ocr_confidence=1.0
                )
                
                # Update display_name
                display_name = metadata.get('display_name', '')
                if display_name:
                    doc.display_name = display_name
                    db.commit()
                    logger.info(f"✅ Document {doc.id}: {display_name}")
                else:
                    logger.warning(f"⚠️ No display_name generated for document {doc.id}")
                
            except Exception as e:
                logger.error(f"❌ Error processing document {doc.id}: {e}")
                continue
        
        logger.info("✅ Display names regeneration complete!")
        
    finally:
        db.close()


if __name__ == "__main__":
    asyncio.run(regenerate_display_names())

