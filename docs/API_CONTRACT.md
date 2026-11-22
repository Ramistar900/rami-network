# RAMİ TV Viewer API Contract (Draft)

This document defines the provider-neutral contract between RAMİ NETWORK and viewer applications. It is a design contract, not a claim that the API or broadcast runtime is deployed.

## Rules

- Base path is versioned: `/api/v1`.
- Viewer endpoints are read-only and return published channels only.
- Admin endpoints require authenticated administrator authorization.
- All timestamps use ISO 8601 UTC unless a schedule explicitly includes an IANA timezone.
- Every response includes a stable identifier; display names and channel numbers are not identifiers.
- Supabase stores channel configuration, source metadata, playlists, schedules, and playback state. It does not generate or relay the video stream.

## Viewer endpoints

### GET /api/v1/channels

Returns the published channel list.

Example response:

```json
{
  "version": 1,
  "serverTime": "2026-01-01T12:00:00Z",
  "channels": [
    {
      "id": "channel-uuid",
      "number": 1,
      "name": "RAMI TV",
      "logoUrl": "https://cdn.example.invalid/logo.png",
      "enabled": true,
      "stateUrl": "/api/v1/channels/channel-uuid/state"
    }
  ]
}
```

### GET /api/v1/channels/{channelId}/state

Returns the authoritative state for the channel. Both SINJAR TV SHOW and ROYAL+ must consume this same state; neither viewer app may start its own independent playlist clock.

Example response for a continuous HLS channel output:

```json
{
  "channelId": "channel-uuid",
  "sequence": 12,
  "serverTime": "2026-01-01T12:00:00Z",
  "status": "playing",
  "stream": {
    "url": "https://stream.example.invalid/channel-1/index.m3u8",
    "format": "hls"
  },
  "currentItem": {
    "id": "media-uuid",
    "title": "Programme title"
  },
  "startedAt": "2026-01-01T11:45:00Z"
}
```

The channel HLS URL is a stable output produced by the independent broadcast runtime. It is not a Supabase Storage URL unless a separately reviewed media-hosting design explicitly chooses that approach.

For clients that play a finite HLS VOD playlist directly rather than a stitched channel output, the server must publish a common playlist anchor (ordered item IDs, each item's known duration, and one UTC start timestamp). Viewers calculate the same current item and offset from that anchor and server time. The pure calculation is covered by `PlaybackTimelineTest` in the Android project. Live HLS inputs do not have a predictable finite duration and must be represented as a separate live event/anchor, not treated as ordinary finite playlist items.

Clients should estimate server-time offset from the response, periodically refresh state or subscribe to a supported update mechanism, and re-check when `sequence` changes. They must not trust the device wall clock as the authoritative schedule clock.

The example hostnames are reserved placeholders, not real stream URLs.

## Admin endpoint families (planned)

- `/api/v1/admin/channels` — create, update, enable/disable, and publish channels.
- `/api/v1/admin/media` — register and validate media source metadata.
- `/api/v1/admin/channels/{channelId}/playlist` — add, remove, and reorder playlist entries.
- `/api/v1/admin/schedules` — create and validate broadcast schedules.
- `/api/v1/admin/audit` — read administrative change history.

## Error shape

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "The supplied schedule is invalid.",
    "requestId": "request-uuid"
  }
}
```

Do not expose stack traces, secrets, internal database details, or privileged credentials in API errors.
