"use client";

import { TrendingUp, TrendingDown, FileText, Wallet } from "lucide-react";
import { formatCurrency } from "@/lib/utils";

interface SummaryCardProps {
  title: string;
  value: string;
  change?: number;
  icon: React.ReactNode;
  color: string;
}

function SummaryCard({ title, value, change, icon, color }: SummaryCardProps) {
  return (
    <div className="bg-white p-6 rounded-lg shadow">
      <div className="flex items-center justify-between mb-4">
        <div className={`w-12 h-12 ${color} rounded-lg flex items-center justify-center`}>
          {icon}
        </div>
        {change !== undefined && (
          <div className={`flex items-center gap-1 text-sm ${
            change >= 0 ? "text-green-600" : "text-red-600"
          }`}>
            {change >= 0 ? (
              <TrendingUp className="w-4 h-4" />
            ) : (
              <TrendingDown className="w-4 h-4" />
            )}
            <span>{Math.abs(change)}%</span>
          </div>
        )}
      </div>
      <h3 className="text-gray-600 text-sm font-medium mb-1">{title}</h3>
      <p className="text-2xl font-bold text-gray-900">{value}</p>
    </div>
  );
}

interface FinancialSummaryProps {
  documentCount: number;
  totalExpenses: number;
  monthlyAverage: number;
}

export function FinancialSummary({
  documentCount,
  totalExpenses,
  monthlyAverage,
}: FinancialSummaryProps) {
  return (
    <div className="grid md:grid-cols-3 gap-6">
      <SummaryCard
        title="Documents Importés"
        value={documentCount.toString()}
        icon={<FileText className="w-6 h-6 text-blue-600" />}
        color="bg-blue-100"
      />
      
      <SummaryCard
        title="Dépenses Totales"
        value={formatCurrency(totalExpenses)}
        change={-5.2}
        icon={<Wallet className="w-6 h-6 text-purple-600" />}
        color="bg-purple-100"
      />
      
      <SummaryCard
        title="Moyenne Mensuelle"
        value={formatCurrency(monthlyAverage)}
        icon={<TrendingUp className="w-6 h-6 text-green-600" />}
        color="bg-green-100"
      />
    </div>
  );
}

