# RAMI TEST LIVE

This is the first published test channel in the RAMI NETWORK backend.

## Channel
- Number: 1
- Slug: `rami-test-live`
- Display name: `RAMI TEST LIVE`
- Channel ID: `a72c1471-42fa-4548-bb58-ff23c498a2ed`
- Playback mode: live HLS
- Schedule: active for seven days from 2026-11-22 02:45:50 UTC, then it stops unless renewed.

## Temporary demo source
- Provider: Unified Streaming live reference/demo stream.
- HLS URL: `https://demo.unified-streaming.com/k8s/live/stable/live.isml/.m3u8`
- Scope: temporary technical playback validation only. Do not rehost, record, redistribute, or use as production programming.
- The upstream demo is independently hosted and may be changed or unavailable without notice.

## Viewer API
Base URL:
`https://ymujjcdsdclptbpgckll.supabase.co/functions/v1/rami-network-viewer`

- Published channels: `?action=channels`
- This channel state: `?action=state&channel_id=a72c1471-42fa-4548-bb58-ff23c498a2ed`

The database viewer RPC currently reports `status=playing`, `playbackMode=live`, and the expected public HLS URL. The scheduler runs every minute and the latest recorded run succeeded.

## Validation boundary
The database and scheduler state are verified. The Supabase Edge Function HTTP request and playback on a physical Android/TV device have not yet been independently verified in this test. Confirm both before treating the channel as operational. This demo source is not a permanent broadcaster or a production content origin.
