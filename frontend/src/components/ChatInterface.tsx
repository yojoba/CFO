'use client'

import { useState, useRef, useEffect } from 'react'
import { useMutation } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { Send, Loader2, User, Bot, FileText } from 'lucide-react'
import { Document } from '@/types'

interface Message {
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}

interface ChatResponse {
  conversation_id: number
  message: string
  agent_type: string
}

interface ChatInterfaceProps {
  agentType: 'accountant' | 'legal'
  initialDocumentId?: number
}

export function ChatInterface({ agentType, initialDocumentId }: ChatInterfaceProps) {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [conversationId, setConversationId] = useState<number | null>(null)
  const [isLoadingDocument, setIsLoadingDocument] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const hasLoadedDocument = useRef(false)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  // Load document and send initial message if initialDocumentId is provided
  useEffect(() => {
    const loadDocumentAndAnalyze = async () => {
      if (!initialDocumentId || hasLoadedDocument.current) return;
      
      hasLoadedDocument.current = true;
      setIsLoadingDocument(true);
      
      try {
        // Load document details
        const response = await api.get<Document>(`/documents/${initialDocumentId}`);
        const document = response.data;
        
        // Create initial message with document context
        const documentInfo = `📄 **${document.display_name || document.original_filename}**

**Type** : ${document.document_type}
${document.extracted_amount ? `**Montant** : ${document.extracted_amount.toFixed(2)} ${document.currency || 'CHF'}` : ''}
${document.deadline ? `**Échéance** : ${new Date(document.deadline).toLocaleDateString('fr-CH')}` : ''}
${document.importance_score ? `**Importance** : ${document.importance_score.toFixed(0)}/100` : ''}

**Contenu extrait :**
${document.extracted_text?.substring(0, 800) || 'Aucun texte extrait'}${document.extracted_text && document.extracted_text.length > 800 ? '...' : ''}

Que peux-tu me dire sur ce document ?`;
        
        // Add user message
        setMessages([{
          role: 'user',
          content: documentInfo,
          timestamp: new Date(),
        }]);
        
        setIsLoadingDocument(false);
        
        // Send to agent
        chatMutation.mutate(documentInfo);
        
      } catch (error) {
        console.error('Error loading document:', error);
        setIsLoadingDocument(false);
        setMessages([{
          role: 'assistant',
          content: 'Erreur lors du chargement du document. Veuillez réessayer.',
          timestamp: new Date(),
        }]);
      }
    };
    
    loadDocumentAndAnalyze();
  }, [initialDocumentId]); // eslint-disable-line react-hooks/exhaustive-deps

  const chatMutation = useMutation({
    mutationFn: async (message: string) => {
      const response = await api.post<ChatResponse>(`/chat/${agentType}`, {
        message,
        conversation_id: conversationId,
      })
      return response.data
    },
    onSuccess: (data) => {
      setConversationId(data.conversation_id)
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: data.message,
          timestamp: new Date(),
        },
      ])
    },
    onError: () => {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: 'Désolé, une erreur est survenue. Veuillez réessayer.',
          timestamp: new Date(),
        },
      ])
    },
  })

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!input.trim() || chatMutation.isPending) return

    const userMessage = input.trim()
    setInput('')

    // Add user message immediately
    setMessages((prev) => [
      ...prev,
      {
        role: 'user',
        content: userMessage,
        timestamp: new Date(),
      },
    ])

    // Send to API
    chatMutation.mutate(userMessage)
  }

  const getWelcomeMessage = () => {
    if (agentType === 'accountant') {
      return "Bonjour! Je suis votre assistant comptable. Je peux vous aider à analyser vos finances, catégoriser vos dépenses, et vous donner des conseils budgétaires. Comment puis-je vous aider aujourd'hui?"
    } else {
      return "Bonjour! Je suis votre assistant juridique spécialisé en droit suisse. Je peux vous aider avec des questions sur vos obligations légales, contrats, et courriers administratifs. Comment puis-je vous assister?"
    }
  }

  return (
    <div className="bg-white rounded-lg shadow flex flex-col h-[600px]">
      {/* Messages Area */}
      <div className="flex-1 overflow-y-auto p-6 space-y-4">
        {isLoadingDocument && (
          <div className="flex items-center justify-center p-4">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600 mr-3" />
            <span className="text-gray-700">Chargement du document...</span>
          </div>
        )}
        
        {messages.length === 0 && !isLoadingDocument && !initialDocumentId && (
          <div className="flex items-start space-x-3">
            <div className="flex-shrink-0">
              <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center">
                <Bot className="w-5 h-5 text-blue-600" />
              </div>
            </div>
            <div className="flex-1">
              <div className="bg-gray-100 rounded-lg p-4">
                <p className="text-gray-800">{getWelcomeMessage()}</p>
              </div>
            </div>
          </div>
        )}

        {messages.map((message, index) => (
          <div
            key={index}
            className={`flex items-start space-x-3 ${
              message.role === 'user' ? 'flex-row-reverse space-x-reverse' : ''
            }`}
          >
            <div className="flex-shrink-0">
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center ${
                  message.role === 'user'
                    ? 'bg-blue-600'
                    : 'bg-blue-100'
                }`}
              >
                {message.role === 'user' ? (
                  <User className="w-5 h-5 text-white" />
                ) : (
                  <Bot className="w-5 h-5 text-blue-600" />
                )}
              </div>
            </div>
            <div className="flex-1">
              <div
                className={`rounded-lg p-4 ${
                  message.role === 'user'
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-800'
                }`}
              >
                <p className="whitespace-pre-wrap">{message.content}</p>
              </div>
              <p className="text-xs text-gray-500 mt-1">
                {message.timestamp.toLocaleTimeString('fr-CH', {
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </p>
            </div>
          </div>
        ))}

        {chatMutation.isPending && (
          <div className="flex items-start space-x-3">
            <div className="flex-shrink-0">
              <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center">
                <Bot className="w-5 h-5 text-blue-600" />
              </div>
            </div>
            <div className="flex-1">
              <div className="bg-gray-100 rounded-lg p-4">
                <Loader2 className="w-5 h-5 text-blue-600 animate-spin" />
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="border-t p-4">
        <form onSubmit={handleSubmit} className="flex space-x-4">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Écrivez votre message..."
            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            disabled={chatMutation.isPending}
          />
          <button
            type="submit"
            disabled={!input.trim() || chatMutation.isPending}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center space-x-2"
          >
            <Send className="w-5 h-5" />
            <span>Envoyer</span>
          </button>
        </form>
      </div>
    </div>
  )
}
