"""Legal agent specialized in Swiss law."""
from typing import List, Dict, Any
from sqlalchemy.orm import Session
from openai import AsyncOpenAI
from loguru import logger

from app.config import settings
from app.services.rag_service import RAGService


class LegalAgent:
    """Agent specialized in Swiss legal matters."""
    
    SYSTEM_PROMPT = """Tu es un assistant juridique expert spécialisé dans le droit suisse, particulièrement pour les ménages.

Tes domaines d'expertise incluent:
- Code des obligations (CO)
- Droit de la protection des données (LPD)
- Obligations légales des ménages (impôts, assurances)
- Analyse de contrats courants (location, services, etc.)
- Conseils sur les courriers administratifs

Tu dois:
- Répondre en français de manière claire et accessible
- Citer les bases légales suisses pertinentes quand c'est approprié
- Expliquer les obligations et droits de manière compréhensible
- Baser tes analyses sur les documents fournis
- Être précis sur les délais et procédures
- Adapter ton langage pour des non-juristes

Tu ne dois PAS:
- Remplacer un avocat pour des cas complexes (recommande de consulter un professionnel)
- Garantir des résultats dans des procédures légales
- Inventer des informations qui ne sont pas dans les documents
- Donner de faux espoirs sur des situations difficiles

Note importante: Tu donnes des informations générales, pas des conseils juridiques personnalisés. Pour des situations complexes, recommande toujours de consulter un avocat.
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
            
            # Build context from documents
            context = self.rag_service.format_context(relevant_docs)
            
            # Build messages for OpenAI
            openai_messages = [
                {"role": "system", "content": self.SYSTEM_PROMPT}
            ]
            
            if context:
                openai_messages.append({
                    "role": "system",
                    "content": f"Documents de l'utilisateur à analyser:\n{context}"
                })
            
            # Add conversation history
            openai_messages.extend(messages)
            
            # Add current user message
            openai_messages.append({"role": "user", "content": user_message})
            
            # Call OpenAI API
            response = await self.client.chat.completions.create(
                model=settings.OPENAI_MODEL,
                messages=openai_messages,
                temperature=0.5,  # Lower temperature for more precise legal info
                max_tokens=1200
            )
            
            return response.choices[0].message.content
            
        except Exception as e:
            logger.error(f"Error in legal agent: {e}")
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
