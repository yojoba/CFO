"""Chat API endpoints."""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from loguru import logger

from app.models.database import get_db
from app.models.user import User
from app.models.conversation import Conversation, Message, AgentType, MessageRole
from app.schemas.chat import ChatRequest, ChatResponse
from app.api.auth import get_current_user
from app.agents.accountant_agent import AccountantAgent
from app.agents.legal_agent import LegalAgent

router = APIRouter()


@router.post("/accountant", response_model=ChatResponse)
async def chat_with_accountant(
    request: ChatRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Chat with the accountant agent."""
    try:
        # Get or create conversation
        if request.conversation_id:
            conversation = db.query(Conversation)\
                .filter(
                    Conversation.id == request.conversation_id,
                    Conversation.user_id == current_user.id,
                    Conversation.agent_type == AgentType.ACCOUNTANT
                )\
                .first()
            if not conversation:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Conversation not found"
                )
        else:
            conversation = Conversation(
                user_id=current_user.id,
                agent_type=AgentType.ACCOUNTANT,
                title=request.message[:50]  # First 50 chars as title
            )
            db.add(conversation)
            db.commit()
            db.refresh(conversation)
        
        # Save user message
        user_message = Message(
            conversation_id=conversation.id,
            role=MessageRole.USER,
            content=request.message
        )
        db.add(user_message)
        db.commit()
        
        # Get agent response
        agent = AccountantAgent(db, current_user.id)
        response = await agent.process_message(request.message, conversation.id)
        
        # Save agent response
        agent_message = Message(
            conversation_id=conversation.id,
            role=MessageRole.ASSISTANT,
            content=response
        )
        db.add(agent_message)
        db.commit()
        
        return ChatResponse(
            conversation_id=conversation.id,
            message=response,
            agent_type=AgentType.ACCOUNTANT
        )
        
    except Exception as e:
        logger.error(f"Error in accountant chat: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error processing chat message"
        )


@router.post("/legal", response_model=ChatResponse)
async def chat_with_legal(
    request: ChatRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Chat with the legal agent."""
    try:
        # Get or create conversation
        if request.conversation_id:
            conversation = db.query(Conversation)\
                .filter(
                    Conversation.id == request.conversation_id,
                    Conversation.user_id == current_user.id,
                    Conversation.agent_type == AgentType.LEGAL
                )\
                .first()
            if not conversation:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Conversation not found"
                )
        else:
            conversation = Conversation(
                user_id=current_user.id,
                agent_type=AgentType.LEGAL,
                title=request.message[:50]
            )
            db.add(conversation)
            db.commit()
            db.refresh(conversation)
        
        # Save user message
        user_message = Message(
            conversation_id=conversation.id,
            role=MessageRole.USER,
            content=request.message
        )
        db.add(user_message)
        db.commit()
        
        # Get agent response
        agent = LegalAgent(db, current_user.id)
        response = await agent.process_message(request.message, conversation.id)
        
        # Save agent response
        agent_message = Message(
            conversation_id=conversation.id,
            role=MessageRole.ASSISTANT,
            content=response
        )
        db.add(agent_message)
        db.commit()
        
        return ChatResponse(
            conversation_id=conversation.id,
            message=response,
            agent_type=AgentType.LEGAL
        )
        
    except Exception as e:
        logger.error(f"Error in legal chat: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error processing chat message"
        )
