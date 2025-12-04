"""Dashboard schemas."""
from pydantic import BaseModel
from typing import Dict, List
from decimal import Decimal


class CategorySpending(BaseModel):
    """Schema for spending by category."""
    category: str
    amount: Decimal
    count: int


class DashboardStats(BaseModel):
    """Schema for dashboard statistics."""
    total_documents: int
    total_transactions: int
    total_spending: Decimal
    currency: str
    spending_by_category: List[CategorySpending]
    recent_transactions_count: int

