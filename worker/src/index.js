// AppMarket download gateway (Cloudflare Worker)
// - /apks/...  : requires a valid Firebase ID token (Authorization: Bearer <token>)
// - /icons/... : public, cached

const APK_PREFIX = "apks/";
const ICON_PREFIX = "icons/";

export default {
  async fetch(request, env) {
    if (request.method !== "GET" && request.method !== "HEAD") {
      return new Response("Method not allowed", { status: 405 });
    }

    const key = decodeURIComponent(new URL(request.url).pathname.slice(1));
    const isApk = key.startsWith(APK_PREFIX);
    const isIcon = key.startsWith(ICON_PREFIX);
    if (!(isApk || isIcon) || key.includes("..")) {
      return new Response("Not found", { status: 404 });
    }

    if (isApk) {
      const token = (request.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
      if (!token) return new Response("Unauthorized", { status: 401 });
      const valid = await verifyFirebaseToken(token, env.FIREBASE_API_KEY);
      if (!valid) return new Response("Unauthorized", { status: 401 });
    }

    const object = await env.APK_BUCKET.get(key);
    if (!object) return new Response("Not found", { status: 404 });

    const headers = new Headers();
    object.writeHttpMetadata(headers);
    headers.set("etag", object.httpEtag);
    headers.set("content-length", String(object.size));

    if (isApk) {
      headers.set("content-type", "application/vnd.android.package-archive");
      headers.set("cache-control", "private, no-store");
    } else {
      headers.set("cache-control", "public, max-age=86400");
    }

    return new Response(request.method === "HEAD" ? null : object.body, { headers });
  },
};

// Firebase Auth REST lookup: returns users[] only when the ID token is valid.
async function verifyFirebaseToken(idToken, apiKey) {
  if (!apiKey) throw new Error("FIREBASE_API_KEY secret is not set");
  const res = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${apiKey}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idToken }),
    }
  );
  if (!res.ok) return false;
  const data = await res.json();
  return Array.isArray(data.users) && data.users.length > 0;
}
