import { createClient } from "npm:@supabase/supabase-js@2.95.0";
import postgres from "npm:postgres@3.4.7";

const ALLOWED_ORIGINS = new Set([
  "https://rami-storage.netlify.app",
  "https://deploy-preview-4--rami-storage.netlify.app"
]);
const ADMIN_EMAIL = "relaxmusic9000@gmail.com";

function headersFor(origin: string) {
  return {
    "Access-Control-Allow-Origin": origin,
    "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Vary": "Origin",
    "Content-Type": "application/json",
    "Cache-Control": "no-store"
  };
}

function json(body: Record<string, unknown>, status: number, origin: string) {
  return new Response(JSON.stringify(body), { status, headers: headersFor(origin) });
}

async function digest(value: string): Promise<Uint8Array> {
  return new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)));
}

async function safeEqual(left: string, right: string): Promise<boolean> {
  const a = await digest(left);
  const b = await digest(right);
  let difference = 0;
  for (let i = 0; i < a.length; i++) difference |= a[i] ^ b[i];
  return difference === 0;
}

class BootstrapError extends Error {
  status: number;
  code: string;
  constructor(code: string, status: number) {
    super(code);
    this.code = code;
    this.status = status;
  }
}

Deno.serve(async (req: Request) => {
  const origin = req.headers.get("Origin") ?? "";
  if (!ALLOWED_ORIGINS.has(origin)) {
    return new Response(JSON.stringify({ error: "origin_not_allowed" }), {
      status: 403,
      headers: { "Content-Type": "application/json", "Cache-Control": "no-store" }
    });
  }

  if (req.method === "OPTIONS") {
    return new Response("ok", { status: 200, headers: headersFor(origin) });
  }
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405, origin);

  const contentLength = Number(req.headers.get("content-length") ?? "0");
  if (contentLength > 8192) return json({ error: "request_too_large" }, 413, origin);

  try {
    const expectedToken = Deno.env.get("RAMI_STORAGE_OWNER_BOOTSTRAP_TOKEN") ?? "";
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const secretKeysRaw = Deno.env.get("SUPABASE_SECRET_KEYS");
    const legacyServiceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const dbUrl = Deno.env.get("SUPABASE_DB_URL") ?? "";
    let secretKey = legacyServiceRole;
    let secretKeysParseFailed = false;
    if (secretKeysRaw) {
      try {
        const secretKeys = JSON.parse(secretKeysRaw);
        secretKey = secretKeys.default ?? legacyServiceRole;
      } catch {
        secretKeysParseFailed = true;
        secretKey = legacyServiceRole;
      }
    }

    const missing: string[] = [];
    if (expectedToken.length < 43) missing.push("RAMI_STORAGE_OWNER_BOOTSTRAP_TOKEN");
    if (!supabaseUrl) missing.push("SUPABASE_URL");
    if (!secretKey) missing.push("SUPABASE_SECRET_KEYS.default_or_SUPABASE_SERVICE_ROLE_KEY");
    if (!dbUrl) missing.push("SUPABASE_DB_URL");
    if (secretKeysParseFailed) missing.push("SUPABASE_SECRET_KEYS_invalid_json");

    if (missing.length) {
      console.error("Owner bootstrap configuration missing:", missing.join(", "));
      return json({ error: "bootstrap_not_configured" }, 503, origin);
    }

    let body: { bootstrapToken?: unknown; password?: unknown };
    try {
      body = await req.json();
    } catch {
      return json({ error: "invalid_request" }, 400, origin);
    }

    const suppliedToken = typeof body.bootstrapToken === "string" ? body.bootstrapToken : "";
    const password = typeof body.password === "string" ? body.password : "";
    if (suppliedToken.length < 43 || suppliedToken.length > 256 ||
        password.length < 12 || password.length > 72 ||
        new TextEncoder().encode(password).length > 72) {
      return json({ error: "invalid_request" }, 400, origin);
    }

    if (!(await safeEqual(suppliedToken, expectedToken))) {
      return json({ error: "invalid_bootstrap_token" }, 401, origin);
    }

    const adminClient = createClient(supabaseUrl, secretKey, {
      auth: { persistSession: false, autoRefreshToken: false }
    });
    const db = postgres(dbUrl, {
      max: 1,
      prepare: false,
      connect_timeout: 10,
      idle_timeout: 5
    });

    try {
      const ownerRows = await db`
        select user_id, email, role, is_active
        from rami_storage.admin_accounts
        where singleton = true
        limit 1
      `;
      const candidate = ownerRows[0];
      if (!candidate || candidate.is_active !== true ||
          candidate.role !== "owner" ||
          String(candidate.email).toLowerCase() !== ADMIN_EMAIL) {
        throw new BootstrapError("owner_not_authorized", 403);
      }
      const owner = { user_id: String(candidate.user_id), email: String(candidate.email) };

      const { data: userResult, error: lookupError } =
        await adminClient.auth.admin.getUserById(owner.user_id);
      const user = userResult?.user;
      if (lookupError || !user || user.email?.toLowerCase() !== ADMIN_EMAIL) {
        throw new BootstrapError("owner_auth_account_not_found", 409);
      }
      if (!user.email_confirmed_at) {
        throw new BootstrapError("owner_email_not_confirmed", 409);
      }

      // Commit the one-time claim before changing Auth credentials. This avoids
      // keeping a Postgres transaction open across an external Auth API call.
      await db.begin(async (tx) => {
        const stateRows = await tx`
          select consumed_at, claimed_at
          from rami_storage.owner_password_bootstrap
          where singleton = true
          for update
        `;
        if (!stateRows.length) throw new BootstrapError("bootstrap_state_missing", 500);
        if (stateRows[0].consumed_at) throw new BootstrapError("bootstrap_already_used", 409);
        if (stateRows[0].claimed_at) throw new BootstrapError("bootstrap_already_claimed", 409);

        const claim = await tx`
          update rami_storage.owner_password_bootstrap
          set claimed_at = now(), claimed_by = ${owner.user_id}
          where singleton = true and consumed_at is null and claimed_at is null
          returning singleton
        `;
        if (!claim.length) throw new BootstrapError("bootstrap_already_claimed", 409);
      });

      const { error: updateError } =
        await adminClient.auth.admin.updateUserById(owner.user_id, { password });
      if (updateError) {
        console.error("Owner password update failed:", updateError.message);
        // Fail closed after an ambiguous Auth result; never silently allow replay.
        throw new BootstrapError("password_update_failed_claim_locked", 500);
      }

      const consumed = await db`
        update rami_storage.owner_password_bootstrap
        set consumed_at = now(), consumed_by = ${owner.user_id}
        where singleton = true
          and consumed_at is null
          and claimed_by = ${owner.user_id}
        returning singleton
      `;
      if (!consumed.length) {
        console.error("Password changed but bootstrap consumption could not be confirmed.");
        return json({ error: "password_changed_state_confirmation_failed" }, 500, origin);
      }

      return json({ success: true, email: ADMIN_EMAIL }, 200, origin);
    } finally {
      await db.end({ timeout: 3 });
    }
  } catch (error) {
    if (error instanceof BootstrapError) {
      if (error.code === "bootstrap_already_used" ||
          error.code === "bootstrap_already_claimed" ||
          error.code === "owner_not_authorized" ||
          error.code === "owner_email_not_confirmed") {
        return json({ error: error.code }, error.status, origin);
      }
      console.error("Owner bootstrap failed:", error.code);
      return json({ error: "bootstrap_failed" }, error.status, origin);
    }
    console.error("Owner bootstrap failed:", error instanceof Error ? error.message : "unknown error");
    return json({ error: "bootstrap_failed" }, 500, origin);
  }
});
