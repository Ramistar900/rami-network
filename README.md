# RAMİ NETWORK

**Simple IPTV playlist management**

RAMİ NETWORK is the private Android administration app for the owner's IPTV channel.

## How it works

- Upload video files that you own or are authorized to distribute.
- Choose their playback order.
- The server plays video 1 to its end, then video 2, then video 3, and so on.
- After the last video, playback returns to the first video and repeats.
- Add, remove, or reorder videos while the channel is running; ordinary edits take effect at the next video boundary.
- Start or stop the channel from the administration app.

The first pilot is **RAMİ 1TV**. Keep the first version simple; do not build a satellite station, studio workflow, or complex broadcast scheduler.

## Product boundaries

- **RAMİ NETWORK** — owner-only control app for the video library, ordered playlist, and channel status.
- **SINJAR TV SHOW** — viewer application.
- **ROYAL+ Launcher** — viewer client.
- Viewer apps consume the published HLS playback address and viewer-safe status. They never administer the network.

## Current repository state

The Android/Kotlin/Jetpack Compose project and Android CI baseline are on `main`. The dashboard is an early shell; real upload, server control, continuous playlist playback, and production HLS delivery are not yet complete.

See [the simple IPTV architecture](docs/ARCHITECTURE.md), [the playback foundation and safety gates](docs/SELF_HOSTED_BROADCAST.md), and [the viewer API contract](docs/API_CONTRACT.md).

## Important rules

1. The server, not the phone, controls playback.
2. Each channel has one ordered playlist that loops automatically.
3. The HLS endpoint must remain playable across video transitions and be tested in VLC and target viewer apps.
4. Production library videos must be at least 500 MB, as agreed.
5. Content must be owned or authorized for distribution.
6. Hosting budget is **$0 only**. No card, PayPal, billing activation, paid trial, automatic upgrade, or debt.
7. Do not upload the owner's real videos or activate production credentials until hosting, security, and backup are approved.

## Build

The GitHub Actions workflow `.github/workflows/android-ci.yml` builds the debug APK on pushes and pull requests targeting `main`. The project uses Android Gradle Plugin 8.7.3, Kotlin 2.2.0, Java 17, and compile SDK 35.

## Security

Never commit credentials, signing keys, private tokens, or service secrets. Owner actions must be authenticated and authorized on the server; hiding controls in the app is not a security boundary.

## Languages

The product is planned to support Arabic, Turkish, and English.
