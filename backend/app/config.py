"""Application configuration management."""
from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""
    
    # Database
    DATABASE_URL: str = "postgresql://agentcfo:changeme@localhost:5432/agentcfo"
    
    # OpenAI
    OPENAI_API_KEY: str
    OPENAI_MODEL: str = "gpt-4-turbo-preview"
    OPENAI_EMBEDDING_MODEL: str = "text-embedding-3-small"
    
    # JWT Authentication
    JWT_SECRET: str = "your-secret-key-change-in-production"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRATION_MINUTES: int = 1440
    
    # Application
    ENVIRONMENT: str = "development"
    UPLOAD_DIR: str = "/app/uploads"
    MAX_UPLOAD_SIZE_MB: int = 10
    
    # CORS
    CORS_ORIGINS: list[str] = ["http://localhost:3000", "http://localhost:3008"]
    
    # RAG Configuration
    CHUNK_SIZE: int = 500
    CHUNK_OVERLAP: int = 50
    SIMILARITY_THRESHOLD: float = 0.7
    MAX_RESULTS: int = 5
    
    # OCR Configuration
    GOOGLE_CLOUD_VISION_API_KEY: Optional[str] = None  # API Key (simple auth)
    GOOGLE_CLOUD_VISION_CREDENTIALS: Optional[str] = None  # Path to credentials JSON file (recommended)
    OCR_FALLBACK_TO_LOCAL: bool = True  # Fallback to Tesseract if cloud fails
    
    # Document Intelligence Configuration
    IMPORTANCE_THRESHOLD_HIGH: float = 80.0  # Score above which document is high importance
    IMPORTANCE_THRESHOLD_URGENT: float = 70.0  # Score above which document needs attention
    URGENT_DEADLINE_DAYS: int = 7  # Days until deadline to consider urgent
    HIGH_AMOUNT_THRESHOLD: float = 500.0  # Amount (CHF) considered high
    
    # Filing Cabinet Configuration
    FILING_CABINET_ROOT: str = "/app/uploads"  # Root directory for organized document storage
    KEEP_ORIGINAL_FILES: bool = True  # Keep original uploaded files alongside OCR PDFs
    OCR_PDF_QUALITY: str = "high"  # PDF quality: 'low', 'medium', 'high'
    FILING_CABINET_YEAR_SOURCE: str = "document_date"  # Use 'document_date' or 'upload_date' for year organization
    
    # Image Preprocessing Configuration
    ENABLE_AUTO_CROP: bool = True  # Auto-detect and crop document from image
    ENABLE_DESKEW: bool = True  # Automatically straighten rotated documents
    ENABLE_CONTRAST_ENHANCEMENT: bool = True  # Improve contrast with CLAHE
    ENABLE_NOISE_REDUCTION: bool = True  # Reduce noise while preserving edges
    MIN_DOCUMENT_AREA_RATIO: float = 0.1  # Minimum document area (10% of image)
    DESKEW_ANGLE_THRESHOLD: float = 0.5  # Minimum angle in degrees to trigger deskew
    
    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
