# RAMİ NETWORK — Simple IPTV Architecture

## 1. Product boundary

RAMİ NETWORK is the private Android administration app for one simple IPTV channel at first: **RAMİ 1TV**.

The owner uploads authorized videos, chooses their order, and starts or stops the channel. The server plays each video to completion, automatically starts the next one, and loops to the first item after the last.

- **RAMİ NETWORK** — owner-only administration and playlist control.
- **SINJAR TV SHOW** and **ROYAL+ Launcher** — viewer clients only.
- Viewer apps never receive owner credentials or administration access.

This is not a satellite station, studio system, or complex television scheduling platform.

## 2. Minimal system

1. **Android administration app** — upload, list, remove, reorder, and inspect videos; start/stop the channel.
2. **Small control service** — owner authentication, upload coordination, playlist persistence, and confirmed status.
3. **Persistent media storage** — original video files on server-controlled disk.
4. **Playlist playback controller** — server process that plays the ordered list, loops it, and applies edits at video boundaries.
5. **FFmpeg** — inspect and prepare media; prefer stream copy/remux when compatible and transcode only when required.
6. **HLS endpoint** — stable playback URL for VLC and supported viewer clients.

Use the simplest reliable metadata persistence for the prototype, such as a validated playlist file or small local database. Do not introduce PostgreSQL, MediaMTX, a CDN, Kubernetes, Cloudflare Workers, Supabase, or a paid platform unless measured requirements prove they are needed.

## 3. Playlist contract

- The server is authoritative for current item and playback timing.
- Play each item until its end, then advance to the next enabled item.
- After the final item, wrap to the first item.
- The owner can add, remove, and reorder items while playback continues.
- Ordinary edits become active at the next video boundary, so the current video is not cut off.
- An empty list means stopped/unavailable.
- Missing or unreadable media must produce a visible error and a documented safe skip/stop outcome.
- The HLS endpoint must remain playable across item changes. A prototype is not accepted until transitions and looping are tested in VLC and the target viewer clients.

## 4. Media handling

- Only owner-owned or authorized-to-distribute content may be uploaded.
- Production library items must meet the agreed minimum size of 500 MB.
- Preserve original uploads unchanged.
- Inspect file readability, container, codecs, resolution, frame rate, and duration before playlist activation.
- Keep originals separate from temporary segments or playback renditions.
- Verify uploads before making them playable; use resumable upload where practical.
- Enforce storage limits and report remaining disk space.

## 5. Runtime independence

- Playback runs on a persistent Linux server, not inside the Android app.
- Closing the owner's phone must not stop the channel.
- Persist the playlist and original files across server restarts.
- Define and test restart recovery; do not claim exact playback resume unless verified.
- Never show a channel as live when its publisher/controller is stopped or unhealthy.

## 6. Security

- No credentials, stream keys, signing keys, or production URLs in source control.
- Every owner write is authenticated and authorized on the server.
- Keep storage credentials and control endpoints private.
- Viewer clients receive only public playback information.
- Use HTTPS wherever supported.
- Back up playlist metadata and the media inventory; test recovery before real uploads.

## 7. Free-first deployment

No hosting provider is selected. Budget remains **$0**: no card, PayPal, billing activation, paid trial, automatic upgrade, or debt.

A provider must officially document persistent Linux execution, persistent disk, CPU/RAM/storage limits, outbound bandwidth, permission for FFmpeg/video streaming, reclaim rules, and a reliable stop-before-charge mechanism. If no provider passes, do not deploy a public stream. Continue development and non-production testing without uploading the owner's videos.

A CI runner is for builds/tests, not a guaranteed always-on IPTV server. Do not describe free bandwidth as unlimited.

## 8. Viewer and audience measurement

Viewer apps consume the stable HLS playback endpoint and viewer-safe channel status. They do not control playback.

- One device counts as one viewer.
- Opening a channel does not count.
- Count one view after 10 minutes of continuous actual playback.
- Leaving earlier produces no counted view.
- Count at most once per session; track watch duration separately.
- Validate player heartbeats server-side; an open app alone is not proof.
- Short reconnects must not create duplicate views.
- The policy for counting the same device again after a completed session remains undecided.

## 9. Delivery sequence

1. Keep the Android project and CI baseline stable.
2. Implement and test the ordered playlist model and boundary-safe editing.
3. Prototype the server-side playback controller with authorized test media.
4. Verify a continuous HLS endpoint, transitions, looping, and VLC playback.
5. Measure resource and bandwidth usage.
6. Qualify a $0 hosting option before any public deployment.
7. Integrate the viewer clients only after the playback contract is proven.
8. Expand beyond RAMİ 1TV only after the single-channel pilot succeeds.

No phase is complete merely because its UI exists. Do not upload real owner videos or activate production credentials before hosting, security, and backup approval.
