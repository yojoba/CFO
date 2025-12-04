"""Tests for DocumentAgent."""
import pytest
from datetime import date, timedelta
from app.agents.document_agent import DocumentAgent


@pytest.fixture
def document_agent():
    """Create a DocumentAgent instance."""
    return DocumentAgent()


class TestDocumentAgent:
    """Test suite for DocumentAgent."""
    
    def test_parse_date_iso_format(self, document_agent):
        """Test parsing ISO format date."""
        result = document_agent._parse_date("2024-12-15")
        assert result == "2024-12-15"
    
    def test_parse_date_swiss_format(self, document_agent):
        """Test parsing Swiss date format (DD.MM.YYYY)."""
        result = document_agent._parse_date("15.12.2024")
        assert result == "2024-12-15"
    
    def test_parse_date_slash_format(self, document_agent):
        """Test parsing date with slashes."""
        result = document_agent._parse_date("15/12/2024")
        assert result == "2024-12-15"
    
    def test_parse_date_invalid(self, document_agent):
        """Test parsing invalid date."""
        result = document_agent._parse_date("invalid-date")
        assert result is None
    
    def test_parse_date_none(self, document_agent):
        """Test parsing None date."""
        result = document_agent._parse_date(None)
        assert result is None
    
    def test_parse_amount_float(self, document_agent):
        """Test parsing float amount."""
        result = document_agent._parse_amount(123.45)
        assert result == 123.45
    
    def test_parse_amount_string_with_apostrophe(self, document_agent):
        """Test parsing Swiss format amount (1'234.50)."""
        result = document_agent._parse_amount("1'234.50")
        assert result == 1234.50
    
    def test_parse_amount_string_with_comma(self, document_agent):
        """Test parsing amount with comma as decimal separator."""
        result = document_agent._parse_amount("1234,50")
        assert result == 1234.50
    
    def test_parse_amount_invalid(self, document_agent):
        """Test parsing invalid amount."""
        result = document_agent._parse_amount("not-a-number")
        assert result is None
    
    def test_calculate_importance_score_base(self, document_agent):
        """Test base importance score calculation."""
        metadata = {
            'importance_factors': {},
            'confidence': 1.0
        }
        score = document_agent._calculate_importance_score(metadata)
        assert 40 <= score <= 60  # Base score around 50
    
    def test_calculate_importance_score_overdue_deadline(self, document_agent):
        """Test importance score with overdue deadline."""
        yesterday = (date.today() - timedelta(days=1)).isoformat()
        metadata = {
            'deadline': yesterday,
            'importance_factors': {},
            'confidence': 1.0
        }
        score = document_agent._calculate_importance_score(metadata)
        assert score >= 80  # High score for overdue
    
    def test_calculate_importance_score_urgent_deadline(self, document_agent):
        """Test importance score with urgent deadline (3 days)."""
        urgent_date = (date.today() + timedelta(days=3)).isoformat()
        metadata = {
            'deadline': urgent_date,
            'importance_factors': {},
            'confidence': 1.0
        }
        score = document_agent._calculate_importance_score(metadata)
        assert score >= 70  # High score for urgent deadline
    
    def test_calculate_importance_score_high_amount(self, document_agent):
        """Test importance score with high amount."""
        metadata = {
            'amount': 1500.0,
            'importance_factors': {'has_high_amount': True},
            'confidence': 1.0
        }
        score = document_agent._calculate_importance_score(metadata)
        assert score >= 65  # Increased score for high amount
    
    def test_calculate_importance_score_urgent_keywords(self, document_agent):
        """Test importance score with urgent keywords."""
        metadata = {
            'importance_factors': {'is_urgent': True},
            'confidence': 1.0
        }
        score = document_agent._calculate_importance_score(metadata)
        assert score >= 60  # Increased score for urgency
    
    def test_calculate_importance_score_requires_action(self, document_agent):
        """Test importance score when action is required."""
        metadata = {
            'importance_factors': {'requires_action': True},
            'confidence': 1.0
        }
        score = document_agent._calculate_importance_score(metadata)
        assert score >= 55  # Increased score for required action
    
    def test_calculate_importance_score_combined_factors(self, document_agent):
        """Test importance score with multiple factors."""
        urgent_date = (date.today() + timedelta(days=2)).isoformat()
        metadata = {
            'deadline': urgent_date,
            'amount': 2000.0,
            'importance_factors': {
                'has_deadline': True,
                'is_urgent': True,
                'has_high_amount': True,
                'requires_action': True
            },
            'confidence': 1.0
        }
        score = document_agent._calculate_importance_score(metadata)
        assert score >= 95  # Very high score with all factors
    
    def test_calculate_importance_score_low_confidence(self, document_agent):
        """Test importance score adjustment with low confidence."""
        metadata = {
            'amount': 1000.0,
            'importance_factors': {'has_high_amount': True},
            'confidence': 0.5
        }
        score = document_agent._calculate_importance_score(metadata)
        # Score should be reduced due to low confidence
        assert score < 70
    
    def test_calculate_importance_score_max_cap(self, document_agent):
        """Test that importance score is capped at 100."""
        # Create extreme conditions
        yesterday = (date.today() - timedelta(days=10)).isoformat()
        metadata = {
            'deadline': yesterday,
            'amount': 10000.0,
            'importance_factors': {
                'has_deadline': True,
                'is_urgent': True,
                'has_high_amount': True,
                'requires_action': True
            },
            'confidence': 1.0
        }
        score = document_agent._calculate_importance_score(metadata)
        assert score <= 100  # Should not exceed 100
    
    def test_validate_metadata_structure(self, document_agent):
        """Test metadata validation ensures all required fields."""
        raw_metadata = {
            'document_type': 'invoice',
            'amount': 100.0
        }
        validated = document_agent._validate_metadata(raw_metadata, 0.9)
        
        # Check all required fields are present
        assert 'document_type' in validated
        assert 'document_date' in validated
        assert 'deadline' in validated
        assert 'amount' in validated
        assert 'currency' in validated
        assert 'keywords' in validated
        assert 'importance_factors' in validated
        assert 'confidence' in validated
        assert 'summary' in validated
        assert 'importance_score' in validated
    
    def test_validate_metadata_importance_factors(self, document_agent):
        """Test that importance_factors has all required sub-fields."""
        raw_metadata = {
            'importance_factors': {'is_urgent': True}
        }
        validated = document_agent._validate_metadata(raw_metadata, 1.0)
        
        factors = validated['importance_factors']
        assert 'has_deadline' in factors
        assert 'is_urgent' in factors
        assert 'has_high_amount' in factors
        assert 'requires_action' in factors
    
    def test_validate_metadata_confidence_cap(self, document_agent):
        """Test that confidence is capped by OCR confidence."""
        raw_metadata = {
            'confidence': 0.95
        }
        validated = document_agent._validate_metadata(raw_metadata, 0.7)
        
        # Confidence should be limited by OCR confidence
        assert validated['confidence'] == 0.7
    
    def test_validate_metadata_keywords_limit(self, document_agent):
        """Test that keywords are limited to 10."""
        raw_metadata = {
            'keywords': [f'keyword{i}' for i in range(20)]
        }
        validated = document_agent._validate_metadata(raw_metadata, 1.0)
        
        assert len(validated['keywords']) == 10
    
    def test_validate_metadata_summary_limit(self, document_agent):
        """Test that summary is limited to 500 characters."""
        raw_metadata = {
            'summary': 'x' * 1000
        }
        validated = document_agent._validate_metadata(raw_metadata, 1.0)
        
        assert len(validated['summary']) == 500
    
    def test_get_default_metadata(self, document_agent):
        """Test default metadata structure."""
        default = document_agent._get_default_metadata()
        
        assert default['document_type'] == 'other'
        assert default['confidence'] == 0.0
        assert default['importance_score'] == 50.0
        assert isinstance(default['keywords'], list)
        assert isinstance(default['importance_factors'], dict)


@pytest.mark.asyncio
class TestDocumentAgentAsync:
    """Async tests for DocumentAgent."""
    
    async def test_analyze_document_empty_text(self):
        """Test analyzing empty document."""
        agent = DocumentAgent()
        result = await agent.analyze_document("", 1.0)
        
        assert result['document_type'] == 'other'
        assert result['confidence'] == 0.0
    
    # Note: Full integration tests with OpenAI would require mocking
    # or actual API calls, which should be done in integration tests

