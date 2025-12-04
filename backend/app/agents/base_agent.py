"""Base agent class"""

from typing import List, Dict, Any
from langchain_openai import ChatOpenAI
from langchain.agents import AgentExecutor, create_openai_functions_agent
from langchain.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain.tools import Tool
from loguru import logger

from app.config import settings


class BaseAgent:
    """Base class for all LLM agents"""
    
    def __init__(
        self,
        name: str,
        system_prompt: str,
        tools: List[Tool] = None,
        temperature: float = 0.7,
    ):
        self.name = name
        self.system_prompt = system_prompt
        self.tools = tools or []
        
        # Initialize LLM
        self.llm = ChatOpenAI(
            model=settings.OPENAI_MODEL,
            temperature=temperature,
            api_key=settings.OPENAI_API_KEY,
        )
        
        # Create prompt template
        self.prompt = ChatPromptTemplate.from_messages([
            ("system", system_prompt),
            MessagesPlaceholder(variable_name="chat_history", optional=True),
            ("human", "{input}"),
            MessagesPlaceholder(variable_name="agent_scratchpad"),
        ])
        
        # Create agent if tools are provided
        if self.tools:
            agent = create_openai_functions_agent(
                llm=self.llm,
                tools=self.tools,
                prompt=self.prompt,
            )
            self.agent_executor = AgentExecutor(
                agent=agent,
                tools=self.tools,
                verbose=True,
                max_iterations=5,
            )
        else:
            self.agent_executor = None
    
    async def run(
        self,
        query: str,
        chat_history: List[Dict[str, str]] = None,
        context: str = None,
    ) -> str:
        """
        Run the agent with a query.
        
        Args:
            query: User query
            chat_history: Previous conversation history
            context: Additional context (e.g., from RAG)
            
        Returns:
            str: Agent response
        """
        try:
            # Add context to query if provided
            if context:
                enhanced_query = f"Contexte des documents:\n{context}\n\nQuestion: {query}"
            else:
                enhanced_query = query
            
            if self.agent_executor:
                # Run with tools
                result = await self.agent_executor.ainvoke({
                    "input": enhanced_query,
                    "chat_history": chat_history or [],
                })
                return result["output"]
            else:
                # Run direct LLM call
                messages = [
                    ("system", self.system_prompt),
                ]
                if chat_history:
                    for msg in chat_history:
                        messages.append((msg["role"], msg["content"]))
                messages.append(("human", enhanced_query))
                
                response = await self.llm.ainvoke(messages)
                return response.content
                
        except Exception as e:
            logger.error(f"Error running agent {self.name}: {str(e)}")
            raise

