// AppMarket Worker (Cloudflare)
// - GET  /apks/...        : requires a valid Firebase ID token (Authorization: Bearer <token>)
// - GET  /icons/...       : public, cached
// - GET  /screenshots/... : public, cached
// - GET  /app/{id}        : share page that opens appmarket://app/{id}
// - POST /upload-apk      : admin only. Raw APK bytes. Headers: X-Package-Name, X-Version-Code
// - POST /upload-icon     : admin only. Raw image bytes. Header: X-Package-Name
// - POST /upload-screenshot : admin only. Raw image bytes. Headers: X-Package-Name, X-Shot-Id
// - POST /upload-slider   : admin only. Raw image bytes. Header: X-Slider-Id
// - POST /delete-slider   : admin only. Header: X-Slider-Id
// - GET  /sliders/...     : public, cached
// - POST /notify          : admin only. Body: { "title": "...", "body": "..." }

const APK_PREFIX = "apks/";
const ICON_PREFIX = "icons/";
const SHOT_PREFIX = "screenshots/";
const NOTIFY_TOPIC = "all_users";
const MAX_TITLE = 65;
const MAX_BODY = 240;
const MAX_APK_BYTES = 50 * 1024 * 1024;
const MAX_ICON_BYTES = 2 * 1024 * 1024;
const MAX_SHOT_BYTES = 3 * 1024 * 1024;
const BANNER_PREFIX = "banners/";
const MAX_BANNER_BYTES = 3 * 1024 * 1024;
const SLIDER_PREFIX = "sliders/";
const MAX_SLIDER_BYTES = 3 * 1024 * 1024;

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
    if (request.method === "POST" && url.pathname === "/upload-banner") {
      return handleUploadBanner(request, env, url.origin);
    }
    if (request.method === "POST" && url.pathname === "/delete-banner") {
      return handleDeleteBanner(request, env);
    }
    if (request.method === "POST" && url.pathname === "/upload-screenshot") {
      return handleUploadScreenshot(request, env, url.origin);
    }
    if (request.method === "POST" && url.pathname === "/upload-slider") {
      return handleUploadSlider(request, env, url.origin);
    }
    if (request.method === "POST" && url.pathname === "/delete-slider") {
      return handleDeleteSlider(request, env);
    }
    if (request.method === "GET" && url.pathname.startsWith("/app/")) {
      return appLinkPage(url.pathname.slice("/app/".length));
    }

    if (request.method !== "GET" && request.method !== "HEAD") {
      return new Response("Method not allowed", { status: 405 });
    }

    const key = decodeURIComponent(url.pathname.slice(1));
    const isApk = key.startsWith(APK_PREFIX);
    const isIcon = key.startsWith(ICON_PREFIX);
    const isShot = key.startsWith(SHOT_PREFIX);
    const isBanner = key.startsWith(BANNER_PREFIX);
    const isSlider = key.startsWith(SLIDER_PREFIX);
    if (!(isApk || isIcon || isShot || isBanner || isSlider) || key.includes("..")) {
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

// ---------- Share page ----------

// A small web page. It opens the app with appmarket://app/{id}.
// Only safe id characters are accepted, so the page cannot be used for injection.
function appLinkPage(rawId) {
  const id = decodeURIComponent(rawId);
  if (!/^[A-Za-z0-9_-]{1,64}$/.test(id)) {
    return new Response("Not found", { status: 404 });
  }
  const deepLink = `appmarket://app/${id}`;
  const html = `<!doctype html>
<html lang="bn"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>AppMarket</title></head>
<body style="font-family:sans-serif;text-align:center;padding:48px 16px;background:#0B1120;color:#F1F5F9">
<h2>AppMarket</h2>
<p>অ্যাপটি দেখতে নিচের বাটনে চাপুন।</p>
<p><a href="${deepLink}" style="display:inline-block;padding:14px 26px;border-radius:14px;background:#4F46E5;color:#fff;text-decoration:none">AppMarket-এ খুলুন</a></p>
<p style="color:#94A3B8;font-size:13px">AppMarket ইনস্টল না থাকলে আগে সেটি ইনস্টল করুন।</p>
<script>setTimeout(function(){ location.href = "${deepLink}"; }, 400);</script>
</body></html>`;
  return new Response(html, {
    headers: { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "public, max-age=3600" },
  });
}

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

function validPackage(pkg) {
  return /^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$/.test(pkg);
}

// Recognises PNG, JPEG and WebP by their first bytes.
function imageType(bytes) {
  if (bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47) {
    return { ext: "png", type: "image/png" };
  }
  if (bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
    return { ext: "jpg", type: "image/jpeg" };
  }
  if (
    bytes[0] === 0x52 && bytes[1] === 0x49 && bytes[2] === 0x46 && bytes[3] === 0x46 &&
    bytes[8] === 0x57 && bytes[9] === 0x45 && bytes[10] === 0x42 && bytes[11] === 0x50
  ) {
    return { ext: "webp", type: "image/webp" };
  }
  return null;
}

// ---------- APK upload (admin only) ----------

async function handleUploadApk(request, env) {
  const denied = await requireAdmin(request, env);
  if (denied) return denied;

  const pkg = (request.headers.get("X-Package-Name") || "").trim();
  if (!validPackage(pkg)) return json({ error: "invalid package name" }, 400);

  const versionCode = Number(request.headers.get("X-Version-Code"));
  if (!Number.isInteger(versionCode) || versionCode <= 0) {
    return json({ error: "invalid version code" }, 400);
  }

  const declared = Number(request.headers.get("content-length") || "0");
  if (declared > MAX_APK_BYTES) return json({ error: "APK is larger than 50 MB" }, 413);

  const bytes = new Uint8Array(await request.arrayBuffer());
  if (bytes.length === 0 || bytes.length > MAX_APK_BYTES) {
    return json({ error: "APK is empty or larger than 50 MB" }, 413);
  }

  // An APK is a ZIP archive, so it must start with "PK\x03\x04".
  const isZip = bytes[0] === 0x50 && bytes[1] === 0x4b && bytes[2] === 0x03 && bytes[3] === 0x04;
  if (!isZip) return json({ error: "file is not a valid APK" }, 400);

  const key = `${APK_PREFIX}${pkg}-${versionCode}.apk`;
  await env.APK_BUCKET.put(key, bytes, {
    httpMetadata: { contentType: "application/vnd.android.package-archive" },
  });

  return json({ ok: true, apkPath: key, sizeBytes: bytes.length }, 200);
}

// ---------- Icon upload (admin only) ----------

async function handleUploadIcon(request, env, origin) {
  const denied = await requireAdmin(request, env);
  if (denied) return denied;

  const pkg = (request.headers.get("X-Package-Name") || "").trim();
  if (!validPackage(pkg)) return json({ error: "invalid package name" }, 400);

  const declared = Number(request.headers.get("content-length") || "0");
  if (declared > MAX_ICON_BYTES) return json({ error: "icon is larger than 2 MB" }, 413);

  const bytes = new Uint8Array(await request.arrayBuffer());
  if (bytes.length === 0 || bytes.length > MAX_ICON_BYTES) {
    return json({ error: "icon is empty or larger than 2 MB" }, 413);
  }

  const img = imageType(bytes);
  if (!img) return json({ error: "icon must be PNG, JPEG or WebP" }, 400);

  // Remove older icons with a different extension so only one remains per app.
  for (const other of ["png", "jpg", "webp"]) {
    if (other !== img.ext) await env.APK_BUCKET.delete(`${ICON_PREFIX}${pkg}.${other}`);
  }

  const key = `${ICON_PREFIX}${pkg}.${img.ext}`;
  await env.APK_BUCKET.put(key, bytes, { httpMetadata: { contentType: img.type } });

  return json({ ok: true, iconUrl: `${origin}/${key}` }, 200);
}

// ---------- Screenshot upload (admin only) ----------

async function handleUploadScreenshot(request, env, origin) {
  const denied = await requireAdmin(request, env);
  if (denied) return denied;

  const pkg = (request.headers.get("X-Package-Name") || "").trim();
  if (!validPackage(pkg)) return json({ error: "invalid package name" }, 400);

  // A random id from the app. Only letters and digits are allowed.
  const shotId = (request.headers.get("X-Shot-Id") || "").trim();
  if (!/^[A-Za-z0-9]{1,32}$/.test(shotId)) return json({ error: "invalid screenshot id" }, 400);

  const declared = Number(request.headers.get("content-length") || "0");
  if (declared > MAX_SHOT_BYTES) return json({ error: "screenshot is larger than 3 MB" }, 413);

  const bytes = new Uint8Array(await request.arrayBuffer());
  if (bytes.length === 0 || bytes.length > MAX_SHOT_BYTES) {
    return json({ error: "screenshot is empty or larger than 3 MB" }, 413);
  }

  const img = imageType(bytes);
  if (!img) return json({ error: "screenshot must be PNG, JPEG or WebP" }, 400);

  const key = `${SHOT_PREFIX}${pkg}/${shotId}.${img.ext}`;
  await env.APK_BUCKET.put(key, bytes, { httpMetadata: { contentType: img.type } });

  return json({ ok: true, screenshotUrl: `${origin}/${key}` }, 200);
}

// ---------- Banner upload and delete (admin only) ----------

// One banner per app. A new upload replaces the old file (same name), even with a different extension.
async function handleUploadBanner(request, env, origin) {
  const denied = await requireAdmin(request, env);
  if (denied) return denied;

  const pkg = (request.headers.get("X-Package-Name") || "").trim();
  if (!validPackage(pkg)) return json({ error: "invalid package name" }, 400);

  const declared = Number(request.headers.get("content-length") || "0");
  if (declared > MAX_BANNER_BYTES) return json({ error: "banner is larger than 3 MB" }, 413);

  const bytes = new Uint8Array(await request.arrayBuffer());
  if (bytes.length === 0 || bytes.length > MAX_BANNER_BYTES) {
    return json({ error: "banner is empty or larger than 3 MB" }, 413);
  }

  const img = imageType(bytes);
  if (!img) return json({ error: "banner must be PNG, JPEG or WebP" }, 400);

  for (const other of ["png", "jpg", "webp"]) {
    if (other !== img.ext) await env.APK_BUCKET.delete(`${BANNER_PREFIX}${pkg}.${other}`);
  }

  const key = `${BANNER_PREFIX}${pkg}.${img.ext}`;
  await env.APK_BUCKET.put(key, bytes, { httpMetadata: { contentType: img.type } });

  return json({ ok: true, bannerUrl: `${origin}/${key}` }, 200);
}

async function handleDeleteBanner(request, env) {
  const denied = await requireAdmin(request, env);
  if (denied) return denied;

  const pkg = (request.headers.get("X-Package-Name") || "").trim();
  if (!validPackage(pkg)) return json({ error: "invalid package name" }, 400);

  for (const ext of ["png", "jpg", "webp"]) {
    await env.APK_BUCKET.delete(`${BANNER_PREFIX}${pkg}.${ext}`);
  }
  return json({ ok: true }, 200);
}

// ---------- Slider banner upload and delete (admin only) ----------

// Slider banners are independent of apps. Each upload gets its own random id, so no file is overwritten.
async function handleUploadSlider(request, env, origin) {
  const denied = await requireAdmin(request, env);
  if (denied) return denied;

  const sliderId = (request.headers.get("X-Slider-Id") || "").trim();
  if (!/^[A-Za-z0-9]{1,32}$/.test(sliderId)) return json({ error: "invalid slider id" }, 400);

  const declared = Number(request.headers.get("content-length") || "0");
  if (declared > MAX_SLIDER_BYTES) return json({ error: "slider image is larger than 3 MB" }, 413);

  const bytes = new Uint8Array(await request.arrayBuffer());
  if (bytes.length === 0 || bytes.length > MAX_SLIDER_BYTES) {
    return json({ error: "slider image is empty or larger than 3 MB" }, 413);
  }

  const img = imageType(bytes);
  if (!img) return json({ error: "slider image must be PNG, JPEG or WebP" }, 400);

  const key = `${SLIDER_PREFIX}${sliderId}.${img.ext}`;
  await env.APK_BUCKET.put(key, bytes, { httpMetadata: { contentType: img.type } });

  return json({ ok: true, sliderUrl: `${origin}/${key}` }, 200);
}

async function handleDeleteSlider(request, env) {
  const denied = await requireAdmin(request, env);
  if (denied) return denied;

  const sliderId = (request.headers.get("X-Slider-Id") || "").trim();
  if (!/^[A-Za-z0-9]{1,32}$/.test(sliderId)) return json({ error: "invalid slider id" }, 400);

  for (const ext of ["png", "jpg", "webp"]) {
    await env.APK_BUCKET.delete(`${SLIDER_PREFIX}${sliderId}.${ext}`);
  }
  return json({ ok: true }, 200);
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
