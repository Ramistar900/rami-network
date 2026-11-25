package com.rami.network

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.rami.network.domain.PlaylistControllerState
import com.rami.network.domain.PlaylistPlaybackController
import com.rami.network.domain.TimelineEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class LocalVideo(
    val id: String,
    val uri: Uri,
    val title: String,
    val durationMillis: Long
) {
    fun toTimelineEntry() = TimelineEntry(id, durationMillis)
}

@Composable
fun PlaylistPlayerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember(context) { ExoPlayer.Builder(context).build() }
    val videos = remember { mutableStateListOf<LocalVideo>() }
    var controllerState by remember { mutableStateOf<PlaylistControllerState?>(null) }
    var status by remember { mutableStateOf("أضف فيديوهات من جهازك لبدء اختبار المشغل.") }
    var isLoading by remember { mutableStateOf(false) }

    fun playCurrent(state: PlaylistControllerState) {
        val current = state.currentEntry ?: return
        val video = videos.firstOrNull { it.id == current.mediaItemId } ?: run {
            status = "تعذر العثور على ملف الفيديو الحالي."
            return
        }
        player.setMediaItem(MediaItem.fromUri(video.uri))
        player.prepare()
        player.playWhenReady = true
        status = "يعمل الآن: ${video.title}"
    }

    val onEndedState = rememberUpdatedState(newValue = {
        val oldState = controllerState
        if (oldState != null && oldState.isRunning) {
            val nextState = PlaylistPlaybackController.onItemEnded(oldState)
            controllerState = nextState
            if (nextState.isRunning) {
                playCurrent(nextState)
            } else {
                player.pause()
                status = "انتهت القائمة وتم إيقاف التشغيل."
            }
        }
    })

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onEndedState.value.invoke()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            scope.launch {
                isLoading = true
                status = "جارٍ قراءة بيانات الفيديوهات..."
                val imported = withContext(Dispatchers.IO) {
                    selectedUris.mapIndexedNotNull { index, uri ->
                        runCatching {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                        readVideoMetadata(context, uri, index)
                    }
                }
                if (imported.isNotEmpty()) {
                    val existingIds = videos.map { it.id }.toSet()
                    val unique = imported.filterNot { it.id in existingIds }
                    if (unique.isNotEmpty()) {
                        val activeState = controllerState
                        if (activeState?.isRunning == true) {
                            val base = activeState.pendingEntries ?: activeState.entries
                            val updated = PlaylistPlaybackController.requestPlaylistUpdate(
                                activeState,
                                base + unique.map { it.toTimelineEntry() }
                            )
                            if (updated != null) {
                                controllerState = updated
                                videos.addAll(unique)
                                status = "أضيفت الفيديوهات؛ ستدخل القائمة بعد انتهاء الفيديو الحالي."
                            }
                        } else {
                            videos.addAll(unique)
                            controllerState = null
                            status = "تمت إضافة ${unique.size} فيديوهات إلى القائمة."
                        }
                    } else {
                        status = "الفيديوهات المحددة موجودة مسبقاً في القائمة."
                    }
                } else {
                    status = "لم نتمكن من قراءة مدة الفيديوهات المحددة. جرّب ملفات MP4 أو ملفات يدعمها الجهاز."
                }
                isLoading = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "مشغل قائمة RAMİ 1TV",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "اختبار محلي على هذا الجهاز. لا ينشئ بثاً عاماً أو رابط HLS.",
            style = MaterialTheme.typography.bodySmall
        )

        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                    keepScreenOn = true
                }
            },
            update = { it.player = player },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { picker.launch(arrayOf("video/*")) },
                enabled = !isLoading
            ) { Text("إضافة فيديوهات") }
            Button(
                onClick = {
                    val state = PlaylistPlaybackController.start(videos.map { it.toTimelineEntry() })
                    if (state == null) {
                        status = "أضف فيديو واحداً صالحاً على الأقل أولاً."
                    } else {
                        controllerState = state
                        playCurrent(state)
                    }
                },
                enabled = videos.isNotEmpty() && controllerState?.isRunning != true && !isLoading
            ) { Text("تشغيل القائمة") }
            OutlinedButton(
                onClick = {
                    controllerState = controllerState?.let(PlaylistPlaybackController::stop)
                    player.stop()
                    status = "تم إيقاف التشغيل."
                },
                enabled = controllerState?.isRunning == true
            ) { Text("إيقاف") }
        }

        Text(status, style = MaterialTheme.typography.bodySmall)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("قائمة الفيديوهات (${videos.size})", style = MaterialTheme.typography.titleMedium)
                if (videos.isEmpty()) {
                    Text("لا توجد فيديوهات مضافة بعد.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(videos, key = { _, item -> item.id }) { index, video ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${index + 1}. ${video.title}")
                                    Text(formatDuration(video.durationMillis), style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val activeState = controllerState
                                        val newVideos = videos.filterNot { it.id == video.id }
                                        if (activeState?.isRunning == true) {
                                            val base = activeState.pendingEntries ?: activeState.entries
                                            val updatedEntries = base.filterNot { it.mediaItemId == video.id }
                                            val updated = PlaylistPlaybackController.requestPlaylistUpdate(
                                                activeState,
                                                updatedEntries
                                            )
                                            if (updated != null) {
                                                controllerState = updated
                                                videos.remove(video)
                                                status = "سيُطبق الحذف عند نهاية الفيديو الحالي."
                                            }
                                        } else {
                                            videos.remove(video)
                                            controllerState = null
                                            status = "تم حذف الفيديو من القائمة."
                                        }
                                    },
                                    enabled = controllerState?.isRunning != true ||
                                        controllerState?.currentEntry?.mediaItemId != video.id
                                ) { Text("حذف") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun readVideoMetadata(context: Context, uri: Uri, index: Int): LocalVideo? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: return null
        if (duration <= 0L) return null
        val name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "فيديو ${index + 1}"
        val stableId = "${uri}#entry-${System.nanoTime()}-$index"
        LocalVideo(stableId, uri, name, duration)
    } catch (_: Exception) {
        null
    } finally {
        retriever.release()
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}
