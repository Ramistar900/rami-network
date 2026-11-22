# RAMİ NETWORK Viewer API

Public, read-only viewer API, separate from the authenticated owner control API.

## Base URL
`https://ymujjcdsdclptbpgckll.supabase.co/functions/v1/rami-network-viewer`

## Published channels
`GET ?action=channels`

Returns only enabled, non-archived channels. It never returns source URLs, rights notes, private settings, or audit records.

## Channel playback state
`GET ?action=state&channel_id=<uuid>`

Returns the server timestamp, monotonically increasing sequence, playback mode, and either a final HLS output URL for live mode or the shared VOD playlist anchor and current item calculation.

Both SINJAR TV SHOW and ROYAL+ must consume this same response and must not start independent channel clocks.

## Security
- Viewer function is public but read-only.
- It calls a service-role-only RPC; the service-role key never leaves Supabase.
- The RPC returns only published channels and playback fields required by viewers.
- Admin writes use the JWT-protected `rami-network-control` function.
- A stream URL must point to a separately provisioned, authorized HLS output. Supabase is not a video transcoder or HLS origin.

## Admin function
`POST /functions/v1/rami-network-control`

Requires a valid Supabase Auth access token and the configured RAMI owner account. The Android app uses only the publishable key; it contains no service-role or GitHub secret.
