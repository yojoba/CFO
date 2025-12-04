"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api, downloadOcrPdf, downloadOriginalDocument, getPreviewUrl } from "@/lib/api";
import { Document } from "@/types";
import { FileText, Trash2, Download, Loader2, AlertCircle, Calendar, DollarSign, Copy, Eye, Printer } from "lucide-react";
import { formatFileSize, formatDate } from "@/lib/utils";
import { DocumentDetailModal } from "./DocumentDetailModal";
import { DocumentViewer } from "./DocumentViewer";

const documentTypeLabels: Record<string, string> = {
  invoice: "Facture",
  receipt: "Reçu",
  contract: "Contrat",
  letter: "Courrier",
  tax_document: "Document fiscal",
  insurance: "Assurance",
  other: "Autre",
};

const statusLabels: Record<string, string> = {
  uploading: "En cours",
  processing: "Traitement",
  completed: "Traité",
  failed: "Échec",
};

// Badge d'importance avec couleurs
function ImportanceBadge({ score }: { score?: number }) {
  if (!score) return null;
  
  let color = "bg-green-100 text-green-800";
  let label = "Normal";
  
  if (score >= 80) {
    color = "bg-red-100 text-red-800";
    label = "Urgent";
  } else if (score >= 60) {
    color = "bg-yellow-100 text-yellow-800";
    label = "Important";
  }
  
  return (
    <span className={`px-2 py-1 text-xs rounded-full font-medium ${color}`}>
      {label} ({score.toFixed(0)})
    </span>
  );
}

