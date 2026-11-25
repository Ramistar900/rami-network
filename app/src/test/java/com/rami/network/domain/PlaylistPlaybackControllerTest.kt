package com.rami.network.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistPlaybackControllerTest {

    private val first = TimelineEntry("video-1", 10_000L)
    private val second = TimelineEntry("video-2", 20_000L)
    private val third = TimelineEntry("video-3", 30_000L)
    private val playlist = listOf(first, second, third)

    @Test
    fun startsWithFirstVideo() {
        val state = PlaylistPlaybackController.start(playlist)

        assertEquals(first, state?.currentEntry)
        assertEquals(0, state?.currentIndex)
        assertTrue(state?.isRunning == true)
    }

    @Test
    fun advancesToNextVideoOnlyWhenCurrentEnds() {
        val started = PlaylistPlaybackController.start(playlist)!!

        val next = PlaylistPlaybackController.onItemEnded(started)

        assertEquals(second, next.currentEntry)
        assertEquals(0L, next.cycleNumber)
    }

    @Test
    fun loopsFromLastVideoToFirstAndIncrementsCycle() {
        var state = PlaylistPlaybackController.start(playlist)!!
        state = PlaylistPlaybackController.onItemEnded(state)
        state = PlaylistPlaybackController.onItemEnded(state)
        state = PlaylistPlaybackController.onItemEnded(state)

        assertEquals(first, state.currentEntry)
        assertEquals(1L, state.cycleNumber)
    }

    @Test
    fun playlistEditsWaitUntilCurrentVideoEnds() {
        val started = PlaylistPlaybackController.start(playlist)!!
        val updated = PlaylistPlaybackController.requestPlaylistUpdate(
            started,
            listOf(first, third)
        )!!

        assertEquals(first, updated.currentEntry)
        assertEquals(playlist, updated.entries)
        assertTrue(updated.hasPendingChanges)

        val afterBoundary = PlaylistPlaybackController.onItemEnded(updated)

        assertEquals(listOf(first, third), afterBoundary.entries)
        assertEquals(third, afterBoundary.currentEntry)
        assertFalse(afterBoundary.hasPendingChanges)
    }

    @Test
    fun reorderedPlaylistContinuesAfterCurrentVideo() {
        var state = PlaylistPlaybackController.start(playlist)!!
        state = PlaylistPlaybackController.requestPlaylistUpdate(
            state,
            listOf(third, first, second)
        )!!

        state = PlaylistPlaybackController.onItemEnded(state)

        assertEquals(listOf(third, first, second), state.entries)
        assertEquals(second, state.currentEntry)
    }

    @Test
    fun removingCurrentVideoSelectsItemAtItsFormerPosition() {
        var state = PlaylistPlaybackController.start(playlist)!!
        state = PlaylistPlaybackController.onItemEnded(state) // video-2
        state = PlaylistPlaybackController.requestPlaylistUpdate(
            state,
            listOf(first, third)
        )!!

        state = PlaylistPlaybackController.onItemEnded(state)

        assertEquals(third, state.currentEntry)
    }

    @Test
    fun emptyPendingPlaylistStopsAtCurrentVideoBoundary() {
        val started = PlaylistPlaybackController.start(playlist)!!
        val updated = PlaylistPlaybackController.requestPlaylistUpdate(started, emptyList())!!

        assertEquals(first, updated.currentEntry)
        assertTrue(updated.isRunning)

        val ended = PlaylistPlaybackController.onItemEnded(updated)

        assertNull(ended.currentEntry)
        assertFalse(ended.isRunning)
        assertTrue(ended.entries.isEmpty())
    }

    @Test
    fun stoppedPlaylistCanBeEditedWithoutStartingPlayback() {
        val started = PlaylistPlaybackController.start(playlist)!!
        val stopped = PlaylistPlaybackController.stop(started)
        val updated = PlaylistPlaybackController.requestPlaylistUpdate(stopped, listOf(third))!!

        assertFalse(updated.isRunning)
        assertEquals(third, updated.entries.single())
        assertNull(updated.currentEntry)
    }

    @Test
    fun rejectsInvalidPlaylistAndCannotStartEmptyList() {
        assertNull(PlaylistPlaybackController.start(emptyList()))
        assertNull(
            PlaylistPlaybackController.requestPlaylistUpdate(
                PlaylistPlaybackController.start(playlist)!!,
                listOf(TimelineEntry("bad", 0L))
            )
        )
    }

    @Test
    fun stopDiscardsPendingEdit() {
        val started = PlaylistPlaybackController.start(playlist)!!
        val updated = PlaylistPlaybackController.requestPlaylistUpdate(started, listOf(third))!!
        val stopped = PlaylistPlaybackController.stop(updated)

        assertFalse(stopped.isRunning)
        assertFalse(stopped.hasPendingChanges)
        assertEquals(playlist, stopped.entries)
    }
}
