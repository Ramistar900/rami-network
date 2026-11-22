package com.rami.network.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackTimelineTest {

    private val playlist = listOf(
        TimelineEntry("episode-1", 10_000L),
        TimelineEntry("episode-2", 20_000L),
        TimelineEntry("episode-3", 30_000L)
    )

    @Test
    fun startsAtFirstItemAtAnchor() {
        assertEquals(
            PlaybackPosition("episode-1", 0L, 0L, 0),
            PlaybackTimeline.resolve(playlist, 1_000L, 1_000L)
        )
    }

    @Test
    fun lateViewerJoinsAtSameItemAndOffset() {
        assertEquals(
            PlaybackPosition("episode-2", 5_000L, 0L, 1),
            PlaybackTimeline.resolve(playlist, 1_000L, 16_000L)
        )
    }

    @Test
    fun exactBoundaryMovesToNextItem() {
        assertEquals(
            PlaybackPosition("episode-2", 0L, 0L, 1),
            PlaybackTimeline.resolve(playlist, 0L, 10_000L)
        )
    }

    @Test
    fun playlistRepeatsFromBeginningWithIncreasingCycleNumber() {
        assertEquals(
            PlaybackPosition("episode-1", 0L, 1L, 0),
            PlaybackTimeline.resolve(playlist, 0L, 60_000L)
        )
        assertEquals(
            PlaybackPosition("episode-2", 5_000L, 1L, 1),
            PlaybackTimeline.resolve(playlist, 0L, 75_000L)
        )
    }

    @Test
    fun futurePlaylistAnchorIsNotPlayingYet() {
        assertNull(PlaybackTimeline.resolve(playlist, 10_000L, 9_999L))
    }

    @Test
    fun rejectsEmptyOrInvalidPlaylist() {
        assertNull(PlaybackTimeline.resolve(emptyList(), 0L, 1L))
        assertNull(
            PlaybackTimeline.resolve(
                listOf(TimelineEntry("episode-1", 0L)),
                0L,
                1L
            )
        )
        assertNull(
            PlaybackTimeline.resolve(
                listOf(TimelineEntry(" ", 1_000L)),
                0L,
                1L
            )
        )
    }

    @Test
    fun rejectsDurationOverflowAndTimestampOverflow() {
        assertNull(
            PlaybackTimeline.resolve(
                listOf(
                    TimelineEntry("episode-1", Long.MAX_VALUE),
                    TimelineEntry("episode-2", 1L)
                ),
                0L,
                1L
            )
        )
        assertNull(
            PlaybackTimeline.resolve(
                listOf(TimelineEntry("episode-1", 1L)),
                Long.MIN_VALUE,
                Long.MAX_VALUE
            )
        )
    }
}
