package com.rami.network.domain

/**
 * A finite VOD item in a channel's shared playlist timeline.
 *
 * The stream URL is intentionally not stored here: media-source metadata is
 * managed separately, while this class only calculates synchronized position.
 */
data class TimelineEntry(
    val mediaItemId: String,
    val durationMillis: Long
)

/** The exact item and offset a viewer should play at the supplied server time. */
data class PlaybackPosition(
    val mediaItemId: String,
    val positionMillis: Long,
    val cycleNumber: Long,
    val playlistIndex: Int
)

/**
 * Resolves a deterministic, looping playlist position from one server anchor.
 *
 * Every viewer receives the same playlist, start timestamp, and server time
 * reference, so late joiners land on the same item and offset instead of
 * restarting the playlist locally.
 *
 * Live HLS inputs are not finite VOD entries and must be published as a
 * separate live playback anchor by the broadcast runtime.
 */
object PlaybackTimeline {

    fun resolve(
        entries: List<TimelineEntry>,
        playlistStartedAtEpochMillis: Long,
        nowEpochMillis: Long
    ): PlaybackPosition? {
        if (entries.isEmpty() || nowEpochMillis < playlistStartedAtEpochMillis) {
            return null
        }
        if (entries.any { it.mediaItemId.isBlank() || it.durationMillis <= 0L }) {
            return null
        }

        val totalDuration = try {
            entries.fold(0L) { total, entry ->
                Math.addExact(total, entry.durationMillis)
            }
        } catch (_: ArithmeticException) {
            return null
        }
        if (totalDuration <= 0L) return null

        val elapsed = try {
            Math.subtractExact(nowEpochMillis, playlistStartedAtEpochMillis)
        } catch (_: ArithmeticException) {
            return null
        }
        val cycleNumber = elapsed / totalDuration
        var positionInCycle = elapsed % totalDuration

        entries.forEachIndexed { index, entry ->
            if (positionInCycle < entry.durationMillis) {
                return PlaybackPosition(
                    mediaItemId = entry.mediaItemId,
                    positionMillis = positionInCycle,
                    cycleNumber = cycleNumber,
                    playlistIndex = index
                )
            }
            positionInCycle -= entry.durationMillis
        }

        // Modulo guarantees a match for a valid non-empty playlist.
        return null
    }
}
