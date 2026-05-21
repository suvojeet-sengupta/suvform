"use client";

export const runtime = "edge";

import { useAuth } from "@/context/AuthContext";
import { useApi } from "@/lib/api";
import { useRouter, useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { 
  ArrowLeft, 
  BarChart2, 
  Download, 
  MessageSquare, 
  Loader2,
  Calendar,
  Table as TableIcon,
  Sparkles,
  FileText,
  ChevronDown
} from "lucide-react";
import { Parser } from 'json2csv';
import jsPDF from 'jspdf';
import 'jspdf-autotable';

interface ResponseItem {
  id: string;
  submitted_at: number;
  answers: Record<string, any>;
  calculated: Record<string, number>;
}

interface Field {
  id: string;
  label: string;
}

interface Calculation {
  id: string;
  label: string;
}

interface FormDetail {
  id: string;
  title: string;
  fields: Field[];
  calculations: Calculation[];
}

export default function ResponsesPage() {
  const { user, loading: authLoading } = useAuth();
  const api = useApi();
  const router = useRouter();
  const { id } = useParams();

  const [form, setForm] = useState<FormDetail | null>(null);
  const [responses, setResponses] = useState<ResponseItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [isGeneratingInsights, setIsGeneratingInsights] = useState(false);
  const [insights, setInsights] = useState<{ summary: string; response_count: number } | null>(null);
  const [isExporting, setIsExporting] = useState(false);

  useEffect(() => {
    if (!authLoading && !user) {
      router.push("/login");
    }
  }, [user, authLoading, router]);

  useEffect(() => {
    if (user && id) {
      fetchData();
    }
  }, [user, id]);

  const fetchData = async () => {
    try {
      const [formData, responsesData] = await Promise.all([
        api.get(`/v1/forms/${id}`),
        api.get(`/v1/forms/${id}/responses`)
      ]);
      setForm(formData);
      setResponses(responsesData.responses);
    } catch (error) {
      console.error("Failed to fetch data", error);
    } finally {
      setLoading(false);
    }
  };

  const exportCSV = () => {
    if (!form || responses.length === 0) return;
    setIsExporting(true);
    try {
      const fields = [
        { label: 'Submission Date', value: 'date' },
        ...form.fields.map(f => ({ label: f.label, value: `answers.${f.id}` })),
        ...form.calculations.map(c => ({ label: c.label, value: `calculated.${c.id}` }))
      ];

      const data = responses.map(r => ({
        date: new Date(r.submitted_at).toLocaleString(),
        answers: r.answers,
        calculated: r.calculated
      }));

      const parser = new Parser({ fields });
      const csv = parser.parse(data);
      
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.setAttribute('download', `${form.title.replace(/\s+/g, '_')}_responses.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (err) {
      console.error(err);
    } finally {
      setIsExporting(false);
    }
  };

  const exportPDF = () => {
    if (!form || responses.length === 0) return;
    setIsExporting(true);
    try {
      const doc = new jsPDF('landscape');
      doc.text(`${form.title} - Responses`, 14, 15);
      doc.setFontSize(10);
      doc.text(`Total Submissions: ${responses.length}`, 14, 22);

      const tableColumn = ["Date", ...form.fields.map(f => f.label), ...form.calculations.map(c => c.label)];
      const tableRows = responses.map(r => [
        new Date(r.submitted_at).toLocaleString(),
        ...form.fields.map(f => r.answers[f.id] || "-"),
        ...form.calculations.map(c => r.calculated[c.id]?.toFixed(2) || "-")
      ]);

      (doc as any).autoTable({
        head: [tableColumn],
        body: tableRows,
        startY: 30,
        theme: 'grid',
        headStyles: { fillColor: [37, 99, 235] },
        styles: { fontSize: 8 }
      });

      doc.save(`${form.title.replace(/\s+/g, '_')}_responses.pdf`);
    } catch (err) {
      console.error(err);
    } finally {
      setIsExporting(false);
    }
  };

  const handleGenerateInsights = async () => {
    if (isGeneratingInsights) return;
    setIsGeneratingInsights(true);
    try {
      const data = await api.post(`/v1/forms/${id}/insights`);
      setInsights(data);
    } catch (error) {
      console.error("Failed to generate insights", error);
      alert("Failed to generate insights. Ensure you have enough responses.");
    } finally {
      setIsGeneratingInsights(false);
    }
  };

  if (loading || !form) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-8 shrink-0">
        <div className="flex items-center gap-4">
          <button onClick={() => router.back()} className="p-2 hover:bg-gray-100 rounded-full transition-colors">
            <ArrowLeft className="h-5 w-5 text-gray-600" />
          </button>
          <div>
            <h1 className="text-xl font-bold text-gray-900">{form.title}</h1>
            <p className="text-xs text-gray-500">Responses Dashboard</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="relative group">
            <button 
              disabled={isExporting || responses.length === 0}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 text-gray-700 text-sm font-semibold rounded-lg hover:bg-gray-50 transition-all disabled:opacity-50"
            >
              <Download className="h-4 w-4" />
              Export
              <ChevronDown className="h-4 w-4" />
            </button>
            <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-xl shadow-xl z-20 overflow-hidden opacity-0 group-hover:opacity-100 pointer-events-none group-hover:pointer-events-auto transition-all">
              <button 
                onClick={exportCSV}
                className="w-full flex items-center gap-2 px-4 py-3 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              >
                <TableIcon className="h-4 w-4 text-green-600" />
                Export as CSV (Sheets)
              </button>
              <button 
                onClick={exportPDF}
                className="w-full flex items-center gap-2 px-4 py-3 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              >
                <FileText className="h-4 w-4 text-red-600" />
                Export as PDF
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="flex-1 p-8 overflow-y-auto">
        <div className="max-w-7xl mx-auto space-y-8">
          {/* Stats Summary */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div className="p-2 bg-blue-50 rounded-lg">
                  <MessageSquare className="h-5 w-5 text-blue-600" />
                </div>
                <span className="text-xs font-bold text-green-600 bg-green-50 px-2 py-0.5 rounded">+12%</span>
              </div>
              <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">Total Responses</p>
              <h3 className="text-3xl font-bold text-gray-900 mt-1">{responses.length}</h3>
            </div>
            
            <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div className="p-2 bg-purple-50 rounded-lg">
                  <Sparkles className="h-5 w-5 text-purple-600" />
                </div>
              </div>
              <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">Completion Rate</p>
              <h3 className="text-3xl font-bold text-gray-900 mt-1">94.2%</h3>
            </div>

            <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div className="p-2 bg-orange-50 rounded-lg">
                  <Calendar className="h-5 w-5 text-orange-600" />
                </div>
              </div>
              <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">Avg. Time to Fill</p>
              <h3 className="text-3xl font-bold text-gray-900 mt-1">2m 45s</h3>
            </div>
          </div>

          {/* AI Insights Section */}
          <div className="bg-gradient-to-br from-indigo-600 to-blue-700 rounded-3xl p-8 text-white shadow-xl shadow-blue-200">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <Sparkles className="h-6 w-6 text-yellow-300" />
                  <h2 className="text-2xl font-bold">Gemini AI Insights</h2>
                </div>
                <p className="text-blue-100 max-w-xl">
                  Automatically analyze trends, patterns, and sentiment across all your form submissions using advanced AI.
                </p>
              </div>
              <button 
                onClick={handleGenerateInsights}
                disabled={isGeneratingInsights || responses.length === 0}
                className="bg-white text-blue-600 px-8 py-4 rounded-2xl font-bold hover:bg-blue-50 transition-all flex items-center justify-center gap-3 disabled:opacity-50 disabled:cursor-not-allowed shrink-0"
              >
                {isGeneratingInsights ? (
                  <>
                    <Loader2 className="h-5 w-5 animate-spin" />
                    Analyzing Data...
                  </>
                ) : (
                  <>
                    <BarChart2 className="h-5 w-5" />
                    Generate Insights
                  </>
                )}
              </button>
            </div>

            {insights && (
              <div className="mt-8 p-6 bg-white/10 backdrop-blur-md rounded-2xl border border-white/20">
                <div className="prose prose-invert max-w-none">
                  <p className="whitespace-pre-wrap leading-relaxed text-blue-50">
                    {insights.summary}
                  </p>
                </div>
                <div className="mt-4 pt-4 border-t border-white/10 text-xs text-blue-200">
                  Analysis based on {insights.response_count} responses.
                </div>
              </div>
            )}
          </div>

          {/* Table Section */}
          <div className="bg-white border border-gray-200 rounded-3xl overflow-hidden shadow-sm">
            <div className="p-6 border-b border-gray-100 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-gray-50 rounded-lg">
                  <TableIcon className="h-5 w-5 text-gray-500" />
                </div>
                <h3 className="font-bold text-gray-900 text-lg">Submissions</h3>
              </div>
              <div className="text-sm text-gray-500">
                Showing {responses.length} responses
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-gray-50/50">
                    <th className="px-6 py-4 text-xs font-bold text-gray-400 uppercase tracking-wider">Date</th>
                    {form.fields.map(f => (
                      <th key={f.id} className="px-6 py-4 text-xs font-bold text-gray-400 uppercase tracking-wider min-w-[150px]">{f.label}</th>
                    ))}
                    {form.calculations.map(c => (
                      <th key={c.id} className="px-6 py-4 text-xs font-bold text-purple-400 uppercase tracking-wider min-w-[150px] bg-purple-50/30">{c.label}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {responses.length === 0 ? (
                    <tr>
                      <td colSpan={1 + form.fields.length + form.calculations.length} className="px-6 py-20 text-center text-gray-400">
                        No responses submitted yet.
                      </td>
                    </tr>
                  ) : (
                    responses.map((resp) => (
                      <tr key={resp.id} className="hover:bg-gray-50 transition-colors group">
                        <td className="px-6 py-4 text-sm text-gray-500 whitespace-nowrap">
                          {new Date(resp.submitted_at).toLocaleString()}
                        </td>
                        {form.fields.map(f => (
                          <td key={f.id} className="px-6 py-4 text-sm text-gray-900 font-medium">
                            {resp.answers[f.id]?.toString() || "-"}
                          </td>
                        ))}
                        {form.calculations.map(c => (
                          <td key={c.id} className="px-6 py-4 text-sm text-purple-700 font-bold bg-purple-50/20">
                            {resp.calculated[c.id]?.toFixed(2) || "-"}
                          </td>
                        ))}
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
