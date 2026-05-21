"use client";

import { useAuth } from "@/context/AuthContext";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "https://api.suvforms.suvojeetsengupta.in";

export function useApi() {
  const { getToken } = useAuth();

  async function request(endpoint: string, options: RequestInit = {}) {
    const token = await getToken();
    
    const headers = new Headers(options.headers);
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
    if (!(options.body instanceof FormData) && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || "Request failed");
    }

    return response.json();
  }

  return {
    get: (endpoint: string) => request(endpoint, { method: "GET" }),
    post: (endpoint: string, body?: any) => 
      request(endpoint, { 
        method: "POST", 
        body: body ? JSON.stringify(body) : undefined 
      }),
    put: (endpoint: string, body?: any) => 
      request(endpoint, { 
        method: "PUT", 
        body: body ? JSON.stringify(body) : undefined 
      }),
    delete: (endpoint: string) => request(endpoint, { method: "DELETE" }),
  };
}
