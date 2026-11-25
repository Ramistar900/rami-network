# RAMİ NETWORK — Self-Hosted Broadcast Foundation

## Status

- Decision: RAMİ owns and operates the application code, broadcast engine, transcoding pipeline, channel configuration, and operational data.
- Initial pilot: one channel, up to 10 broadcast hours per day.
- Expansion target: four independent channels after the pilot passes reliability and resource tests.
- Hosting: a self-managed Linux server (cloud VM or physical machine). A cloud VM is only rented infrastructure; it is not a third-party broadcast platform.
- Cost guardrail: $0 only. No payment card, PayPal, billing activation, automatic upgrade, paid trial, or debt. If a free host cannot meet the requirements, deployment pauses rather than creating charges.
- No video uploads, live credentials, or production secrets are permitted during this foundation phase.

## 1. Ownership boundary

RAMİ owns and maintains these software components:

1. **RAMİ Control API** — authenticated administration, channel configuration, media metadata, schedules, publication, and audit history.
2. **RAMİ Broadcast Engine** — server-side scheduler and playout controller. It starts, stops, loops, and monitors channel output independently of the administrator's phone.
3. **FFmpeg** — media inspection, remuxing, and transcoding when required. Prefer stream copy when the source already matches the target format; transcoding is CPU-intensive and should not be performed unnecessarily.
4. **MediaMTX** — media routing and delivery. It supports publishing and reading streams over protocols including RTMP, SRT, WebRTC, RTSP, and HLS, and can expose operational metrics.
5. **RAMİ Storage** — the media data plane. Original video files live on storage controlled by RAMİ. The control database stores metadata and storage keys, not video binaries.
6. **PostgreSQL** — authoritative metadata, channel configuration, playlists, schedules, user roles, playback sessions, and audit events.

The selected host supplies only the underlying compute, disk, and network connection. It must not own the channel logic or require RAMİ to use a proprietary playout service.

## 2. Initial deployment shape

For the first pilot, keep the system small and separable:

- One Linux server.
- One RAMİ Control API process.
- One PostgreSQL instance.
- One MediaMTX process.
- One FFmpeg playout worker for the pilot channel.
- One persistent media directory/volume owned by RAMİ.
- HTTPS reverse proxy and firewall rules.
- Health checks, disk-space checks, and outbound-traffic counters.

Do not split into multiple servers or add Kubernetes, a CDN, Cloudflare Workers, Supabase, or a paid media platform during the pilot. Revisit separation only after measured load demonstrates a need.

## 3. Pilot channel behaviour

- Start with one test channel for up to 10 hours per day.
- The scheduler uses server time and a configured timezone, never the phone's clock.
- The playlist plays in its defined order and automatically returns to the first item when the end is reached.
- The channel must expose a stable HLS playback endpoint suitable for supported viewer clients and VLC.
- A stopped or unhealthy publisher must be reported as unavailable; the system must never display a false "live" state.
- The administrator's phone is only a control client. Closing it must not stop scheduled playback.
- Production library items must respect the agreed minimum file size of 500 MB. Synthetic test patterns may be used only for infrastructure smoke tests and must not be presented as real library content.

## 4. Media and transcoding policy

- Inspect the source file before scheduling it: container, video/audio codecs, dimensions, frame rate, duration, and readability.
- Prefer remuxing or stream copy when compatible with the target playback profile.
- Transcode only when required for compatibility or a selected quality profile.
- Do not assume that four simultaneous 1080p transcodes fit on a small free VM. Measure CPU, RAM, disk I/O, and thermal/host limits using the actual source media.
- The initial pilot should use one output profile. Adaptive bitrate profiles are a later phase after resource measurements.
- Keep the original source file unchanged. Generated playback renditions and temporary HLS segments must be stored separately and cleaned according to a documented retention policy.

## 5. Viewer delivery and bandwidth

