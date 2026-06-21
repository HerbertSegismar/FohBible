package com.fountofhopedotorg.fohbible.videoeditor

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.fountofhopedotorg.fohbible.data.CanvasNote
import java.util.UUID

class VideoEditorViewModel : ViewModel() {
    var videoUri by mutableStateOf<Uri?>(null)
        private set

    var playbackPositionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)     // populate after video is loaded
    var trimStartMs by mutableLongStateOf(0L)
    var trimEndMs by mutableLongStateOf(0L)
    var isPlaying by mutableStateOf(false)
    var isFullScreen by mutableStateOf(false)

    var overlays by mutableStateOf(listOf<CanvasNote>())
        private set

    /** Loads a new video and resets the timeline */
    fun loadVideo(uri: Uri) {          // <-- this is the correct method name
        videoUri = uri
        durationMs = 30_000L           // placeholder; replace with MediaMetadataRetriever query
        trimEndMs = durationMs
        playbackPositionMs = 0L
    }

    fun addOverlay(note: CanvasNote) {
        overlays = overlays + note.copy(id = UUID.randomUUID().toString())
    }

    fun removeOverlay(id: String) {
        overlays = overlays.filter { it.id != id }
    }

    fun updateOverlayContent(id: String, newContent: String) {
        overlays = overlays.map {
            if (it.id == id) it.copy(content = newContent) else it
        }
    }

    fun updateOverlayColor(id: String, color: Color) {
        overlays = overlays.map {
            if (it.id == id) it.copy(textColor = color) else it
        }
    }

    fun createGroup(ids: Set<String>) {
        val groupId = UUID.randomUUID().toString()
        overlays = overlays.map {
            if (it.id in ids) it.copy(groupId = groupId) else it
        }
    }

    fun ungroupNotes(ids: Set<String>) {
        overlays = overlays.map {
            if (it.id in ids) it.copy(groupId = null) else it
        }
    }

    fun exportVideo(context: Context) {
        // TODO: implement using Media3 Transformer
        Toast.makeText(context, "Export not yet implemented", Toast.LENGTH_SHORT).show()
    }
}