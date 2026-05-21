export function safeParse<T>(s: string, fallback: T): T {
  try {
    return JSON.parse(s) as T;
  } catch {
    return fallback;
  }
}

export async function sha256Short(input: string): Promise<string> {
  const buf = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(input));
  const arr = Array.from(new Uint8Array(buf)).slice(0, 8);
  return arr.map((b) => b.toString(16).padStart(2, "0")).join("");
}

export function makeSlug(len = 8): string {
  const alphabet = "23456789abcdefghjkmnpqrstuvwxyz"; // omit confusing chars (0/o, 1/l, i)
  let s = "";
  const bytes = new Uint8Array(len);
  crypto.getRandomValues(bytes);
  for (let i = 0; i < len; i++) s += alphabet[bytes[i] % alphabet.length];
  return s;
}
