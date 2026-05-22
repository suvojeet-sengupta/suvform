/**
 * Time and timezone utilities for the SuvForm backend.
 */

/**
 * Returns the current date in YYYY-MM-DD format for a given timezone.
 * Defaults to UTC if the timezone is invalid or missing.
 */
export function getLocalDay(timezone: string = "UTC"): string {
  try {
    return new Intl.DateTimeFormat("en-CA", {
      timeZone: timezone,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }).format(new Date());
  } catch {
    return new Date().toISOString().slice(0, 10);
  }
}

/**
 * Formats a timestamp (ms) into a human-readable localized string.
 */
export function formatLocalized(ms: number, timezone: string = "UTC"): string {
  try {
    return new Intl.DateTimeFormat("en-IN", {
      timeZone: timezone,
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: true,
    }).format(new Date(ms));
  } catch {
    return new Date(ms).toISOString();
  }
}
