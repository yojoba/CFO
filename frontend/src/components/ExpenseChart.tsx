"use client";

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts";

const categoryData = [
  { name: "Logement", value: 1500, color: "#3b82f6" },
  { name: "Alimentation", value: 800, color: "#10b981" },
  { name: "Transport", value: 400, color: "#f59e0b" },
  { name: "Assurances", value: 600, color: "#8b5cf6" },
  { name: "Loisirs", value: 300, color: "#ec4899" },
  { name: "Autres", value: 400, color: "#6b7280" },
];

const monthlyData = [
  { month: "Jan", dépenses: 3200 },
  { month: "Fév", dépenses: 3800 },
  { month: "Mar", dépenses: 3500 },
  { month: "Avr", dépenses: 4000 },
  { month: "Mai", dépenses: 3600 },
  { month: "Juin", dépenses: 4200 },
];

export function ExpenseChart() {
  return (
    <div className="grid md:grid-cols-2 gap-6">
      <div className="bg-white p-6 rounded-lg shadow">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Dépenses par Catégorie
        </h3>
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={categoryData}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={({ name, percent }) =>
                `${name}: ${(percent * 100).toFixed(0)}%`
              }
              outerRadius={80}
              fill="#8884d8"
              dataKey="value"
            >
              {categoryData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip formatter={(value) => `${value} CHF`} />
          </PieChart>
        </ResponsiveContainer>
        <div className="mt-4 grid grid-cols-2 gap-2">
          {categoryData.map((cat) => (
            <div key={cat.name} className="flex items-center gap-2">
              <div
                className="w-3 h-3 rounded-full"
                style={{ backgroundColor: cat.color }}
              />
              <span className="text-sm text-gray-600">
                {cat.name}: {cat.value} CHF
              </span>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-white p-6 rounded-lg shadow">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Évolution Mensuelle
        </h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={monthlyData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="month" />
            <YAxis />
            <Tooltip formatter={(value) => `${value} CHF`} />
            <Legend />
            <Bar dataKey="dépenses" fill="#3b82f6" />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

