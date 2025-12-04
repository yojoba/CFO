'use client'

import { useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { Layout } from '@/components/Layout'
import { ChatInterface } from '@/components/ChatInterface'

export default function LegalChatPage() {
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
          <h1 className="text-3xl font-bold text-gray-900">Agent Juridique</h1>
          <p className="text-gray-600 mt-1">
            {documentId 
              ? "Analyse juridique du document en cours..." 
              : "Questions sur le droit suisse, contrats et obligations légales"}
          </p>
        </div>

        <ChatInterface 
          agentType="legal" 
          initialDocumentId={documentId ? parseInt(documentId) : undefined}
        />
      </div>
    </Layout>
  )
}
