"use client";

import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { Document } from "@/types";
import { 
  X, FileText, Calendar, DollarSign, AlertCircle, Edit2, 
  Save, MessageSquare, Scale, Calculator, Download, Trash2, Loader2 
} from "lucide-react";
import { formatDate } from "@/lib/utils";
import { useRouter } from "next/navigation";

interface DocumentDetailModalProps {
  documentId: number;
  onClose: () => void;
  onDelete?: () => void;
}

type TabType = "overview" | "ocr" | "analysis";

export function DocumentDetailModal({ documentId, onClose, onDelete }: DocumentDetailModalProps) {
  const [activeTab, setActiveTab] = useState<TabType>("overview");
  const [isEditing, setIsEditing] = useState(false);
  const [editedDoc, setEditedDoc] = useState<Partial<Document>>({});
  const queryClient = useQueryClient();
  const router = useRouter();

  // Fetch document details
  const { data: document, isLoading } = useQuery<Document>({
    queryKey: ["document", documentId],
    queryFn: async () => {
      const response = await api.get(`/documents/${documentId}`);
      return response.data;
    },
  });

  // ALL HOOKS MUST BE BEFORE ANY CONDITIONAL RETURNS
  const updateMutation = useMutation({
    mutationFn: async (data: Partial<Document>) => {
      if (!document) return;
      const response = await api.patch(`/documents/${document.id}`, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      setIsEditing(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async () => {
      if (!document) return;
      await api.delete(`/documents/${document.id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      onClose();
      if (onDelete) onDelete();
    },
  });

  // Initialize edited doc when document loads
  useEffect(() => {
    if (document) {
      setEditedDoc(document);
    }
  }, [document]);

  // Loading state check AFTER all hooks
  if (isLoading) {
    return (
      <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-2xl p-8">
          <Loader2 className="w-12 h-12 animate-spin text-blue-600 mx-auto" />
          <p className="text-gray-700 mt-4">Chargement...</p>
        </div>
      </div>
    );
  }

  if (!document) {
    return null;
  }

  const handleSave = () => {
    const updates: Partial<Document> = {
      display_name: editedDoc.display_name,
      importance_score: editedDoc.importance_score,
      deadline: editedDoc.deadline,
      extracted_amount: editedDoc.extracted_amount,
      currency: editedDoc.currency,
    };
    updateMutation.mutate(updates);
  };

  const handleAnalyzeWith = (agentType: "accountant" | "legal") => {
    // Rediriger vers la page chat avec le document en contexte
    router.push(`/chat/${agentType}?documentId=${document.id}`);
  };

  const getImportanceBadge = () => {
    const score = editedDoc.importance_score || 0;
    if (score >= 80) return { color: "bg-red-100 text-red-800", label: "Urgent" };
    if (score >= 60) return { color: "bg-yellow-100 text-yellow-800", label: "Important" };
    return { color: "bg-green-100 text-green-800", label: "Normal" };
  };

  const badge = getImportanceBadge();

  // Safe JSON parsing for keywords
  let keywords: string[] = [];
  try {
    keywords = editedDoc.keywords ? JSON.parse(editedDoc.keywords) : [];
  } catch (e) {
    console.error("Error parsing keywords:", e);
    keywords = [];
  }

  // Safe JSON parsing for extracted_data
  let extractedData: any = null;
  try {
    extractedData = document.extracted_data ? JSON.parse(document.extracted_data) : null;
  } catch (e) {
    console.error("Error parsing extracted_data:", e);
    extractedData = null;
  }

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-2xl max-w-5xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white p-6">
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-2">
                <FileText className="w-6 h-6" />
                <h2 className="text-2xl font-bold">
                  {editedDoc.display_name || editedDoc.original_filename}
                </h2>
              </div>
              <div className="flex items-center gap-3 text-sm">
                <span className={`px-3 py-1 rounded-full ${badge.color} text-xs font-semibold`}>
                  {badge.label} ({editedDoc.importance_score?.toFixed(0) || 0})
                </span>
                <span className="px-3 py-1 bg-white bg-opacity-20 rounded-full">
                  {document.document_type}
                </span>
                <span className="px-3 py-1 bg-white bg-opacity-20 rounded-full">
                  {document.status === "completed" ? "✓ Traité" : document.status}
                </span>
              </div>
            </div>
            <button
              onClick={onClose}
              className="text-white hover:bg-white hover:bg-opacity-20 rounded-full p-2"
            >
              <X className="w-6 h-6" />
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-gray-200 px-6">
          <button
            onClick={() => setActiveTab("overview")}
            className={`px-4 py-3 font-medium text-sm border-b-2 transition-colors ${
              activeTab === "overview"
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            📋 Vue d'ensemble
          </button>
          <button
            onClick={() => setActiveTab("ocr")}
            className={`px-4 py-3 font-medium text-sm border-b-2 transition-colors ${
              activeTab === "ocr"
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            📄 Texte OCR
          </button>
          <button
            onClick={() => setActiveTab("analysis")}
            className={`px-4 py-3 font-medium text-sm border-b-2 transition-colors ${
              activeTab === "analysis"
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            🤖 Analyse IA
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {activeTab === "overview" && (
            <div className="space-y-6">
              {/* Métadonnées */}
              <div className="bg-gray-50 rounded-lg p-4 space-y-3">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold text-gray-900">Métadonnées</h3>
                  {!isEditing ? (
                    <button
                      onClick={() => setIsEditing(true)}
                      className="flex items-center gap-2 px-3 py-1 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm"
                    >
                      <Edit2 className="w-4 h-4" />
                      Éditer
                    </button>
                  ) : (
                    <div className="flex gap-2">
                      <button
                        onClick={() => {
                          setEditedDoc(document);
                          setIsEditing(false);
                        }}
                        className="px-3 py-1 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 text-sm"
                      >
                        Annuler
                      </button>
                      <button
                        onClick={handleSave}
                        disabled={updateMutation.isPending}
                        className="flex items-center gap-2 px-3 py-1 bg-green-600 text-white rounded-lg hover:bg-green-700 text-sm disabled:opacity-50"
                      >
                        <Save className="w-4 h-4" />
                        Sauvegarder
                      </button>
                    </div>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Nom du document
                    </label>
                    {isEditing ? (
                      <input
                        type="text"
                        value={editedDoc.display_name || ""}
                        onChange={(e) => setEditedDoc({ ...editedDoc, display_name: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                      />
                    ) : (
                      <p className="text-gray-900">{editedDoc.display_name || "Non défini"}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Type de document
                    </label>
                    <p className="text-gray-900 capitalize">{document.document_type}</p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Score d'importance
                    </label>
                    {isEditing ? (
                      <input
                        type="number"
                        min="0"
                        max="100"
                        value={editedDoc.importance_score || 0}
                        onChange={(e) => setEditedDoc({ ...editedDoc, importance_score: parseFloat(e.target.value) })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                      />
                    ) : (
                      <p className="text-gray-900">{editedDoc.importance_score?.toFixed(0) || "N/A"}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Date du document
                    </label>
                    <p className="text-gray-900">
                      {document.document_date ? formatDate(document.document_date) : "Non détectée"}
                    </p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Échéance
                    </label>
                    {isEditing ? (
                      <input
                        type="date"
                        value={editedDoc.deadline || ""}
                        onChange={(e) => setEditedDoc({ ...editedDoc, deadline: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                      />
                    ) : (
                      <p className="text-gray-900">
                        {editedDoc.deadline ? formatDate(editedDoc.deadline) : "Aucune"}
                      </p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Montant
                    </label>
                    {isEditing ? (
                      <div className="flex gap-2">
                        <input
                          type="number"
                          step="0.01"
                          value={editedDoc.extracted_amount || ""}
                          onChange={(e) => setEditedDoc({ ...editedDoc, extracted_amount: parseFloat(e.target.value) })}
                          className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        />
                        <input
                          type="text"
                          value={editedDoc.currency || "CHF"}
                          onChange={(e) => setEditedDoc({ ...editedDoc, currency: e.target.value })}
                          className="w-20 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        />
                      </div>
                    ) : (
                      <p className="text-gray-900">
                        {editedDoc.extracted_amount ? 
                          `${editedDoc.extracted_amount.toFixed(2)} ${editedDoc.currency || "CHF"}` : 
                          "Aucun"}
                      </p>
                    )}
                  </div>
                </div>

                {/* File info */}
                <div className="pt-4 border-t border-gray-200 mt-4">
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <span className="text-gray-600">Nom de fichier :</span>
                      <p className="text-gray-900 font-mono text-xs">{document.original_filename}</p>
                    </div>
                    <div>
                      <span className="text-gray-600">Uploadé le :</span>
                      <p className="text-gray-900">{formatDate(document.created_at)}</p>
                    </div>
                    <div>
                      <span className="text-gray-600">Taille :</span>
                      <p className="text-gray-900">{(document.file_size / 1024).toFixed(0)} KB</p>
                    </div>
                    <div>
                      <span className="text-gray-600">Confiance IA :</span>
                      <p className="text-gray-900">
                        {document.classification_confidence ? 
                          `${(document.classification_confidence * 100).toFixed(0)}%` : 
                          "N/A"}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Analyser avec Agent */}
              <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg p-4 border border-blue-200">
                <h3 className="text-lg font-semibold text-gray-900 mb-3 flex items-center gap-2">
                  <MessageSquare className="w-5 h-5 text-blue-600" />
                  Analyser avec un Agent
                </h3>
                <p className="text-sm text-gray-700 mb-4">
                  Envoyez ce document à un agent spécialisé pour une analyse approfondie
                </p>
                <div className="flex gap-3">
                  <button
                    onClick={() => handleAnalyzeWith("accountant")}
                    className="flex-1 flex items-center justify-center gap-2 px-4 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    <Calculator className="w-5 h-5" />
                    <div className="text-left">
                      <div className="font-semibold">Agent Comptable</div>
                      <div className="text-xs opacity-90">Analyse financière</div>
                    </div>
                  </button>
                  <button
                    onClick={() => handleAnalyzeWith("legal")}
                    className="flex-1 flex items-center justify-center gap-2 px-4 py-3 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
                  >
                    <Scale className="w-5 h-5" />
                    <div className="text-left">
                      <div className="font-semibold">Agent Juridique</div>
                      <div className="text-xs opacity-90">Analyse légale</div>
                    </div>
                  </button>
                </div>
              </div>
            </div>
          )}

          {activeTab === "ocr" && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold text-gray-900">
                  Texte Extrait par OCR
                </h3>
                <span className="text-sm text-gray-600">
                  {document.extracted_text?.length || 0} caractères
                </span>
              </div>
              
              <textarea
                value={document.extracted_text || "Aucun texte extrait"}
                readOnly
                className="w-full h-96 px-4 py-3 border border-gray-300 rounded-lg bg-gray-50 font-mono text-sm text-gray-800 focus:ring-2 focus:ring-blue-500"
                style={{ whiteSpace: "pre-wrap" }}
              />

              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <p className="text-sm text-blue-800">
                  💡 <strong>Astuce :</strong> Ce texte est utilisé par les agents IA pour répondre à vos questions
                  et par le système RAG pour la recherche sémantique.
                </p>
              </div>
            </div>
          )}

          {activeTab === "analysis" && (
            <div className="space-y-6">
              <div className="bg-gray-50 rounded-lg p-4">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">Analyse Automatique</h3>
                
                {/* Keywords */}
                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Mots-clés détectés
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {keywords.length > 0 ? (
                      keywords.map((keyword: string, idx: number) => (
                        <span
                          key={idx}
                          className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm"
                        >
                          {keyword}
                        </span>
                      ))
                    ) : (
                      <span className="text-gray-500 text-sm">Aucun mot-clé détecté</span>
                    )}
                  </div>
                </div>

                {/* Summary */}
                {extractedData?.summary && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Résumé IA
                    </label>
                    <div className="bg-white rounded-lg p-4 border border-gray-200">
                      <p className="text-gray-800">
                        {extractedData.summary || "Aucun résumé disponible"}
                      </p>
                    </div>
                  </div>
                )}

                {/* Importance Factors */}
                {extractedData?.importance_factors && (
                  <div className="mt-4">
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Facteurs d'importance
                    </label>
                    <div className="grid grid-cols-2 gap-3">
                      {Object.entries(extractedData.importance_factors).map(
                        ([key, value]) => (
                          <div
                            key={key}
                            className={`flex items-center gap-2 px-3 py-2 rounded-lg ${
                              value ? "bg-green-100 text-green-800" : "bg-gray-100 text-gray-600"
                            }`}
                          >
                            <span className="text-lg">{value ? "✓" : "○"}</span>
                            <span className="text-sm capitalize">
                              {key.replace(/_/g, " ")}
                            </span>
                          </div>
                        )
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Footer Actions */}
        <div className="border-t border-gray-200 px-6 py-4 bg-gray-50 flex items-center justify-between">
          <button
            onClick={() => deleteMutation.mutate()}
            disabled={deleteMutation.isPending}
            className="flex items-center gap-2 px-4 py-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50"
          >
            <Trash2 className="w-4 h-4" />
            Supprimer
          </button>

          <div className="flex gap-3">
            <button
              onClick={onClose}
              className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
            >
              Fermer
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
