'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Layout } from '@/components/Layout'
import { FilingCabinetBrowser } from '@/components/FilingCabinetBrowser'
import { FolderTree, Upload, ArrowLeft } from 'lucide-react'
import Link from 'next/link'

export default function FilingCabinetPage() {
  const router = useRouter()

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      router.push('/')
    }
  }, [router])

  return (
    <Layout>
      <div className="space-y-6">
        {/* Page Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link
              href="/documents"
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
              title="Retour aux documents"
            >
              <ArrowLeft className="w-5 h-5 text-gray-600" />
            </Link>
            <div>
              <div className="flex items-center gap-3">
                <div className="p-3 bg-blue-100 rounded-lg">
                  <FolderTree className="w-8 h-8 text-blue-600" />
                </div>
                <div>
                  <h1 className="text-4xl font-bold text-gray-900">Classeur Virtuel</h1>
                  <p className="text-gray-600 mt-1">
                    Explorez vos documents organisés par année, catégorie et type
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Quick actions */}
          <div className="flex gap-3">
            <Link
              href="/documents"
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg inline-flex items-center gap-2 transition-colors"
            >
              <Upload className="w-4 h-4" />
              Uploader
            </Link>
          </div>
        </div>

        {/* Info banner */}
        <div className="bg-blue-50 border-l-4 border-blue-500 p-4 rounded-lg">
          <div className="flex items-start gap-3">
            <div className="p-1">
              <svg className="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
              </svg>
            </div>
            <div>
              <h3 className="font-semibold text-blue-900 mb-1">Comment utiliser le classeur</h3>
              <ul className="text-sm text-blue-800 space-y-1">
                <li>• <strong>Navigation 3 niveaux</strong> : Année → Catégorie (Impots, Assurance...) → Type (Factures, Courrier...)</li>
                <li>• <strong>Documents "Non classé"</strong> : Reclassifiez-les manuellement avec le bouton "Choisir une catégorie"</li>
                <li>• <strong>Recherche</strong> : Mode local (dans dossier actuel) ou global (tous documents)</li>
                <li>• <strong>Filtres avancés</strong> : Par montant, importance, dates</li>
                <li>• <strong>Sélection multiple</strong> : Téléchargez plusieurs documents en masse</li>
                <li>• <strong>Statistiques</strong> : Visualisez vos documents par année et catégorie</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Filing Cabinet Browser */}
        <FilingCabinetBrowser />
      </div>
    </Layout>
  )
}

