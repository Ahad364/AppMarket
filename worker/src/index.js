// AppMarket Worker (Cloudflare)
// - GET  /apks/...      : requires a valid Firebase ID token (Authorization: Bearer <token>)
// - GET  /icons/...     : public, cached
// - POST /upload-apk    : admin only. Raw APK bytes in the body.
//                         Headers: X-Package-Name, X-Version-Code
//                         Returns: { ok, apkPath, sizeBytes }
// - POST /notify        : admin only. Sends an FCM push to topic "all_users".
//                         Body: { "title": "...", "body": "..." }

const APK_PREFIX = "apks/";
const ICON_PREFIX = "icons/";
const NOTIFY_TOPIC = "all_users";
const MAX_TITLE = 65;
const MAX_BODY = 240;
const MAX_APK_BYTES = 50 * 1024 * 1024;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "POST" && url.pathname === "/notify") {
      return handleNotify(request, env);
    }
    if (request.method === "POST" && url.pathname === "/upload-apk") {
      return handleUploadApk(request, env);
    }
    if (request.method === "POST" && url.pathname === "/upload-icon") {
      return handleUploadIcon(request, env, url.origin);
    }

    if (request.method !== "GET" && request.method !== "HEAD") {
      return new Response("Method not allowed", { status: 405 });
    }

    const key = decodeURIComponent(url.pathname.slice(1));
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

// ---------- Admin check (shared) ----------

