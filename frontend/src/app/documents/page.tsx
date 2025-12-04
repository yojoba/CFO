'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Layout } from '@/components/Layout'
import { DocumentUploader } from '@/components/DocumentUploader'
import { DocumentList } from '@/components/DocumentList'
import { FilingCabinetBrowser } from '@/components/FilingCabinetBrowser'
import { List, FolderTree } from 'lucide-react'

type ViewType = 'list' | 'cabinet';

export default function DocumentsPage() {
  const router = useRouter()
  const [viewType, setViewType] = useState<ViewType>('list')

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      router.push('/')
    }
  }, [router])

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Documents</h1>
            <p className="text-gray-600 mt-1">
              Gérez vos factures, contrats et courriers
            </p>
          </div>

          {/* View Toggle */}
          <div className="flex bg-white rounded-lg border border-gray-200 p-1">
            <button
              onClick={() => setViewType('list')}
              className={`px-4 py-2 rounded-md flex items-center gap-2 transition-colors ${
                viewType === 'list'
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              <List className="w-4 h-4" />
              <span className="font-medium">Liste</span>
            </button>
            <button
              onClick={() => setViewType('cabinet')}
              className={`px-4 py-2 rounded-md flex items-center gap-2 transition-colors ${
                viewType === 'cabinet'
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              <FolderTree className="w-4 h-4" />
              <span className="font-medium">Classeur</span>
            </button>
          </div>
        </div>

        {/* Upload Section */}
        <DocumentUploader />

        {/* View Content */}
        {viewType === 'list' ? (
          <DocumentList />
        ) : (
          <FilingCabinetBrowser />
        )}
      </div>
    </Layout>
  )
}
