package com.rami.network.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {

    @Test
    fun channelNameRejectsBlankAndTooLong() {
        assertFalse(Validation.isValidChannelName(""))
        assertFalse(Validation.isValidChannelName("   "))
        assertFalse(Validation.isValidChannelName("a".repeat(Validation.MAX_NAME_LENGTH + 1)))
        assertTrue(Validation.isValidChannelName("قناة الأخبار"))
    }

    @Test
    fun channelNumberMustBePositive() {
        assertFalse(Validation.isValidChannelNumber(0))
        assertFalse(Validation.isValidChannelNumber(-1))
        assertTrue(Validation.isValidChannelNumber(1))
    }

    @Test
    fun externalSourceRequiresHttpsAndMatchingExtension() {
        assertFalse(Validation.isValidSourceUrl("http://cdn.example.com/a.m3u8", SourceType.HLS))
        assertFalse(Validation.isValidSourceUrl("https://cdn.example.com/a.mp4", SourceType.HLS))
        assertFalse(Validation.isValidSourceUrl("https://cdn.example.com/a.m3u8", SourceType.MP4))
        assertTrue(Validation.isValidSourceUrl("https://cdn.example.com/a.m3u8?token=abc", SourceType.HLS))
        assertTrue(Validation.isValidSourceUrl("https://cdn.example.com/a.mp4", SourceType.MP4))
    }

    @Test
    fun externalWebmSourceRequiresHttpsAndWebmExtension() {
        assertFalse(Validation.isValidSourceUrl("http://cdn.example.com/a.webm", SourceType.WEBM))
        assertFalse(Validation.isValidSourceUrl("https://cdn.example.com/a.mp4", SourceType.WEBM))
        assertTrue(Validation.isValidSourceUrl("https://upload.wikimedia.org/video/a.webm", SourceType.WEBM))
    }

    @Test
    fun rejectsCredentialsFragmentsAndNonStandardHttpsPorts() {
        assertFalse(Validation.isValidSourceUrl("https://user:pass@cdn.example.com/a.m3u8", SourceType.HLS))
        assertFalse(Validation.isValidSourceUrl("https://cdn.example.com:8443/a.m3u8", SourceType.HLS))
        assertFalse(Validation.isValidSourceUrl("https://cdn.example.com/a.m3u8#fragment", SourceType.HLS))
    }

    @Test
    fun rejectsPrivateAndReservedIpv4Hosts() {
        listOf(
            "localhost", "127.0.0.1", "10.0.0.5", "192.168.1.10",
            "169.254.169.254", "172.20.0.1", "100.64.0.1", "224.0.0.1"
        ).forEach { host ->
            assertFalse("$host should be rejected", Validation.isValidSourceUrl("https://$host/a.m3u8", SourceType.HLS))
        }
    }

    @Test
    fun rejectsLocalIpv6Hosts() {
        listOf("::1", "fc00::1", "fe80::1").forEach { host ->
            assertFalse(
                "$host should be rejected",
                Validation.isValidSourceUrl("https://[$host]/a.m3u8", SourceType.HLS)
            )
        }
    }

    @Test
    fun allowsPublicIpLiteral() {
        assertTrue(Validation.isValidSourceUrl("https://8.8.8.8/a.m3u8", SourceType.HLS))
    }

    @Test
    fun validatesAppConnectorSourceWithoutEmbeddingAppUrl() {
        val source = ContentSource(
            id = "source-1",
            kind = ContentSourceKind.APP_CONNECTOR,
            displayName = "Almalaki",
            sourceType = SourceType.MP4,
            appConnector = AppConnectorType.ALMALAKI,
            appContentId = "promo-123",
            rightsNote = "Owner-approved promotional content"
        )
        assertTrue(Validation.isValidContentSource(source))
    }

    @Test
    fun rejectsAppConnectorWithoutContentIdOrWithExternalUrl() {
        val missingId = ContentSource(
            id = "source-1",
            kind = ContentSourceKind.APP_CONNECTOR,
            displayName = "ROYAL TV",
            sourceType = SourceType.HLS,
            appConnector = AppConnectorType.ROYAL_TV,
            rightsNote = "Approved"
        )
        assertFalse(Validation.isValidContentSource(missingId))
        assertFalse(
            Validation.isValidContentSource(
                missingId.copy(appContentId = "episode-1", sourceUrl = "https://cdn.example.com/a.m3u8")
            )
        )
    }

    @Test
    fun validatesExternalAndLiveSources() {
        val external = ContentSource(
            id = "ext-1",
            kind = ContentSourceKind.EXTERNAL_URL,
            displayName = "External HLS",
            sourceType = SourceType.HLS,
            sourceUrl = "https://cdn.example.com/live/index.m3u8",
            rightsNote = "Redistribution licensed"
        )
        val live = external.copy(
            id = "live-1",
            kind = ContentSourceKind.LIVE_INPUT,
            displayName = "Secure live input",
            sourceType = SourceType.RTMP,
            sourceUrl = "rtmps://ingest.example.com/live/key"
        )
        assertTrue(Validation.isValidContentSource(external))
        assertTrue(Validation.isValidContentSource(live))
        assertFalse(Validation.isValidLiveSourceUrl("rtmp://ingest.example.com/live/key", SourceType.RTMP))
    }

    @Test
    fun playlistRequiresContiguousUniquePositionsAndMatchingChannel() {
        val valid = listOf(
            PlaylistEntry("ch1", "m1", 0),
            PlaylistEntry("ch1", "m2", 1),
            PlaylistEntry("ch1", "m3", 2)
        )
        assertTrue(Validation.isValidPlaylist("ch1", valid))
        assertTrue(Validation.isValidPlaylist("ch1", emptyList()))

        assertFalse(Validation.isValidPlaylist("ch1", listOf(
            PlaylistEntry("ch1", "m1", 0), PlaylistEntry("ch1", "m2", 2)
        )))
        assertFalse(Validation.isValidPlaylist("ch1", listOf(
            PlaylistEntry("ch1", "m1", 0), PlaylistEntry("ch1", "m2", 0)
        )))
        assertFalse(Validation.isValidPlaylist("ch1", listOf(PlaylistEntry("ch2", "m1", 0))))
        assertFalse(Validation.isValidPlaylist("ch1", listOf(PlaylistEntry("ch1", " ", 0))))
    }
}