// Returns a Response when the caller is NOT an admin, or null when they are.
async function requireAdmin(request, env) {
  const idToken = (request.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
  if (!idToken) return json({ error: "Unauthorized" }, 401);

  let uid;
  let isAdmin;
  try {
    uid = await getUid(idToken, env.FIREBASE_API_KEY);
    if (!uid) return json({ error: "Unauthorized" }, 401);
    isAdmin = await isAdminUid(uid, idToken, env.FIREBASE_DB_URL);
  } catch (e) {
    return json({ error: e.message }, 500);
  }
  if (!isAdmin) return json({ error: "Forbidden" }, 403);
  return null;
}

// ---------- APK upload (admin only) ----------

async function handleUploadApk(request, env) {
  const denied = await requireAdmin(request, env);
  if (denied) return denied;

  const pkg = (request.headers.get("X-Package-Name") || "").trim();
  if (!/^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$/.test(pkg)) {
    return json({ error: "invalid package name" }, 400);
  }

  const versionCode = Number(request.headers.get("X-Version-Code"));
  if (!Number.isInteger(versionCode) || versionCode <= 0) {
    return json({ error: "invalid version code" }, 400);
  }

  const declared = Number(request.headers.get("content-length") || "0");
  if (declared > MAX_APK_BYTES) {
    return json({ error: "APK is larger than 50 MB" }, 413);
  }

  const bytes = new Uint8Array(await request.arrayBuffer());
  if (bytes.length === 0 || bytes.length > MAX_APK_BYTES) {
    return json({ error: "APK is empty or larger than 50 MB" }, 413);
  }

  // An APK is a ZIP archive, so it must start with "PK\x03\x04".
  const isZip = bytes[0] === 0x50 && bytes[1] === 0x4b && bytes[2] === 0x03 && bytes[3] === 0x04;
  if (!isZip) {
    return json({ error: "file is not a valid APK" }, 400);
  }

  const key = `${APK_PREFIX}${pkg}-${versionCode}.apk`;
  await env.APK_BUCKET.put(key, bytes, {
    httpMetadata: { contentType: "application/vnd.android.package-archive" },
  });

  return json({ ok: true, apkPath: key, sizeBytes: bytes.length }, 200);
}

// ---------- Icon upload (admin only) ----------

const MAX_ICON_BYTES = 2 * 1024 * 1024;

async function handleUploadIcon(request, env, origin) {
  const denied = await requireAdmin(request, env);
  if (denied) return denied;

  const pkg = (request.headers.get("X-Package-Name") || "").trim();
  if (!/^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$/.test(pkg)) {
    return json({ error: "invalid package name" }, 400);
  }

  const declared = Number(request.headers.get("content-length") || "0");
  if (declared > MAX_ICON_BYTES) {
    return json({ error: "icon is larger than 2 MB" }, 413);
  }

  const bytes = new Uint8Array(await request.arrayBuffer());
  if (bytes.length === 0 || bytes.length > MAX_ICON_BYTES) {
    return json({ error: "icon is empty or larger than 2 MB" }, 413);
  }

  // Check the file signature, not the header the client sent.
  let ext;
  let type;
  if (bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47) {
    ext = "png"; type = "image/png";
  } else if (bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
    ext = "jpg"; type = "image/jpeg";
  } else if (
    bytes[0] === 0x52 && bytes[1] === 0x49 && bytes[2] === 0x46 && bytes[3] === 0x46 &&
    bytes[8] === 0x57 && bytes[9] === 0x45 && bytes[10] === 0x42 && bytes[11] === 0x50
  ) {
    ext = "webp"; type = "image/webp";
  } else {
    return json({ error: "icon must be PNG, JPEG or WebP" }, 400);
  }

  // Remove older icons with a different extension so only one remains per app.
  for (const other of ["png", "jpg", "webp"]) {
    if (other !== ext) await env.APK_BUCKET.delete(`${ICON_PREFIX}${pkg}.${other}`);
  }

  const key = `${ICON_PREFIX}${pkg}.${ext}`;
  await env.APK_BUCKET.put(key, bytes, { httpMetadata: { contentType: type } });

  return json({ ok: true, iconUrl: `${origin}/${key}` }, 200);
}

// ---------- Notifications (admin only) ----------

async function handleNotify(request, env) {
  const denied = await requireAdmin(request, env);
  if (denied) return denied;

  let payload;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const title = String(payload.title ?? "").trim();
  const body = String(payload.body ?? "").trim();
  if (!title || !body) return json({ error: "title and body are required" }, 400);
  if (title.length > MAX_TITLE || body.length > MAX_BODY) {
    return json({ error: "title or body is too long" }, 400);
  }

  try {
    const sa = JSON.parse(env.FCM_SERVICE_ACCOUNT);
    const accessToken = await getGoogleAccessToken(sa);
    const res = await fetch(
      `https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            topic: NOTIFY_TOPIC,
            notification: { title, body },
            android: { priority: "high" },
          },
        }),
      }
    );
    if (!res.ok) {
      return json({ error: "FCM send failed", detail: await res.text() }, 502);
    }
    return json({ ok: true }, 200);
  } catch (e) {
    return json({ error: e.message }, 500);
  }
}

// Returns the uid for a valid Firebase ID token, or null.
async function getUid(idToken, apiKey) {
  if (!apiKey) throw new Error("FIREBASE_API_KEY secret is not set");
  const res = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${apiKey}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idToken }),
    }
  );
  if (!res.ok) return null;
  const data = await res.json();
  return data.users?.[0]?.localId ?? null;
}

// Reads admins/{uid} with the user's own token, so the database rules decide access.
async function isAdminUid(uid, idToken, dbUrl) {
  if (!dbUrl) throw new Error("FIREBASE_DB_URL variable is not set");
  const res = await fetch(
    `${dbUrl}/admins/${encodeURIComponent(uid)}.json?auth=${encodeURIComponent(idToken)}`
  );
  if (!res.ok) return false;
  return (await res.json()) === true;
}

// Signs a JWT with the service account key and exchanges it for a Google access token.
async function getGoogleAccessToken(sa) {
  const now = Math.floor(Date.now() / 1000);
  const header = base64UrlFromBytes(new TextEncoder().encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const claims = base64UrlFromBytes(
    new TextEncoder().encode(
      JSON.stringify({
        iss: sa.client_email,
        scope: "https://www.googleapis.com/auth/firebase.messaging",
        aud: "https://oauth2.googleapis.com/token",
        iat: now,
        exp: now + 3600,
      })
    )
  );
  const unsigned = `${header}.${claims}`;

  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToDer(sa.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned)
  );
  const jwt = `${unsigned}.${base64UrlFromBytes(new Uint8Array(signature))}`;

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });
  if (!res.ok) throw new Error("Google OAuth failed: " + (await res.text()));
  return (await res.json()).access_token;
}

function pemToDer(pem) {
  const b64 = pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes.buffer;
}

function base64UrlFromBytes(bytes) {
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/=+$/, "").replace(/\+/g, "-").replace(/\//g, "_");
}

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

// ---------- Downloads ----------

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
