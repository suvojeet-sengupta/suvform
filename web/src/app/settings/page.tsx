"use client";

export const runtime = "edge";

import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { 
  ArrowLeft, 
  User, 
  Shield, 
  Bell, 
  Database, 
  Smartphone,
  ChevronRight,
  LogOut
} from "lucide-react";

export default function SettingsPage() {
  const { user, loading: authLoading, logout } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!authLoading && !user) {
      router.push("/login");
    }
  }, [user, authLoading, router]);

  if (authLoading || !user) return null;

  const sections = [
    { 
      title: "Profile", 
      icon: User, 
      desc: "Manage your display name and email preferences",
      color: "bg-blue-50 text-blue-600"
    },
    { 
      title: "Security", 
      icon: Shield, 
      desc: "Authorized domains and session management",
      color: "bg-green-50 text-green-600"
    },
    { 
      title: "Data & Storage", 
      icon: Database, 
      desc: "Connected Cloudflare D1 database and KV usage",
      color: "bg-purple-50 text-purple-600"
    },
    { 
      title: "Mobile App", 
      icon: Smartphone, 
      desc: "Connect your Android device for push notifications",
      color: "bg-orange-50 text-orange-600"
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <header className="h-16 bg-white border-b border-gray-200 flex items-center px-8 shrink-0">
        <button 
          onClick={() => router.push("/")}
          className="p-2 hover:bg-gray-100 rounded-full transition-colors mr-4"
        >
          <ArrowLeft className="h-5 w-5 text-gray-600" />
        </button>
        <h1 className="text-xl font-bold text-gray-900">Settings</h1>
      </header>

      <main className="flex-1 p-8 overflow-y-auto">
        <div className="max-w-3xl mx-auto space-y-8">
          {/* User Profile Header */}
          <div className="bg-white p-8 rounded-3xl border border-gray-200 shadow-sm flex items-center gap-6">
            <img 
              src={user.photoURL || `https://ui-avatars.com/api/?name=${user.displayName}`} 
              alt="Avatar" 
              className="h-20 w-20 rounded-full border-4 border-gray-50 shadow-sm"
            />
            <div>
              <h2 className="text-2xl font-bold text-gray-900">{user.displayName}</h2>
              <p className="text-gray-500">{user.email}</p>
              <div className="mt-2 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                Administrator
              </div>
            </div>
          </div>

          {/* Settings Grid */}
          <div className="grid grid-cols-1 gap-4">
            {sections.map((section) => (
              <button
                key={section.title}
                className="w-full flex items-center justify-between p-6 bg-white border border-gray-200 rounded-2xl hover:border-blue-200 hover:shadow-md transition-all group"
              >
                <div className="flex items-center gap-4">
                  <div className={`p-3 rounded-xl ${section.color}`}>
                    <section.icon className="h-6 w-6" />
                  </div>
                  <div className="text-left">
                    <h3 className="font-bold text-gray-900">{section.title}</h3>
                    <p className="text-sm text-gray-500">{section.desc}</p>
                  </div>
                </div>
                <ChevronRight className="h-5 w-5 text-gray-300 group-hover:text-blue-500 transition-colors" />
              </button>
            ))}
          </div>

          {/* Danger Zone */}
          <div className="pt-8 mt-8 border-t border-gray-200">
            <button 
              onClick={logout}
              className="w-full flex items-center justify-center gap-2 p-4 bg-red-50 text-red-600 rounded-2xl font-bold hover:bg-red-100 transition-all border border-red-100"
            >
              <LogOut className="h-5 w-5" />
              Sign Out of Account
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
