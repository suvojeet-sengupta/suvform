"use client";

export const runtime = "edge";

import { useAuth } from "@/context/AuthContext";
import { useApi } from "@/lib/api";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { 
  Layout, 
  Plus, 
  FileText, 
  Settings, 
  LogOut, 
  BarChart2, 
  Globe,
  MoreVertical,
  Search,
  ChevronRight,
  Trash2,
  Copy,
  Check
} from "lucide-react";

interface FormSummary {
  id: string;
  title: string;
  description: string;
  published: number;
  public_slug: string | null;
  created_at: number;
  updated_at: number;
}

export default function Dashboard() {
  const { user, loading: authLoading, logout } = useAuth();
  const api = useApi();
  const router = useRouter();
  const [forms, setForms] = useState<FormSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeFilter, setActiveFilter] = useState<"all" | "public">("all");
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const [copyingId, setCopyingId] = useState<string | null>(null);

  useEffect(() => {
    if (!authLoading && !user) {
      router.push("/login");
    }
  }, [user, authLoading, router]);

  useEffect(() => {
    if (user) {
      fetchForms();
    }
  }, [user]);

  const fetchForms = async () => {
    try {
      const data = await api.get("/v1/forms");
      setForms(data.forms);
    } catch (error) {
      console.error("Failed to fetch forms", error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm("Are you sure you want to delete this form? This action cannot be undone.")) return;
    
    try {
      await api.delete(`/v1/forms/${id}`);
      setForms(forms.filter(f => f.id !== id));
      setOpenMenuId(null);
    } catch (error) {
      console.error("Failed to delete form", error);
      alert("Failed to delete form.");
    }
  };

  const handleCopyLink = (form: FormSummary, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!form.public_slug) return;
    const url = `https://suvforms.suvojeetsengupta.in/f/${form.public_slug}`;
    navigator.clipboard.writeText(url);
    setCopyingId(form.id);
    setTimeout(() => setCopyingId(null), 2000);
    setOpenMenuId(null);
  };

  const filteredForms = activeFilter === "public" 
    ? forms.filter(f => f.published === 1)
    : forms;

  if (authLoading || !user) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Sidebar */}
      <aside className="w-64 bg-white border-r border-gray-200 hidden md:flex flex-col">
        <div className="p-6 border-b border-gray-100">
          <div className="flex items-center gap-3">
            <div className="bg-blue-600 p-2 rounded-lg">
              <Layout className="h-6 w-6 text-white" />
            </div>
            <span className="text-xl font-bold text-gray-900 tracking-tight">SuvForm</span>
          </div>
        </div>
        
        <nav className="flex-1 p-4 space-y-2 overflow-y-auto">
          <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2 px-3">Main</div>
          <button 
            onClick={() => setActiveFilter("all")}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition-colors ${
              activeFilter === "all" ? "text-blue-600 bg-blue-50" : "text-gray-600 hover:bg-gray-50"
            }`}
          >
            <Layout className="h-5 w-5" />
            My Forms
          </button>
          <button 
            onClick={() => setActiveFilter("public")}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition-colors ${
              activeFilter === "public" ? "text-blue-600 bg-blue-50" : "text-gray-600 hover:bg-gray-50"
            }`}
          >
            <Globe className="h-5 w-5" />
            Public Forms
          </button>

          <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider mt-8 mb-2 px-3">System</div>
          <button 
            onClick={() => router.push("/settings")}
            className="w-full flex items-center gap-3 px-3 py-2 text-gray-600 hover:bg-gray-50 rounded-lg transition-colors"
          >
            <Settings className="h-5 w-5" />
            Settings
          </button>
          <button 
            onClick={logout}
            className="w-full flex items-center gap-3 px-3 py-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors mt-auto"
          >
            <LogOut className="h-5 w-5" />
            Sign Out
          </button>
        </nav>

        <div className="p-4 border-t border-gray-100">
          <div className="flex items-center gap-3 p-2">
            <img 
              src={user.photoURL || `https://ui-avatars.com/api/?name=${user.displayName}`} 
              alt="Avatar" 
              className="h-8 w-8 rounded-full border border-gray-200"
            />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-gray-900 truncate">{user.displayName}</p>
              <p className="text-xs text-gray-500 truncate">{user.email}</p>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Header */}
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-8">
          <div className="flex items-center gap-4 flex-1 max-xl">
            <div className="relative w-full">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
              <input 
                type="text" 
                placeholder="Search forms..." 
                className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-full bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
              />
            </div>
          </div>
          <div className="flex items-center gap-4">
            <button 
              onClick={() => router.push("/form/create")}
              className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-5 py-2 rounded-full font-medium shadow-md shadow-blue-500/20 transition-all active:scale-95"
            >
              <Plus className="h-5 w-5" />
              Create Form
            </button>
          </div>
        </header>

        {/* Dashboard Content */}
        <div className="flex-1 overflow-y-auto p-8" onClick={() => setOpenMenuId(null)}>
          <div className="max-w-6xl mx-auto">
            <div className="flex items-end justify-between mb-8">
              <div>
                <h2 className="text-2xl font-bold text-gray-900">
                  {activeFilter === "public" ? "Publicly Shared Forms" : "Your Forms"}
                </h2>
                <p className="text-gray-500">
                  {activeFilter === "public" 
                    ? "These forms are currently live and accepting responses." 
                    : "Manage and analyze your active collection forms"}
                </p>
              </div>
              <div className="text-sm text-gray-500">
                Total: <span className="font-semibold text-gray-900">{filteredForms.length}</span>
              </div>
            </div>

            {loading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="bg-white border border-gray-200 rounded-2xl h-48 animate-pulse"></div>
                ))}
              </div>
            ) : filteredForms.length === 0 ? (
              <div className="text-center py-20 bg-white border-2 border-dashed border-gray-200 rounded-3xl">
                <div className="bg-blue-50 h-16 w-16 rounded-full flex items-center justify-center mx-auto mb-4">
                  <FileText className="h-8 w-8 text-blue-500" />
                </div>
                <h3 className="text-lg font-semibold text-gray-900">No forms found</h3>
                <p className="text-gray-500 max-w-xs mx-auto mt-2">
                  {activeFilter === "public" 
                    ? "You haven't published any forms yet." 
                    : "Create your first form using Gemini AI to start collecting data."}
                </p>
                {activeFilter !== "public" && (
                  <button 
                    onClick={() => router.push("/form/create")}
                    className="mt-6 text-blue-600 font-semibold hover:underline"
                  >
                    Create my first form &rarr;
                  </button>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredForms.map((form) => (
                  <div 
                    key={form.id} 
                    className="group bg-white border border-gray-200 rounded-2xl p-6 hover:shadow-xl hover:border-blue-200 transition-all duration-300 flex flex-col cursor-pointer relative"
                    onClick={() => router.push(`/form/${form.id}/edit`)}
                  >
                    <div className="flex justify-between items-start mb-4">
                      <div className={`p-2 rounded-lg ${form.published ? 'bg-green-50 text-green-600' : 'bg-gray-100 text-gray-400'}`}>
                        <FileText className="h-6 w-6" />
                      </div>
                      <div className="relative">
                        <button 
                          onClick={(e) => {
                            e.stopPropagation();
                            setOpenMenuId(openMenuId === form.id ? null : form.id);
                          }}
                          className="p-1 hover:bg-gray-100 rounded-md text-gray-400 hover:text-gray-600"
                        >
                          <MoreVertical className="h-5 w-5" />
                        </button>
                        
                        {/* Dropdown Menu */}
                        {openMenuId === form.id && (
                          <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-xl shadow-xl z-20 overflow-hidden animate-in fade-in zoom-in duration-200">
                            {form.published === 1 && (
                              <button 
                                onClick={(e) => handleCopyLink(form, e)}
                                className="w-full flex items-center gap-2 px-4 py-3 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                              >
                                {copyingId === form.id ? <Check className="h-4 w-4 text-green-600" /> : <Copy className="h-4 w-4" />}
                                {copyingId === form.id ? "Copied!" : "Copy Link"}
                              </button>
                            )}
                            <button 
                              onClick={() => router.push(`/form/${form.id}/responses`)}
                              className="w-full flex items-center gap-2 px-4 py-3 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                            >
                              <BarChart2 className="h-4 w-4" />
                              View Responses
                            </button>
                            <div className="border-t border-gray-100"></div>
                            <button 
                              onClick={(e) => handleDelete(form.id, e)}
                              className="w-full flex items-center gap-2 px-4 py-3 text-sm text-red-600 hover:bg-red-50 transition-colors"
                            >
                              <Trash2 className="h-4 w-4" />
                              Delete Form
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                    
                    <h3 className="text-lg font-bold text-gray-900 group-hover:text-blue-600 transition-colors truncate">
                      {form.title}
                    </h3>
                    <p className="text-sm text-gray-500 line-clamp-2 mt-1 flex-1">
                      {form.description || "No description provided."}
                    </p>

                    <div className="mt-6 pt-4 border-t border-gray-50 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        {form.published ? (
                          <span className="flex items-center gap-1.5 px-2 py-1 bg-green-50 text-green-700 text-[10px] font-bold uppercase tracking-wider rounded-md border border-green-100">
                            <span className="h-1.5 w-1.5 rounded-full bg-green-500 animate-pulse"></span>
                            Live
                          </span>
                        ) : (
                          <span className="px-2 py-1 bg-gray-50 text-gray-500 text-[10px] font-bold uppercase tracking-wider rounded-md border border-gray-100">
                            Draft
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-gray-400 font-medium flex items-center gap-1">
                        Edited {new Date(form.updated_at).toLocaleDateString()}
                        <ChevronRight className="h-3 w-3 opacity-0 group-hover:opacity-100 -translate-x-2 group-hover:translate-x-0 transition-all" />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
