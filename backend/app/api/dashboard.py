"""Dashboard API endpoints."""
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import func
from decimal import Decimal

from app.models.database import get_db
from app.models.user import User
from app.models.document import Document
from app.models.transaction import Transaction
from app.schemas.dashboard import DashboardStats, CategorySpending
from app.api.auth import get_current_user

router = APIRouter()


@router.get("/", response_model=DashboardStats)
def get_dashboard_stats(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get dashboard statistics for the user."""
    # Count documents
    total_documents = db.query(Document)\
        .filter(Document.user_id == current_user.id)\
        .count()
    
    # Count transactions
    total_transactions = db.query(Transaction)\
        .filter(Transaction.user_id == current_user.id)\
        .count()
    
    # Calculate total spending
    total_spending_result = db.query(func.sum(Transaction.amount))\
        .filter(Transaction.user_id == current_user.id)\
        .scalar()
    total_spending = total_spending_result if total_spending_result else Decimal(0)
    
    # Group by category
    category_stats = db.query(
        Transaction.category,
        func.sum(Transaction.amount).label('total'),
        func.count(Transaction.id).label('count')
    )\
        .filter(Transaction.user_id == current_user.id)\
        .group_by(Transaction.category)\
        .all()
    
    spending_by_category = [
        CategorySpending(
            category=stat.category or "Non catégorisé",
            amount=stat.total,
            count=stat.count
        )
        for stat in category_stats
    ]
    
    return DashboardStats(
        total_documents=total_documents,
        total_transactions=total_transactions,
        total_spending=total_spending,
        currency="CHF",
        spending_by_category=spending_by_category,
        recent_transactions_count=min(total_transactions, 10)
    )

