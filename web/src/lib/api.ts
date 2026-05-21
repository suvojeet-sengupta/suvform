"use client";

import { useAuth } from "@/context/AuthContext";
import { useCallback, useRef } from "react";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "https://api.suvforms.suvojeetsengupta.in";

// Simple in-memory cache
const cache: Record<string, { data: any; timestamp: number }> = {};
const CACHE_TTL = 30000; // 30 seconds

export function useApi() {
  const { getToken } = useAuth();
  const cacheRef = useRef(cache);

  const request = useCallback(async (endpoint: string, options: RequestInit = {}, useCache = false) => {
    const cacheKey = `${endpoint}-${options.method || 'GET'}`;
    
    // Check cache
    if (useCache && options.method === "GET") {
      const cached = cacheRef.current[cacheKey];
      if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
        return cached.data;
      }
    }

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

    const data = await response.json();

    // Store in cache
    if (options.method === "GET" || !options.method) {
      cacheRef.current[cacheKey] = { data, timestamp: Date.now() };
    } else {
      // Invalidate cache on mutations
      Object.keys(cacheRef.current).forEach(key => {
        if (key.includes(endpoint.split('/')[2])) { // Basic invalidation for related resources
           delete cacheRef.current[key];
        }
      });
    }

    return data;
  }, [getToken]);

  return {
    get: (endpoint: string, useCache = true) => request(endpoint, { method: "GET" }, useCache),
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
