import { createClient } from "npm:@supabase/supabase-js@2.95.0";
import postgres from "npm:postgres@3.4.7";

function isAllowedOrigin(origin: string | null): boolean {
  return origin === "https://rami-storage.netlify.app" ||
    Boolean(origin && /^https:\/\/deploy-preview-\d+--rami-storage\.netlify\.app$/.test(origin));
}

function headersFor(origin: string | null) {
  const allowedOrigin = isAllowedOrigin(origin)
    ? origin
    : "https://rami-storage.netlify.app";
  return {
    "Access-Control-Allow-Origin": allowedOrigin,
    "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Vary": "Origin",
    "Content-Type": "application/json"
  };
}

function json(body: Record<string, unknown>, status = 200, origin: string | null = null) {
  return new Response(JSON.stringify(body), { status, headers: headersFor(origin) });
}

Deno.serve(async (req: Request) => {
  const origin = req.headers.get("Origin");
  if (origin && !isAllowedOrigin(origin)) {
    return json({ error: "origin_not_allowed" }, 403, null);
  }

  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: headersFor(origin) });
  }
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405, origin);

  const authorization = req.headers.get("Authorization") ?? "";
  const match = authorization.match(/^Bearer\s+(.+)$/i);
  if (!match) return json({ error: "unauthorized" }, 401, origin);

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const publishableKeysRaw = Deno.env.get("SUPABASE_PUBLISHABLE_KEYS");
    const legacyAnon = Deno.env.get("SUPABASE_ANON_KEY");
    const dbUrl = Deno.env.get("SUPABASE_DB_URL");
    const publishableKey = publishableKeysRaw ? JSON.parse(publishableKeysRaw).default : legacyAnon;

    if (!supabaseUrl || !publishableKey || !dbUrl) {
      console.error("Required Supabase function environment is unavailable.");
      return json({ error: "server_configuration_error" }, 500, origin);
    }

    const userClient = createClient(supabaseUrl, publishableKey, {
      auth: { persistSession: false, autoRefreshToken: false },
      global: { headers: { Authorization: authorization } }
    });

    const { data: userData, error: userError } = await userClient.auth.getUser(match[1]);
    if (userError || !userData.user?.id || !userData.user.email) {
      return json({ error: "unauthorized" }, 401, origin);
    }

    const db = postgres(dbUrl, { max: 1, prepare: false, connect_timeout: 10, idle_timeout: 5 });
    let adminRows: Array<{ email: string; role: string; is_active: boolean }>;
    try {
      adminRows = await db.unsafe(
        "select email, role, is_active from rami_storage.admin_accounts where user_id = $1::uuid limit 1",
        [userData.user.id]
      );
    } finally {
      await db.end({ timeout: 2 });
    }

    const admin = adminRows[0];
    const allowed = Boolean(
      admin &&
      admin.is_active === true &&
      admin.role === "owner" &&
      admin.email.toLowerCase() === userData.user.email.toLowerCase()
    );

    if (!allowed) return json({ allowed: false, error: "forbidden" }, 403, origin);
    return json({ allowed: true, role: "owner", email: admin.email }, 200, origin);
  } catch (error) {
    console.error("Admin verification failed:", error instanceof Error ? error.message : "unknown error");
    return json({ error: "authorization_check_failed" }, 500, origin);
  }
});
