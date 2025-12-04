"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { 
  getHierarchicalFilingCabinet, 
  getDocumentsByYearCategoryType, 
  downloadOcrPdf, 
  downloadOriginalDocument, 
  getPreviewUrl,
  getAllCategories,
  updateDocumentCategory,
  searchDocuments
} from "@/lib/api";
import { HierarchicalFilingCabinetStats, Document } from "@/types";
import { 
  ChevronDown, ChevronRight, FolderOpen, Folder, FileText, Loader2, 
  Calendar, Search, Download, Eye, Printer, Filter, BarChart3, 
  AlertCircle, Edit2, Check, X 
} from "lucide-react";
import { formatDate } from "@/lib/utils";
import { DocumentViewer } from "./DocumentViewer";
import { FilingCabinetStatsComponent } from "./FilingCabinetStats";

const documentTypeLabels: Record<string, string> = {
  invoice: "Factures",
  receipt: "Reçus",
  contract: "Contrats",
  letter: "Courrier",
  tax_document: "Documents fiscaux",
  insurance: "Assurance",
  other: "Autre",
};

const categoryIcons: Record<string, string> = {
  "Impots": "📋",
  "Poursuites": "⚖️",
  "Assurance": "🛡️",
  "Banque": "🏦",
  "Energie": "⚡",
  "Telecom": "📱",
  "Sante": "🏥",
  "Immobilier": "🏠",
  "Emploi": "💼",
  "Non classé": "📁",
};

interface NavigationPath {
  year: number | null;
  category: string | null;
  type: string | null;
}

