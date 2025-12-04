"""Transaction model for financial data."""
from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Numeric, Date
from sqlalchemy.orm import relationship
from datetime import datetime
from app.models.database import Base


class Transaction(Base):
    """Transaction model for extracted financial data."""
    
    __tablename__ = "transactions"
    
    id = Column(Integer, primary_key=True, index=True)
    
    # Financial details
    amount = Column(Numeric(10, 2), nullable=False)
    currency = Column(String, default="CHF")
    transaction_date = Column(Date, nullable=True)
    
    # Categorization
    category = Column(String, nullable=True)
    subcategory = Column(String, nullable=True)
    description = Column(String, nullable=True)
    
    # Source
    document_id = Column(Integer, ForeignKey("documents.id"), nullable=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    
    # Metadata
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relationships
    document = relationship("Document", back_populates="transactions")
    user = relationship("User")
