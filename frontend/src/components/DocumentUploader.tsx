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
  const [selectedType, setSelectedType] = useState('invoice')
  const [uploadedFiles, setUploadedFiles] = useState<string[]>([])

  const uploadMutation = useMutation({
    mutationFn: async ({ file, type }: { file: File; type: string }) => {
      const formData = new FormData()
      formData.append('file', file)
      
      const response = await api.post<UploadResponse>(
        `/documents/upload?document_type=${type}`,
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
        uploadMutation.mutate({ file, type: selectedType })
      })
    },
    [uploadMutation, selectedType]
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
      {/* Document Type Selector */}
      <div className="bg-white rounded-lg shadow p-4">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Type de document
        </label>
        <select
          value={selectedType}
          onChange={(e) => setSelectedType(e.target.value)}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        >
          <option value="invoice">Facture</option>
          <option value="contract">Contrat</option>
          <option value="letter">Courrier</option>
          <option value="receipt">Reçu</option>
          <option value="other">Autre</option>
        </select>
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
            <span className="text-blue-700">Téléchargement en cours...</span>
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
              <div className="flex items-center">
                <FileText className="w-5 h-5 text-green-600 mr-3" />
                <span className="text-green-700">
                  {filename} téléchargé avec succès!
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
