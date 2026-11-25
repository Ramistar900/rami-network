package com.rami.network.domain

/**
 * Runtime state for one ordered, looping IPTV playlist.
 *
 * This is a pure domain controller: it does not perform media I/O or claim that
 * a stream is live. A server-side player must call [onItemEnded] only after it
 * confirms that the current media file has actually finished.
 */
data class PlaylistControllerState(
    val entries: List<TimelineEntry>,
    val currentIndex: Int,
    val cycleNumber: Long,
    val isRunning: Boolean,
    val pendingEntries: List<TimelineEntry>? = null
) {
    val currentEntry: TimelineEntry?
        get() = if (isRunning) entries.getOrNull(currentIndex) else null

    val hasPendingChanges: Boolean
        get() = pendingEntries != null
}

/**
 * Deterministic playlist state transitions for the first RAMİ 1TV prototype.
 *
 * Playlist edits requested during playback are staged and become active only
 * when the current item ends. This prevents an edit from cutting off a video.
 */
object PlaylistPlaybackController {

    /** Starts at the first item. Returns null for an empty or invalid playlist. */
    fun start(entries: List<TimelineEntry>): PlaylistControllerState? {
        if (!isValid(entries) || entries.isEmpty()) return null
        return PlaylistControllerState(
            entries = entries.toList(),
            currentIndex = 0,
            cycleNumber = 0L,
            isRunning = true
        )
    }

    /** Stops playback and discards any not-yet-applied playlist edit. */
    fun stop(state: PlaylistControllerState): PlaylistControllerState =
        state.copy(isRunning = false, pendingEntries = null)

    /**
     * Requests a new order.
     *
     * While playing, the new order is staged until the current item ends.
     * While stopped, it is saved immediately but playback remains stopped.
     * An empty list is a valid edit and stops playback at the next boundary.
     * Invalid media entries are rejected by returning null.
     */
    fun requestPlaylistUpdate(
        state: PlaylistControllerState,
        newEntries: List<TimelineEntry>
    ): PlaylistControllerState? {
        if (!isValid(newEntries)) return null
        val snapshot = newEntries.toList()

        if (state.isRunning) {
            return state.copy(pendingEntries = snapshot)
        }

        return state.copy(
            entries = snapshot,
            currentIndex = 0,
            pendingEntries = null
        )
    }

    /**
     * Advances after the player confirms end-of-file.
     *
     * If an edit is pending, it is applied first. If the current item remains
     * in the new order, playback advances to the item after it. If it was
     * removed, the item now occupying its former position is selected.
     */
    fun onItemEnded(state: PlaylistControllerState): PlaylistControllerState {
        if (!state.isRunning || state.entries.isEmpty()) return state

        val pending = state.pendingEntries
        if (pending != null) {
            if (pending.isEmpty()) {
                return state.copy(
                    entries = emptyList(),
                    currentIndex = 0,
                    isRunning = false,
                    pendingEntries = null
                )
            }

            val currentId = state.entries.getOrNull(state.currentIndex)?.mediaItemId
            val retainedIndex = pending.indexOfFirst { it.mediaItemId == currentId }
            val nextIndex = if (retainedIndex >= 0) {
                (retainedIndex + 1) % pending.size
            } else {
                state.currentIndex.coerceAtMost(pending.lastIndex)
            }
            val wrapped = retainedIndex >= 0 && retainedIndex == pending.lastIndex

            return state.copy(
                entries = pending,
                currentIndex = nextIndex,
                cycleNumber = if (wrapped) incrementCycle(state.cycleNumber) else state.cycleNumber,
                pendingEntries = null
            )
        }

        val nextIndex = state.currentIndex + 1
        return if (nextIndex < state.entries.size) {
            state.copy(currentIndex = nextIndex)
        } else {
            state.copy(
                currentIndex = 0,
                cycleNumber = incrementCycle(state.cycleNumber)
            )
        }
    }

    private fun isValid(entries: List<TimelineEntry>): Boolean {
        if (entries.isEmpty()) return true
        if (entries.any { it.mediaItemId.isBlank() || it.durationMillis <= 0L }) return false

        return try {
            entries.fold(0L) { total, entry ->
                Math.addExact(total, entry.durationMillis)
            }
            true
        } catch (_: ArithmeticException) {
            false
        }
    }

    private fun incrementCycle(value: Long): Long =
        if (value == Long.MAX_VALUE) Long.MAX_VALUE else value + 1L
}
