import { createClient } from "npm:@supabase/supabase-js@2.95.0";
import postgres from "npm:postgres@3.4.7";

const ADMIN_EMAIL = "relaxmusic9000@gmail.com";
const CHANNELS = new Set([
  "9e902951-be47-4530-90e7-f150175e6090",
  "3f44bb3c-f801-4b50-b138-a392e52e639e",
  "669de648-ae31-4806-a15a-aaa67cd200a1",
  "5cfd2662-307b-4ca7-8b67-89c11f2d46b7"
]);

function allowedOrigin(origin: string | null): string | null {
  if (!origin) return null;
  if (origin === "https://rami-storage.netlify.app") return origin;
  if (/^https:\/\/deploy-preview-\d+--rami-storage\.netlify\.app$/.test(origin)) return origin;
  return null;
}
function headers(origin: string | null) {
  const result: Record<string, string> = {
    "Content-Type": "application/json", "Cache-Control": "no-store", "Vary": "Origin",
    "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
    "Access-Control-Allow-Methods": "POST, OPTIONS"
  };
  if (origin) result["Access-Control-Allow-Origin"] = origin;
  return result;
}
function json(body: Record<string, unknown>, status: number, origin: string | null) {
  return new Response(JSON.stringify(body), { status, headers: headers(origin) });
}
function decodeBase64Url(value: string): Uint8Array {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const binary = atob(normalized + "=".repeat((4 - normalized.length % 4) % 4));
  return Uint8Array.from(binary, (char) => char.charCodeAt(0));
}
async function encryptionKey(secret: string): Promise<CryptoKey> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(secret));
  return crypto.subtle.importKey("raw", digest, "AES-GCM", false, ["decrypt", "encrypt"]);
}
async function decrypt(value: string, secret: string): Promise<string> {
  const bytes = decodeBase64Url(value);
  if (bytes.length < 29) throw new Error("invalid_ciphertext");
  const iv = bytes.slice(0, 12);
  const clear = await crypto.subtle.decrypt({ name: "AES-GCM", iv }, await encryptionKey(secret), bytes.slice(12));
  return new TextDecoder().decode(clear);
}
async function encrypt(value: string, secret: string): Promise<string> {
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const ciphertext = new Uint8Array(await crypto.subtle.encrypt(
    { name: "AES-GCM", iv }, await encryptionKey(secret), new TextEncoder().encode(value)
  ));
  return btoa(String.fromCharCode(...new Uint8Array([...iv, ...ciphertext])))
    .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

Deno.serve(async (req: Request) => {
  const origin = allowedOrigin(req.headers.get("Origin"));
  if (req.headers.get("Origin") && !origin) return json({ error: "origin_not_allowed" }, 403, null);
  if (req.method === "OPTIONS") return new Response("ok", { headers: headers(origin ?? "https://rami-storage.netlify.app") });
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405, origin);

  const match = (req.headers.get("Authorization") ?? "").match(/^Bearer\s+(.+)$/i);
  if (!match) return json({ error: "unauthorized" }, 401, origin);
  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  const dbUrl = Deno.env.get("SUPABASE_DB_URL") ?? "";
  const encryptionSecret = Deno.env.get("GOOGLE_TOKEN_ENCRYPTION_KEY") ?? "";
  const googleClientId = Deno.env.get("GOOGLE_CLIENT_ID") ?? "";
  const googleClientSecret = Deno.env.get("GOOGLE_CLIENT_SECRET") ?? "";
  if (![supabaseUrl, serviceKey, dbUrl, encryptionSecret, googleClientId, googleClientSecret].every(Boolean)) {
    return json({ error: "server_configuration_error" }, 503, origin);
  }

  let body: Record<string, unknown>;
  try { body = await req.json(); } catch { return json({ error: "invalid_json" }, 400, origin); }
  const fileId = body.fileId;
  const channelId = body.channelId;
  if (typeof fileId !== "string" || !/^[A-Za-z0-9_-]{1,256}$/.test(fileId)) {
    return json({ error: "invalid_file_id" }, 400, origin);
  }
  if (typeof channelId !== "string" || !CHANNELS.has(channelId)) {
    return json({ error: "invalid_channel" }, 400, origin);
  }

  const admin = createClient(supabaseUrl, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });
  const { data: userData, error: userError } = await admin.auth.getUser(match[1]);
  const user = userData.user;
  if (userError || !user?.id || !user.email || user.email.toLowerCase() !== ADMIN_EMAIL) {
    return json({ error: "unauthorized" }, 401, origin);
  }

  const db = postgres(dbUrl, { max: 1, prepare: false, connect_timeout: 10, idle_timeout: 5 });
  try {
    const owners = await db`
      select email, role, is_active from rami_storage.admin_accounts
      where user_id = ${user.id}::uuid limit 1
    `;
    const owner = owners[0];
    if (!owner || owner.is_active !== true || owner.role !== "owner" ||
        String(owner.email).toLowerCase() !== ADMIN_EMAIL) {
      return json({ error: "forbidden" }, 403, origin);
    }

    const connections = await db`
      select email, access_token_ciphertext, refresh_token_ciphertext, token_expires_at
      from rami_storage.google_drive_connections where user_id = ${user.id}::uuid limit 1
    `;
    const connection = connections[0];
    if (!connection?.access_token_ciphertext) return json({ error: "google_drive_not_connected" }, 409, origin);

    let accessToken = await decrypt(String(connection.access_token_ciphertext), encryptionSecret);
    let refreshToken = connection.refresh_token_ciphertext
      ? await decrypt(String(connection.refresh_token_ciphertext), encryptionSecret) : "";
    let expiresAt = connection.token_expires_at ? new Date(connection.token_expires_at).getTime() : 0;
    if (expiresAt <= Date.now() + 60_000) {
      if (!refreshToken) return json({ error: "google_reconnect_required" }, 409, origin);
      const refreshResponse = await fetch("https://oauth2.googleapis.com/token", {
        method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
          client_id: googleClientId, client_secret: googleClientSecret,
          refresh_token: refreshToken, grant_type: "refresh_token"
        })
      });
      const refreshed = await refreshResponse.json();
      if (!refreshResponse.ok || !refreshed.access_token) {
        console.error("Google Drive token refresh failed:", refreshResponse.status);
        return json({ error: "google_reconnect_required" }, 409, origin);
      }
      accessToken = String(refreshed.access_token);
      if (refreshed.refresh_token) refreshToken = String(refreshed.refresh_token);
      expiresAt = Date.now() + Number(refreshed.expires_in ?? 3600) * 1000;
      const accessCipher = await encrypt(accessToken, encryptionSecret);
      const refreshCipher = refreshToken ? await encrypt(refreshToken, encryptionSecret) : null;
      await db`
        update rami_storage.google_drive_connections
        set access_token_ciphertext = ${accessCipher},
            refresh_token_ciphertext = coalesce(${refreshCipher}, refresh_token_ciphertext),
            token_expires_at = ${new Date(expiresAt).toISOString()}::timestamptz, updated_at = now()
        where user_id = ${user.id}::uuid
      `;
    }

    const driveResponse = await fetch(
      "https://www.googleapis.com/drive/v3/files/" + encodeURIComponent(fileId) +
      "?fields=" + encodeURIComponent("id,name,mimeType,size,modifiedTime,trashed"),
      { headers: { Authorization: "Bearer " + accessToken } }
    );
    const file = await driveResponse.json().catch(() => ({}));
    if (!driveResponse.ok) {
      console.error("Google Drive import metadata request failed:", driveResponse.status);
      return json({ error: driveResponse.status === 404 ? "selected_file_unavailable" : "drive_metadata_failed" }, 502, origin);
    }
    if (file.trashed === true || String(file.id ?? "") !== fileId ||
        typeof file.mimeType !== "string" || !file.mimeType.startsWith("video/")) {
      return json({ error: "selected_file_not_video" }, 422, origin);
    }
    // Google Drive returns file sizes as decimal strings. Keep unknown sizes as null;
    // there is intentionally no application-level minimum or maximum size restriction.
    const size = typeof file.size === "string" && /^\d+$/.test(file.size) ? file.size : null;
    const title = String(file.name ?? "Untitled video").trim().slice(0, 500) || "Untitled video";
    const objectKey = "google-drive:" + fileId;

    const channelRows = await db`
      select id, channel_number, display_name, status from rami_storage.channels
      where id = ${channelId}::uuid limit 1
    `;
    const channel = channelRows[0];
    if (!channel || channel.status !== "active") return json({ error: "channel_unavailable" }, 409, origin);

    const existing = await db`
      select id, status from rami_storage.media_assets
      where channel_id = ${channelId}::uuid and object_key = ${objectKey} limit 1
    `;
    if (existing.length) {
      return json({ asset: { id: existing[0].id, status: existing[0].status }, duplicate: true }, 200, origin);
    }

    const inserted = await db`
      insert into rami_storage.media_assets
        (channel_id, provider_id, title, object_key, byte_size, status)
      values
        (${channelId}::uuid, null, ${title}, ${objectKey}, ${size === null ? null : size.toString()}::bigint, 'verification_pending')
      returning id, channel_id, title, byte_size, status, created_at
    `;
    await db`
      insert into rami_storage.activity_logs (actor_user_id, action, entity_type, entity_id, details)
      values (
        ${user.id}::uuid, 'google_drive_video_registered', 'media_asset', ${inserted[0].id}::uuid,
        ${JSON.stringify({
          channel_number: channel.channel_number,
          channel_name: channel.display_name,
          file_name: title,
          byte_size: size === null ? null : size.toString(),
          source: "google_drive",
          source_file_id_suffix: fileId.slice(-6)
        })}::jsonb
      )
    `;
    return json({
      asset: {
        id: inserted[0].id, channelId: inserted[0].channel_id, title: inserted[0].title,
        byteSize: inserted[0].byte_size === null ? null : String(inserted[0].byte_size), status: inserted[0].status
      },
      duplicate: false,
      playbackReady: false
    }, 201, origin);
  } catch (error) {
    console.error("Google Drive video import failed:", error instanceof Error ? error.message : "unknown error");
    return json({ error: "drive_import_failed" }, 500, origin);
  } finally {
    await db.end({ timeout: 3 });
  }
});
