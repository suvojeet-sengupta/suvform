"use client";

export const runtime = "edge";

import { useAuth } from "@/context/AuthContext";
import { useForms, useDeleteForm } from "@/lib/queries";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
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
  Check,
  MessageSquare,
  Menu,
  X,
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
  const router = useRouter();
  const { data, isLoading } = useForms(!!user);
  const deleteForm = useDeleteForm();
  const forms: FormSummary[] = data?.forms ?? [];

  const [activeFilter, setActiveFilter] = useState<"all" | "public" | "responses">("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const [copyingId, setCopyingId] = useState<string | null>(null);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    if (!authLoading && !user) router.push("/login");
  }, [user, authLoading, router]);

  const handleDelete = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm("Are you sure you want to delete this form? This action cannot be undone.")) return;
    deleteForm.mutate(id, {
      onSettled: () => setOpenMenuId(null),
      onError: () => alert("Failed to delete form."),
    });
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

  const filteredForms = forms.filter((f) => {
    const matchesSearch =
      f.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (f.description?.toLowerCase() || "").includes(searchQuery.toLowerCase());
    if (!matchesSearch) return false;
    if (activeFilter === "public") return f.published === 1;
    return true;
  });

  if (authLoading || !user) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-paper">
        <div className="animate-spin rounded-full h-10 w-10 border-2 border-line border-t-accent"></div>
      </div>
    );
  }

  const NavItem = ({
    filter,
    icon: Icon,
    label,
  }: {
    filter: typeof activeFilter;
    icon: React.ElementType;
    label: string;
  }) => (
    <button
      onClick={() => {
        setActiveFilter(filter);
        setIsMobileMenuOpen(false);
      }}
      className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition-colors ${
        activeFilter === filter ? "text-accent bg-accent-soft" : "text-muted hover:bg-paper2"
      }`}
    >
      <Icon className="h-5 w-5" />
      {label}
    </button>
  );

  const BrandMark = () => (
    <div className="flex items-center gap-3">
      <div className="bg-accent h-9 w-9 rounded-xl flex items-center justify-center">
        <span className="font-serif italic text-white text-lg leading-none">S</span>
      </div>
      <span className="text-xl font-serif text-ink tracking-tight">SuvForm</span>
    </div>
  );

  return (
    <div className="flex min-h-screen bg-paper">
      {/* Sidebar - Desktop */}
      <aside className="w-64 bg-card border-r border-line hidden md:flex flex-col">
        <div className="p-6 border-b border-line">
          <BrandMark />
        </div>

        <nav className="flex-1 p-4 space-y-2 overflow-y-auto">
          <div className="mono-label text-muted2 mb-2 px-3">Main</div>
          <NavItem filter="all" icon={FileText} label="My Forms" />
          <NavItem filter="responses" icon={MessageSquare} label="Responses" />
          <NavItem filter="public" icon={Globe} label="Public Forms" />

          <div className="mono-label text-muted2 mt-8 mb-2 px-3">System</div>
          <button
            onClick={() => router.push("/settings")}
            className="w-full flex items-center gap-3 px-3 py-2 text-muted hover:bg-paper2 rounded-lg transition-colors"
          >
            <Settings className="h-5 w-5" />
            Settings
          </button>
          <button
            onClick={logout}
            className="w-full flex items-center gap-3 px-3 py-2 text-accent hover:bg-accent-soft rounded-lg transition-colors"
          >
            <LogOut className="h-5 w-5" />
            Sign Out
          </button>
        </nav>

        <div className="p-4 border-t border-line">
          <div className="flex items-center gap-3 p-2">
            <img
              src={user.photoURL || `https://ui-avatars.com/api/?name=${user.displayName}`}
              alt="Avatar"
              className="h-8 w-8 rounded-full border border-line"
            />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-ink truncate">{user.displayName}</p>
              <p className="text-xs text-muted truncate">{user.email}</p>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Header */}
        <header className="h-16 bg-card border-b border-line flex items-center justify-between px-4 md:px-8">
          <div className="flex items-center gap-4 flex-1 max-w-xl">
            <button
              onClick={() => setIsMobileMenuOpen(true)}
              className="md:hidden p-2 hover:bg-paper2 rounded-lg"
            >
              <Menu className="h-6 w-6 text-muted" />
            </button>
            <div className="relative w-full">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted2" />
              <input
                type="text"
                placeholder="Search forms..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-line rounded-full bg-paper text-sm text-ink placeholder:text-muted2 focus:outline-none focus:ring-2 focus:ring-accent/20 focus:border-accent transition-all"
              />
            </div>
          </div>
          <div className="flex items-center gap-4 ml-4">
            <button
              onClick={() => router.push("/form/create")}
              className="flex items-center gap-2 bg-accent hover:bg-accent-deep text-white px-4 md:px-5 py-2 rounded-full font-medium transition-all active:scale-95 text-sm"
            >
              <Plus className="h-5 w-5" />
              <span className="hidden sm:inline">Create Form</span>
              <span className="sm:hidden">Create</span>
            </button>
          </div>
        </header>

        {/* Dashboard Content */}
        <div className="flex-1 overflow-y-auto p-4 md:p-8" onClick={() => setOpenMenuId(null)}>
          <div className="max-w-6xl mx-auto">
            <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-8 gap-4">
              <div>
                <h2 className="text-3xl font-serif text-ink tracking-tight">
                  {activeFilter === "public"
                    ? "Publicly shared forms"
                    : activeFilter === "responses"
                      ? "Form submissions"
                      : "Your forms"}
                </h2>
                <p className="text-muted text-sm mt-1">
                  {activeFilter === "public"
                    ? "These forms are currently live and accepting responses."
                    : activeFilter === "responses"
                      ? "Select a form to view its submitted data and insights."
                      : "Manage and analyze your active collection forms."}
                </p>
              </div>
              <div className="mono-label text-muted bg-card px-3 py-1.5 rounded-full border border-line self-start sm:self-auto">
                Total: <span className="text-ink">{filteredForms.length}</span>
              </div>
            </div>

            {isLoading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="bg-card border border-line rounded-2xl h-48 animate-pulse"></div>
                ))}
              </div>
            ) : filteredForms.length === 0 ? (
              <div className="text-center py-20 bg-card border border-dashed border-line rounded-3xl">
                <div className="bg-accent-soft h-16 w-16 rounded-full flex items-center justify-center mx-auto mb-4">
                  <FileText className="h-8 w-8 text-accent" />
                </div>
                <h3 className="text-lg font-serif text-ink">No forms found</h3>
                <p className="text-muted max-w-xs mx-auto mt-2 text-sm px-4">
                  {searchQuery
                    ? "No results match your search."
                    : activeFilter === "public"
                      ? "You haven't published any forms yet."
                      : "Create your first form using Gemini AI to start collecting data."}
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredForms.map((form) => (
                  <div
                    key={form.id}
                    className="group bg-card border border-line rounded-2xl p-6 hover:shadow-lg hover:border-accent/40 transition-all duration-300 flex flex-col cursor-pointer relative"
                    onClick={() => {
                      if (activeFilter === "responses") {
                        router.push(`/form/${form.id}/responses`);
                      } else {
                        router.push(`/form/${form.id}/edit`);
                      }
                    }}
                  >
                    <div className="flex justify-between items-start mb-4">
                      <div
                        className={`p-2 rounded-lg ${
                          form.published ? "bg-ok-soft text-ok" : "bg-paper2 text-muted2"
                        }`}
                      >
                        {activeFilter === "responses" ? (
                          <BarChart2 className="h-6 w-6" />
                        ) : (
                          <FileText className="h-6 w-6" />
                        )}
                      </div>
                      <div className="relative">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setOpenMenuId(openMenuId === form.id ? null : form.id);
                          }}
                          className="p-1 hover:bg-paper2 rounded-md text-muted2 hover:text-ink"
                        >
                          <MoreVertical className="h-5 w-5" />
                        </button>

                        {openMenuId === form.id && (
                          <div className="absolute right-0 mt-2 w-48 bg-card border border-line rounded-xl shadow-xl z-20 overflow-hidden">
                            {form.published === 1 && (
                              <button
                                onClick={(e) => handleCopyLink(form, e)}
                                className="w-full flex items-center gap-2 px-4 py-3 text-sm text-ink hover:bg-paper2 transition-colors"
                              >
                                {copyingId === form.id ? (
                                  <Check className="h-4 w-4 text-ok" />
                                ) : (
                                  <Copy className="h-4 w-4" />
                                )}
                                {copyingId === form.id ? "Copied!" : "Copy Link"}
                              </button>
                            )}
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                router.push(`/form/${form.id}/responses`);
                              }}
                              className="w-full flex items-center gap-2 px-4 py-3 text-sm text-ink hover:bg-paper2 transition-colors"
                            >
                              <BarChart2 className="h-4 w-4" />
                              View Responses
                            </button>
                            <div className="border-t border-line"></div>
                            <button
                              onClick={(e) => handleDelete(form.id, e)}
                              className="w-full flex items-center gap-2 px-4 py-3 text-sm text-accent hover:bg-accent-soft transition-colors"
                            >
                              <Trash2 className="h-4 w-4" />
                              Delete Form
                            </button>
                          </div>
                        )}
                      </div>
                    </div>

                    <h3 className="text-lg font-serif text-ink group-hover:text-accent transition-colors truncate">
                      {form.title}
                    </h3>
                    <p className="text-sm text-muted line-clamp-2 mt-1 flex-1">
                      {form.description || "No description provided."}
                    </p>

                    <div className="mt-6 pt-4 border-t border-line2 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        {form.published ? (
                          <span className="flex items-center gap-1.5 px-2 py-1 bg-ok-soft text-ok mono-label rounded-md">
                            <span className="h-1.5 w-1.5 rounded-full bg-ok animate-pulse"></span>
                            Live
                          </span>
                        ) : (
                          <span className="px-2 py-1 bg-paper2 text-muted mono-label rounded-md">
                            Draft
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-muted2 font-medium flex items-center gap-1">
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

      {/* Mobile Sidebar Overlay */}
      {isMobileMenuOpen && (
        <div className="fixed inset-0 z-50 flex md:hidden">
          <div className="fixed inset-0 bg-ink/50" onClick={() => setIsMobileMenuOpen(false)}></div>
          <aside className="relative w-80 max-w-[80%] bg-card h-full flex flex-col shadow-2xl">
            <div className="p-6 border-b border-line flex items-center justify-between">
              <BrandMark />
              <button
                onClick={() => setIsMobileMenuOpen(false)}
                className="p-2 hover:bg-paper2 rounded-full"
              >
                <X className="h-6 w-6 text-muted2" />
              </button>
            </div>

            <nav className="flex-1 p-4 space-y-2">
              <div className="mono-label text-muted2 mb-2 px-3">Main</div>
              <NavItem filter="all" icon={FileText} label="My Forms" />
              <NavItem filter="responses" icon={MessageSquare} label="Responses" />
              <NavItem filter="public" icon={Globe} label="Public Forms" />

              <div className="mono-label text-muted2 mt-8 mb-2 px-3">System</div>
              <button
                onClick={() => router.push("/settings")}
                className="w-full flex items-center gap-3 px-3 py-2 text-muted hover:bg-paper2 rounded-lg transition-colors"
              >
                <Settings className="h-5 w-5" />
                Settings
              </button>
              <button
                onClick={logout}
                className="w-full flex items-center gap-3 px-3 py-2 text-accent hover:bg-accent-soft rounded-lg transition-colors"
              >
                <LogOut className="h-5 w-5" />
                Sign Out
              </button>
            </nav>
          </aside>
        </div>
      )}
    </div>
  );
}