// Affichage deadline avec alerte si proche
function DeadlineDisplay({ deadline }: { deadline?: string }) {
  if (!deadline) return <span className="text-gray-400">-</span>;
  
  const deadlineDate = new Date(deadline);
  const today = new Date();
  const diffDays = Math.ceil((deadlineDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  
  let color = "text-gray-600";
  let icon = null;
  
  if (diffDays < 0) {
    color = "text-red-600 font-semibold";
    icon = <AlertCircle className="w-4 h-4 inline mr-1" />;
  } else if (diffDays <= 7) {
    color = "text-orange-600 font-semibold";
    icon = <AlertCircle className="w-4 h-4 inline mr-1" />;
  }
  
  return (
    <span className={color}>
      {icon}
      {formatDate(deadline)}
      {diffDays < 0 && " (Dépassée)"}
    </span>
  );
}

type ViewMode = "all" | "urgent" | "by-importance" | "by-deadline";

export function DocumentList() {
  const queryClient = useQueryClient();
  const [selectedType, setSelectedType] = useState<string>("");
  const [viewMode, setViewMode] = useState<ViewMode>("all");
  const [selectedDocumentId, setSelectedDocumentId] = useState<number | null>(null);
  const [viewerDocumentId, setViewerDocumentId] = useState<number | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ["documents", selectedType, viewMode],
    queryFn: async () => {
      let endpoint = "/documents/";
      
      // Selon le mode de vue, appeler l'endpoint approprié
      if (viewMode === "urgent") {
        endpoint = "/documents/urgent";
      } else if (viewMode === "by-importance") {
        endpoint = "/documents/by-importance";
      } else if (viewMode === "by-deadline") {
        endpoint = "/documents/by-deadline";
      }
      
      const response = await api.get(endpoint, {
        params: selectedType ? { document_type: selectedType } : {}
      });
      
      return { documents: response.data };
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/documents/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
    },
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600">
          Erreur lors du chargement des documents
        </p>
      </div>
    );
  }

  const documents: Document[] = data?.documents || [];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">Mes Documents</h2>
        
        <select
          value={selectedType}
          onChange={(e) => setSelectedType(e.target.value)}
          className="px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Tous les types</option>
          {Object.entries(documentTypeLabels).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </div>

      {/* Onglets de vue */}
      <div className="flex space-x-2 border-b border-gray-200">
        <button
          onClick={() => setViewMode("all")}
          className={`px-4 py-2 font-medium text-sm border-b-2 transition-colors ${
            viewMode === "all"
              ? "border-blue-600 text-blue-600"
              : "border-transparent text-gray-500 hover:text-gray-700"
          }`}
        >
          📄 Tous
        </button>
        <button
          onClick={() => setViewMode("urgent")}
          className={`px-4 py-2 font-medium text-sm border-b-2 transition-colors ${
            viewMode === "urgent"
              ? "border-red-600 text-red-600"
              : "border-transparent text-gray-500 hover:text-gray-700"
          }`}
        >
          🚨 Urgents
        </button>
        <button
          onClick={() => setViewMode("by-importance")}
          className={`px-4 py-2 font-medium text-sm border-b-2 transition-colors ${
            viewMode === "by-importance"
              ? "border-orange-600 text-orange-600"
              : "border-transparent text-gray-500 hover:text-gray-700"
          }`}
        >
          ⭐ Par importance
        </button>
        <button
          onClick={() => setViewMode("by-deadline")}
          className={`px-4 py-2 font-medium text-sm border-b-2 transition-colors ${
            viewMode === "by-deadline"
              ? "border-purple-600 text-purple-600"
              : "border-transparent text-gray-500 hover:text-gray-700"
          }`}
        >
          📅 Par échéance
        </button>
      </div>

      {documents.length === 0 ? (
        <div className="text-center py-12 bg-gray-50 rounded-lg">
          <FileText className="w-12 h-12 text-gray-400 mx-auto mb-2" />
          <p className="text-gray-600">Aucun document trouvé</p>
          <p className="text-sm text-gray-500 mt-1">
            Uploadez votre premier document ci-dessus
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Nom
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Type
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Catégorie
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Année
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Importance
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Échéance
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Montant
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Statut
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {documents.map((doc) => (
                <tr 
                  key={doc.id} 
                  className={`hover:bg-gray-50 cursor-pointer transition-colors ${doc.is_duplicate ? 'bg-yellow-50' : ''}`}
                  onClick={() => setSelectedDocumentId(doc.id)}
                >
                  <td className="px-4 py-4">
                    <div className="flex flex-col max-w-xs">
                      <div className="flex items-center">
                        <FileText className="w-5 h-5 text-gray-400 mr-2 flex-shrink-0" />
                        <div className="flex flex-col">
                          <span className="text-sm font-medium text-gray-900 truncate" title={doc.display_name || doc.original_filename}>
                            {doc.display_name || doc.original_filename}
                          </span>
                          {doc.is_duplicate && (
                            <span className="text-xs text-orange-600 font-medium flex items-center mt-1">
                              <Copy className="w-3 h-3 mr-1" />
                              Doublon détecté ({(doc.similarity_score! * 100).toFixed(0)}% similaire)
                            </span>
                          )}
                        </div>
                      </div>
                      {doc.display_name && (
                        <span className="text-xs text-gray-500 ml-7 truncate" title={doc.original_filename}>
                          {doc.original_filename}
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <span className="px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-800">
                      {documentTypeLabels[doc.document_type]}
                    </span>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm">
                    {doc.category ? (
                      <span className="px-2 py-1 bg-purple-100 text-purple-800 rounded-full text-xs font-medium">
                        {doc.category}
                      </span>
                    ) : (
                      <span className="text-gray-400">-</span>
                    )}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                    {doc.storage_year || '-'}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <ImportanceBadge score={doc.importance_score} />
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm">
                    <DeadlineDisplay deadline={doc.deadline} />
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm">
                    {doc.extracted_amount ? (
                      <span className="font-medium text-gray-900">
                        {doc.extracted_amount.toFixed(2)} {doc.currency || "CHF"}
                      </span>
                    ) : (
                      <span className="text-gray-400">-</span>
                    )}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <span
                      className={`px-2 py-1 text-xs rounded-full ${
                        doc.status === "completed"
                          ? "bg-green-100 text-green-800"
                          : doc.status === "failed"
                          ? "bg-red-100 text-red-800"
                          : "bg-yellow-100 text-yellow-800"
                      }`}
                    >
                      {statusLabels[doc.status]}
                    </span>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setViewerDocumentId(doc.id);
                        }}
                        className="text-blue-600 hover:text-blue-900"
                        title="Prévisualiser"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                      {doc.ocr_pdf_path && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            window.open(getPreviewUrl(doc.id), '_blank');
                          }}
                          className="text-green-600 hover:text-green-900"
                          title="Imprimer"
                        >
                          <Printer className="w-4 h-4" />
                        </button>
                      )}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          const url = doc.ocr_pdf_path ? downloadOcrPdf(doc.id) : downloadOriginalDocument(doc.id);
                          window.open(url, '_blank');
                        }}
                        className="text-gray-600 hover:text-gray-900"
                        title="Télécharger"
                      >
                        <Download className="w-4 h-4" />
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          deleteMutation.mutate(doc.id);
                        }}
                        disabled={deleteMutation.isPending}
                        className="text-red-600 hover:text-red-900 disabled:opacity-50"
                        title="Supprimer"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Document Detail Modal */}
      {selectedDocumentId && (
        <DocumentDetailModal
          documentId={selectedDocumentId}
          onClose={() => setSelectedDocumentId(null)}
        />
      )}

      {/* Document Viewer */}
      {viewerDocumentId && (
        <DocumentViewer
          documentId={viewerDocumentId}
          onClose={() => setViewerDocumentId(null)}
        />
      )}
    </div>
  );
}

