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

## 7. Security

- Keep the repository public only for source code; never commit secrets, signing keys, private stream credentials, or admin tokens.
- Use server-side authorization for every write.
- Viewer endpoints expose published data only.
- Keep audit records for channel, playlist, schedule, and branding changes.
- Use HTTPS and short-lived access tokens.
- Never embed a privileged API key in the Android APK.

## 8. Delivery sequence

1. Android shell and CI baseline. (Started)
2. Domain models, local validation, and automated unit tests.
3. Provider-neutral API contract and security model.
4. Compare free-tier providers and choose only after limits are understood.
5. Implement authenticated channel CRUD and transactional numbering.
6. Implement media library and playlist ordering.
7. Implement schedule publication and server-authoritative playback timeline.
8. Integrate viewer clients and test synchronized playback.
9. Add release signing and automatic GitHub Releases after a verified build.

No later phase is considered complete merely because its UI exists.
