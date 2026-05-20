// Firebase ID-token verification via Google's secure-token JWKS.
// No Admin SDK needed — we verify the JWT signature + claims manually.

export type FirebaseUser = {
  uid: string;
  email?: string;
  name?: string;
  picture?: string;
};

const JWKS_URL =
  "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";

type JwksCache = { keys: Record<string, string>; expiresAt: number };
let jwksCache: JwksCache | null = null;

async function getJwks(): Promise<Record<string, string>> {
  if (jwksCache && jwksCache.expiresAt > Date.now()) return jwksCache.keys;
  const res = await fetch(JWKS_URL);
  if (!res.ok) throw new Error(`jwks_fetch_failed_${res.status}`);
  const keys = (await res.json()) as Record<string, string>;
  // The endpoint sets Cache-Control max-age; cache for 1h in-memory.
  jwksCache = { keys, expiresAt: Date.now() + 60 * 60 * 1000 };
  return keys;
}

function base64UrlDecode(input: string): Uint8Array {
  const pad = input.length % 4 === 0 ? "" : "=".repeat(4 - (input.length % 4));
  const b64 = (input + pad).replace(/-/g, "+").replace(/_/g, "/");
  const bin = atob(b64);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

function utf8Decode(bytes: Uint8Array): string {
  return new TextDecoder().decode(bytes);
}

async function importX509PublicKey(pem: string): Promise<CryptoKey> {
  // Strip PEM headers and decode base64 → DER
  const b64 = pem
    .replace(/-----BEGIN CERTIFICATE-----/, "")
    .replace(/-----END CERTIFICATE-----/, "")
    .replace(/\s+/g, "");
  const der = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));

  // Parse the X.509 certificate to extract the SubjectPublicKeyInfo (SPKI).
  // Minimal ASN.1 walk: find the SPKI which starts with the RSA OID sequence.
  // Workers' Web Crypto can't import x509 directly, so we locate SPKI by scanning.
  // The cert is: SEQUENCE { SEQUENCE { ..., SPKI, ... } }
  // We search for the SPKI signature: 30 0d 06 09 2a 86 48 86 f7 0d 01 01 01 05 00
  // (AlgorithmIdentifier for rsaEncryption + NULL params)
  const algoMarker = new Uint8Array([
    0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00,
  ]);
  let spkiStart = -1;
  outer: for (let i = 0; i < der.length - algoMarker.length; i++) {
    for (let j = 0; j < algoMarker.length; j++) {
      if (der[i + j] !== algoMarker[j]) continue outer;
    }
    // SPKI is the SEQUENCE that *contains* this AlgorithmIdentifier.
    // Walk back to find the enclosing SEQUENCE start (0x30, length byte(s)).
    // We scan back: previous 0x30 whose length covers algoMarker.
    for (let k = i - 1; k >= 0; k--) {
      if (der[k] === 0x30) {
        // length encoding
        const lenByte = der[k + 1];
        let lenSize = 1;
        let len = lenByte;
        if (lenByte & 0x80) {
          lenSize = lenByte & 0x7f;
          len = 0;
          for (let m = 0; m < lenSize; m++) len = (len << 8) | der[k + 2 + m];
          lenSize += 1;
        }
        const headerLen = 1 + lenSize;
        if (k + headerLen <= i && k + headerLen + len >= i + algoMarker.length) {
          spkiStart = k;
          const spkiEnd = k + headerLen + len;
          const spki = der.slice(spkiStart, spkiEnd);
          return await crypto.subtle.importKey(
            "spki",
            spki,
            { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
            false,
            ["verify"],
          );
        }
      }
    }
  }
  throw new Error("spki_not_found_in_cert");
}

export async function verifyFirebaseIdToken(token: string, projectId: string): Promise<FirebaseUser> {
  // Defensive trim — CLI tools sometimes inject CRLF/whitespace when secrets are piped.
  const expectedProjectId = (projectId ?? "").trim();
  if (!expectedProjectId) throw new Error("missing_project_id_config");

  const parts = token.split(".");
  if (parts.length !== 3) throw new Error("malformed_jwt");
  const [headerB64, payloadB64, signatureB64] = parts;

  const header = JSON.parse(utf8Decode(base64UrlDecode(headerB64))) as { kid: string; alg: string };
  if (header.alg !== "RS256") throw new Error("unexpected_alg");

  const payload = JSON.parse(utf8Decode(base64UrlDecode(payloadB64))) as {
    iss: string;
    aud: string;
    sub: string;
    exp: number;
    iat: number;
    email?: string;
    name?: string;
    picture?: string;
  };

  const now = Math.floor(Date.now() / 1000);
  if (payload.exp < now) throw new Error("token_expired");
  if (payload.iat > now + 60) throw new Error("token_iat_in_future");
  if (payload.aud !== expectedProjectId) {
    throw new Error(`wrong_audience: got=${payload.aud} expected=${expectedProjectId}`);
  }
  if (payload.iss !== `https://securetoken.google.com/${expectedProjectId}`) throw new Error("wrong_issuer");
  if (!payload.sub) throw new Error("missing_sub");

  const jwks = await getJwks();
  const pem = jwks[header.kid];
  if (!pem) throw new Error("unknown_kid");

  const key = await importX509PublicKey(pem);
  const sigBytes = base64UrlDecode(signatureB64);
  const signedData = new TextEncoder().encode(`${headerB64}.${payloadB64}`);
  const ok = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", key, sigBytes, signedData);
  if (!ok) throw new Error("bad_signature");

  return {
    uid: payload.sub,
    email: payload.email,
    name: payload.name,
    picture: payload.picture,
  };
}
