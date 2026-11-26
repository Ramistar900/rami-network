import { createClient } from "npm:@supabase/supabase-js@2.95.0";
import postgres from "npm:postgres@3.4.7";

const ADMIN_EMAIL = "relaxmusic9000@gmail.com";

function allowedOrigin(origin: string | null): string | null {
  if (!origin) return null;
  if (origin === "https://rami-storage.netlify.app") return origin;
  if (/^https:\/\/deploy-preview-\d+--rami-storage\.netlify\.app$/.test(origin)) return origin;
  return null;
}
function response(body: Record<string, unknown>, status: number, origin: string | null) {
  const headers: Record<string, string> = {
    "Content-Type": "application/json", "Cache-Control": "no-store", "Vary": "Origin",
    "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
    "Access-Control-Allow-Methods": "POST, OPTIONS"
  };
  if (origin) headers["Access-Control-Allow-Origin"] = origin;
  return new Response(JSON.stringify(body), { status, headers });
}

Deno.serve(async (req: Request) => {
  const origin = allowedOrigin(req.headers.get("Origin"));
  if (req.headers.get("Origin") && !origin) return response({ error: "origin_not_allowed" }, 403, null);
  if (req.method === "OPTIONS") return new Response("ok", {
    headers: {
      "Access-Control-Allow-Origin": origin ?? "https://rami-storage.netlify.app",
      "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
      "Access-Control-Allow-Methods": "POST, OPTIONS", "Vary": "Origin"
    }
  });
  if (req.method !== "POST") return response({ error: "method_not_allowed" }, 405, origin);

  const match = (req.headers.get("Authorization") ?? "").match(/^Bearer\s+(.+)$/i);
  if (!match) return response({ error: "unauthorized" }, 401, origin);
  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  const dbUrl = Deno.env.get("SUPABASE_DB_URL") ?? "";
  if (![supabaseUrl, serviceKey, dbUrl].every(Boolean)) return response({ error: "server_configuration_error" }, 503, origin);

  const admin = createClient(supabaseUrl, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });
  const { data, error } = await admin.auth.getUser(match[1]);
  const user = data.user;
  if (error || !user?.id || user.email?.toLowerCase() !== ADMIN_EMAIL) return response({ error: "unauthorized" }, 401, origin);

  const db = postgres(dbUrl, { max: 1, prepare: false, connect_timeout: 10, idle_timeout: 5 });
  try {
    const owners = await db`
      select email, role, is_active
      from rami_storage.admin_accounts
      where user_id = ${user.id}::uuid
      limit 1
    `;
    const owner = owners[0];
    if (!owner || owner.is_active !== true || owner.role !== "owner" ||
        String(owner.email).toLowerCase() !== ADMIN_EMAIL) {
      return response({ error: "forbidden" }, 403, origin);
    }

    const assets = await db`
      select
        a.id, a.channel_id, a.title, a.byte_size, a.status,
        a.created_at, a.updated_at, c.display_name as channel_name
      from rami_storage.media_assets a
      inner join rami_storage.channels c on c.id = a.channel_id
      order by a.created_at desc
      limit 250
    `;
    return response({ assets }, 200, origin);
  } catch (error) {
    console.error("RAMI media library read failed:", error instanceof Error ? error.message : "unknown error");
    return response({ error: "media_library_read_failed" }, 500, origin);
  } finally {
    await db.end({ timeout: 3 });
  }
});
