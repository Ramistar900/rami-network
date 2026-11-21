package com.rami.network.domain

/** Supported media transport/container types. */
enum class SourceType {
    HLS,
    MP4,
    RTMP,
    SRT
}

/** Where content is managed before it is scheduled on a RAMI TV channel. */
enum class ContentSourceKind {
    APP_CONNECTOR,
    EXTERNAL_URL,
    LIVE_INPUT
}

/** Known first-party app connectors. OTHER is reserved for future approved apps. */
enum class AppConnectorType {
    ALMALAKI,
    ROYAL_TV,
    OTHER
}

/**
 * A source registered in RAMI NETWORK.
 *
 * App connectors expose only content explicitly made available for redistribution.
 * External and live sources are resolved/ingested by the backend, never by trusting
 * a URL supplied by a viewer device.
 */
data class ContentSource(
    val id: String,
    val kind: ContentSourceKind,
    val displayName: String,
    val sourceType: SourceType,
    val appConnector: AppConnectorType? = null,
    val appContentId: String? = null,
    val sourceUrl: String? = null,
    val rightsNote: String,
    val enabled: Boolean = true
)

/** A channel in the RAMI TV network. Channel numbers are assigned by the backend. */
data class Channel(
    val id: String,
    val number: Int,
    val name: String,
    val logoUrl: String?,
    val enabled: Boolean
)

/** A schedulable item in a channel's media library. */
data class MediaItem(
    val id: String,
    val channelId: String,
    val contentSourceId: String,
    val title: String,
    val durationSeconds: Int?,
    val rightsNote: String
)

/** An ordered item in a channel playlist. Repeated media may be intentional. */
data class PlaylistEntry(
    val channelId: String,
    val mediaItemId: String,
    val position: Int
)

/** A scheduled playlist window. Times are UTC epoch milliseconds. */
data class Schedule(
    val id: String,
    val channelId: String,
    val playlistId: String,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val enabled: Boolean
)

/** Server-authoritative playback position shared by all viewers of a channel. */
data class PlaybackState(
    val channelId: String,
    val mediaItemId: String,
    val startedAtEpochMillis: Long,
    val offsetAtStartMillis: Long,
    val live: Boolean
)
