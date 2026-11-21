# RAMI NETWORK

**RAMİ TV Network Management Platform**

RAMI NETWORK is the independent administration application and service workspace for the RAMİ TV multi-channel network.

## Product boundaries

- **RAMI NETWORK** — create and manage channels, media libraries, playlists, schedules, channel identity, and broadcast operations.
- **SINJAR TV SHOW** — viewer application.
- **ROYAL+ Launcher** — viewer client for the Sinjar TV section.
- Viewer applications consume published channel data; they do not administer the network.

## Core principles

1. A channel has its own identity, media library, and ordered playlist.
2. Channel numbering is assigned automatically; channel names and logos can be customized.
3. Published channel changes are delivered through a central service/API without requiring viewer-app updates.
4. Scheduled playback is controlled by a continuously available broadcast service, not by the administration app or an individual viewer device.
5. Streaming should use supported streaming formats such as HLS where the media source and rights permit.
6. Content must be owned, licensed, or otherwise authorized for distribution.
7. Infrastructure must be evaluated against actual free-tier limits. No paid service, billing account, or payment method may be introduced without explicit approval.

## Repository status

This repository is being initialized. No production backend, streaming service, or Android application is claimed to exist yet.

## Planned work areas

- Android administration client
- Channel and playlist data model
- Authentication and role-based access
- Central channel catalogue/API
- Scheduling and broadcast runtime
- Viewer integration contracts
- Automated checks and release workflows

## Security

Never commit credentials, signing keys, private tokens, or service secrets. Store deployment secrets in the appropriate GitHub or service secret manager.

## Language

Project discussions and user-facing administration are expected to support Arabic, Turkish, and English.
