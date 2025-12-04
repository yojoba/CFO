"""Accountant agent for financial analysis."""
from typing import List, Dict, Any
from sqlalchemy.orm import Session
from openai import AsyncOpenAI
from loguru import logger

from app.config import settings
from app.services.rag_service import RAGService
from app.models.transaction import Transaction
from app.models.conversation import Message, MessageRole


class AccountantAgent:
    """Agent specialized in financial analysis and budgeting."""
    
    SYSTEM_PROMPT = """Tu es un assistant comptable expert spécialisé dans la gestion financière des ménages suisses.

Tes compétences incluent:
- Analyse des factures et relevés financiers
- Catégorisation des dépenses
- Conseils budgétaires et optimisation des dépenses
- Suivi des transactions
- Suggestions d'économies

Tu dois:
- Répondre en français de manière claire et professionnelle
- Baser tes réponses sur les documents fournis quand c'est pertinent
- Donner des conseils pratiques et actionnables
- Utiliser le franc suisse (CHF) comme devise par défaut
- Être précis avec les chiffres et les montants

Tu ne dois PAS:
- Donner des conseils d'investissement complexes (ce n'est pas ton rôle)
- Promettre des résultats garantis
- Inventer des informations qui ne sont pas dans les documents
"""
    
    def __init__(self, db: Session, user_id: int):
        self.db = db
        self.user_id = user_id
        self.client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)
        self.rag_service = RAGService()
    
    async def process_message(self, user_message: str, conversation_id: int) -> str:
        """Process user message and generate response.
        
        Args:
            user_message: User's message
            conversation_id: ID of conversation
            
        Returns:
            Agent's response
        """
        try:
            # Get conversation history
            messages = self._get_conversation_history(conversation_id)
            
            # Search relevant documents using RAG
            relevant_docs = await self.rag_service.search_documents(
                query=user_message,
                user_id=self.user_id,
                db=self.db
            )
            
            # Get user's transactions for context
            recent_transactions = self._get_recent_transactions(limit=10)
            
            # Build context
            context = self._build_context(relevant_docs, recent_transactions)
            
            # Build messages for OpenAI
            openai_messages = [
                {"role": "system", "content": self.SYSTEM_PROMPT}
            ]
            
            if context:
                openai_messages.append({
                    "role": "system",
                    "content": f"Contexte disponible:\n{context}"
                })
            
            # Add conversation history
            openai_messages.extend(messages)
            
            # Add current user message
            openai_messages.append({"role": "user", "content": user_message})
            
            # Call OpenAI API
            response = await self.client.chat.completions.create(
                model=settings.OPENAI_MODEL,
                messages=openai_messages,
                temperature=0.7,
                max_tokens=1000
            )
            
            return response.choices[0].message.content
            
        except Exception as e:
            logger.error(f"Error in accountant agent: {e}")
            return "Désolé, j'ai rencontré une erreur lors du traitement de votre message. Veuillez réessayer."
    
    def _get_conversation_history(self, conversation_id: int) -> List[Dict[str, str]]:
        """Get conversation history for context.
        
        Args:
            conversation_id: ID of conversation
            
        Returns:
            List of messages in OpenAI format
        """
        from app.models.conversation import Conversation
        
        conversation = self.db.query(Conversation).filter(
            Conversation.id == conversation_id
        ).first()
        
        if not conversation:
            return []
        
        messages = []
        for msg in conversation.messages[:-1]:  # Exclude current message
            messages.append({
                "role": msg.role.value,
                "content": msg.content
            })
        
        return messages
    
    def _get_recent_transactions(self, limit: int = 10) -> List[Transaction]:
        """Get user's recent transactions.
        
        Args:
            limit: Maximum number of transactions
            
        Returns:
            List of transactions
        """
        transactions = self.db.query(Transaction)\
            .filter(Transaction.user_id == self.user_id)\
            .order_by(Transaction.transaction_date.desc())\
            .limit(limit)\
            .all()
        
        return transactions
    
    def _build_context(
        self,
        relevant_docs: List[Dict[str, Any]],
        transactions: List[Transaction]
    ) -> str:
        """Build context from documents and transactions.
        
        Args:
            relevant_docs: Relevant documents from RAG
            transactions: Recent transactions
            
        Returns:
            Formatted context string
        """
        context_parts = []
        
        # Add documents context
        if relevant_docs:
            doc_context = self.rag_service.format_context(relevant_docs)
            context_parts.append(doc_context)
        
        # Add transactions context
        if transactions:
            trans_context = "\nTransactions récentes:\n"
            for trans in transactions:
                trans_context += f"- {trans.transaction_date}: {trans.amount} CHF - {trans.category or 'Non catégorisé'}\n"
            context_parts.append(trans_context)
        
        return "\n\n".join(context_parts)
