'use client'

import { useCallback, useState } from 'react'
import { useDropzone } from 'react-dropzone'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { Upload, FileText, X } from 'lucide-react'

interface UploadResponse {
  message: string
  document: {
    id: number
    filename: string
    original_filename: string
    status: string
  }
}

export function DocumentUploader() {
  const queryClient = useQueryClient()
  const [uploadedFiles, setUploadedFiles] = useState<string[]>([])

  const uploadMutation = useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData()
      formData.append('file', file)
      
      // Le type sera détecté automatiquement par l'IA
      const response = await api.post<UploadResponse>(
        `/documents/upload?document_type=other`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        }
      )
      return response.data
    },
    onSuccess: (data) => {
      setUploadedFiles((prev) => [...prev, data.document.original_filename])
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      setTimeout(() => {
        setUploadedFiles((prev) => 
          prev.filter((name) => name !== data.document.original_filename)
        )
      }, 3000)
    },
  })

  const onDrop = useCallback(
    (acceptedFiles: File[]) => {
      acceptedFiles.forEach((file) => {
        uploadMutation.mutate(file)
      })
    },
    [uploadMutation]
  )

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/pdf': ['.pdf'],
      'image/*': ['.png', '.jpg', '.jpeg', '.tiff', '.bmp'],
    },
    maxSize: 10 * 1024 * 1024, // 10MB
  })

  return (
    <div className="space-y-4">
      {/* Info Classification Automatique */}
      <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg shadow-sm p-4 border border-blue-200">
        <div className="flex items-start">
          <div className="flex-shrink-0">
            <svg className="h-6 w-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div className="ml-3 flex-1">
            <h3 className="text-sm font-semibold text-blue-900">
              🤖 Classification Automatique par IA
            </h3>
            <p className="mt-1 text-sm text-blue-700">
              Uploadez simplement vos documents ! Notre IA détecte automatiquement le type 
              (facture, courrier, contrat...), extrait les dates, montants et calcule le niveau d'urgence.
            </p>
          </div>
        </div>
      </div>

      {/* Dropzone */}
      <div
        {...getRootProps()}
        className={`bg-white rounded-lg shadow p-8 border-2 border-dashed transition-colors cursor-pointer ${
          isDragActive
            ? 'border-blue-500 bg-blue-50'
            : 'border-gray-300 hover:border-gray-400'
        }`}
      >
        <input {...getInputProps()} />
        <div className="flex flex-col items-center justify-center text-center">
          <Upload className={`w-12 h-12 mb-4 ${isDragActive ? 'text-blue-500' : 'text-gray-400'}`} />
          {isDragActive ? (
            <p className="text-lg text-blue-600">Déposez les fichiers ici...</p>
          ) : (
            <>
              <p className="text-lg text-gray-700 mb-1">
                Glissez-déposez vos fichiers ici
              </p>
              <p className="text-sm text-gray-500 mb-4">
                ou cliquez pour sélectionner
              </p>
              <p className="text-xs text-gray-400">
                PDF ou images (PNG, JPG, TIFF) - Max 10MB
              </p>
            </>
          )}
        </div>
      </div>

      {/* Upload Status */}
      {uploadMutation.isPending && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-center">
            <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600 mr-3" />
            <div>
              <span className="text-blue-700 font-medium">Upload en cours...</span>
              <p className="text-xs text-blue-600 mt-1">L'analyse IA démarrera automatiquement après l'upload</p>
            </div>
          </div>
        </div>
      )}

      {uploadMutation.isError && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <span className="text-red-700">
              Erreur lors du téléchargement. Veuillez réessayer.
            </span>
            <button
              onClick={() => uploadMutation.reset()}
              className="text-red-600 hover:text-red-800"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>
      )}

      {/* Success Messages */}
      {uploadedFiles.length > 0 && (
        <div className="space-y-2">
          {uploadedFiles.map((filename, idx) => (
            <div
              key={idx}
              className="bg-green-50 border border-green-200 rounded-lg p-4"
            >
              <div className="flex items-start">
                <FileText className="w-5 h-5 text-green-600 mr-3 mt-0.5 flex-shrink-0" />
                <div className="flex-1">
                  <p className="text-green-700 font-medium">
                    ✅ {filename} uploadé avec succès !
                  </p>
                  <p className="text-sm text-green-600 mt-1">
                    🔍 Analyse en cours : OCR + classification + extraction métadonnées...
                  </p>
                  <p className="text-xs text-green-500 mt-1">
                    Le document apparaîtra dans la liste dès que l'analyse sera terminée (~10-15 sec)
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
