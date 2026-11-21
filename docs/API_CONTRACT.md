# RAMİ TV Viewer API Contract (Draft)

This document defines the first provider-neutral contract between the central RAMİ TV service and viewer applications. It is a design contract, not a claim that an API server exists.

## Rules

- Base path is versioned: `/api/v1`.
- Viewer endpoints are read-only and return published channels only.
- Admin endpoints require authenticated administrator authorization.
- All timestamps use ISO 8601 UTC unless a schedule explicitly includes an IANA timezone.
- Every response includes a stable identifier; display names and channel numbers are not identifiers.

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

Returns the server-authoritative playback anchor.

Example response:

```json
{
  "channelId": "channel-uuid",
  "sequence": 12,
  "serverTime": "2026-01-01T12:00:00Z",
  "item": {
    "id": "media-uuid",
    "title": "Programme title",
    "streamUrl": "https://stream.example.invalid/live/index.m3u8",
    "format": "hls"
  },
  "startedAt": "2026-01-01T11:45:00Z",
  "positionSeconds": 900,
  "status": "playing"
}
```

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
