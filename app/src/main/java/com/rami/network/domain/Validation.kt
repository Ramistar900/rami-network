package com.rami.network.domain

import java.net.InetAddress
import java.net.URI
import java.util.Locale

/**
 * Client-side validation for fast feedback.
 *
 * This is NOT a security boundary. Before fetching an external URL, the backend
 * must independently validate DNS answers, block private/reserved IP ranges,
 * re-check every redirect target, and defend against DNS rebinding/TOCTOU.
 */
object Validation {

    const val MAX_NAME_LENGTH = 60
    private const val HTTPS_PORT = 443

    fun isValidChannelName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.isNotEmpty() && trimmed.length <= MAX_NAME_LENGTH
    }

    fun isValidChannelNumber(number: Int): Boolean = number >= 1

    fun isValidContentSource(source: ContentSource): Boolean {
        if (source.id.isBlank() || source.displayName.isBlank() || source.rightsNote.isBlank()) {
            return false
        }

        return when (source.kind) {
            ContentSourceKind.APP_CONNECTOR ->
                source.appConnector != null &&
                    !source.appContentId.isNullOrBlank() &&
                    source.sourceUrl == null

            ContentSourceKind.EXTERNAL_URL ->
                source.appConnector == null &&
                    source.appContentId == null &&
                    !source.sourceUrl.isNullOrBlank() &&
                    isValidSourceUrl(source.sourceUrl, source.sourceType)

            ContentSourceKind.LIVE_INPUT ->
                source.appConnector == null &&
                    source.appContentId == null &&
                    !source.sourceUrl.isNullOrBlank() &&
                    source.sourceType in setOf(SourceType.HLS, SourceType.RTMP, SourceType.SRT) &&
                    isValidLiveSourceUrl(source.sourceUrl, source.sourceType)
        }
    }

    /** HTTPS external file/playlist URL with a matching path extension. */
    fun isValidSourceUrl(url: String, type: SourceType): Boolean {
        if (type != SourceType.HLS && type != SourceType.MP4) return false
        val uri = parseSafeUri(url) ?: return false
        val path = uri.rawPath?.lowercase(Locale.ROOT) ?: return false
        return when (type) {
            SourceType.HLS -> path.endsWith(".m3u8")
            SourceType.MP4 -> path.endsWith(".mp4")
            else -> false
        }
    }

    /**
     * Live sources may use HTTPS HLS or secure RTMPS/SRT endpoints.
     * Plain RTMP is deliberately rejected because it does not encrypt transport.
     */
    fun isValidLiveSourceUrl(url: String, type: SourceType): Boolean {
        val uri = parseSafeUri(url) ?: return false
        return when (type) {
            SourceType.HLS -> uri.scheme.equals("https", ignoreCase = true) &&
                uri.rawPath?.lowercase(Locale.ROOT)?.endsWith(".m3u8") == true
            SourceType.RTMP -> uri.scheme.equals("rtmps", ignoreCase = true)
            SourceType.SRT -> uri.scheme.equals("srt", ignoreCase = true)
            SourceType.MP4 -> false
        }
    }

    private fun parseSafeUri(value: String): URI? {
        val uri = try {
            URI(value.trim())
        } catch (_: Exception) {
            return null
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        if (uri.rawUserInfo != null || uri.rawFragment != null) return null
        val host = uri.host?.trim('[', ']')?.lowercase(Locale.ROOT) ?: return null
        if (host.isBlank() || isPrivateOrReservedHost(host)) return null

        // HTTPS/RTMPS use their standard secure port. SRT requires an explicit
        // valid listener port because SRT endpoints normally identify an ingest port.
        val allowedPort = when (scheme) {
            "https", "rtmps" -> uri.port == -1 || uri.port == HTTPS_PORT
            "srt" -> uri.port in 1..65535
            else -> false
        }
        if (!allowedPort) return null
        return uri
    }

    internal fun isPrivateOrReservedHost(host: String): Boolean {
        val normalized = host.trimEnd('.').lowercase(Locale.ROOT)
        if (normalized == "localhost" ||
            normalized.endsWith(".localhost") ||
            normalized.endsWith(".local") ||
            normalized.endsWith(".internal")
        ) return true

        // Parse IP literals only. Do not resolve hostnames here.
        if (':' in normalized) {
            val address = try {
                InetAddress.getByName(normalized)
            } catch (_: Exception) {
                return true
            }
            val bytes = address.address
            if (address.isAnyLocalAddress || address.isLoopbackAddress ||
                address.isLinkLocalAddress || address.isSiteLocalAddress ||
                address.isMulticastAddress
            ) return true

            // Unique-local IPv6 (fc00::/7) and documentation range (2001:db8::/32).
            if (bytes.size == 16 &&
                ((bytes[0].toInt() and 0xfe) == 0xfc ||
                    (bytes[0] == 0x20.toByte() && bytes[1] == 0x01.toByte() &&
                        bytes[2] == 0x0d.toByte() && bytes[3] == 0xb8.toByte()))
            ) return true

            // Explicitly reject IPv4-mapped IPv6 literals.
            if (bytes.size == 16 && bytes.take(10).all { it == 0.toByte() } &&
                bytes[10] == 0xff.toByte() && bytes[11] == 0xff.toByte()
            ) {
                val ipv4 = bytes.takeLast(4).map { it.toInt() and 0xff }
                return isPrivateIpv4(ipv4)
            }
            return false
        }

        val ipv4 = normalized.split('.')
        if (ipv4.size == 4 && ipv4.all { it.isNotEmpty() && it.all(Char::isDigit) }) {
            val octets = ipv4.map { it.toIntOrNull() ?: return true }
            if (octets.any { it !in 0..255 }) return true
            return isPrivateIpv4(octets)
        }
        return false
    }

    private fun isPrivateIpv4(octets: List<Int>): Boolean {
        if (octets.size != 4) return true
        val (a, b) = octets[0] to octets[1]
        return a == 0 || a == 10 || a == 127 ||
            (a == 100 && b in 64..127) ||
            (a == 169 && b == 254) ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 0) ||
            (a == 192 && b == 168) ||
            (a == 198 && b in 18..19) ||
            (a == 198 && b == 51 && octets[2] == 100) ||
            (a == 203 && b == 0 && octets[2] == 113) ||
            a >= 224
    }

    /**
     * Playlist positions must be unique and contiguous from zero; every entry
     * must belong to the requested channel and reference a non-blank media ID.
     * An empty playlist is valid.
     */
    fun isValidPlaylist(channelId: String, entries: List<PlaylistEntry>): Boolean {
        if (channelId.isBlank()) return false
        if (entries.any { it.channelId != channelId || it.mediaItemId.isBlank() || it.position < 0 }) {
            return false
        }
        val positions = entries.map { it.position }.sorted()
        return positions == entries.indices.toList()
    }
}
