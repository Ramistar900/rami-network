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
    "Content-Type": "application/json",
    "Cache-Control": "no-store"
  };
  if (origin) {
    headers["Access-Control-Allow-Origin"] = origin;
    headers["Access-Control-Allow-Headers"] = "authorization, apikey, content-type, x-client-info";
    headers["Access-Control-Allow-Methods"] = "GET, PATCH, OPTIONS";
    headers["Vary"] = "Origin";
  }
  return new Response(JSON.stringify(body), { status, headers });
}

Deno.serve(async (req: Request) => {
  const origin = allowedOrigin(req.headers.get("Origin"));
  if (req.headers.get("Origin") && !origin) {
    return response({ error: "origin_not_allowed" }, 403, null);
  }
  if (req.method === "OPTIONS") return new Response("ok", { headers: {
    "Access-Control-Allow-Origin": origin ?? "https://rami-storage.netlify.app",
    "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
    "Access-Control-Allow-Methods": "GET, PATCH, OPTIONS",
    "Vary": "Origin"
  }});
  if (req.method !== "GET" && req.method !== "PATCH") {
    return response({ error: "method_not_allowed" }, 405, origin);
  }

  const authorization = req.headers.get("Authorization") ?? "";
  const match = authorization.match(/^Bearer\s+(.+)$/i);
  if (!match) return response({ error: "unauthorized" }, 401, origin);

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const publishableRaw = Deno.env.get("SUPABASE_PUBLISHABLE_KEYS");
    const legacyAnon = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const dbUrl = Deno.env.get("SUPABASE_DB_URL") ?? "";
    let publishableKey = legacyAnon;
    if (publishableRaw) {
      const parsed = JSON.parse(publishableRaw);
      publishableKey = parsed.default ?? legacyAnon;
    }
    if (!supabaseUrl || !publishableKey || !dbUrl) {
      console.error("Channel admin configuration is incomplete.");
      return response({ error: "server_configuration_error" }, 500, origin);
    }

    const userClient = createClient(supabaseUrl, publishableKey, {
      auth: { persistSession: false, autoRefreshToken: false },
      global: { headers: { Authorization: authorization } }
    });
    const { data: userData, error: userError } = await userClient.auth.getUser(match[1]);
    const user = userData.user;
    if (userError || !user?.id || user.email?.toLowerCase() !== ADMIN_EMAIL) {
      return response({ error: "unauthorized" }, 401, origin);
    }

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

      if (req.method === "PATCH") {
        let body: { display_name?: unknown; status?: unknown };
        try {
          body = await req.json();
        } catch {
          return response({ error: "invalid_json" }, 400, origin);
        }
        const displayName = typeof body.display_name === "string" ? body.display_name.trim() : undefined;
        const status = typeof body.status === "string" ? body.status : undefined;
        if (displayName !== undefined && (displayName.length < 2 || displayName.length > 60)) {
          return response({ error: "invalid_display_name" }, 400, origin);
        }
        if (status !== undefined && status !== "active" && status !== "paused") {
          return response({ error: "invalid_status" }, 400, origin);
        }
        if (displayName === undefined && status === undefined) {
          return response({ error: "no_changes" }, 400, origin);
        }

        let updated;
        if (displayName !== undefined && status !== undefined) {
          updated = await db`
            update rami_storage.channels
            set display_name = ${displayName}, status = ${status}, updated_at = now()
            where channel_number = 1
            returning id, channel_number, display_name, storage_group_id, status, created_at, updated_at
          `;
        } else if (displayName !== undefined) {
          updated = await db`
            update rami_storage.channels
            set display_name = ${displayName}, updated_at = now()
            where channel_number = 1
            returning id, channel_number, display_name, storage_group_id, status, created_at, updated_at
          `;
        } else {
          updated = await db`
            update rami_storage.channels
            set status = ${status}, updated_at = now()
            where channel_number = 1
            returning id, channel_number, display_name, storage_group_id, status, created_at, updated_at
          `;
        }
        if (!updated.length) return response({ error: "channel_not_found" }, 404, origin);
        await db`
          insert into rami_storage.activity_logs (actor_user_id, action, entity_type, entity_id, details)
          values (
            ${user.id}::uuid,
            'channel_updated',
            'channel',
            ${updated[0].id}::uuid,
            ${JSON.stringify({ channel_number: 1, display_name: updated[0].display_name, status: updated[0].status })}::jsonb
          )
        `;
      }

      const result = await db`
        select
          c.id, c.channel_number, c.display_name, c.storage_group_id, c.status,
          c.created_at, c.updated_at,
          g.group_number, g.display_name as storage_group_name, g.status as storage_group_status,
          g.provider_id,
          (select count(*)::int from rami_storage.media_assets m where m.channel_id = c.id) as media_count,
          (select count(*)::int from rami_storage.media_assets m where m.channel_id = c.id and m.status = 'ready') as ready_media_count,
          (select count(*)::int from rami_storage.channel_schedules s where s.channel_id = c.id and s.status = 'scheduled') as scheduled_count,
          (select count(*)::int from rami_storage.broadcast_sessions b where b.channel_id = c.id and b.status = 'live') as live_broadcast_count
        from rami_storage.channels c
        join rami_storage.storage_groups g on g.id = c.storage_group_id
        where c.channel_number = 1
        limit 1
      `;
      if (!result.length) return response({ error: "channel_not_found" }, 404, origin);
      return response({ channel: result[0] }, 200, origin);
    } finally {
      await db.end({ timeout: 3 });
    }
  } catch (error) {
    console.error("Channel admin request failed:", error instanceof Error ? error.message : "unknown error");
    return response({ error: "channel_admin_failed" }, 500, origin);
  }
});