The server sends a separate unicast media stream to each viewer unless a separately validated distribution layer is introduced. Outbound bandwidth therefore grows with viewer count.

For planning only, at 2.5 Mbps per viewer:
- One viewer for 10 hours is approximately 11.25 GB.
- Ten viewers watching one channel for 10 hours are approximately 112.5 GB.
- Four channels each running 10 hours with one viewer per channel are approximately 45 GB total.

These are estimates before protocol overhead and reconnects. A host must publish a clear outbound-traffic allowance and hard-stop behaviour. Do not describe any free quota as unlimited.

## 6. Audience measurement contract

- A device is one viewer; do not estimate the number of people in front of a TV.
- Opening a channel page does not count as a view.
- Count one view only after the player confirms continuous actual playback for 10 minutes.
- Leaving before 10 minutes produces no counted view.
- Count at most once per viewing session; do not add another view every 10 minutes.
- Track actual watch duration separately.
- Use player playback signals/heartbeats and server-side session validation. A merely open app or background page is not proof of playback.
- A short reconnect must not create a second view.
- The rule for when the same device may earn another view after ending a session remains undecided; do not invent that rule.

## 7. Security requirements

- No secrets, passwords, signing keys, stream tokens, or production URLs in Git.
- Use SSH keys, least-privilege server accounts, firewall allowlists where practical, HTTPS, and short-lived viewer tokens.
- Keep database and MediaMTX control/metrics interfaces private; expose only required viewer endpoints through the reverse proxy.
- Protect all administrative writes with server-side authentication and authorization.
- Keep an audit record for channel, playlist, schedule, storage, and user changes.
- Back up database metadata and the media inventory. A backup plan must be tested before production uploads.
- Never expose an administrator or storage credential to SINJAR TV SHOW or ROYAL+ Launcher.

## 8. Free-host qualification gate

No provider is selected yet. A candidate is rejected unless all of the following are verified from the provider's own current terms:

1. A persistent Linux server is allowed; not a 15-minute sandbox or sleeping application container.
2. No payment card, PayPal, paid trial, recurring subscription, billing activation, or automatic upgrade is required.
3. The exact CPU, RAM, disk, region, and reclaim/idle policy are documented.
4. Outbound bandwidth and transfer limits are documented and sufficient for the pilot test.
5. The provider permits video streaming and FFmpeg/MediaMTX workloads under its acceptable-use policy.
6. There is a reliable way to stop the service before any paid usage can accrue.
7. The account can remain at $0 without a debt or post-trial charge.

If no candidate passes, do not deploy a public broadcast service. Continue building and testing the software locally or in non-production CI without uploading the owner's videos.

## 9. Acceptance gates

### Gate A — configuration and security
- Server image and dependency versions are pinned.
- No secrets are committed.
- Firewall and authentication are reviewed.
- Quotas and stop conditions are written down.

### Gate B — one-channel technical test
- A rights-cleared test file is inspected successfully.
- FFmpeg starts and stops deterministically.
- MediaMTX publishes and serves the stream.
- HLS playback works in a compatible player and VLC.
- Playlist looping works across the end boundary.
- The administrator's phone can close without interrupting the server process.
- CPU, RAM, disk, and outbound traffic are recorded.

### Gate C — ten-hour pilot
- One channel completes the planned ten-hour window.
- No unexplained playback gap or false-live status occurs.
- Recovery after a process restart is verified.
- View counting follows the 10-minute rule.
- No quota or cost guardrail is crossed.

Only after all gates pass may the design be expanded to the four channels.

## 10. Explicitly out of scope for this foundation

- Selecting or activating a hosting provider.
- Creating a paid account or enabling billing.
- Uploading the owner's video library.
- Adding production stream keys or viewer credentials.
- Promising unlimited free bandwidth or guaranteed 24/7 availability.
- Changing SINJAR TV SHOW or ROYAL+ Launcher before inspecting their current integration contracts.
