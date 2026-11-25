# RAMİ NETWORK — Simple IPTV Playlist

## Product decision

RAMİ NETWORK is a simple, owner-controlled IPTV playlist system. It is not a satellite television station, studio, or complex broadcast scheduling platform.

The owner uploads authorized video files, chooses their order, and controls one channel. The server plays the list continuously:

1. Play video 1 until it ends.
2. Start video 2 automatically, then video 3, and so on.
3. After the final video, return to the first video and repeat.
4. The owner can add, remove, or reorder items while the channel is running.
5. The owner can start or stop the channel from the Android administration app.

The first pilot is one channel: **RAMİ 1TV**. Do not build the other planned channels until this pilot is proven.

## What the first version needs

Keep the system to the smallest reliable set of parts:

- **RAMİ NETWORK Android app** — the private control panel for uploading videos, editing the ordered playlist, and starting/stopping the channel.
- **Small server-side control service** — authenticates the owner, stores playlist changes, and reports the confirmed channel state.
- **RAMİ Storage** — persistent disk space on the server for the original video files.
- **Playlist playback controller** — runs continuously on the server, selects the next item, handles looping, and applies playlist edits safely at video boundaries.
- **FFmpeg** — inspects media and prepares or serves compatible playback. Prefer stream copy/remux where possible; transcode only when compatibility requires it.
- **HLS output** — one stable playback address that supported IPTV players and VLC can open.

Do not add PostgreSQL, MediaMTX, a CDN, Kubernetes, Cloudflare Workers, Supabase, or a paid media platform by default. Add a component only if a measured technical requirement proves it is necessary. A small local metadata store or playlist file is sufficient for the first prototype.

## Playlist behaviour and safe editing

- The server, not the phone, owns playback timing and the current item.
- The current video should finish before ordinary playlist edits take effect.
- Additions, removals, and reordering are applied at the next video boundary.
- Removing the currently playing item must not abruptly cut it off; the new list takes effect after it ends.
- If the playlist is empty, the channel is stopped/unavailable. Never report a false live state.
- If a file is missing or unreadable, report the error and follow a documented safe skip/stop policy; never silently claim that the missing video is playing.
- Restart recovery must restore the saved playlist and resume from a clearly defined safe point. Do not claim frame-accurate resume unless it is tested.
- The HLS playback address must remain usable across transitions. The implementation must be tested in VLC and the intended viewer apps; merely generating separate HLS files for each video is not sufficient proof of a continuous channel.

## Media library rules

- Only upload content the owner owns or is authorized to distribute.
- The agreed minimum production-library file size is **500 MB**. Do not treat tiny synthetic clips as real library content.
- Keep each original file unchanged.
- Inspect container, codecs, dimensions, frame rate, duration, and readability before adding a video to the playlist.
- Store playback outputs and temporary HLS segments separately from original uploads.
- Track available disk space and prevent uploads that would exceed the configured safe storage limit.
- Uploads should be resumable where practical and verified before the item becomes available in the playlist.

## Server and independence

- The playback controller and media files live on a persistent Linux server.
- Closing the owner's phone or uninstalling the admin app must not interrupt playback.
- The server must have persistent storage and a continuously running process for the hours the channel is intended to broadcast.
- A server restart must not lose the playlist or original videos.
- Do not select or activate a hosting provider until its current free limits and terms pass the gate below.

## Zero-cost hosting gate

The budget is **$0 only**. No payment card, PayPal, billing activation, paid trial, automatic upgrade, subscription, or debt.

A hosting candidate is rejected unless current official provider terms confirm:

1. A persistent Linux process is allowed; not a short-lived sandbox or sleeping app.
2. Persistent disk is available and its limits are clear.
3. CPU, RAM, storage, region, reclaim/idle rules, and outbound bandwidth are documented.
4. Video streaming and the required FFmpeg workload are permitted.
5. There is a reliable way to stop the workload before any paid usage can accrue.
6. The account can remain at $0 without post-trial charges.

A free Android CI runner is not a production IPTV server. If no provider passes, continue software prototyping and tests without public streaming or uploading the owner's real videos. Never silently switch to a paid service.

## Bandwidth reality

HLS delivery to viewers generally uses outbound data for each viewer. As a planning example, one 2.5 Mbps viewer watching for 10 hours consumes about 11.25 GB before protocol overhead and reconnects. Ten viewers consume about 112.5 GB over the same period. No free quota may be described as unlimited.

## Security and ownership

- Never commit passwords, API tokens, stream keys, signing keys, or production URLs.
- Protect all owner controls with server-side authentication and authorization.
- Keep administration endpoints and storage credentials private.
- Viewer apps receive only the published playback address and viewer-safe channel information, never administrative credentials.
- Use HTTPS for administration and playback where supported.
- Keep a backup of the playlist and media inventory; test recovery before production uploads.

## View counting contract

- One television/device is one viewer.
- Opening the channel or player does not count as a view.
- Count one view only after continuous actual playback reaches 10 minutes.
- If the device leaves before 10 minutes, count no view.
- Count at most one view per viewing session; do not add another view every 10 minutes.
- Track actual watch duration separately.
- Use player playback signals and server-side session validation; an open app alone is not proof of playback.
- A short reconnect must not create a second view.
- The rule for when the same device can earn another view after a session ends remains undecided. Do not invent it.

## Acceptance tests

### Gate A — safe prototype
- No secrets in Git.
- Playlist ordering, editing, looping, and empty-list behaviour have automated tests.
- Missing/unreadable files are handled explicitly.
- Changes are applied at video boundaries without corrupting the saved list.

### Gate B — one-channel playback
- A rights-cleared test file is inspected.
- At least two compatible test videos play in order, then loop to the first.
- The HLS address remains playable through transitions in VLC and a supported viewer.
- Add/remove/reorder operations take effect at the next boundary.
- Closing the phone does not stop server playback.
- Restart recovery and CPU/RAM/disk/outbound usage are measured.

### Gate C — real pilot
- Only after a hosting candidate passes the $0 gate and the owner approves it.
- Run the intended daily schedule and monitor gaps, restarts, storage, and bandwidth.
- Verify the 10-minute view-counting rule.
- Stop before any quota or cost guardrail is crossed.

## Explicitly not in the first version

- Satellite reception, studio equipment, or traditional broadcast scheduling.
- Multiple channels before RAMİ 1TV passes its pilot.
- Complex programme grids, ad insertion, subscriptions, or paid streaming platforms.
- CDN or multi-server deployment before measured need.
- Uploading the owner's real video library before hosting, security, and backup are approved.
