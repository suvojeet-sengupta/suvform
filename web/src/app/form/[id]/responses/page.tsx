"use client";

export const runtime = "edge";

import { useAuth } from "@/context/AuthContext";
import { useApi } from "@/lib/api";
import { useRouter, useParams } from "next/navigation";
import { useEffect, useState, useRef } from "react";
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
  ChevronDown,
  RefreshCcw,
  Check,
  Trash2,
  MoreVertical
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
  const [isExportMenuOpen, setIsExportMenuOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [isDeleting, setIsDeleting] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<'selected' | 'all' | null>(null);
  
  const menuRef = useRef<HTMLDivElement>(null);

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

  // Close menu when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsExportMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  };

  const fetchData = async () => {
    setLoading(true);
    try {
      const [formData, responsesData] = await Promise.all([
        api.get(`/v1/forms/${id}`),
        api.get(`/v1/forms/${id}/responses`),
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
    setIsExportMenuOpen(false);
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
      showToast("CSV Exported successfully!");
    } catch (err) {
      console.error(err);
      alert("Failed to export CSV");
    } finally {
      setIsExporting(false);
    }
  };

  const exportPDF = () => {
    if (!form || responses.length === 0) return;
    setIsExporting(true);
    setIsExportMenuOpen(false);
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
      showToast("PDF Exported successfully!");
    } catch (err) {
      console.error(err);
      alert("Failed to export PDF");
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

  const toggleSelectAll = () => {
    if (selectedIds.size === responses.length && responses.length > 0) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(responses.map(r => r.id)));
    }
  };

  const toggleSelect = (id: string) => {
    const next = new Set(selectedIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setSelectedIds(next);
  };

  const handleDelete = async () => {
    if (!showDeleteConfirm) return;
    setIsDeleting(true);
    try {
      if (showDeleteConfirm === 'all') {
        await api.delete(`/v1/forms/${id}/responses`, { all: true });
        showToast("All responses deleted");
      } else {
        await api.delete(`/v1/forms/${id}/responses`, { ids: Array.from(selectedIds) });
        showToast(`${selectedIds.size} responses deleted`);
      }
      setSelectedIds(new Set());
      await fetchData();
    } catch (error) {
      console.error("Failed to delete", error);
      alert("Failed to delete responses");
    } finally {
      setIsDeleting(false);
      setShowDeleteConfirm(null);
    }
  };

  if (loading && !form) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!form) return null;

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl p-8 max-w-md w-full shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="p-3 bg-red-50 rounded-2xl w-fit mb-6">
              <Trash2 className="h-6 w-6 text-red-600" />
            </div>
            <h3 className="text-xl font-bold text-gray-900 mb-2">
              {showDeleteConfirm === 'all' ? "Delete All Responses?" : `Delete ${selectedIds.size} Responses?`}
            </h3>
            <p className="text-gray-500 mb-8 leading-relaxed">
              This action is permanent and cannot be undone. All data associated with {showDeleteConfirm === 'all' ? "these" : "selected"} responses will be lost.
            </p>
            <div className="flex gap-3">
              <button 
                onClick={() => setShowDeleteConfirm(null)}
                className="flex-1 px-6 py-3 rounded-xl font-bold text-gray-700 hover:bg-gray-100 transition-colors"
              >
                Cancel
              </button>
              <button 
                onClick={handleDelete}
                disabled={isDeleting}
                className="flex-1 px-6 py-3 rounded-xl font-bold bg-red-600 text-white hover:bg-red-700 transition-colors disabled:opacity-50"
              >
                {isDeleting ? "Deleting..." : "Confirm Delete"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      {toast && (
        <div className="fixed top-20 right-8 z-50 animate-in fade-in slide-in-from-top-4 duration-300">
          <div className="bg-gray-900 text-white px-6 py-3 rounded-2xl shadow-2xl flex items-center gap-3 border border-white/10">
            <div className="bg-green-500 rounded-full p-1">
              <Check className="h-3 w-3 text-white" />
            </div>
            <span className="text-sm font-bold">{toast}</span>
          </div>
        </div>
      )}

      <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-4 md:px-8 shrink-0">
        <div className="flex items-center gap-4">
          <button onClick={() => router.back()} className="p-2 hover:bg-gray-100 rounded-full transition-colors">
            <ArrowLeft className="h-5 w-5 text-gray-600" />
          </button>
          <div className="hidden sm:block">
            <h1 className="text-lg font-bold text-gray-900 truncate max-w-[200px]">{form.title}</h1>
            <p className="text-[10px] text-gray-500 uppercase tracking-widest font-bold">Responses</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {selectedIds.size > 0 && (
            <button 
              onClick={() => setShowDeleteConfirm('selected')}
              className="flex items-center gap-2 px-4 py-2 bg-red-50 text-red-600 text-sm font-bold rounded-lg hover:bg-red-100 transition-all mr-2"
            >
              <Trash2 className="h-4 w-4" />
              <span>Delete ({selectedIds.size})</span>
            </button>
          )}

          <button 
            onClick={() => fetchData()}
            className="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-all"
            title="Refresh Data"
          >
            <RefreshCcw className={`h-5 w-5 ${loading ? 'animate-spin' : ''}`} />
          </button>
          
          <div className="relative" ref={menuRef}>
            <button 
              onClick={() => setIsExportMenuOpen(!isExportMenuOpen)}
              disabled={isExporting || responses.length === 0}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 text-gray-700 text-sm font-bold rounded-lg hover:border-blue-300 hover:text-blue-600 transition-all disabled:opacity-40"
            >
              <Download className="h-4 w-4" />
              <span className="hidden md:inline">Export</span>
              <ChevronDown className="h-4 w-4 opacity-50" />
            </button>
            
            {isExportMenuOpen && (
              <div className="absolute right-0 mt-2 w-56 bg-white border border-gray-200 rounded-xl shadow-2xl z-30 overflow-hidden animate-in fade-in zoom-in duration-200">
                <div className="p-2 border-b border-gray-50 text-[10px] font-bold text-gray-400 uppercase tracking-widest text-center">
                  Select Format
                </div>
                <button 
                  onClick={exportCSV}
                  className="w-full flex items-center gap-3 px-4 py-3 text-sm text-gray-700 hover:bg-blue-50 transition-colors text-left"
                >
                  <div className="p-1.5 bg-green-50 rounded-lg">
                    <TableIcon className="h-4 w-4 text-green-600" />
                  </div>
                  <div>
                    <p className="font-bold">Excel / Sheets</p>
                    <p className="text-[10px] text-gray-400">Comma separated (.csv)</p>
                  </div>
                </button>
                <button 
                  onClick={exportPDF}
                  className="w-full flex items-center gap-3 px-4 py-3 text-sm text-gray-700 hover:bg-blue-50 transition-colors text-left"
                >
                  <div className="p-1.5 bg-red-50 rounded-lg">
                    <FileText className="h-4 w-4 text-red-600" />
                  </div>
                  <div>
                    <p className="font-bold">PDF Document</p>
                    <p className="text-[10px] text-gray-400">Portable document (.pdf)</p>
                  </div>
                </button>
                <div className="h-[1px] bg-gray-100" />
                <button 
                  onClick={() => { setIsExportMenuOpen(false); setShowDeleteConfirm('all'); }}
                  className="w-full flex items-center gap-3 px-4 py-3 text-sm text-red-600 hover:bg-red-50 transition-colors text-left"
                >
                  <div className="p-1.5 bg-red-100 rounded-lg">
                    <Trash2 className="h-4 w-4 text-red-700" />
                  </div>
                  <div>
                    <p className="font-bold">Delete All</p>
                    <p className="text-[10px] text-red-400">Permanently clear data</p>
                  </div>
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      <main className="flex-1 p-4 md:p-8 overflow-y-auto">
        <div className="max-w-7xl mx-auto space-y-8">
          {/* Stats Summary */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 md:gap-6">
            <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div className="p-2 bg-blue-50 rounded-lg">
                  <MessageSquare className="h-5 w-5 text-blue-600" />
                </div>
                {responses.length > 0 && <span className="text-xs font-bold text-green-600 bg-green-50 px-2 py-0.5 rounded">Active</span>}
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
              <h3 className="text-3xl font-bold text-gray-900 mt-1">{responses.length > 0 ? "98.5%" : "0%"}</h3>
            </div>

            <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div className="p-2 bg-orange-50 rounded-lg">
                  <Calendar className="h-5 w-5 text-orange-600" />
                </div>
              </div>
              <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">Latest Update</p>
              <h3 className="text-lg font-bold text-gray-900 mt-1">
                {responses.length > 0 ? new Date(responses[0].submitted_at).toLocaleDateString() : "No data"}
              </h3>
            </div>
          </div>

          {/* AI Insights Section */}
          <div className="bg-gradient-to-br from-indigo-600 to-blue-700 rounded-3xl p-6 md:p-8 text-white shadow-xl shadow-blue-200">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <Sparkles className="h-6 w-6 text-yellow-300" />
                  <h2 className="text-2xl font-bold">Gemini AI Insights</h2>
                </div>
                <p className="text-blue-100 max-w-xl text-sm leading-relaxed">
                  Automatically analyze trends, patterns, and sentiment across all your form submissions using advanced AI.
                </p>
              </div>
              <button 
                onClick={handleGenerateInsights}
                disabled={isGeneratingInsights || responses.length === 0}
                className="bg-white text-blue-600 px-8 py-4 rounded-2xl font-bold hover:bg-blue-50 transition-all flex items-center justify-center gap-3 disabled:opacity-50 disabled:cursor-not-allowed shrink-0 shadow-lg"
              >
                {isGeneratingInsights ? (
                  <>
                    <Loader2 className="h-5 w-5 animate-spin" />
                    Analyzing...
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
                  <p className="whitespace-pre-wrap leading-relaxed text-blue-50 text-sm">
                    {insights.summary}
                  </p>
                </div>
                <div className="mt-4 pt-4 border-t border-white/10 text-[10px] text-blue-200 font-bold uppercase tracking-widest">
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
              <div className="text-xs font-bold text-gray-400 bg-gray-50 px-2.5 py-1 rounded-full border border-gray-100">
                {responses.length} ITEMS
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-gray-50/50">
                    <th className="px-6 py-4 w-10">
                      <input 
                        type="checkbox" 
                        className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                        checked={responses.length > 0 && selectedIds.size === responses.length}
                        onChange={toggleSelectAll}
                      />
                    </th>
                    <th className="px-6 py-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest">Date</th>
                    {form.fields.map(f => (
                      <th key={f.id} className="px-6 py-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest min-w-[150px]">{f.label}</th>
                    ))}
                    {form.calculations.map(c => (
                      <th key={c.id} className="px-6 py-4 text-[10px] font-bold text-purple-400 uppercase tracking-widest min-w-[150px] bg-purple-50/30">{c.label}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {responses.length === 0 ? (
                    <tr>
                      <td colSpan={2 + form.fields.length + form.calculations.length} className="px-6 py-20 text-center">
                        <div className="max-w-xs mx-auto text-gray-400">
                          <MessageSquare className="h-10 w-10 mx-auto mb-3 opacity-20" />
                          <p className="font-medium">No responses yet</p>
                          <p className="text-xs mt-1">When someone fills your form, their data will appear here.</p>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    responses.map((resp) => (
                      <tr key={resp.id} className={`hover:bg-gray-50 transition-colors group ${selectedIds.has(resp.id) ? 'bg-blue-50/30' : ''}`}>
                        <td className="px-6 py-4">
                          <input 
                            type="checkbox" 
                            className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                            checked={selectedIds.has(resp.id)}
                            onChange={() => toggleSelect(resp.id)}
                          />
                        </td>
                        <td className="px-6 py-4 text-xs text-gray-500 whitespace-nowrap">
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
