package com.fountofhopedotorg.fohbible.gfx_animator

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.provider.MediaStore
import android.view.Surface
import androidx.annotation.RequiresApi
import java.io.File
import java.util.LinkedList
import androidx.core.graphics.createBitmap
import kotlin.math.pow

@RequiresApi(Build.VERSION_CODES.Q)
class ComposeVideoEncoder(
    private val context: Context,
    private val width: Int,
    private val height: Int,
    private val frameRate: Int = 120,
    private val bitRate: Int = 20_000_000,
    private val isDark: Boolean = false,
    private val themePrimaryColorInt: Int = 0xFF000000.toInt()
) {
    companion object {
        private const val PROFILE = CodecProfileLevel.HEVCProfileMain
        private const val I_FRAME_INTERVAL_SEC = 2
        private const val HEVC_LEVEL_31 = 0x100
        private const val HEVC_LEVEL_4  = 0x200
        private const val HEVC_LEVEL_5  = 0x400
        private const val HEVC_LEVEL_51 = 0x500
        private const val HEVC_LEVEL_52 = 0x600
        private const val HEVC_LEVEL_6  = 0x800
        private const val HEVC_LEVEL_61 = 0x900
        private const val HEVC_LEVEL_62 = 0xA00

        private fun getHevcLevel(width: Int, height: Int, frameRate: Int): Int {
            val samplesPerSec = width.toLong() * height * frameRate
            return when {
                samplesPerSec <= 31_500_000  -> HEVC_LEVEL_31
                samplesPerSec <= 133_700_000 -> HEVC_LEVEL_4
                samplesPerSec <= 249_000_000 -> HEVC_LEVEL_5
                samplesPerSec <= 498_000_000 -> HEVC_LEVEL_51
                samplesPerSec <= 996_000_000 -> HEVC_LEVEL_52
                samplesPerSec <= 1_064_000_000 -> HEVC_LEVEL_6
                samplesPerSec <= 2_129_000_000 -> HEVC_LEVEL_61
                else                         -> HEVC_LEVEL_62
            }
        }
    }

    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var inputSurface: Surface? = null
    private var trackIndex = -1
    private var isMuxerStarted = false
    private val bufferInfo = MediaCodec.BufferInfo()
    private var outputFile: File? = null

    private val pendingPtsQueue = LinkedList<Long>()

    private val maxHistorySize = 1
    private val frameHistory = LinkedList<Bitmap>()
    private var bitmapPool = listOf<Bitmap>()
    private var videoWidth = 0
    private var videoHeight = 0

    init {
        setupEncoder()
    }

    private fun setupEncoder() {
        videoWidth = if (width % 2 == 0) width else width - 1
        videoHeight = if (height % 2 == 0) height else height - 1

        bitmapPool = List(maxHistorySize) {
            createBitmap(videoWidth, videoHeight)
        }

        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_HEVC, videoWidth, videoHeight
        ).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(
                MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
            )
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SEC)
            setInteger(MediaFormat.KEY_PROFILE, PROFILE)
            setInteger(
                MediaFormat.KEY_LEVEL,
                getHevcLevel(videoWidth, videoHeight, frameRate)
            )
        }

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = createInputSurface()
            start()
        }

        outputFile = File(context.cacheDir, "export_${System.currentTimeMillis()}.mp4")
        mediaMuxer = MediaMuxer(
            outputFile!!.absolutePath,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )
    }

    fun drainEncoder(endOfStream: Boolean) {
        val codec = mediaCodec ?: return
        val muxer = mediaMuxer ?: return

        if (endOfStream) {
            codec.signalEndOfInputStream()
        }

        while (true) {
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isMuxerStarted) throw RuntimeException("Format changed twice!")
                val newFormat = codec.outputFormat
                trackIndex = muxer.addTrack(newFormat)
                muxer.start()
                isMuxerStarted = true
            } else if (outputBufferIndex >= 0) {
                val encodedData = codec.getOutputBuffer(outputBufferIndex) ?: continue
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0 && isMuxerStarted) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)

                    bufferInfo.presentationTimeUs = pendingPtsQueue.poll() ?: 0L

                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                }

                codec.releaseOutputBuffer(outputBufferIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
    }

    fun addFrame(bitmap: Bitmap, ptsUs: Long) {
        drainEncoder(false)
        val surface = inputSurface ?: return

        val nextHistoryBitmap = if (frameHistory.size >= maxHistorySize) {
            frameHistory.removeFirst()
        } else {
            bitmapPool[frameHistory.size]
        }
        val historyCanvas = android.graphics.Canvas(nextHistoryBitmap)
        historyCanvas.drawColor(
            android.graphics.Color.TRANSPARENT,
            android.graphics.PorterDuff.Mode.CLEAR
        )

        val isHardware = bitmap.config == Bitmap.Config.HARDWARE
        val safeBitmap = if (isHardware) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        historyCanvas.drawBitmap(safeBitmap, 0f, 0f, null)

        if (isHardware) {
            safeBitmap.recycle()
        }

        frameHistory.addLast(nextHistoryBitmap)

        val canvas = surface.lockHardwareCanvas()

        if (isDark) {
            canvas.drawColor(0xFF1E2937.toInt())
        } else {
            canvas.drawColor(android.graphics.Color.WHITE)
            val tintPaint = android.graphics.Paint().apply {
                color = themePrimaryColorInt
                alpha = 25
            }
            canvas.drawRect(0f, 0f, videoWidth.toFloat(), videoHeight.toFloat(), tintPaint)
        }

        val paint = android.graphics.Paint()
        frameHistory.forEachIndexed { index, histBitmap ->
            val distanceFromCurrent = frameHistory.size - 1 - index
            val alphaPercent = 0.60.pow(distanceFromCurrent.toDouble()).toFloat()
            paint.alpha = (alphaPercent * 255).toInt()
            canvas.drawBitmap(histBitmap, 0f, 0f, paint)
        }

        surface.unlockCanvasAndPost(canvas)

        pendingPtsQueue.add(ptsUs)
    }

    fun releaseAndSaveToGallery(fileName: String): String? {
        drainEncoder(true)
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            if (isMuxerStarted) {
                mediaMuxer?.stop()
                mediaMuxer?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        frameHistory.clear()
        bitmapPool = emptyList()

        val file = outputFile ?: return null
        if (!file.exists()) return null

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$fileName.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/VideoEditor")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val videoUri = resolver.insert(collection, contentValues)

        videoUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            file.delete()
            return uri.toString()
        }
        return null
    }

    fun releaseAndDiscard() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            if (isMuxerStarted) {
                mediaMuxer?.stop()
                mediaMuxer?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        outputFile?.delete()
    }
}