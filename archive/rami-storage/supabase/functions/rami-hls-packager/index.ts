import postgres from "npm:postgres@3.4.7";
import { createRemoteJWKSet, decodeJwt, jwtVerify } from "npm:jose@5.9.6";

const REPOSITORY = "github.com/Ramistar900/rami-storage";
const ALLOWED_REFS = new Set(["refs/heads/feature/hls-drive-packaging", "refs/heads/main"]);
const ADMIN_EMAIL = "relaxmusic9000@gmail.com";

function json(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", "Cache-Control": "no-store" }
  });
}
function decodeBase64Url(value: string): Uint8Array {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const binary = atob(normalized + "=".repeat((4 - normalized.length % 4) % 4));
  return Uint8Array.from(binary, char => char.charCodeAt(0));
}
async function encryptionKey(secret: string): Promise<CryptoKey> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(secret));
  return crypto.subtle.importKey("raw", digest, "AES-GCM", false, ["decrypt", "encrypt"]);
}
async function decrypt(value: string, secret: string): Promise<string> {
  const bytes = decodeBase64Url(value);
  if (bytes.length < 29) throw new Error("invalid_ciphertext");
  const clear = await crypto.subtle.decrypt(
    { name: "AES-GCM", iv: bytes.slice(0, 12) },
    await encryptionKey(secret), bytes.slice(12)
  );
  return new TextDecoder().decode(clear);
}
async function encrypt(value: string, secret: string): Promise<string> {
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const ciphertext = new Uint8Array(await crypto.subtle.encrypt(
    { name: "AES-GCM", iv }, await encryptionKey(secret), new TextEncoder().encode(value)
  ));
  const bytes = new Uint8Array([...iv, ...ciphertext]);
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}
async function verifyRunner(req: Request) {
  const match = (req.headers.get("Authorization") ?? "").match(/^Bearer\s+(.+)$/i);
  if (!match) { console.error("HLS OIDC rejected: bearer_missing"); return null; }
  try {
    const unverified = decodeJwt(match[1]);
    const orgId = String(unverified["oidc.circleci.com/org-id"] ?? "");
    const issuer = String(unverified.iss ?? "");
    if (!/^[0-9a-f-]{36}$/i.test(orgId) || issuer !== `https://oidc.circleci.com/org/${orgId}`) { console.error("HLS OIDC rejected: issuer_or_org_mismatch"); return null; }

    const jwks = createRemoteJWKSet(new URL(issuer + "/.well-known/jwks-pub.json"));
    const { payload } = await jwtVerify(match[1], jwks, {
      issuer,
      audience: orgId,
      algorithms: ["RS256"]
    });
    const origin = String(payload["oidc.circleci.com/vcs-origin"] ?? "");
    const ref = String(payload["oidc.circleci.com/vcs-ref"] ?? "");
    const projectId = String(payload["oidc.circleci.com/project-id"] ?? "");
    const jobId = String(payload["oidc.circleci.com/job-id"] ?? "");
    const workflowId = String(payload["oidc.circleci.com/workflow-id"] ?? "");
    const configSource = String(payload["oidc.circleci.com/config-source"] ?? "");
    const sub = String(payload.sub ?? "");
    const prefix = `org/${orgId}/project/${projectId}/user/`;
    const suffix = `/vcs-origin/${REPOSITORY}/vcs-ref/${ref}`;
    const userId = sub.startsWith(prefix) && sub.endsWith(suffix)
      ? sub.slice(prefix.length, -suffix.length) : "";

    const checks = {
      originMatches: origin === REPOSITORY,
      configSourceMatches: configSource === REPOSITORY,
      refAllowed: ALLOWED_REFS.has(ref),
      projectIdValid: /^[0-9a-f-]{36}$/i.test(projectId),
      jobIdValid: /^[0-9a-f-]{36}$/i.test(jobId),
      workflowIdValid: /^[0-9a-f-]{36}$/i.test(workflowId),
      userIdValid: /^[0-9a-f-]{36}$/i.test(userId)
    };
    const sshRerun = payload["oidc.circleci.com/ssh-rerun"] === true;
    if (Object.values(checks).some(value => value === false) || sshRerun) {
      console.error("HLS OIDC claim check failed:", JSON.stringify({
        checks, sshRerun, origin, configSource, ref, audience: payload.aud
      }));
      return null;
    }

    return { run_id: jobId, run_attempt: 1 };
  } catch (error) {
    console.error("HLS OIDC signature verification failed:", error instanceof Error ? error.message : "unknown");
    return null;
  }
}

