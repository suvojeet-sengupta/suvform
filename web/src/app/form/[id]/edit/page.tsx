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
  Settings2,
  Eye,
  Send,
  CheckCircle2,
  XCircle,
  Hash,
  Type,
  List as ListIcon,
  CheckSquare,
  ChevronDown,
  ExternalLink,
  Info
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

interface FormDetail {
  id: string;
  title: string;
  description: string;
  fields: Field[];
  calculations: Calculation[];
  published: number;
  public_slug: string | null;
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
  const [activeTab, setActiveTab] = useState<"fields" | "calculations" | "settings">("fields");

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
        calculations: form.calculations
      });
      // Show success toast or similar
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
      <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 shrink-0 z-10 shadow-sm">
        <div className="flex items-center gap-4">
          <button onClick={() => router.push("/")} className="p-2 hover:bg-gray-100 rounded-full transition-colors">
            <ArrowLeft className="h-5 w-5 text-gray-600" />
          </button>
          <div className="h-6 w-[1px] bg-gray-200 mx-2"></div>
          <div>
            <input 
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              className="text-lg font-bold text-gray-900 bg-transparent border-none focus:ring-0 p-0 w-64 truncate"
              placeholder="Form Title"
            />
          </div>
          {form.published === 1 && (
            <span className="flex items-center gap-1.5 px-2 py-0.5 bg-green-50 text-green-700 text-[10px] font-bold uppercase tracking-wider rounded border border-green-100">
              Live
            </span>
          )}
        </div>

        <div className="flex items-center gap-3">
          <button 
            onClick={() => router.push(`/form/${id}/responses`)}
            className="flex items-center gap-2 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <Eye className="h-4 w-4" />
            View Responses
          </button>
          <button 
            onClick={handleSave}
            disabled={saving}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white text-sm font-bold rounded-lg shadow-md shadow-blue-500/20 transition-all"
          >
            {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
            Save
          </button>
          <div className="h-6 w-[1px] bg-gray-200 mx-1"></div>
          {form.published === 0 ? (
            <button 
              onClick={handlePublish}
              className="flex items-center gap-2 px-4 py-2 bg-green-600 hover:bg-green-700 text-white text-sm font-bold rounded-lg shadow-md shadow-green-500/20 transition-all"
            >
              <Send className="h-4 w-4" />
              Publish
            </button>
          ) : (
            <button 
              onClick={handleUnpublish}
              className="flex items-center gap-2 px-4 py-2 border border-red-200 text-red-600 hover:bg-red-50 text-sm font-bold rounded-lg transition-all"
            >
              <XCircle className="h-4 w-4" />
              Unpublish
            </button>
          )}
        </div>
      </header>

      {/* Main Layout */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left Sidebar - Toolset */}
        <aside className="w-80 bg-white border-r border-gray-200 flex flex-col shrink-0">
          <div className="flex border-b border-gray-100">
            {(["fields", "calculations", "settings"] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`flex-1 py-4 text-xs font-bold uppercase tracking-widest transition-all border-b-2 ${
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
                
                <div className="p-4 bg-blue-50 rounded-2xl border border-blue-100">
                  <div className="flex items-start gap-3">
                    <Info className="h-5 w-5 text-blue-500 shrink-0 mt-0.5" />
                    <p className="text-xs text-blue-700 leading-relaxed">
                      Select an element to add it to your form. You can drag to reorder them later.
                    </p>
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
                <div className="p-4 bg-purple-50 rounded-2xl border border-purple-100 mt-6">
                  <div className="flex items-start gap-3">
                    <Info className="h-5 w-5 text-purple-500 shrink-0 mt-0.5" />
                    <div className="space-y-2">
                      <p className="text-xs text-purple-700 font-bold">Calculation Tips</p>
                      <p className="text-[11px] text-purple-600 leading-relaxed">
                        Use field IDs in curly braces like <code className="bg-white px-1 rounded border border-purple-200">{"{field_id}"}</code> to reference values.
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {activeTab === "settings" && (
              <div className="space-y-6">
                <div className="space-y-2">
                  <label className="text-xs font-bold text-gray-500 uppercase">Form Description</label>
                  <textarea 
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    className="w-full p-3 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 h-32"
                    placeholder="Describe what this form is for..."
                  />
                </div>

                {form.published === 1 && publicUrl && (
                  <div className="space-y-2">
                    <label className="text-xs font-bold text-gray-500 uppercase">Public Link</label>
                    <div className="flex items-center gap-2">
                      <input 
                        readOnly
                        value={publicUrl}
                        className="flex-1 p-2 bg-gray-50 border border-gray-200 rounded-lg text-xs truncate"
                      />
                      <button 
                        onClick={() => window.open(publicUrl, "_blank")}
                        className="p-2 bg-blue-50 text-blue-600 rounded-lg hover:bg-blue-100 transition-colors"
                      >
                        <ExternalLink className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </aside>

        {/* Form Preview Area */}
        <main className="flex-1 overflow-y-auto p-12 bg-gray-50/50 flex justify-center">
          <div className="w-full max-w-3xl space-y-8 pb-20">
            {activeTab === "fields" && (
              <div className="space-y-6">
                {form.fields.length === 0 ? (
                  <div className="text-center py-20 bg-white border-2 border-dashed border-gray-200 rounded-3xl">
                    <p className="text-gray-400 font-medium">Your form is empty. Add some fields from the sidebar!</p>
                  </div>
                ) : (
                  form.fields.map((field) => (
                    <div 
                      key={field.id}
                      className="bg-white border border-gray-200 rounded-2xl p-6 shadow-sm group relative hover:border-blue-200 transition-all"
                    >
                      <div className="absolute -left-3 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-all cursor-grab active:cursor-grabbing p-1 bg-white border border-gray-200 rounded-lg shadow-sm">
                        <GripVertical className="h-4 w-4 text-gray-400" />
                      </div>

                      <div className="flex items-start justify-between gap-4 mb-4">
                        <div className="flex-1">
                          <input 
                            value={field.label}
                            onChange={(e) => updateField(field.id, { label: e.target.value })}
                            className="text-base font-bold text-gray-900 w-full border-none focus:ring-0 p-0"
                            placeholder="Field Label"
                          />
                          <p className="text-[10px] font-mono text-gray-400 mt-1 uppercase tracking-tighter">ID: {field.id}</p>
                        </div>
                        <div className="flex items-center gap-2">
                          <button 
                            onClick={() => removeField(field.id)}
                            className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </div>

                      <div className="space-y-4">
                        {/* Mock Input Preview */}
                        <div className="w-full p-3 bg-gray-50 border border-gray-100 rounded-xl text-gray-400 text-sm">
                          {field.placeholder || `Enter ${field.label.toLowerCase()}...`}
                        </div>

                        {/* Field Options for Choice Types */}
                        {(field.type === "select" || field.type === "radio" || field.type === "checkbox") && (
                          <div className="pt-4 border-t border-gray-50">
                            <label className="text-[10px] font-bold text-gray-400 uppercase tracking-widest block mb-3">Options</label>
                            <div className="space-y-2">
                              {field.options.map((opt, idx) => (
                                <div key={idx} className="flex items-center gap-2">
                                  <div className="h-4 w-4 rounded border border-gray-300 bg-gray-50"></div>
                                  <input 
                                    value={opt}
                                    onChange={(e) => {
                                      const newOpts = [...field.options];
                                      newOpts[idx] = e.target.value;
                                      updateField(field.id, { options: newOpts });
                                    }}
                                    className="flex-1 text-sm text-gray-700 bg-transparent border-none focus:ring-0 p-0"
                                  />
                                  <button 
                                    onClick={() => {
                                      const newOpts = field.options.filter((_, i) => i !== idx);
                                      updateField(field.id, { options: newOpts });
                                    }}
                                    className="p-1 text-gray-300 hover:text-red-400"
                                  >
                                    <Trash2 className="h-3 w-3" />
                                  </button>
                                </div>
                              ))}
                              <button 
                                onClick={() => updateField(field.id, { options: [...field.options, `Option ${field.options.length + 1}`] })}
                                className="flex items-center gap-2 text-xs font-bold text-blue-600 hover:text-blue-700 mt-2"
                              >
                                <Plus className="h-3 w-3" />
                                Add Option
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

            {activeTab === "calculations" && (
              <div className="space-y-6">
                {form.calculations.length === 0 ? (
                   <div className="text-center py-20 bg-white border-2 border-dashed border-gray-200 rounded-3xl">
                   <p className="text-gray-400 font-medium">No calculations yet. Add one to perform math on form data.</p>
                 </div>
                ) : (
                  form.calculations.map((calc) => (
                    <div 
                      key={calc.id}
                      className="bg-white border border-gray-200 rounded-2xl p-6 shadow-sm hover:border-purple-200 transition-all"
                    >
                      <div className="flex items-start justify-between gap-4 mb-6">
                        <div className="flex-1">
                          <input 
                            value={calc.label}
                            onChange={(e) => updateCalculation(calc.id, { label: e.target.value })}
                            className="text-base font-bold text-gray-900 w-full border-none focus:ring-0 p-0"
                            placeholder="Calculation Label"
                          />
                          <p className="text-[10px] font-mono text-gray-400 mt-1 uppercase tracking-tighter">ID: {calc.id}</p>
                        </div>
                        <button 
                          onClick={() => removeCalculation(calc.id)}
                          className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>

                      <div className="space-y-4">
                        <div className="space-y-2">
                          <label className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Expression</label>
                          <input 
                            value={calc.expression}
                            onChange={(e) => updateCalculation(calc.id, { expression: e.target.value })}
                            className="w-full p-3 bg-gray-900 text-green-400 font-mono text-sm rounded-xl focus:ring-2 focus:ring-purple-500/20"
                            placeholder="{field_1} * 2 + {field_2}"
                          />
                        </div>
                        <div className="flex items-center gap-4">
                           <div className="flex-1 space-y-2">
                             <label className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Format</label>
                             <select 
                               value={calc.format || ""}
                               onChange={(e) => updateCalculation(calc.id, { format: e.target.value })}
                               className="w-full p-2 border border-gray-200 rounded-lg text-sm bg-white"
                             >
                               <option value="">Default</option>
                               <option value="currency">Currency ($)</option>
                               <option value="percentage">Percentage (%)</option>
                               <option value="decimal">2 Decimal Places</option>
                             </select>
                           </div>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}

function Loader2(props: any) {
  return (
    <svg
      {...props}
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M21 12a9 9 0 1 1-6.219-8.56" />
    </svg>
  );
}
