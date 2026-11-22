# RAMİ NETWORK Viewer API

Public, read-only viewer API, separate from the authenticated owner control API.

## Base URL
`https://ymujjcdsdclptbpgckll.supabase.co/functions/v1/rami-network-viewer`

## Published channels
`GET ?action=channels`

Returns only enabled, non-archived channels. It never returns source URLs, rights notes, private settings, or audit records.

## Channel playback state
`GET ?action=state&channel_id=<uuid>`

Returns the server timestamp and monotonically increasing sequence. Supported playback modes:

- `live`: returns the final public HLS output URL.
- `playlist`: returns the shared playlist anchor, current item, media URL, and calculated position. Viewers must use the server timeline rather than starting their own clock.
- `scheduled`: returns the currently scheduled single media item, its public media URL, source type, duration, position, and the schedule block start/end timestamps.
- `stopped`: no active playback is scheduled.

The schedule engine selects the active block using the highest priority first, then the most recent start time. It evaluates schedules once per minute. A block transition increments the channel sequence; unchanged blocks do not reset playback.

Both SINJAR TV SHOW and ROYAL+ must consume this same response and must not start independent channel clocks.

## Security
- Viewer function is public but read-only.
- It calls service-role-only RPCs; the service-role key never leaves Supabase.
- Private origin URLs stay in `content_sources.source_url`. Viewer responses use only `viewer_url`, which must be a public HTTPS URL without query strings or fragments. The private origin is never returned.
- The RPC returns only published channels and playback fields required by viewers.
- Admin writes use the JWT-protected `rami-network-control` function.
- A live stream URL must point to a separately provisioned, authorized HLS output. Supabase is not a video transcoder, broadcaster, or HLS origin.

## Admin function
`POST /functions/v1/rami-network-control`

Requires a valid Supabase Auth access token and the configured RAMI owner account. The Android app uses only the publishable key; it contains no service-role or GitHub secret.
