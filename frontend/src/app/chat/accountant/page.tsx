'use client'

import { useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { Layout } from '@/components/Layout'
import { ChatInterface } from '@/components/ChatInterface'

export default function AccountantChatPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const documentId = searchParams.get('documentId')

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      router.push('/')
    }
  }, [router])

  return (
    <Layout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Agent Comptable</h1>
          <p className="text-gray-600 mt-1">
            {documentId 
              ? "Analyse du document en cours..." 
              : "Posez vos questions sur vos finances, budgets et dépenses"}
          </p>
        </div>

        <ChatInterface 
          agentType="accountant" 
          initialDocumentId={documentId ? parseInt(documentId) : undefined}
        />
      </div>
    </Layout>
  )
}
