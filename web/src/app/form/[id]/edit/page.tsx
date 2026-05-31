"use client";

export const runtime = "edge";

import { useAuth } from "@/context/AuthContext";
import { useApi } from "@/lib/api";
import { useRouter, useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { 
  ArrowLeft, 
  Save, 
  Plus, 
  Trash2, 
  GripVertical, 
  Eye,
  Send,
  XCircle,
  Hash,
  Type,
  List as ListIcon,
  CheckSquare,
  ChevronDown,
  ExternalLink,
  Info,
  Menu,
  X,
  Loader2,
  ChevronRight,
  Palette,
  Sparkles
} from "lucide-react";

interface Field {
  id: string;
  type: string;
  label: string;
  required: boolean;
  options: string[];
  placeholder?: string;
}

interface Calculation {
  id: string;
  label: string;
  expression: string;
  format?: string;
}

interface FormTheme {
  backgroundColor: string;
  primaryColor: string;
  accentColor: string;
  textColor: string;
  mutedTextColor: string;
  cardBackgroundColor: string;
  fontFamily: "serif" | "sans" | "mono";
  borderRadius: "none" | "small" | "medium" | "large" | "full";
  coverImageKeyword?: string;
}

interface FormDetail {
  id: string;
  title: string;
  description: string;
  fields: Field[];
  calculations: Calculation[];
  theme: FormTheme | null;
  published: number;
  public_slug: string | null;
  response_limit?: number | null;
}

const FIELD_TYPES = [
  { type: "text", label: "Short Text", icon: Type },
  { type: "number", label: "Number", icon: Hash },
  { type: "select", label: "Dropdown", icon: ChevronDown },
  { type: "radio", label: "Single Choice", icon: ListIcon },
  { type: "checkbox", label: "Multiple Choice", icon: CheckSquare },
];

export default function EditFormPage() {
  const { user, loading: authLoading } = useAuth();
  const api = useApi();
  const router = useRouter();
  const { id } = useParams();
  
  const [form, setForm] = useState<FormDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<"fields" | "calculations" | "theme" | "settings">("fields");
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [themePrompt, setThemePrompt] = useState("");
  const [generatingTheme, setGeneratingTheme] = useState(false);

  useEffect(() => {
    if (!authLoading && !user) {
      router.push("/login");
    }
  }, [user, authLoading, router]);

  useEffect(() => {
    if (user && id) {
      fetchForm();
    }
  }, [user, id]);

  const fetchForm = async () => {
    try {
      const data = await api.get(`/v1/forms/${id}`);
      setForm(data);
    } catch (error) {
      console.error("Failed to fetch form", error);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!form || saving) return;
    setSaving(true);
    try {
      await api.put(`/v1/forms/${id}`, {
        title: form.title,
        description: form.description,
        fields: form.fields,
        calculations: form.calculations,
        theme: form.theme,
        response_limit: form.response_limit ?? null
      });
    } catch (error) {
      console.error("Failed to save form", error);
      alert("Failed to save changes.");
    } finally {
      setSaving(false);
    }
  };

  const handlePublish = async () => {
    if (!form) return;
    try {
      const data = await api.post(`/v1/forms/${id}/publish`);
      setForm({ ...form, published: 1, public_slug: data.slug });
    } catch (error) {
      console.error("Failed to publish", error);
    }
  };

  const handleUnpublish = async () => {
    if (!form) return;
    try {
      await api.post(`/v1/forms/${id}/unpublish`);
      setForm({ ...form, published: 0 });
    } catch (error) {
      console.error("Failed to unpublish", error);
    }
  };

  const addField = (type: string) => {
    if (!form) return;
    const newField: Field = {
      id: `field_${Date.now()}`,
      type,
      label: `New ${type} field`,
      required: false,
      options: type === "select" || type === "radio" || type === "checkbox" ? ["Option 1"] : [],
    };
    setForm({ ...form, fields: [...form.fields, newField] });
    if (window.innerWidth < 1024) setIsSidebarOpen(false);
  };

  const updateField = (fieldId: string, updates: Partial<Field>) => {
    if (!form) return;
    setForm({
      ...form,
      fields: form.fields.map(f => f.id === fieldId ? { ...f, ...updates } : f)
    });
  };

  const removeField = (fieldId: string) => {
    if (!form) return;
    setForm({
      ...form,
      fields: form.fields.filter(f => f.id !== fieldId)
    });
  };

  const addCalculation = () => {
    if (!form) return;
    const newCalc: Calculation = {
      id: `calc_${Date.now()}`,
      label: "New Calculation",
      expression: "",
    };
    setForm({ ...form, calculations: [...form.calculations, newCalc] });
    if (window.innerWidth < 1024) setIsSidebarOpen(false);
  };

  const updateCalculation = (calcId: string, updates: Partial<Calculation>) => {
    if (!form) return;
    setForm({
      ...form,
      calculations: form.calculations.map(c => c.id === calcId ? { ...c, ...updates } : c)
    });
  };

  const removeCalculation = (calcId: string) => {
    if (!form) return;
    setForm({
      ...form,
      calculations: form.calculations.filter(c => c.id !== calcId)
    });
  };

  const generateAITheme = async () => {
    if (!themePrompt.trim() || generatingTheme) return;
    setGeneratingTheme(true);
    try {
      const data = await api.post("/v1/ai/generate-theme", { prompt: themePrompt });
      if (form) setForm({ ...form, theme: data });
    } catch (error) {
      console.error("Failed to generate theme", error);
      alert("Failed to generate theme.");
    } finally {
      setGeneratingTheme(false);
    }
  };

  if (loading || !form) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  const publicUrl = form.public_slug 
    ? `https://suvforms.suvojeetsengupta.in/f/${form.public_slug}`
    : null;

  return (
    <div className="h-screen flex flex-col bg-gray-50 overflow-hidden">
      {/* Top Bar */}
      <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-4 md:px-6 shrink-0 z-20 shadow-sm">
        <div className="flex items-center gap-2 md:gap-4 overflow-hidden">
          <button onClick={() => router.push("/")} className="p-2 hover:bg-gray-100 rounded-full transition-colors shrink-0">
            <ArrowLeft className="h-5 w-5 text-gray-600" />
          </button>
          <div className="hidden sm:block h-6 w-[1px] bg-gray-200 mx-1"></div>
          <div className="truncate">
            <input 
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              className="text-sm md:text-lg font-bold text-gray-900 bg-transparent border-none focus:ring-0 p-0 w-full truncate"
              placeholder="Form Title"
            />
          </div>
          {form.published === 1 && (
            <span className="hidden xs:flex items-center gap-1.5 px-2 py-0.5 bg-green-50 text-green-700 text-[10px] font-bold uppercase tracking-wider rounded border border-green-100">
              Live
            </span>
          )}
        </div>

        <div className="flex items-center gap-2">
          <button 
            onClick={() => setIsSidebarOpen(!isSidebarOpen)}
            className="lg:hidden p-2 hover:bg-gray-100 rounded-lg text-blue-600"
          >
            {isSidebarOpen ? <X className="h-5 w-5" /> : <Plus className="h-5 w-5" />}
          </button>
          
          <button 
            onClick={handleSave}
            disabled={saving}
            className="p-2 md:px-4 md:py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white rounded-lg shadow-md shadow-blue-500/20 transition-all flex items-center gap-2"
          >
            {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
            <span className="hidden md:inline text-sm font-bold">Save</span>
          </button>
          
          <div className="h-6 w-[1px] bg-gray-200 mx-1"></div>
          
          {form.published === 0 ? (
            <button 
              onClick={handlePublish}
              className="p-2 md:px-4 md:py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg shadow-md shadow-green-500/20 transition-all flex items-center gap-2"
            >
              <Send className="h-4 w-4" />
              <span className="hidden md:inline text-sm font-bold">Publish</span>
            </button>
          ) : (
            <button 
              onClick={handleUnpublish}
              className="p-2 md:px-4 md:py-2 border border-red-200 text-red-600 hover:bg-red-50 rounded-lg transition-all flex items-center gap-2"
            >
              <XCircle className="h-4 w-4" />
              <span className="hidden md:inline text-sm font-bold">Unpublish</span>
            </button>
          )}
        </div>
      </header>

      {/* Main Layout */}
      <div className="flex-1 flex overflow-hidden relative">
        {/* Sidebar - Toolset */}
        <aside className={`
          fixed lg:relative inset-y-0 left-0 z-10 w-80 bg-white border-r border-gray-200 flex flex-col shrink-0 transition-transform duration-300 lg:translate-x-0
          ${isSidebarOpen ? "translate-x-0" : "-translate-x-full"}
        `}>
          <div className="flex border-b border-gray-100">
            {(["fields", "calculations", "theme", "settings"] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`flex-1 py-4 text-[10px] md:text-xs font-bold uppercase tracking-widest transition-all border-b-2 ${
                  activeTab === tab 
                    ? "text-blue-600 border-blue-600" 
                    : "text-gray-400 border-transparent hover:text-gray-600"
                }`}
              >
                {tab}
              </button>
            ))}
          </div>

          <div className="flex-1 overflow-y-auto p-6">
            {activeTab === "fields" && (
              <div className="space-y-6">
                <div>
                  <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-4">Add Elements</h4>
                  <div className="grid grid-cols-1 gap-2">
                    {FIELD_TYPES.map((ft) => (
                      <button
                        key={ft.type}
                        onClick={() => addField(ft.type)}
                        className="flex items-center gap-3 w-full p-3 border border-gray-100 rounded-xl hover:border-blue-200 hover:bg-blue-50/50 hover:text-blue-600 transition-all text-sm font-medium text-gray-600 group text-left"
                      >
                        <div className="p-2 bg-gray-50 rounded-lg group-hover:bg-blue-100 transition-colors">
                          <ft.icon className="h-4 w-4" />
                        </div>
                        {ft.label}
                        <Plus className="h-4 w-4 ml-auto opacity-0 group-hover:opacity-100 transition-all" />
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {activeTab === "calculations" && (
              <div className="space-y-4">
                <button
                  onClick={addCalculation}
                  className="w-full flex items-center justify-center gap-2 p-3 bg-gray-900 text-white rounded-xl text-sm font-bold hover:bg-gray-800 transition-all shadow-lg shadow-gray-200"
                >
                  <Plus className="h-4 w-4" />
                  Add Calculation
                </button>
              </div>
            )}

            {activeTab === "theme" && (
              <div className="space-y-6">
                <div className="p-4 bg-blue-50 rounded-2xl border border-blue-100">
                  <h4 className="flex items-center gap-2 text-xs font-bold text-blue-700 uppercase mb-3">
                    <Sparkles className="h-3.5 w-3.5" />
                    AI Theme Designer
                  </h4>
                  <textarea
                    value={themePrompt}
                    onChange={(e) => setThemePrompt(e.target.value)}
                    placeholder="e.g. 'wedding invite', 'corporate blue', 'dark mode with neon green accents'"
                    className="w-full p-3 bg-white border border-blue-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 min-h-[80px]"
                  />
                  <button
                    onClick={generateAITheme}
                    disabled={generatingTheme || !themePrompt.trim()}
                    className="w-full mt-3 p-3 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white rounded-xl text-sm font-bold transition-all flex items-center justify-center gap-2"
                  >
                    {generatingTheme ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                    Generate Theme
                  </button>
                </div>

                {form.theme && (
                  <div className="space-y-4 pt-2">
                    <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider">Current Theme</h4>
                    
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="text-[10px] font-bold text-gray-500 uppercase">Primary</label>
                        <div className="flex items-center gap-2 mt-1">
                          <div className="h-8 w-8 rounded-lg border border-gray-200" style={{ backgroundColor: form.theme.primaryColor }}></div>
                          <span className="text-[10px] font-mono text-gray-500">{form.theme.primaryColor}</span>
                        </div>
                      </div>
                      <div>
                        <label className="text-[10px] font-bold text-gray-500 uppercase">Background</label>
                        <div className="flex items-center gap-2 mt-1">
                          <div className="h-8 w-8 rounded-lg border border-gray-200" style={{ backgroundColor: form.theme.backgroundColor }}></div>
                          <span className="text-[10px] font-mono text-gray-500">{form.theme.backgroundColor}</span>
                        </div>
                      </div>
                    </div>

                    <div className="space-y-1">
                      <label className="text-[10px] font-bold text-gray-500 uppercase">Font Style</label>
                      <div className="flex bg-gray-100 p-1 rounded-lg">
                        {(["sans", "serif", "mono"] as const).map((f) => (
                          <button
                            key={f}
                            onClick={() => setForm({ ...form, theme: { ...form.theme!, fontFamily: f } })}
                            className={`flex-1 py-1.5 text-xs rounded-md transition-all ${
                              form.theme?.fontFamily === f ? "bg-white text-gray-900 shadow-sm" : "text-gray-500"
                            }`}
                          >
                            {f.charAt(0).toUpperCase() + f.slice(1)}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="space-y-1">
                      <label className="text-[10px] font-bold text-gray-500 uppercase">Corner Radius</label>
                      <div className="flex bg-gray-100 p-1 rounded-lg">
                        {(["none", "small", "medium", "large"] as const).map((r) => (
                          <button
                            key={r}
                            onClick={() => setForm({ ...form, theme: { ...form.theme!, borderRadius: r } })}
                            className={`flex-1 py-1.5 text-[10px] rounded-md transition-all ${
                              form.theme?.borderRadius === r ? "bg-white text-gray-900 shadow-sm" : "text-gray-500"
                            }`}
                          >
                            {r.charAt(0).toUpperCase() + r.slice(1)}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="space-y-2">
                      <label className="text-[10px] font-bold text-gray-500 uppercase">Cover Keyword</label>
                      <input
                        value={form.theme.coverImageKeyword || ""}
                        onChange={(e) => setForm({ ...form, theme: { ...form.theme!, coverImageKeyword: e.target.value } })}
                        placeholder="e.g. 'abstract', 'nature'"
                        className="w-full p-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                      />
                    </div>
                  </div>
                )}
              </div>
            )}

            {activeTab === "settings" && (
              <div className="space-y-6">
                <div className="space-y-2">
                  <label className="text-xs font-bold text-gray-500 uppercase">Description</label>
                  <textarea 
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    className="w-full p-3 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 h-32"
                    placeholder="Form description..."
                  />
                </div>
                {form.published === 1 && publicUrl && (
                  <div className="space-y-2">
                    <label className="text-xs font-bold text-gray-500 uppercase">Public Link</label>
                    <div className="flex items-center gap-2">
                      <input readOnly value={publicUrl} className="flex-1 p-2 bg-gray-50 border border-gray-200 rounded-lg text-[10px] truncate" />
                      <button onClick={() => window.open(publicUrl, "_blank")} className="p-2 bg-blue-50 text-blue-600 rounded-lg hover:bg-blue-100">
                        <ExternalLink className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                )}
                <button 
                  onClick={() => router.push(`/form/${id}/responses`)}
                  className="w-full flex items-center justify-center gap-2 p-3 border border-gray-200 rounded-xl text-sm font-bold text-gray-700 hover:bg-gray-50"
                >
                  <Eye className="h-4 w-4" />
                  View Responses
                </button>

                {/* Response Limit */}
                <div className="space-y-2 pt-4 border-t border-gray-100">
                  <label className="text-xs font-bold text-gray-500 uppercase">Response Limit</label>
                  <div className="flex items-center gap-3">
                    <input
                      type="number"
                      min="0"
                      placeholder="Unlimited"
                      value={form.response_limit ?? ''}
                      onChange={(e) => {
                        const val = e.target.value ? parseInt(e.target.value) : null;
                        setForm({ ...form, response_limit: val && val > 0 ? val : null });
                      }}
                      className="w-32 p-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                    />
                    <span className="text-sm text-gray-500">responses (leave blank for unlimited)</span>
                  </div>
                  <p className="text-[11px] text-gray-400">When the limit is reached, the public form will show as closed.</p>
                </div>
              </div>
            )}
          </div>
        </aside>

        {/* Backdrop for mobile sidebar */}
        {isSidebarOpen && (
          <div className="fixed inset-0 bg-black/20 z-0 lg:hidden" onClick={() => setIsSidebarOpen(false)}></div>
        )}

        {/* Form Preview Area */}
        <main 
          className="flex-1 overflow-y-auto p-4 md:p-12 transition-colors flex justify-center"
          style={{ backgroundColor: form.theme?.backgroundColor || "transparent" }}
        >
          <div className="w-full max-w-3xl space-y-8 pb-20">
            <div 
              className="bg-white shadow-sm overflow-hidden transition-all"
              style={{ 
                backgroundColor: form.theme?.cardBackgroundColor || "#ffffff",
                borderRadius: form.theme?.borderRadius === 'large' ? '40px' : 
                             form.theme?.borderRadius === 'small' ? '12px' : 
                             form.theme?.borderRadius === 'none' ? '0px' : '24px',
                fontFamily: form.theme?.fontFamily === 'serif' ? 'var(--font-serif)' : 
                           form.theme?.fontFamily === 'mono' ? 'var(--font-mono)' : 'inherit'
              }}
            >
              {form.theme?.coverImageKeyword && (
                <div className="h-32 sm:h-48 w-full overflow-hidden bg-gray-100">
                  <img 
                    src={`https://source.unsplash.com/featured/1200x400/?${encodeURIComponent(form.theme.coverImageKeyword)}`}
                    className="w-full h-full object-cover"
                    alt="Theme cover"
                  />
                </div>
              )}

              <div className="p-6 md:p-10">
                {activeTab === "fields" && (
                  <div className="space-y-4 md:space-y-6">
                    {form.fields.length === 0 ? (
                      <div className="text-center py-20 bg-white border-2 border-dashed border-gray-200 rounded-3xl">
                        <p className="text-gray-400 font-medium px-4">Your form is empty. Use the + button to add fields!</p>
                      </div>
                    ) : (
                      form.fields.map((field) => (
                        <div 
                          key={field.id}
                          className="bg-white border border-gray-200 rounded-2xl p-4 md:p-6 shadow-sm group relative hover:border-blue-200 transition-all"
                          style={{ 
                            borderRadius: form.theme?.borderRadius === 'large' ? '24px' : 
                                         form.theme?.borderRadius === 'small' ? '8px' : 
                                         form.theme?.borderRadius === 'none' ? '0px' : '16px',
                          }}
                        >
                          <div className="flex items-start justify-between gap-4 mb-4">
                            <div className="flex-1">
                              <input 
                                value={field.label}
                                onChange={(e) => updateField(field.id, { label: e.target.value })}
                                className="text-sm md:text-base font-bold text-gray-900 w-full border-none focus:ring-0 p-0 bg-transparent"
                                placeholder="Field Label"
                                style={{ color: form.theme?.textColor || "inherit" }}
                              />
                              <p className="text-[9px] font-mono text-gray-400 mt-1 uppercase">ID: {field.id}</p>
                            </div>
                            <button 
                              onClick={() => removeField(field.id)}
                              className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </div>

                          <div className="space-y-4">
                            <div 
                              className="w-full p-3 bg-gray-50 border border-gray-100 rounded-xl text-gray-400 text-xs md:text-sm"
                              style={{ 
                                borderRadius: form.theme?.borderRadius === 'large' ? '16px' : 
                                             form.theme?.borderRadius === 'small' ? '6px' : 
                                             form.theme?.borderRadius === 'none' ? '0px' : '12px',
                                borderColor: form.theme?.primaryColor ? `${form.theme.primaryColor}33` : undefined
                              }}
                            >
                              {field.placeholder || `Enter ${field.label.toLowerCase()}...`}
                            </div>

                            {(field.type === "select" || field.type === "radio" || field.type === "checkbox") && (
                              <div className="pt-4 border-t border-gray-50">
                                <div className="space-y-2">
                                  {field.options.map((opt, idx) => (
                                    <div key={idx} className="flex items-center gap-2">
                                      <div 
                                        className="h-4 w-4 rounded border border-gray-300 bg-gray-50"
                                        style={{ borderColor: form.theme?.primaryColor ? `${form.theme.primaryColor}66` : undefined }}
                                      ></div>
                                      <input 
                                        value={opt}
                                        onChange={(e) => {
                                          const newOpts = [...field.options];
                                          newOpts[idx] = e.target.value;
                                          updateField(field.id, { options: newOpts });
                                        }}
                                        className="flex-1 text-xs md:text-sm text-gray-700 bg-transparent border-none focus:ring-0 p-0"
                                        style={{ color: form.theme?.textColor || "inherit" }}
                                      />
                                      <button onClick={() => updateField(field.id, { options: field.options.filter((_, i) => i !== idx) })} className="p-1 text-gray-300 hover:text-red-400">
                                        <Trash2 className="h-3 w-3" />
                                      </button>
                                    </div>
                                  ))}
                                  <button 
                                    onClick={() => updateField(field.id, { options: [...field.options, `Option ${field.options.length + 1}`] })} 
                                    className="flex items-center gap-2 text-xs font-bold mt-2"
                                    style={{ color: form.theme?.primaryColor || "#2563eb" }}
                                  >
                                    <Plus className="h-3 w-3" /> Add Option
                                  </button>
                                </div>
                              </div>
                            )}
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                )}

                {(activeTab === "calculations" || activeTab === "theme" || activeTab === "settings") && (
                  <div className="bg-white border border-gray-200 rounded-3xl p-8 md:p-12 text-center space-y-4">
                    <div className="inline-flex p-4 bg-blue-50 text-blue-600 rounded-2xl">
                      {activeTab === "calculations" && <Hash className="h-8 w-8" />}
                      {activeTab === "theme" && <Palette className="h-8 w-8" />}
                      {activeTab === "settings" && <Info className="h-8 w-8" />}
                    </div>
                    <h3 className="text-xl font-bold text-gray-900">
                      {activeTab === "calculations" && "Calculations Mode"}
                      {activeTab === "theme" && "Theme Designer"}
                      {activeTab === "settings" && "Form Settings"}
                    </h3>
                    <p className="text-gray-500 max-w-sm mx-auto text-sm">
                      {activeTab === "calculations" && "Add logic to your form to perform real-time math based on user input."}
                      {activeTab === "theme" && "Use the AI Theme Designer in the sidebar to completely transform the look of your form."}
                      {activeTab === "settings" && "Configure response limits, descriptions, and share your form with the world."}
                    </p>
                    
                    {activeTab === "calculations" && form.calculations.length > 0 && (
                      <div className="pt-8 space-y-4 text-left">
                        {form.calculations.map((calc) => (
                          <div key={calc.id} className="p-4 bg-gray-50 rounded-2xl border border-gray-100">
                            <p className="text-xs font-bold text-gray-400 uppercase mb-1">{calc.label}</p>
                            <code className="text-purple-600 font-mono text-sm">{calc.expression}</code>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
