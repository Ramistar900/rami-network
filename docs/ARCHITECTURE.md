# RAMI NETWORK Architecture

## 1. System boundary

RAMI NETWORK is the private administration surface for the RAMİ TV network. It must not become a viewer app.

- Admin Android app: manage channels, assets/references, playlists, schedules, branding, and operational status.
- Central API and database: store authoritative channel configuration and publish viewer-safe data.
- Broadcast runtime: continuously determine what each channel is playing and expose a stable live stream or playback timeline.
- SINJAR TV SHOW and ROYAL+ Launcher: read published channel data and play channels. They never receive administrative credentials.

The admin app must not be responsible for keeping a channel on air. Closing or uninstalling it must not interrupt scheduled playback.

## 2. Logical components

### Administration client
- Arabic-first UI with Arabic, Turkish, and English localization.
- Administrator sign-in; no public owner/customer registration flow.
- Calls authenticated management endpoints.
- Shows server-confirmed state; never reports a change as saved before the API confirms it.

### Central control plane
- Authentication and role checks on every management request.
- Channel catalogue and automatic channel numbering.
- Media-source metadata and per-channel ordered playlists.
- Schedule validation, publication, and audit history.
- A read-only public/viewer API containing only published channel information.

### Broadcast data plane
- Runs independently of the Android admin app.
- Uses server time as the authoritative clock.
- Maintains a deterministic playback timeline for each channel.
- Publishes HLS or another appropriate stream format when the selected source and hosting setup support it.
- Must report offline/unavailable sources rather than silently pretending that playback is live.

## 3. Initial data model

- Channel: stable UUID, automatically assigned channel number, display name, optional logo URL, enabled state, publication state, timestamps.
- Media item: stable UUID, channel ID, title, source URL, source type, optional duration, rights/authorization note, enabled state.
- Playlist entry: channel ID, media item ID, explicit position, optional playback window.
- Schedule: channel ID, timezone, start/end timestamps, playlist or programme reference, enabled state.
- Playback state: channel ID, current item, server start time, elapsed offset, sequence/version, health state.

Channel numbers must be unique and assigned transactionally on the server. Deleting a channel must not silently renumber existing channels.

## 4. Synchronization contract

Viewer apps fetch a published channel catalogue and then refresh channel state periodically or subscribe to a supported update mechanism. The response includes a version and server timestamp.

For a continuous playlist, the server publishes a playback anchor (item ID + server start timestamp + sequence). Viewers calculate the current position from server time and the published timeline; their local clocks must not independently decide what programme is on air.

A change is not visible to viewers until it is validated and published.

## 5. Media and rights

- Prefer direct HTTPS sources that support HLS or byte-range streaming.
- Do not proxy or re-host third-party media without permission.
- Validate URLs and supported schemes. If the backend ever fetches remote URLs, protect against private-network/metadata-service access and DNS rebinding.
- Store source metadata, not full video files, in the channel database unless a separately approved storage design is adopted.
- Track the distribution rights or authorization basis for each source.

## 6. Free-first infrastructure policy

No provider is selected yet. Before choosing one, compare current free-tier limits for database, API compute, always-on scheduling, storage, and outbound bandwidth.

A true 24/7 multi-viewer stream can exceed free quotas, especially outbound bandwidth. Do not describe a service as permanently free or deploy a paid resource automatically. If free limits are exhausted, fail safely, preserve configuration, and notify the administrator. No card, PayPal, billing activation, or paid upgrade without explicit approval.

## 7. RAMİ STORAGE architecture

RAMİ STORAGE is the network's own media-storage system and logical namespace. It is not tied permanently to one cloud vendor or one physical server.

### Separation of responsibilities
- RAMİ NETWORK control plane: owns media records, titles, rights, channel/playlist relationships, upload state, storage usage, and audit history.
- RAMİ STORAGE data plane: stores the actual video files and serves authorized playback bytes.
- The database stores object keys and metadata, never the video binary itself.
- Viewer apps receive playback URLs only for published, rights-approved media. They never receive admin credentials or storage-provider secrets.

