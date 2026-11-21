# RAMI NETWORK

**RAMİ TV Network Management Platform**

RAMI NETWORK is the independent administration application for the RAMİ TV multi-channel network.

## Product boundaries

- **RAMI NETWORK** — administrator-only app for channel setup, media libraries, playlists, schedules, branding, and broadcast operations.
- **SINJAR TV SHOW** — viewer application.
- **ROYAL+ Launcher** — viewer client for the Sinjar TV section.
- Viewer applications consume published channel data; they do not administer the network.

## Current repository state

The initial Android/Kotlin/Jetpack Compose project is now on `main`, with an Arabic-first black-and-gold dashboard shell and an Android CI workflow. The dashboard cards are placeholders: channel management, media storage, scheduling, authentication, and live broadcast are **not implemented yet**.

See [the architecture plan](docs/ARCHITECTURE.md) and [the viewer API contract](docs/API_CONTRACT.md).

## Core product rules

1. Each channel has its own identity, media library, and ordered playlist.
2. Channel numbers are assigned automatically; channel names and logos can be customized.
3. Published changes reach viewer apps through a central API without requiring app updates.
4. Playback timing is controlled by a continuously available server-side broadcast service, not by the admin app or an individual viewer device.
5. Streaming should use HLS or another suitable streaming protocol when the source and distribution rights permit it. Viewers should not need to download an entire long video before playback.
6. Content must be owned, licensed, or otherwise authorized for distribution.
7. Infrastructure must be assessed against real free-tier limits. No paid service, billing account, or payment method may be introduced without explicit approval.

## Build

The GitHub Actions workflow `.github/workflows/android-ci.yml` builds the debug APK on pushes and pull requests targeting `main`. The project currently uses Android Gradle Plugin 8.7.3, Kotlin 2.2.0, Java 17, and compile SDK 35.

## Security

Never commit credentials, signing keys, private tokens, or service secrets. Use repository/service secret storage. Administrative APIs must enforce server-side authentication and authorization; hiding controls in the app is not a security boundary.

## Languages

The product is planned to support Arabic, Turkish, and English.
