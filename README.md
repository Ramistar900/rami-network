# RAMI NETWORK

**RAMİ TV Network Management Platform**

- **RAMI NETWORK** — owner-only app for channels, media sources, rights approval, playback state, branding and broadcast operations.
- **SINJAR TV SHOW** — viewer application.
- **ROYAL+ Launcher** — viewer client for the Sinjar TV section.
- Viewer applications consume published channel data and playback state; they do not administer the network.

## Connected services
- Supabase project: `ymujjcdsdclptbpgckll`.
- Owner control API: `rami-network-control` (JWT required; owner-account restricted).
- Public viewer API: `rami-network-viewer` (read-only published data).
- Private database schema: `rami_network`.
- Android app includes only the Supabase URL and publishable key. Service-role keys and other secrets remain server-side.

## Playback architecture
All viewers use the same server-generated channel state and anchor. The VOD timeline is calculated against server time. Live playback requires a final HLS output from a separately provisioned, rights-cleared broadcast service. Supabase stores metadata and synchronized playback state; it does not host, transcode, or relay long-form video.

## Build
GitHub Actions runs unit tests and builds the debug APK using Java 17 and Gradle 8.10.2.

## Free-tier requirement
Infrastructure must remain within actual free-tier quotas. No billing, payment method, or paid service may be introduced without explicit approval.
