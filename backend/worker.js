const json = (data, status = 200) => new Response(JSON.stringify(data), { status, headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" } });
const now = () => Math.floor(Date.now() / 1000);
const hex = (buf) => Array.from(new Uint8Array(buf), b => b.toString(16).padStart(2, "0")).join("");
async function sha(value) { return hex(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value))); }
function randomToken(bytes = 32) {
  const a = new Uint8Array(bytes); crypto.getRandomValues(a);
  return btoa(String.fromCharCode(...a)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}
async function bodyJson(request) {
  try { return await request.json(); } catch (_) { return null; }
}
async function authorizedDevice(request, env) {
  const header = request.headers.get("authorization") || "";
  if (!header.startsWith("Bearer ")) return null;
  const hash = await sha(header.slice(7));
  return await env.DB.prepare("SELECT device_id FROM devices WHERE token_hash = ? AND revoked_at IS NULL").bind(hash).first();
}
async function admin(request, env) {
  const expected = env.ADMIN_SECRET;
  const got = (request.headers.get("authorization") || "").replace(/^Bearer\s+/i, "");
  if (!expected || !got || got.length !== expected.length) return false;
  const a = new TextEncoder().encode(got), b = new TextEncoder().encode(expected);
  let diff = 0; for (let i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
  return diff === 0;
}
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") return json({ ok: true, service: "YW app store API" });
    if (request.method === "POST" && url.pathname === "/api/redeem") {
      const input = await bodyJson(request);
      const code = String(input?.code || "").trim().toUpperCase();
      const deviceId = String(input?.deviceId || "");
      if (!/^[A-Z0-9-]{6,80}$/.test(code) || !/^[a-zA-Z0-9-]{8,80}$/.test(deviceId)) return json({ error: "Invalid code or device ID." }, 400);
      const codeHash = await sha(code);
      const consumed = await env.DB.prepare("UPDATE authorization_codes SET used_at = ? WHERE code_hash = ? AND used_at IS NULL AND expires_at > ?").bind(now(), codeHash, now()).run();
      if (!consumed.meta || consumed.meta.changes !== 1) return json({ error: "Code is invalid, expired, or already used." }, 403);
      const token = randomToken();
      const tokenHash = await sha(token);
      try {
        await env.DB.prepare("INSERT INTO devices (device_id, token_hash, created_at, revoked_at) VALUES (?, ?, ?, NULL) ON CONFLICT(device_id) DO UPDATE SET token_hash=excluded.token_hash, created_at=excluded.created_at, revoked_at=NULL")
          .bind(deviceId, tokenHash, now()).run();
      } catch (_) { return json({ error: "Could not register this device." }, 500); }
      return json({ token, deviceId });
    }
    if (request.method === "GET" && url.pathname === "/api/catalog") {
      const device = await authorizedDevice(request, env);
      if (!device) return json({ error: "Device authorization required." }, 401);
      const result = await env.DB.prepare("SELECT id, name, description, version, size_bytes AS sizeBytes, package_name AS packageName, apk_url AS apkUrl, icon_url AS iconUrl FROM apps WHERE enabled = 1 ORDER BY name COLLATE NOCASE").all();
      return json({ apps: result.results || [] });
    }
    if (request.method === "POST" && url.pathname === "/api/report") {
      const device = await authorizedDevice(request, env);
      if (!device) return json({ error: "Device authorization required." }, 401);
      const input = await bodyJson(request);
      const packageName = String(input?.packageName || "");
      const version = String(input?.version || "");
      const status = String(input?.status || "installed");
      if (!/^[A-Za-z0-9_.]{3,200}$/.test(packageName) || version.length > 100 || !["downloaded", "installed", "failed"].includes(status)) return json({ error: "Invalid report." }, 400);
      await env.DB.prepare("INSERT INTO install_reports (device_id, package_name, version, status, reported_at) VALUES (?, ?, ?, ?, ?)")
        .bind(device.device_id, packageName, version, status, now()).run();
      return json({ ok: true });
    }
    if (url.pathname.startsWith("/admin/")) {
      if (!(await admin(request, env))) return json({ error: "Admin authorization required." }, 401);
      if (request.method === "POST" && url.pathname === "/admin/codes") {
        const input = await bodyJson(request) || {};
        const count = Math.max(1, Math.min(50, Number(input.count) || 1));
        const ttlHours = Math.max(1, Math.min(720, Number(input.ttlHours) || 168));
        const codes = [];
        for (let i = 0; i < count; i++) {
          const raw = randomToken(9).toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 12);
          const code = raw.slice(0, 4) + "-" + raw.slice(4, 8) + "-" + raw.slice(8, 12);
          await env.DB.prepare("INSERT INTO authorization_codes (code_hash, expires_at, used_at, created_at) VALUES (?, ?, NULL, ?)")
            .bind(await sha(code), now() + ttlHours * 3600, now()).run();
          codes.push(code);
        }
        return json({ codes, expiresInHours: ttlHours });
      }
      if (request.method === "PUT" && url.pathname === "/admin/apps") {
        const input = await bodyJson(request);
        if (!input || !Array.isArray(input.apps) || input.apps.length > 200) return json({ error: "Expected { apps: [...] } (maximum 200)." }, 400);
        const statements = [env.DB.prepare("DELETE FROM apps")];
        for (const app of input.apps) {
          const id = String(app.id || "");
          const name = String(app.name || "");
          const version = String(app.version || "");
          const apkUrl = String(app.apkUrl || "");
          const packageName = String(app.packageName || "");
          if (!id || id.length > 100 || !name || !version || !apkUrl.startsWith("https://") || !/^[A-Za-z0-9_.]{3,200}$/.test(packageName)) return json({ error: "Each app needs id, name, version, packageName, and an HTTPS apkUrl." }, 400);
          statements.push(env.DB.prepare("INSERT INTO apps (id, name, description, version, size_bytes, package_name, apk_url, icon_url, enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
            .bind(id, name, String(app.description || ""), version, Math.max(0, Number(app.sizeBytes) || 0), packageName, apkUrl, String(app.iconUrl || ""), app.enabled === false ? 0 : 1));
        }
        await env.DB.batch(statements);
        return json({ ok: true, count: input.apps.length });
      }
      if (request.method === "GET" && url.pathname === "/admin/stats") {
        const [devices, reports, codes] = await Promise.all([
          env.DB.prepare("SELECT COUNT(*) AS total FROM devices WHERE revoked_at IS NULL").first(),
          env.DB.prepare("SELECT package_name AS packageName, version, status, COUNT(*) AS count, COUNT(DISTINCT device_id) AS uniqueDevices FROM install_reports GROUP BY package_name, version, status ORDER BY package_name, version").all(),
          env.DB.prepare("SELECT COUNT(*) AS unused FROM authorization_codes WHERE used_at IS NULL AND expires_at > ?").bind(now()).first()
        ]);
        return json({ authorizedDevices: devices?.total || 0, unusedCodes: codes?.unused || 0, reports: reports.results || [] });
      }
      if (request.method === "POST" && url.pathname.startsWith("/admin/revoke/")) {
        const deviceId = decodeURIComponent(url.pathname.slice("/admin/revoke/".length));
        await env.DB.prepare("UPDATE devices SET revoked_at = ? WHERE device_id = ?").bind(now(), deviceId).run();
        return json({ ok: true });
      }
      return json({ error: "Unknown admin endpoint." }, 404);
    }
    return json({ error: "Not found." }, 404);
  }
};
