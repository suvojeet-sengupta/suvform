"use client";

import { useAuth } from "@/context/AuthContext";
import { useCallback, useMemo } from "react";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "https://api.suvforms.suvojeetsengupta.in";

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message?: string,
  ) {
    super(message || code);
    this.name = "ApiError";
  }
}

/**
 * Thin authenticated fetch wrapper. Caching/dedup/retry are handled by
 * TanStack Query (see lib/queries.ts) — this just attaches the token and
 * surfaces structured errors.
 */
export function useApi() {
  const { getToken } = useAuth();

  const request = useCallback(
    async (endpoint: string, options: RequestInit = {}) => {
      const token = await getToken();
      const headers = new Headers(options.headers);
      if (token) headers.set("Authorization", `Bearer ${token}`);
      if (!(options.body instanceof FormData) && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
      }

      const response = await fetch(`${API_BASE_URL}${endpoint}`, { ...options, headers });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({} as { error?: string }));
        throw new ApiError(response.status, errorData.error || "request_failed");
      }

      // 204 / empty bodies
      if (response.status === 204) return null;
      return response.json().catch(() => null);
    },
    [getToken],
  );

  return useMemo(
    () => ({
      get: (endpoint: string) => request(endpoint, { method: "GET" }),
      post: (endpoint: string, body?: unknown) =>
        request(endpoint, { method: "POST", body: body ? JSON.stringify(body) : undefined }),
      put: (endpoint: string, body?: unknown) =>
        request(endpoint, { method: "PUT", body: body ? JSON.stringify(body) : undefined }),
      delete: (endpoint: string) => request(endpoint, { method: "DELETE" }),
    }),
    [request],
  );
}
