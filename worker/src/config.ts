/**
 * Central configuration: all tunable limits live here instead of being scattered
 * as magic numbers across routes. Adjust here and every call site stays in sync.
 */
export const CONFIG = {
  // --- Public form submissions ---
  /** Max submissions per IP per form, per hour. */
  PUBLIC_SUBMIT_PER_HOUR: 10,
  /** Reject request bodies larger than this (bytes) to prevent storage abuse. */
  MAX_BODY_BYTES: 64 * 1024,
  /** Max length of a single text/long-text/date answer. */
  MAX_TEXT_LEN: 5000,

  // --- AI generation ---
  /** Per-user AI form generations per day. */
  AI_DAILY_QUOTA: 50,
  /** Prompt length bounds for AI form generation. */
  PROMPT_MIN_LEN: 10,
  PROMPT_MAX_LEN: 3000,
  /** How many recent responses to feed the insights summarizer. */
  INSIGHTS_SAMPLE_SIZE: 50,

  // --- Forms ---
  FORMS_LIST_LIMIT: 100,
  TITLE_MAX_LEN: 200,
  DESCRIPTION_MAX_LEN: 2000,
  MAX_FIELDS: 200,
  MAX_CALCULATIONS: 50,
  /** Public share slug length and attempts to avoid collisions. */
  SLUG_LEN: 8,
  SLUG_MAX_ATTEMPTS: 5,

  // --- Responses pagination ---
  RESPONSES_PAGE_DEFAULT: 50,
  RESPONSES_PAGE_MAX: 200,

  // --- Auth ---
  /** In-memory JWKS cache lifetime (ms). */
  JWKS_CACHE_MS: 60 * 60 * 1000,
  /** Allowed clock skew (seconds) for iat "not in the future" check. */
  CLOCK_SKEW_SECONDS: 60,

  // --- Caching / TTLs ---
  /** Public form HTML cache (seconds). */
  PUBLIC_FORM_CACHE_SECONDS: 60,
  /** TTL for hourly rate-limit / daily-quota KV keys (seconds). */
  RATE_LIMIT_TTL_SECONDS: 3600,
  AI_QUOTA_TTL_SECONDS: 86400,
} as const;