### Provider-neutral storage contract
Implement a replaceable storage adapter with operations for:
1. Requesting a short-lived upload authorization for a unique object key.
2. Uploading directly from the admin app to the storage provider, with resumable/chunked transfer for large files.
3. Checking that the uploaded object exists and recording its actual size and content type.
4. Issuing short-lived playback access for approved media.
5. Deleting an object only after an explicit, audited admin action and successful provider confirmation.
6. Reporting storage and transfer usage where the provider exposes those measurements.

The Android app must not send large video bodies through a Supabase Edge Function. The API should authorize an upload, then the phone should transfer the bytes directly to the storage provider. Never put a service-role key, S3 secret, or permanent upload credential in the APK.

### Upload lifecycle
1. Admin selects a local video using Android's system file picker.
2. The app displays the detected filename, file size, and media type before upload.
3. The authenticated control API checks the owner's session, file policy, available quota, and a unique media ID.
4. The API issues a short-lived, single-object upload authorization.
5. The app uploads directly, showing progress and allowing retry/resume where supported.
6. The server verifies the completed object and its size/type before marking it ready.
7. Only then can the admin add the media to a channel playlist.
8. Playback is tested in a viewer app before the media is treated as production-ready.

An interrupted or failed upload remains an incomplete draft and must never appear in a published playlist. Do not trust client-supplied file size, duration, MIME type, or completion status without server-side verification. If reliable duration inspection is unavailable on the chosen free host, leave duration unknown and do not schedule that item as a finite-duration programme.

### Ownership and migration
- Use stable RAMİ-owned media IDs and provider-neutral object keys, for example: media/{media-id}/original/{filename}.
- Store the provider name and object key separately from the media ID.
- Do not expose provider-specific URLs as permanent identifiers.
- Keep an export/migration manifest so objects can later be copied to a self-hosted S3-compatible server without changing channel IDs, playlists, or viewer contracts.
- A self-hosted backend is the long-term ownership path, but it requires always-on hardware, storage disks, power, and a reliable internet connection. Open-source software alone does not provide those resources for free.

### Free-tier guardrails and current assessment
Supabase Free is suitable only for a small proof of concept, not a dependable TV video library: its current published limits include 1 GB file storage, a 50 MB maximum upload size, and 10 GB total monthly egress (5 GB cached plus 5 GB uncached). Large video uploads and repeated playback can exhaust these limits quickly.

Therefore:
- Do not use Supabase Storage as the permanent RAMİ TV video-delivery origin.
- Do not create a production video bucket until a provider and its real limits are approved.
- Keep Supabase for authentication, control-plane metadata, and short-lived upload/playback authorization where appropriate.
- Evaluate an owned, self-hosted S3-compatible storage node for the durable media plane. Until suitable hardware and network capacity are confirmed, use only a tightly capped test dataset and never promise 24/7 or unlimited free streaming.
- Add quota preflight checks, usage warnings, and a hard stop before a free quota can be exceeded. Never auto-upgrade, enable billing, or delete existing media to make room.

## 8. Security

- Keep the repository public only for source code; never commit secrets, signing keys, private stream credentials, or admin tokens.
- Use server-side authorization for every write.
- Viewer endpoints expose published data only.
- Keep audit records for channel, playlist, schedule, and branding changes.
- Use HTTPS and short-lived access tokens.
- Never embed a privileged API key in the Android APK.

## 9. Delivery sequence

1. Android shell and CI baseline. (Started)
2. Domain models, local validation, and automated unit tests.
3. Provider-neutral API contract and security model.
4. Define RAMİ STORAGE's provider adapter, media lifecycle, quota guardrails, and migration format. (Design started)
5. Confirm available always-on hardware/network and compare free storage options before selecting a data-plane host.
6. Implement authenticated channel CRUD and transactional numbering.
7. Implement media upload, verification, metadata, and playlist ordering against the selected storage adapter.
8. Implement schedule publication and server-authoritative playback timeline.
9. Integrate viewer clients and test synchronized playback.
10. Add release signing and automatic GitHub Releases after a verified build.

No later phase is considered complete merely because its UI exists.
