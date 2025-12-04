"use client";

import { useQuery } from "@tanstack/react-query";
import { api, getPreviewUrl, downloadOcrPdf, downloadOriginalDocument } from "@/lib/api";
import { Document } from "@/types";
import { X, Download, Printer, Loader2, FileText, Eye } from "lucide-react";
import { useState } from "react";

interface DocumentViewerProps {
  documentId: number;
  onClose: () => void;
}

export function DocumentViewer({ documentId, onClose }: DocumentViewerProps) {
  const [viewMode, setViewMode] = useState<"ocr" | "original">("ocr");

  const { data: document, isLoading, error } = useQuery<Document>({
    queryKey: ["document", documentId],
    queryFn: async () => {
      const response = await api.get(`/documents/${documentId}`);
      return response.data;
    },
  });

  const handlePrint = () => {
    const previewUrl = getPreviewUrl(documentId);
    const printWindow = window.open(previewUrl, '_blank');
    if (printWindow) {
      printWindow.onload = () => {
        printWindow.print();
      };
    }
  };

  const handleDownloadOcr = () => {
    const url = downloadOcrPdf(documentId);
    window.open(url, '_blank');
  };

  const handleDownloadOriginal = () => {
    const url = downloadOriginalDocument(documentId);
    window.open(url, '_blank');
  };

  if (isLoading) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div className="bg-white rounded-lg p-8">
          <Loader2 className="w-8 h-8 animate-spin text-blue-600 mx-auto" />
          <p className="mt-4 text-gray-600">Chargement du document...</p>
        </div>
      </div>
    );
  }

  if (error || !document) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div className="bg-white rounded-lg p-8 max-w-md">
          <div className="text-center">
            <FileText className="w-12 h-12 text-red-500 mx-auto mb-4" />
            <p className="text-red-600 font-semibold mb-2">Erreur de chargement</p>
            <p className="text-gray-600 mb-4">Impossible de charger le document</p>
            <button
              onClick={onClose}
              className="px-4 py-2 bg-gray-200 hover:bg-gray-300 rounded-lg transition-colors"
            >
              Fermer
            </button>
          </div>
        </div>
      </div>
    );
  }

  const previewUrl = getPreviewUrl(documentId);
  const hasOcrPdf = !!document.ocr_pdf_path;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-75 flex flex-col z-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-4 py-3">
        <div className="flex items-center justify-between">
          <div className="flex-1 min-w-0">
            <h3 className="text-lg font-semibold text-gray-900 truncate">
              {document.display_name || document.original_filename}
            </h3>
            {document.storage_year && (
              <p className="text-sm text-gray-500">
                Classement: {document.storage_year}/{document.document_type}
              </p>
            )}
          </div>
          
          <div className="flex items-center gap-2 ml-4">
            {/* View mode toggle */}
            {hasOcrPdf && (
              <div className="flex bg-gray-100 rounded-lg p-1">
                <button
                  onClick={() => setViewMode("ocr")}
                  className={`px-3 py-1 text-sm rounded transition-colors ${
                    viewMode === "ocr"
                      ? "bg-white text-blue-600 shadow-sm"
                      : "text-gray-600 hover:text-gray-900"
                  }`}
                >
                  <Eye className="w-4 h-4 inline mr-1" />
                  OCR
                </button>
                <button
                  onClick={() => setViewMode("original")}
                  className={`px-3 py-1 text-sm rounded transition-colors ${
                    viewMode === "original"
                      ? "bg-white text-blue-600 shadow-sm"
                      : "text-gray-600 hover:text-gray-900"
                  }`}
                >
                  <FileText className="w-4 h-4 inline mr-1" />
                  Original
                </button>
              </div>
            )}

            {/* Actions */}
            <button
              onClick={handlePrint}
              className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
              title="Imprimer"
            >
              <Printer className="w-5 h-5" />
            </button>
            
            <button
              onClick={hasOcrPdf ? handleDownloadOcr : handleDownloadOriginal}
              className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
              title="Télécharger"
            >
              <Download className="w-5 h-5" />
            </button>

            <button
              onClick={onClose}
              className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
              title="Fermer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>

      {/* PDF Viewer */}
      <div className="flex-1 bg-gray-900 overflow-hidden">
        {hasOcrPdf || document.mime_type === 'application/pdf' ? (
          <iframe
            src={previewUrl}
            className="w-full h-full"
            title="Document Preview"
          />
        ) : (
          <div className="flex items-center justify-center h-full">
            <div className="bg-white rounded-lg p-8 max-w-md text-center">
              <FileText className="w-16 h-16 text-gray-400 mx-auto mb-4" />
              <p className="text-gray-600 mb-4">
                Aperçu non disponible pour ce type de fichier
              </p>
              <button
                onClick={handleDownloadOriginal}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors inline-flex items-center gap-2"
              >
                <Download className="w-4 h-4" />
                Télécharger le fichier
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Footer with document info */}
      <div className="bg-white border-t border-gray-200 px-4 py-2">
        <div className="flex items-center justify-between text-sm text-gray-600">
          <div className="flex items-center gap-4">
            {document.document_date && (
              <span>Date: {new Date(document.document_date).toLocaleDateString('fr-CH')}</span>
            )}
            {document.extracted_amount && (
              <span>Montant: {document.extracted_amount.toFixed(2)} {document.currency || 'CHF'}</span>
            )}
          </div>
          <div>
            {hasOcrPdf && (
              <span className="text-green-600 font-medium">✓ Searchable PDF</span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

