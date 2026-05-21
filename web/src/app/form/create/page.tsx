"use client";

import { useAuth } from "@/context/AuthContext";
import { useApi } from "@/lib/api";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { 
  ArrowLeft, 
  Sparkles, 
  Wand2, 
  Layout,
  MessageSquare,
  Loader2
} from "lucide-react";

export default function CreateFormPage() {
  const { user, loading: authLoading } = useAuth();
  const api = useApi();
  const router = useRouter();
  const [prompt, setPrompt] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);

  useEffect(() => {
    if (!authLoading && !user) {
      router.push("/login");
    }
  }, [user, authLoading, router]);

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim() || isGenerating) return;

    setIsGenerating(true);
    try {
      const data = await api.post("/v1/ai/generate-form", { prompt });
      // The API returns { title, description, fields, calculations }
      // We need to save this as a new form
      const newForm = await api.post("/v1/forms", data);
      router.push(`/form/${newForm.id}/edit`);
    } catch (error) {
      console.error("Failed to generate form", error);
      alert("AI generation failed. Please try again.");
    } finally {
      setIsGenerating(false);
    }
  };

  const handleManualCreate = async () => {
    try {
      const newForm = await api.post("/v1/forms", {
        title: "Untitled Form",
        description: "",
        fields: [],
        calculations: []
      });
      router.push(`/form/${newForm.id}/edit`);
    } catch (error) {
      console.error("Failed to create form", error);
    }
  };

  if (authLoading || !user) return null;

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <header className="h-16 bg-white border-b border-gray-200 flex items-center px-8 shrink-0">
        <button 
          onClick={() => router.back()}
          className="p-2 hover:bg-gray-100 rounded-full transition-colors mr-4"
        >
          <ArrowLeft className="h-5 w-5 text-gray-600" />
        </button>
        <h1 className="text-xl font-bold text-gray-900">Create New Form</h1>
      </header>

      <main className="flex-1 flex flex-col items-center justify-center p-8">
        <div className="w-full max-w-2xl space-y-8">
          <div className="text-center space-y-2">
            <div className="inline-flex items-center justify-center p-3 bg-blue-100 rounded-2xl mb-4">
              <Sparkles className="h-8 w-8 text-blue-600" />
            </div>
            <h2 className="text-3xl font-bold text-gray-900">How would you like to start?</h2>
            <p className="text-gray-500">Let Gemini AI build your form in seconds or start from scratch.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* AI Generator Option */}
            <div className="md:col-span-2 bg-white border border-gray-200 rounded-3xl p-8 shadow-sm hover:shadow-md transition-shadow">
              <div className="flex items-center gap-3 mb-6">
                <Wand2 className="h-6 w-6 text-purple-600" />
                <h3 className="text-xl font-bold text-gray-900">AI Form Architect</h3>
              </div>
              
              <form onSubmit={handleGenerate} className="space-y-4">
                <div className="relative">
                  <textarea
                    value={prompt}
                    onChange={(e) => setPrompt(e.target.value)}
                    placeholder="Describe your form... (e.g., 'A registration form for a weekend hiking trip with dietary preferences and emergency contact info')"
                    className="w-full h-32 p-4 border border-gray-200 rounded-2xl bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all resize-none text-gray-700"
                    disabled={isGenerating}
                  />
                  <div className="absolute bottom-4 right-4 text-xs text-gray-400">
                    Powered by Gemini 1.5 Flash
                  </div>
                </div>
                <button
                  type="submit"
                  disabled={!prompt.trim() || isGenerating}
                  className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 text-white py-4 rounded-2xl font-bold shadow-lg shadow-blue-500/20 transition-all flex items-center justify-center gap-2"
                >
                  {isGenerating ? (
                    <>
                      <Loader2 className="h-5 w-5 animate-spin" />
                      Generating your form...
                    </>
                  ) : (
                    <>
                      <Sparkles className="h-5 w-5" />
                      Generate with AI
                    </>
                  )}
                </button>
              </form>
            </div>

            {/* Manual Option */}
            <button 
              onClick={handleManualCreate}
              className="flex items-center gap-4 bg-white border border-gray-200 rounded-2xl p-6 hover:border-blue-500 hover:bg-blue-50 transition-all text-left group"
            >
              <div className="p-3 bg-gray-100 rounded-xl group-hover:bg-blue-100 transition-colors">
                <Layout className="h-6 w-6 text-gray-600 group-hover:text-blue-600" />
              </div>
              <div>
                <h4 className="font-bold text-gray-900">Blank Canvas</h4>
                <p className="text-sm text-gray-500">Start with an empty form and add fields manually.</p>
              </div>
            </button>

            {/* Template Option (Placeholder) */}
            <button className="flex items-center gap-4 bg-white border border-gray-200 rounded-2xl p-6 opacity-60 cursor-not-allowed text-left">
              <div className="p-3 bg-gray-100 rounded-xl">
                <MessageSquare className="h-6 w-6 text-gray-600" />
              </div>
              <div>
                <h4 className="font-bold text-gray-900">Use Template</h4>
                <p className="text-sm text-gray-500">Choose from pre-built industry standard templates.</p>
              </div>
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
