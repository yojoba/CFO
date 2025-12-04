"""Chat schemas."""
from pydantic import BaseModel
from datetime import datetime
from typing import Optional, List
from app.models.conversation import AgentType, MessageRole


class ChatRequest(BaseModel):
    """Schema for chat request."""
    message: str
    conversation_id: Optional[int] = None


class MessageResponse(BaseModel):
    """Schema for message response."""
    role: MessageRole
    content: str
    created_at: datetime
    
    class Config:
        from_attributes = True


class ChatResponse(BaseModel):
    """Schema for chat response."""
    conversation_id: int
    message: str
    agent_type: AgentType


class ConversationResponse(BaseModel):
    """Schema for conversation response."""
    id: int
    agent_type: AgentType
    title: Optional[str]
    created_at: datetime
    messages: List[MessageResponse]
    
    class Config:
        from_attributes = True