export function FilingCabinetBrowser() {
  const queryClient = useQueryClient();
  const [selectedPath, setSelectedPath] = useState<NavigationPath>({
    year: null,
    category: null,
    type: null
  });
  const [searchQuery, setSearchQuery] = useState("");
  const [searchMode, setSearchMode] = useState<"local" | "global">("local");
  const [viewerDocumentId, setViewerDocumentId] = useState<number | null>(null);
  const [selectedDocuments, setSelectedDocuments] = useState<Set<number>>(new Set());
  const [showFilters, setShowFilters] = useState(false);
  const [showStats, setShowStats] = useState(false);
  const [editingDocumentId, setEditingDocumentId] = useState<number | null>(null);
  const [newCategory, setNewCategory] = useState("");
  const [filters, setFilters] = useState({
    minAmount: "",
    maxAmount: "",
    minImportance: "",
    dateFrom: "",
    dateTo: ""
  });

  // Fetch hierarchical overview
  const { data: overview, isLoading, error } = useQuery<HierarchicalFilingCabinetStats>({
    queryKey: ["hierarchical-filing-cabinet"],
    queryFn: getHierarchicalFilingCabinet,
  });

  // Fetch documents for selected path
  const { data: documents, isLoading: isLoadingDocuments } = useQuery<Document[]>({
    queryKey: ["filing-documents", selectedPath.year, selectedPath.category, selectedPath.type],
    queryFn: () => 
      selectedPath.year && selectedPath.category && selectedPath.type
        ? getDocumentsByYearCategoryType(selectedPath.year, selectedPath.category, selectedPath.type)
        : Promise.resolve([]),
    enabled: !!(selectedPath.year && selectedPath.category && selectedPath.type),
  });

  // Fetch all categories for dropdown
  const { data: allCategories } = useQuery<string[]>({
    queryKey: ["all-categories"],
    queryFn: getAllCategories,
  });

  // Fetch search results
  const { data: searchResults, isLoading: isSearching } = useQuery<Document[]>({
    queryKey: ["search-documents", searchQuery],
    queryFn: () => searchDocuments(searchQuery),
    enabled: searchMode === "global" && searchQuery.length >= 2,
  });

  // Update category mutation
  const updateCategoryMutation = useMutation({
    mutationFn: ({ docId, category }: { docId: number; category: string }) =>
      updateDocumentCategory(docId, category),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["hierarchical-filing-cabinet"] });
      queryClient.invalidateQueries({ queryKey: ["filing-documents"] });
      queryClient.invalidateQueries({ queryKey: ["all-categories"] });
      setEditingDocumentId(null);
      setNewCategory("");
    },
  });

  // Filter documents based on local search and filters
  const getFilteredDocuments = () => {
    let docs = searchMode === "global" && searchQuery ? searchResults : documents;
    if (!docs) return [];

    // Local search filter
    if (searchMode === "local" && searchQuery) {
      const query = searchQuery.toLowerCase();
      docs = docs.filter(doc => {
        const matchesName = (doc.display_name || doc.original_filename).toLowerCase().includes(query);
        const matchesText = doc.extracted_text?.toLowerCase().includes(query);
        return matchesName || matchesText;
      });
    }

    // Amount filter
    if (filters.minAmount && docs) {
      docs = docs.filter(doc => doc.extracted_amount && doc.extracted_amount >= parseFloat(filters.minAmount));
    }
    if (filters.maxAmount && docs) {
      docs = docs.filter(doc => doc.extracted_amount && doc.extracted_amount <= parseFloat(filters.maxAmount));
    }

    // Importance filter
    if (filters.minImportance && docs) {
      docs = docs.filter(doc => doc.importance_score && doc.importance_score >= parseFloat(filters.minImportance));
    }

    // Date filters
    if (filters.dateFrom && docs) {
      docs = docs.filter(doc => doc.document_date && new Date(doc.document_date) >= new Date(filters.dateFrom));
    }
    if (filters.dateTo && docs) {
      docs = docs.filter(doc => doc.document_date && new Date(doc.document_date) <= new Date(filters.dateTo));
    }

    return docs;
  };

  const filteredDocuments = getFilteredDocuments();

  const toggleDocumentSelection = (docId: number) => {
    setSelectedDocuments(prev => {
      const next = new Set(prev);
      if (next.has(docId)) {
        next.delete(docId);
      } else {
        next.add(docId);
      }
      return next;
    });
  };

  const handleBulkDownload = () => {
    selectedDocuments.forEach(docId => {
      const url = downloadOcrPdf(docId);
      window.open(url, '_blank');
    });
  };

  const selectAllVisibleDocuments = () => {
    const allIds = new Set(filteredDocuments.map(doc => doc.id));
    setSelectedDocuments(allIds);
  };

  const deselectAll = () => {
    setSelectedDocuments(new Set());
  };

  const handleCategoryChange = (docId: number, category: string) => {
    updateCategoryMutation.mutate({ docId, category });
  };

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
        <p className="text-red-600">Erreur lors du chargement du classeur</p>
      </div>
    );
  }

  if (!overview || overview.years.length === 0) {
    return (
      <div className="text-center py-12 bg-gray-50 rounded-lg">
        <FolderOpen className="w-12 h-12 text-gray-400 mx-auto mb-2" />
        <p className="text-gray-600">Aucun document dans le classeur</p>
        <p className="text-sm text-gray-500 mt-1">
          Uploadez vos premiers documents pour les organiser
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header with stats and search */}
      <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg p-6 border border-blue-200">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <Folder className="w-8 h-8 text-blue-600" />
            <div>
              <h3 className="text-2xl font-bold text-gray-900">Classeur Virtuel</h3>
              <p className="text-sm text-gray-600">Organisé par année, catégorie et type</p>
            </div>
          </div>
          <div className="flex gap-4 text-sm">
            <div className="text-center bg-white rounded-lg px-4 py-2 shadow-sm">
              <div className="text-3xl font-bold text-blue-600">{overview.total_documents}</div>
              <div className="text-gray-600">Documents</div>
            </div>
            <div className="text-center bg-white rounded-lg px-4 py-2 shadow-sm">
              <div className="text-3xl font-bold text-indigo-600">{overview.total_years}</div>
              <div className="text-gray-600">Années</div>
            </div>
          </div>
        </div>

        {/* Search and filters */}
        <div className="flex gap-3 mb-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <input
              type="text"
              placeholder="Rechercher dans vos documents..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`px-4 py-2 rounded-lg border transition-colors inline-flex items-center gap-2 ${
              showFilters ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
            }`}
          >
            <Filter className="w-4 h-4" />
            Filtres
          </button>
          <button
            onClick={() => setShowStats(!showStats)}
            className={`px-4 py-2 rounded-lg border transition-colors inline-flex items-center gap-2 ${
              showStats ? 'bg-indigo-600 text-white border-indigo-600' : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
            }`}
          >
            <BarChart3 className="w-4 h-4" />
            Statistiques
          </button>
        </div>

        {/* Search mode toggle */}
        {searchQuery && (
          <div className="flex items-center gap-2 mb-3">
            <span className="text-sm text-gray-600">Mode de recherche:</span>
            <button
              onClick={() => setSearchMode("local")}
              className={`px-3 py-1 rounded text-sm transition-colors ${
                searchMode === "local" ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-100'
              }`}
            >
              Dans la sélection
            </button>
            <button
              onClick={() => setSearchMode("global")}
              className={`px-3 py-1 rounded text-sm transition-colors ${
                searchMode === "global" ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-100'
              }`}
            >
              Recherche globale
            </button>
          </div>
        )}

        {/* Advanced Filters */}
        {showFilters && (
          <div className="mt-4 p-4 bg-white rounded-lg border border-gray-200 grid grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Montant min (CHF)</label>
              <input
                type="number"
                placeholder="0"
                value={filters.minAmount}
                onChange={(e) => setFilters({...filters, minAmount: e.target.value})}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Montant max (CHF)</label>
              <input
                type="number"
                placeholder="∞"
                value={filters.maxAmount}
                onChange={(e) => setFilters({...filters, maxAmount: e.target.value})}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Importance min</label>
              <input
                type="number"
                placeholder="0"
                min="0"
                max="100"
                value={filters.minImportance}
                onChange={(e) => setFilters({...filters, minImportance: e.target.value})}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Date de</label>
              <input
                type="date"
                value={filters.dateFrom}
                onChange={(e) => setFilters({...filters, dateFrom: e.target.value})}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Date à</label>
              <input
                type="date"
                value={filters.dateTo}
                onChange={(e) => setFilters({...filters, dateTo: e.target.value})}
                className="w-full px-3 py-1.5 border border-gray-300 rounded text-sm"
              />
            </div>
            <div className="flex items-end">
              <button
                onClick={() => setFilters({minAmount: "", maxAmount: "", minImportance: "", dateFrom: "", dateTo: ""})}
                className="w-full px-3 py-1.5 bg-gray-100 hover:bg-gray-200 rounded text-sm transition-colors"
              >
                Réinitialiser
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Visual Statistics */}
      {showStats && overview.years.length > 0 && (
        <FilingCabinetStatsComponent overview={{
          years: overview.years.map(y => ({
            year: y.year,
            document_counts: Object.values(y.categories).reduce((acc, cats) => {
              Object.entries(cats).forEach(([type, count]) => {
                acc[type] = (acc[type] || 0) + count;
              });
              return acc;
            }, {} as Record<string, number>),
            total: y.total
          })),
          total_documents: overview.total_documents,
          total_years: overview.total_years
        }} />
      )}

      {/* Years as visual cards */}
      <div className="space-y-6">
        {overview.years.map((yearData) => (
          <div key={yearData.year} className="bg-white rounded-xl shadow-md border-2 border-gray-200">
            {/* Year header */}
            <div 
              onClick={() => setSelectedPath({ 
                year: selectedPath.year === yearData.year ? null : yearData.year, 
                category: null, 
                type: null 
              })}
              className="bg-gradient-to-r from-blue-500 to-indigo-600 p-6 rounded-t-xl cursor-pointer hover:from-blue-600 hover:to-indigo-700 transition-colors"
            >
              <div className="flex items-center justify-between text-white">
                <div className="flex items-center gap-3">
                  {selectedPath.year === yearData.year ? (
                    <ChevronDown className="w-6 h-6" />
                  ) : (
                    <ChevronRight className="w-6 h-6" />
                  )}
                  <Calendar className="w-8 h-8" />
                  <div>
                    <h3 className="text-3xl font-bold">{yearData.year}</h3>
                    <p className="text-blue-100 text-sm">
                      {selectedPath.year === yearData.year ? 'Cliquez pour fermer' : 'Cliquez pour explorer'}
                    </p>
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-3xl font-bold">{yearData.total}</div>
                  <div className="text-blue-100 text-xs">documents</div>
                </div>
              </div>
            </div>

            {/* Categories (Level 2) */}
            {selectedPath.year === yearData.year && (
              <div className="p-6 space-y-3">
                {Object.entries(yearData.categories).map(([category, typeCounts]) => {
                  const categoryTotal = Object.values(typeCounts).reduce((sum, count) => sum + count, 0);
                  const isCategorySelected = selectedPath.category === category;
                  const isUncategorized = category === "Non classé";
                  
                  return (
                    <div key={category} className="border-2 border-gray-200 rounded-lg overflow-hidden">
                      {/* Category header */}
                      <div
                        onClick={() => setSelectedPath({
                          year: yearData.year,
                          category: isCategorySelected ? null : category,
                          type: null
                        })}
                        className={`p-4 cursor-pointer transition-colors flex items-center justify-between ${
                          isCategorySelected 
                            ? 'bg-indigo-100 hover:bg-indigo-200' 
                            : isUncategorized
                            ? 'bg-yellow-50 hover:bg-yellow-100'
                            : 'bg-gray-50 hover:bg-gray-100'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          {isCategorySelected ? (
                            <ChevronDown className="w-5 h-5 text-gray-700" />
                          ) : (
                            <ChevronRight className="w-5 h-5 text-gray-700" />
                          )}
                          <span className="text-2xl">{categoryIcons[category] || "📂"}</span>
                          <div>
                            <h4 className="font-bold text-gray-900 flex items-center gap-2">
                              {category}
                              {isUncategorized && (
                                <AlertCircle className="w-4 h-4 text-yellow-600" />
                              )}
                            </h4>
                            <p className="text-sm text-gray-600">{categoryTotal} document(s)</p>
                          </div>
                        </div>
                        <span className={`px-4 py-2 rounded-full text-lg font-bold ${
                          isCategorySelected ? 'bg-indigo-600 text-white' : 'bg-gray-200 text-gray-700'
                        }`}>
                          {categoryTotal}
                        </span>
                      </div>

                      {/* Document Types (Level 3) */}
                      {isCategorySelected && (
                        <div className="p-4 bg-white space-y-2">
                          {Object.entries(typeCounts).map(([type, count]) => {
                            const isTypeSelected = selectedPath.type === type;
                            
                            return (
                              <div
                                key={type}
                                onClick={() => setSelectedPath({
                                  year: yearData.year,
                                  category: category,
                                  type: isTypeSelected ? null : type
                                })}
                                className={`flex items-center justify-between p-3 rounded-lg cursor-pointer transition-all ${
                                  isTypeSelected 
                                    ? 'bg-blue-100 border-2 border-blue-500' 
                                    : 'bg-gray-50 hover:bg-gray-100 border-2 border-transparent'
                                }`}
                              >
                                <div className="flex items-center gap-2">
                                  <FileText className="w-5 h-5 text-blue-600" />
                                  <span className="font-medium text-gray-900">{documentTypeLabels[type] || type}</span>
                                </div>
                                <span className={`px-3 py-1 rounded-full text-sm font-semibold ${
                                  isTypeSelected ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700'
                                }`}>
                                  {count}
                                </span>
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Selected documents display */}
      {selectedPath.year && selectedPath.category && selectedPath.type && (
        <div className="bg-white rounded-xl shadow-lg border-2 border-blue-300 p-6">
          {/* Folder header */}
          <div className="flex items-center justify-between mb-4 pb-4 border-b border-gray-200">
            <div className="flex items-center gap-3">
              <FolderOpen className="w-6 h-6 text-blue-600" />
              <div>
                <h3 className="text-xl font-bold text-gray-900">
                  {selectedPath.year} / {selectedPath.category} / {documentTypeLabels[selectedPath.type]}
                </h3>
                <p className="text-sm text-gray-500">
                  {filteredDocuments.length} document(s) {searchQuery || Object.values(filters).some(v => v) ? 'trouvé(s)' : ''}
                </p>
              </div>
            </div>

            {/* Bulk actions */}
            {selectedDocuments.size > 0 && (
              <div className="flex items-center gap-2">
                <span className="text-sm text-gray-600">{selectedDocuments.size} sélectionné(s)</span>
                <button
                  onClick={handleBulkDownload}
                  className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm inline-flex items-center gap-2 transition-colors"
                >
                  <Download className="w-4 h-4" />
                  Télécharger tout
                </button>
                <button
                  onClick={deselectAll}
                  className="px-3 py-1.5 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-lg text-sm transition-colors"
                >
                  Désélectionner
                </button>
              </div>
            )}
          </div>

          {isLoadingDocuments || (searchMode === "global" && isSearching) ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
            </div>
          ) : filteredDocuments.length > 0 ? (
            <>
              {/* Select all button */}
              {filteredDocuments.length > 1 && (
                <div className="mb-3">
                  <button
                    onClick={selectAllVisibleDocuments}
                    className="text-sm text-blue-600 hover:text-blue-800 font-medium"
                  >
                    Tout sélectionner ({filteredDocuments.length})
                  </button>
                </div>
              )}

              {/* Documents as cards */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {filteredDocuments.map((doc) => {
                  const isSelected = selectedDocuments.has(doc.id);
                  const isUncategorized = doc.category === null || doc.category === 'General';
                  const isEditing = editingDocumentId === doc.id;
                  
                  return (
                    <div
                      key={doc.id}
                      className={`relative bg-white border-2 rounded-lg p-4 hover:shadow-md transition-all ${
                        isSelected ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-blue-300'
                      }`}
                    >
                      {/* Selection checkbox */}
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={() => toggleDocumentSelection(doc.id)}
                        className="absolute top-3 right-3 w-5 h-5 rounded cursor-pointer"
                        onClick={(e) => e.stopPropagation()}
                      />

                      {/* Document icon and type */}
                      <div className="flex items-start gap-3 mb-3">
                        <div className="p-2 bg-blue-100 rounded-lg">
                          <FileText className="w-6 h-6 text-blue-600" />
                        </div>
                        <div className="flex-1 min-w-0 pr-8">
                          <h4 className="font-semibold text-gray-900 text-sm truncate" title={doc.display_name || doc.original_filename}>
                            {doc.display_name || doc.original_filename}
                          </h4>
                          {doc.document_date && (
                            <p className="text-xs text-gray-500 mt-1">
                              {formatDate(doc.document_date)}
                            </p>
                          )}
                        </div>
                      </div>

                      {/* Category edit for uncategorized */}
                      {isUncategorized && (
                        <div className="mb-3 p-2 bg-yellow-50 border border-yellow-200 rounded">
                          {isEditing ? (
                            <div className="space-y-2">
                              <select
                                value={newCategory}
                                onChange={(e) => setNewCategory(e.target.value)}
                                className="w-full px-2 py-1 border border-gray-300 rounded text-sm"
                              >
                                <option value="">-- Choisir une catégorie --</option>
                                {allCategories?.map(cat => (
                                  <option key={cat} value={cat}>{cat}</option>
                                ))}
                              </select>
                              <div className="flex gap-2">
                                <button
                                  onClick={() => {
                                    if (newCategory) {
                                      handleCategoryChange(doc.id, newCategory);
                                    }
                                  }}
                                  disabled={!newCategory || updateCategoryMutation.isPending}
                                  className="flex-1 px-2 py-1 bg-green-600 hover:bg-green-700 text-white rounded text-xs inline-flex items-center justify-center gap-1 disabled:opacity-50"
                                >
                                  <Check className="w-3 h-3" />
                                  Valider
                                </button>
                                <button
                                  onClick={() => {
                                    setEditingDocumentId(null);
                                    setNewCategory("");
                                  }}
                                  className="px-2 py-1 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded text-xs inline-flex items-center gap-1"
                                >
                                  <X className="w-3 h-3" />
                                  Annuler
                                </button>
                              </div>
                            </div>
                          ) : (
                            <button
                              onClick={() => setEditingDocumentId(doc.id)}
                              className="w-full px-2 py-1 bg-yellow-100 hover:bg-yellow-200 text-yellow-800 rounded text-xs inline-flex items-center justify-center gap-1 transition-colors"
                            >
                              <Edit2 className="w-3 h-3" />
                              Choisir une catégorie
                            </button>
                          )}
                        </div>
                      )}

                      {/* Document metadata */}
                      <div className="space-y-2 mb-3">
                        {doc.extracted_amount && (
                          <div className="flex items-center justify-between text-sm">
                            <span className="text-gray-600">Montant:</span>
                            <span className="font-semibold text-gray-900">
                              {doc.extracted_amount.toFixed(2)} {doc.currency || 'CHF'}
                            </span>
                          </div>
                        )}
                        {doc.importance_score !== undefined && doc.importance_score !== null && (
                          <div className="flex items-center justify-between text-sm">
                            <span className="text-gray-600">Importance:</span>
                            <span className={`px-2 py-0.5 rounded text-xs font-semibold ${
                              doc.importance_score >= 80 ? 'bg-red-100 text-red-800' :
                              doc.importance_score >= 60 ? 'bg-yellow-100 text-yellow-800' :
                              'bg-green-100 text-green-800'
                            }`}>
                              {doc.importance_score.toFixed(0)}
                            </span>
                          </div>
                        )}
                      </div>

                      {/* Actions */}
                      <div className="flex gap-2">
                        <button
                          onClick={() => setViewerDocumentId(doc.id)}
                          className="flex-1 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded text-xs inline-flex items-center justify-center gap-1 transition-colors"
                        >
                          <Eye className="w-3 h-3" />
                          Voir
                        </button>
                        {doc.ocr_pdf_path && (
                          <button
                            onClick={() => window.open(getPreviewUrl(doc.id), '_blank')}
                            className="px-3 py-1.5 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded text-xs inline-flex items-center gap-1 transition-colors"
                            title="Imprimer"
                          >
                            <Printer className="w-3 h-3" />
                          </button>
                        )}
                        <button
                          onClick={() => {
                            const url = doc.ocr_pdf_path ? downloadOcrPdf(doc.id) : downloadOriginalDocument(doc.id);
                            window.open(url, '_blank');
                          }}
                          className="px-3 py-1.5 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded text-xs inline-flex items-center gap-1 transition-colors"
                          title="Télécharger"
                        >
                          <Download className="w-3 h-3" />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </>
          ) : (
            <div className="text-center py-12 text-gray-500">
              {searchQuery || Object.values(filters).some(v => v) ? 'Aucun document ne correspond à vos critères' : 'Aucun document dans ce dossier'}
            </div>
          )}
        </div>
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