async function getDriveToken(db: ReturnType<typeof postgres>, userId: string, encryptionSecret: string, clientId: string, clientSecret: string) {
  const rows = await db`
    select access_token_ciphertext, refresh_token_ciphertext, token_expires_at
    from rami_storage.google_drive_connections where user_id = ${userId}::uuid limit 1
  `;
  const connection = rows[0];
  if (!connection?.access_token_ciphertext) throw new Error("google_drive_not_connected");
  let accessToken = await decrypt(String(connection.access_token_ciphertext), encryptionSecret);
  const refreshToken = connection.refresh_token_ciphertext
    ? await decrypt(String(connection.refresh_token_ciphertext), encryptionSecret) : "";
  let expiresAt = connection.token_expires_at ? new Date(connection.token_expires_at).getTime() : 0;
  if (expiresAt <= Date.now() + 90_000) {
    if (!refreshToken) throw new Error("google_reconnect_required");
    const response = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ client_id: clientId, client_secret: clientSecret, refresh_token: refreshToken, grant_type: "refresh_token" })
    });
    const refreshed = await response.json().catch(() => ({}));
    if (!response.ok || !refreshed.access_token) throw new Error("google_reconnect_required");
    accessToken = String(refreshed.access_token);
    expiresAt = Date.now() + Number(refreshed.expires_in ?? 3600) * 1000;
    const accessCipher = await encrypt(accessToken, encryptionSecret);
    const refreshCipher = refreshed.refresh_token ? await encrypt(String(refreshed.refresh_token), encryptionSecret) : null;
    await db`
      update rami_storage.google_drive_connections
      set access_token_ciphertext = ${accessCipher},
          refresh_token_ciphertext = coalesce(${refreshCipher}, refresh_token_ciphertext),
          token_expires_at = ${new Date(expiresAt).toISOString()}::timestamptz,
          updated_at = now()
      where user_id = ${userId}::uuid
    `;
  }
  return { accessToken, expiresAt };
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const claims = await verifyRunner(req);
  if (!claims) return json({ error: "runner_not_authorized" }, 401);

  const dbUrl = Deno.env.get("SUPABASE_DB_URL") ?? "";
  const encryptionSecret = Deno.env.get("GOOGLE_TOKEN_ENCRYPTION_KEY") ?? "";
  const googleClientId = Deno.env.get("GOOGLE_CLIENT_ID") ?? "";
  const googleClientSecret = Deno.env.get("GOOGLE_CLIENT_SECRET") ?? "";
  if (![dbUrl, encryptionSecret, googleClientId, googleClientSecret].every(Boolean)) {
    return json({ error: "server_configuration_error" }, 503);
  }
  let body: Record<string, unknown>;
  try { body = await req.json() as Record<string, unknown>; }
  catch { return json({ error: "invalid_json" }, 400); }
  if (!["prepare", "renew", "complete", "fail"].includes(String(body.action))) return json({ error: "invalid_action" }, 400);

  const db = postgres(dbUrl, { max: 1, prepare: false, connect_timeout: 10, idle_timeout: 5 });
  try {
    const owners = await db`
      select user_id from rami_storage.admin_accounts
      where is_active = true and role = 'owner' and lower(email) = ${ADMIN_EMAIL} limit 2
    `;
    if (owners.length !== 1) return json({ error: "owner_configuration_invalid" }, 403);
    const userId = String(owners[0].user_id);

    if (body.action === "prepare") {
      if (typeof body.assetId !== "string" || !/^[0-9a-f-]{36}$/i.test(body.assetId)) return json({ error: "invalid_asset_id" }, 400);
      const assets = await db`
        select a.id, a.channel_id, a.title, a.object_key, a.status,
               c.display_name as channel_name, c.channel_number
        from rami_storage.media_assets a
        join rami_storage.channels c on c.id = a.channel_id
        where a.id = ${body.assetId}::uuid limit 1
      `;
      const asset = assets[0];
      if (!asset) return json({ error: "asset_not_found" }, 404);
      if (asset.status !== "verification_pending") return json({ error: "asset_not_in_verification_pending_state" }, 409);
      const objectKey = String(asset.object_key ?? "");
      if (!objectKey.startsWith("google-drive:")) return json({ error: "asset_not_in_google_drive" }, 422);
      const sourceFileId = objectKey.slice("google-drive:".length);
      if (!/^[A-Za-z0-9_-]{1,256}$/.test(sourceFileId)) return json({ error: "invalid_source_reference" }, 422);

      const { accessToken, expiresAt } = await getDriveToken(db, userId, encryptionSecret, googleClientId, googleClientSecret);
      const metadataResponse = await fetch(
        "https://www.googleapis.com/drive/v3/files/" + encodeURIComponent(sourceFileId) +
          "?fields=" + encodeURIComponent("id,name,mimeType,size,trashed,capabilities(canDownload)"),
        { headers: { Authorization: "Bearer " + accessToken } }
      );
      const metadata = await metadataResponse.json().catch(() => ({}));
      if (!metadataResponse.ok) return json({ error: metadataResponse.status === 404 ? "source_file_unavailable" : "source_metadata_failed" }, 502);
      if (String(metadata.id ?? "") !== sourceFileId || metadata.trashed === true ||
          typeof metadata.mimeType !== "string" || !metadata.mimeType.startsWith("video/") ||
          metadata.capabilities?.canDownload === false) return json({ error: "source_not_downloadable_video" }, 422);

      const inserted = await db`
        insert into rami_storage.hls_packages
          (asset_id, channel_id, source_file_id, status, run_id, run_attempt)
        values (${asset.id}::uuid, ${asset.channel_id}::uuid, ${sourceFileId},
                'processing', ${String(claims.run_id)}, ${Number(claims.run_attempt)})
        returning id
      `;
      return json({
        packageId: inserted[0].id, assetId: asset.id, channelId: asset.channel_id,
        channelName: asset.channel_name, channelNumber: asset.channel_number,
        sourceFileId, sourceName: String(metadata.name ?? asset.title),
        sourceMimeType: String(metadata.mimeType), sourceByteSize: metadata.size ? String(metadata.size) : null,
        accessToken, tokenExpiresAt: new Date(expiresAt).toISOString(), playbackReady: false
      });
    }

    if (typeof body.packageId !== "string" || !/^[0-9a-f-]{36}$/i.test(body.packageId)) return json({ error: "invalid_package_id" }, 400);
    const rows = await db`
      select id, run_id, run_attempt, status from rami_storage.hls_packages
      where id = ${body.packageId}::uuid limit 1
    `;
    const packageRow = rows[0];
    if (!packageRow || String(packageRow.run_id) !== String(claims.run_id) ||
        Number(packageRow.run_attempt) !== Number(claims.run_attempt)) return json({ error: "package_run_mismatch" }, 403);
    if (packageRow.status !== "processing") return json({ error: "package_not_processing" }, 409);

    if (body.action === "renew") {
      const { accessToken, expiresAt } = await getDriveToken(db, userId, encryptionSecret, googleClientId, googleClientSecret);
      return json({ accessToken, tokenExpiresAt: new Date(expiresAt).toISOString() });
    }

    if (body.action === "fail") {
      const errorCode = typeof body.errorCode === "string" && /^[a-z0-9_]{1,64}$/.test(body.errorCode)
        ? body.errorCode : "packaging_failed";
      await db`update rami_storage.hls_packages set status='failed', error_code=${errorCode}, updated_at=now() where id=${body.packageId}::uuid`;
      return json({ recorded: true, status: "failed" });
    }

    const segments = body.segments;
    if (typeof body.folderId !== "string" || !/^[A-Za-z0-9_-]{1,256}$/.test(body.folderId) ||
        typeof body.playlistFileId !== "string" || !/^[A-Za-z0-9_-]{1,256}$/.test(body.playlistFileId) ||
        body.playlistName !== "index.m3u8" || !Number.isFinite(Number(body.durationSeconds)) ||
        Number(body.durationSeconds) <= 0 || !Array.isArray(segments) || segments.length < 1 || segments.length > 20000) {
      return json({ error: "invalid_package_metadata" }, 400);
    }
    const safeSegments = [];
    for (const segment of segments) {
      if (!segment || typeof segment !== "object" ||
          typeof segment.fileId !== "string" || !/^[A-Za-z0-9_-]{1,256}$/.test(segment.fileId) ||
          typeof segment.name !== "string" || !/^segment_\d{5}\.ts$/.test(segment.name) ||
          !Number.isFinite(Number(segment.duration)) || Number(segment.duration) <= 0 ||
          !Number.isSafeInteger(Number(segment.byteSize)) || Number(segment.byteSize) < 1) {
        return json({ error: "invalid_segment_metadata" }, 400);
      }
      safeSegments.push({ fileId: segment.fileId, name: segment.name, duration: Number(segment.duration), byteSize: Number(segment.byteSize) });
    }
    await db`
      update rami_storage.hls_packages
      set drive_folder_id=${body.folderId}, playlist_file_id=${body.playlistFileId},
          playlist_name='index.m3u8', segments=${JSON.stringify(safeSegments)}::jsonb,
          duration_seconds=${Number(body.durationSeconds)}, segment_count=${safeSegments.length},
          status='packaged', error_code=null, updated_at=now()
      where id=${body.packageId}::uuid
    `;
    return json({ recorded: true, status: "packaged", segmentCount: safeSegments.length, playbackReady: false });
  } catch (error) {
    const code = error instanceof Error ? error.message : "unknown_error";
    console.error("HLS packaging bridge failed:", code);
    const status = code === "google_drive_not_connected" || code === "google_reconnect_required" ? 409 : 500;
    return json({ error: status === 409 ? code : "hls_packager_internal_error" }, status);
  } finally {
    await db.end({ timeout: 3 });
  }
});
