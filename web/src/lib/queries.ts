"use client";

import { useApi } from "@/lib/api";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseQueryOptions,
} from "@tanstack/react-query";

/**
 * Centralized data hooks. Components use these instead of calling the API
 * directly — TanStack Query gives request dedup, stale-while-revalidate,
 * retries, and cache invalidation on mutations for free.
 */

export const queryKeys = {
  forms: ["forms"] as const,
  form: (id: string) => ["form", id] as const,
  responses: (id: string) => ["responses", id] as const,
  insights: (id: string) => ["insights", id] as const,
};

// ---- Queries ----

export function useForms(enabled = true) {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.forms,
    queryFn: () => api.get("/v1/forms"),
    enabled,
  });
}

export function useForm(id: string | null, options?: Partial<UseQueryOptions>) {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.form(id ?? ""),
    queryFn: () => api.get(`/v1/forms/${id}`),
    enabled: !!id,
    ...(options as object),
  });
}

export function useResponses(id: string | null, limit = 50, offset = 0) {
  const api = useApi();
  return useQuery({
    queryKey: [...queryKeys.responses(id ?? ""), limit, offset],
    queryFn: () => api.get(`/v1/forms/${id}/responses?limit=${limit}&offset=${offset}`),
    enabled: !!id,
  });
}

// ---- Mutations ----

export function useCreateForm() {
  const api = useApi();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: unknown) => api.post("/v1/forms", body),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.forms }),
  });
}

export function useUpdateForm(id: string) {
  const api = useApi();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: unknown) => api.put(`/v1/forms/${id}`, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.form(id) });
      qc.invalidateQueries({ queryKey: queryKeys.forms });
    },
  });
}

export function useDeleteForm() {
  const api = useApi();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.delete(`/v1/forms/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.forms }),
  });
}

export function usePublishForm() {
  const api = useApi();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, publish }: { id: string; publish: boolean }) =>
      api.post(`/v1/forms/${id}/${publish ? "publish" : "unpublish"}`),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: queryKeys.form(id) });
      qc.invalidateQueries({ queryKey: queryKeys.forms });
    },
  });
}

export function useGenerateForm() {
  const api = useApi();
  return useMutation({
    mutationFn: (body: { prompt: string; locale?: string }) =>
      api.post("/v1/ai/generate-form", body),
  });
}

export function useInsights(id: string) {
  const api = useApi();
  return useMutation({
    mutationFn: () => api.post(`/v1/forms/${id}/insights`),
  });
}
