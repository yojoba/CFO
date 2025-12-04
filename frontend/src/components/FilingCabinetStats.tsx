"use client";

import { FilingCabinetStats } from "@/types";
import { BarChart3, TrendingUp, FileText, Folder } from "lucide-react";

const documentTypeLabels: Record<string, string> = {
  invoice: "Factures",
  receipt: "Reçus",
  contract: "Contrats",
  letter: "Courrier",
  tax_document: "Fiscaux",
  insurance: "Assurance",
  other: "Autre",
};

const documentTypeColors: Record<string, string> = {
  invoice: "bg-blue-500",
  receipt: "bg-green-500",
  contract: "bg-purple-500",
  letter: "bg-yellow-500",
  tax_document: "bg-red-500",
  insurance: "bg-indigo-500",
  other: "bg-gray-500",
};

interface FilingCabinetStatsProps {
  overview: FilingCabinetStats;
}

export function FilingCabinetStatsComponent({ overview }: FilingCabinetStatsProps) {
  // Calculate statistics
  const allTypes = new Set<string>();
  overview.years.forEach(year => {
    Object.keys(year.document_counts).forEach(type => allTypes.add(type));
  });

  // Documents by type (all years combined)
  const documentsByType = {} as Record<string, number>;
  overview.years.forEach(year => {
    Object.entries(year.document_counts).forEach(([type, count]) => {
      documentsByType[type] = (documentsByType[type] || 0) + count;
    });
  });

  const maxCount = Math.max(...Object.values(documentsByType), 1);

  return (
    <div className="space-y-6">
      {/* Overall stats cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-6 text-white shadow-lg">
          <div className="flex items-center justify-between mb-2">
            <FileText className="w-8 h-8 opacity-80" />
            <TrendingUp className="w-5 h-5 opacity-60" />
          </div>
          <div className="text-4xl font-bold mb-1">{overview.total_documents}</div>
          <div className="text-blue-100 text-sm">Total Documents</div>
        </div>

        <div className="bg-gradient-to-br from-indigo-500 to-indigo-600 rounded-xl p-6 text-white shadow-lg">
          <div className="flex items-center justify-between mb-2">
            <Folder className="w-8 h-8 opacity-80" />
            <BarChart3 className="w-5 h-5 opacity-60" />
          </div>
          <div className="text-4xl font-bold mb-1">{overview.total_years}</div>
          <div className="text-indigo-100 text-sm">Années Archivées</div>
        </div>

        <div className="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-6 text-white shadow-lg">
          <div className="flex items-center justify-between mb-2">
            <FileText className="w-8 h-8 opacity-80" />
          </div>
          <div className="text-4xl font-bold mb-1">{allTypes.size}</div>
          <div className="text-purple-100 text-sm">Types de Documents</div>
        </div>

        <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white shadow-lg">
          <div className="flex items-center justify-between mb-2">
            <TrendingUp className="w-8 h-8 opacity-80" />
          </div>
          <div className="text-4xl font-bold mb-1">
            {overview.years.length > 0 ? Math.round(overview.total_documents / overview.years.length) : 0}
          </div>
          <div className="text-green-100 text-sm">Docs/Année (moy.)</div>
        </div>
      </div>

      {/* Bar chart - Documents by type */}
      <div className="bg-white rounded-xl shadow-lg p-6 border border-gray-200">
        <div className="flex items-center gap-2 mb-4">
          <BarChart3 className="w-5 h-5 text-blue-600" />
          <h3 className="text-lg font-semibold text-gray-900">Documents par Type</h3>
        </div>
        <div className="space-y-3">
          {Object.entries(documentsByType)
            .sort(([, a], [, b]) => b - a)
            .map(([type, count]) => {
              const percentage = (count / maxCount) * 100;
              return (
                <div key={type} className="space-y-1">
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-medium text-gray-700">
                      {documentTypeLabels[type] || type}
                    </span>
                    <span className="text-gray-900 font-semibold">{count}</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
                    <div
                      className={`h-full ${documentTypeColors[type] || 'bg-gray-500'} transition-all duration-500`}
                      style={{ width: `${percentage}%` }}
                    />
                  </div>
                </div>
              );
            })}
        </div>
      </div>

      {/* Timeline - Documents by year */}
      <div className="bg-white rounded-xl shadow-lg p-6 border border-gray-200">
        <div className="flex items-center gap-2 mb-4">
          <TrendingUp className="w-5 h-5 text-indigo-600" />
          <h3 className="text-lg font-semibold text-gray-900">Évolution Temporelle</h3>
        </div>
        <div className="space-y-4">
          {overview.years.map((yearData) => (
            <div key={yearData.year} className="flex items-center gap-4">
              <div className="w-16 text-right">
                <span className="font-bold text-gray-900">{yearData.year}</span>
              </div>
              <div className="flex-1">
                <div className="flex items-center gap-1">
                  {Object.entries(yearData.document_counts).map(([type, count]) => {
                    const width = (count / overview.total_documents) * 100;
                    return (
                      <div
                        key={type}
                        className={`h-8 ${documentTypeColors[type] || 'bg-gray-500'} rounded transition-all hover:opacity-80 cursor-pointer`}
                        style={{ width: `${width}%`, minWidth: count > 0 ? '30px' : '0' }}
                        title={`${documentTypeLabels[type]}: ${count}`}
                      >
                        <span className="text-white text-xs font-semibold px-2 leading-8">
                          {count}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
              <div className="w-20 text-right">
                <span className="text-sm font-medium text-gray-600">{yearData.total} docs</span>
              </div>
            </div>
          ))}
        </div>

        {/* Legend */}
        <div className="mt-6 pt-4 border-t border-gray-200">
          <div className="flex flex-wrap gap-3">
            {Array.from(allTypes).map(type => (
              <div key={type} className="flex items-center gap-2">
                <div className={`w-4 h-4 rounded ${documentTypeColors[type] || 'bg-gray-500'}`} />
                <span className="text-xs text-gray-600">{documentTypeLabels[type] || type}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

