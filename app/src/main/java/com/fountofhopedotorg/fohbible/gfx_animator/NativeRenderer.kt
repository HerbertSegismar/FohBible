package com.fountofhopedotorg.fohbible.gfx_animator

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Surface
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import coil.ImageLoader
import coil.request.ImageRequest
import com.fountofhopedotorg.fohbible.data.BezierNodeData
import com.fountofhopedotorg.fohbible.data.CanvasElement
import com.fountofhopedotorg.fohbible.data.GradientConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import com.fountofhopedotorg.fohbible.data.TweenType
import com.fountofhopedotorg.fohbible.gfx_creator.generateThornCrownPaths
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.File
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Paint as ComposePaint
import com.fountofhopedotorg.fohbible.utils.Fonts

private fun adjustOffsetForPivotChange(
    element: CanvasElement,
    oldPivotX: Float,
    oldPivotY: Float,
    newPivotX: Float,
    newPivotY: Float
): Offset {
    val w = element.width
    val h = element.height

    val dx = (newPivotX - oldPivotX) * w
    val dy = (newPivotY - oldPivotY) * h

    val scaledDx = dx * element.scaleX
    val scaledDy = dy * element.scaleY

    val rad = element.rotation * (PI / 180.0).toFloat()
    val cosA = cos(rad)
    val sinA = sin(rad)

    val rotatedDx = scaledDx * cosA - scaledDy * sinA
    val rotatedDy = scaledDx * sinA + scaledDy * cosA

    val deltaOffsetX = rotatedDx - dx
    val deltaOffsetY = rotatedDy - dy

    return element.offset + Offset(deltaOffsetX, deltaOffsetY)
}

private suspend fun preloadFonts(
    elements: List<CanvasElement>,
    context: Context
): Map<String, Typeface> = withContext(Dispatchers.IO) {
    val neededFonts = elements
        .filter { !it.content.startsWith("Shape:") && !it.content.startsWith("Image:") }
        .map { it.fontFamily ?: "system" }
        .toSet()
    neededFonts.associateWith { Fonts.getTypeface(context.applicationContext, it) }
}


suspend fun loadImageBitmaps(
    elements: List<CanvasElement>,
    context: Context,
    targetWidthPx: Float,
    targetHeightPx: Float
): Map<String, Bitmap> = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    val loader = ImageLoader.Builder(appContext)
        .build()

    val map = mutableMapOf<String, Bitmap>()
    val w = targetWidthPx.roundToInt()
    val h = targetHeightPx.roundToInt()

    for (el in elements) {
        if (el.content.startsWith("Image: ")) {
            val uriString = el.content.removePrefix("Image: ").trim()
            val uri = uriString.toUri()

            val request = ImageRequest.Builder(appContext)
                .data(uri)
                .size(w, h)
                .allowHardware(false)
                .listener(
                    onError = { _, result ->
                        android.util.Log.e("NativeExport", "Failed to load image ${el.id}: $uriString, ${result.throwable}")
                    }
                )
                .build()

            val drawable = loader.execute(request).drawable
            if (drawable != null) {
                val bmp = if (drawable is BitmapDrawable) {
                    drawable.bitmap
                } else {
                    createBitmap(w, h).also { canvasBitmap ->
                        val canvas = Canvas(canvasBitmap)
                        drawable.setBounds(0, 0, w, h)
                        drawable.draw(canvas)
                    }
                }
                map[el.id] = bmp
                android.util.Log.d("NativeExport", "Loaded image ${el.id} (${bmp.width}x${bmp.height})")
            } else {
                android.util.Log.w("NativeExport", "Drawable is null for ${el.id}: $uriString")
            }
        }
    }
    map
}

@RequiresApi(Build.VERSION_CODES.O)
fun drawFrame(
    canvas: Canvas,
    elements: List<CanvasElement>,
    timeMs: Long,
    gradientConfigs: Map<String, GradientConfig>,
    imageBitmaps: Map<String, Bitmap>,
    scaleFactor: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    canvasBackgroundColor: ComposeColor?,
    canvasBackgroundBrush: Brush?,
    fontCache: Map<String, Typeface> = emptyMap()
) {
    if (canvasBackgroundBrush != null) {
        val composeCanvas = ComposeCanvas(canvas)
        val composePaint = ComposePaint()
        canvasBackgroundBrush.applyTo(Size(canvasWidth, canvasHeight), composePaint, 1f)

        composeCanvas.drawRect(
            left = 0f,
            top = 0f,
            right = canvasWidth,
            bottom = canvasHeight,
            paint = composePaint
        )
    } else if (canvasBackgroundColor != null) {
        canvas.drawColor(canvasBackgroundColor.toArgb())
    } else {
        canvas.drawColor(android.graphics.Color.WHITE)
    }

    for (element in elements) {
        if (timeMs < element.startTimeMs || timeMs > element.endTimeMs) {
            continue
        }

        val keyframes = element.keyframes.sortedBy { it.timestampMs }
        val (prev, next) = findSurroundingKeyframes(keyframes, timeMs)

        val rawProgress = if (next != null && prev != null && next.timestampMs != prev.timestampMs) {
            ((timeMs - prev.timestampMs).toFloat() / (next.timestampMs - prev.timestampMs)).coerceIn(0f, 1f)
        } else 0f
        val customPoints = if (next?.tweenType == TweenType.CUSTOM) next.customPoints else emptyList()
        val progress = if (next != null) {
            ease(rawProgress, next.tweenType, customPoints ?: emptyList())
        } else rawProgress

        val startX = prev?.x ?: element.offset.x
        val startY = prev?.y ?: element.offset.y
        val endX   = next?.x ?: element.offset.x
        val endY   = next?.y ?: element.offset.y
        val widthPx = element.width * scaleFactor
        val heightPx = element.height * scaleFactor

        val scaleX = lerp(prev?.scaleX ?: element.scaleX, next?.scaleX ?: element.scaleX, progress)
        val scaleY = lerp(prev?.scaleY ?: element.scaleY, next?.scaleY ?: element.scaleY, progress)
        val rotation = lerp(prev?.rotation ?: element.rotation, next?.rotation ?: element.rotation, progress)
        val color = when {
            prev?.color != null && next?.color != null -> lerpColor(prev.color, next.color, progress)
            next?.color != null -> next.color
            prev?.color != null -> prev.color
            else -> null
        }
        val gradient = when {
            prev?.gradientConfig != null && next?.gradientConfig != null ->
                lerpGradient(prev.gradientConfig, next.gradientConfig, progress)
            next?.gradientConfig != null -> next.gradientConfig
            prev?.gradientConfig != null -> prev.gradientConfig
            else -> gradientConfigs[element.id]
        }

        val originalPivotX = element.pivotX
        val originalPivotY = element.pivotY
        val newPivotX = lerp(prev?.pivotX ?: element.pivotX, next?.pivotX ?: element.pivotX, progress)
        val newPivotY = lerp(prev?.pivotY ?: element.pivotY, next?.pivotY ?: element.pivotY, progress)

        var posX: Float
        var posY: Float

        if (next?.ellipticalRotation == true && prev != null) {
            val startOffset = Offset(startX, startY)
            val endOffset = Offset(endX, endY)
            val distance = (endOffset - startOffset).getDistance()
            val rotStart = prev.rotation ?: element.rotation
            val rotEnd = next.rotation ?: element.rotation
            val currentRot = rotation   // already interpolated
            val stretchX = next.ellipticalStretchX.coerceAtLeast(0.01f)
            val stretchY = next.ellipticalStretchY.coerceAtLeast(0.01f)

            if (distance < 0.5f) {
                val px = newPivotX * element.width
                val py = newPivotY * element.height
                val localCx = element.width / 2f - px
                val localCy = element.height / 2f - py
                val rotStartRad = rotStart * (PI.toFloat() / 180f)
                val cosR0 = cos(rotStartRad)
                val sinR0 = sin(rotStartRad)
                val startDx = localCx * cosR0 - localCy * sinR0
                val startDy = localCx * sinR0 + localCy * cosR0
                val r0 = sqrt(startDx * startDx + startDy * startDy)
                if (r0 < 0.5f) {
                    posX = startOffset.x
                    posY = startOffset.y
                } else {
                    val startPhi = atan2(startDy * stretchX, startDx * stretchY)
                    val mag = sqrt((startDx * stretchY) * (startDx * stretchY) +
                            (startDy * stretchX) * (startDy * stretchX))
                    val rBase = mag / (stretchX * stretchY)
                    val a = rBase * stretchX
                    val b = rBase * stretchY
                    val currentRotRad = currentRot * (PI.toFloat() / 180f)
                    val phi = startPhi + (currentRotRad - rotStartRad)
                    val cosR = cos(currentRotRad)
                    val sinR = sin(currentRotRad)
                    val currentDx = localCx * cosR - localCy * sinR
                    val currentDy = localCx * sinR + localCy * cosR
                    posX = startOffset.x + a * cos(phi) - currentDx
                    posY = startOffset.y + b * sin(phi) - currentDy
                }
            } else {
                val center = Offset((startOffset.x + endOffset.x) / 2f,
                    (startOffset.y + endOffset.y) / 2f)
                val delta = endOffset - startOffset
                val halfDist = distance / 2f
                val u = if (distance > 0f) Offset(delta.x / distance, delta.y / distance) else Offset(1f, 0f)
                val v = Offset(-u.y, u.x)
                val b = halfDist * (stretchY / stretchX)
                val t = progress.coerceIn(0f, 1f)
                val deltaRot = rotEnd - rotStart
                val phi: Float = if (deltaRot < 0f) {
                    Math.PI.toFloat() * (1f + t)
                } else {
                    Math.PI.toFloat() * (1f - t)
                }
                val targetCenter = Offset(
                    center.x + halfDist * cos(phi) * u.x + b * sin(phi) * v.x,
                    center.y + halfDist * cos(phi) * u.y + b * sin(phi) * v.y
                )
                val px = newPivotX * element.width
                val py = newPivotY * element.height
                val localCx = element.width / 2f - px
                val localCy = element.height / 2f - py
                val rotRad = currentRot * (PI.toFloat() / 180f)
                val cosR = cos(rotRad)
                val sinR = sin(rotRad)
                val dx = localCx * cosR - localCy * sinR
                val dy = localCx * sinR + localCy * cosR
                posX = targetCenter.x - dx
                posY = targetCenter.y - dy
            }
            posX *= scaleFactor
            posY *= scaleFactor
        } else {
            val linearX = lerp(startX, endX, progress)
            val linearY = lerp(startY, endY, progress)
            val tempElement = CanvasElement(
                offset = Offset(linearX, linearY),
                width = element.width,
                height = element.height,
                scaleX = scaleX,
                scaleY = scaleY,
                rotation = rotation,
                pivotX = newPivotX,
                pivotY = newPivotY,
                content = element.content,
                backgroundColor = element.backgroundColor,
                textColor = element.textColor
            )
            val adjustedOffset = adjustOffsetForPivotChange(
                element = tempElement,
                oldPivotX = originalPivotX,
                oldPivotY = originalPivotY,
                newPivotX = newPivotX,
                newPivotY = newPivotY
            )
            posX = adjustedOffset.x * scaleFactor
            posY = adjustedOffset.y * scaleFactor
        }

        canvas.withTranslation(posX, posY) {
            val pivotOffsetX = newPivotX * widthPx
            val pivotOffsetY = newPivotY * heightPx

            translate(pivotOffsetX, pivotOffsetY)
            rotate(rotation)
            scale(scaleX, scaleY)
            translate(-pivotOffsetX, -pivotOffsetY)

            if (element.shadowColor != null && element.shadowColor.alpha > 0f) {
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = element.shadowColor.toArgb()
                    maskFilter = BlurMaskFilter(2f * scaleFactor, BlurMaskFilter.Blur.NORMAL)
                }
                withTranslation(
                    element.shadowOffsetX * scaleFactor,
                    element.shadowOffsetY * scaleFactor
                ) {
                    drawElementContent(
                        this, element, widthPx, heightPx, shadowPaint, gradient = null,
                        density = scaleFactor, imageBitmaps = imageBitmaps, strokeOnly = false,
                        fontCache = fontCache
                    )
                }
            }

            if (element.borderThickness > 0f && element.borderColor != null) {
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = element.borderColor.toArgb()
                    style = Paint.Style.STROKE
                    strokeWidth = element.borderThickness * scaleFactor
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                }
                drawElementContent(
                    this, element, widthPx, heightPx, borderPaint, gradient = null,
                    density = scaleFactor, imageBitmaps = imageBitmaps, strokeOnly = true,
                    fontCache = fontCache
                )
            }

            val fillColorInt: Int = color?.toArgb()
                ?: if (element.content.startsWith("Shape:") || element.content.startsWith("Image:"))
                    element.backgroundColor.toArgb()
                else
                    (element.textColor ?: ComposeColor.Black).toArgb()

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                if (gradient != null) {
                    shader = LinearGradient(
                        gradient.startOffset.x * widthPx,
                        gradient.startOffset.y * heightPx,
                        gradient.endOffset.x * widthPx,
                        gradient.endOffset.y * heightPx,
                        gradient.startColor.toArgb(),
                        gradient.endColor.toArgb(),
                        Shader.TileMode.CLAMP
                    )
                } else {
                    this.color = fillColorInt
                }
            }

            drawElementContent(
                this, element, widthPx, heightPx, fillPaint, gradient = gradient,
                density = scaleFactor, imageBitmaps = imageBitmaps, strokeOnly = false,
                fontCache = fontCache
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun nativeExport(
    context: Context,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    frameRate: Int,
    bitRateMbps: Int,
    resolutionMultiplier: Float,
    elements: List<CanvasElement>,
    gradientConfigs: Map<String, GradientConfig>,
    startTimeMs: Long,
    endTimeMs: Long,
    canvasBackgroundColor: ComposeColor? = null,
    canvasBackgroundBrush: Brush? = null,
    onProgress: suspend (Float) -> Unit
) {
    val exportWidth = (canvasWidthPx * resolutionMultiplier).toInt()
    val exportHeight = (canvasHeightPx * resolutionMultiplier).toInt()

    val imageBitmaps = loadImageBitmaps(elements, context, exportWidth.toFloat(), exportHeight.toFloat())
    val fontCache = preloadFonts(elements, context)

    val frameDurationMs = 1000L / frameRate
    var currentTimeMs = startTimeMs
    val totalDurationMs = (endTimeMs - startTimeMs).coerceAtLeast(1L)

    val encoder = OffscreenVideoEncoder(
        context, exportWidth, exportHeight, frameRate,
        bitRateMbps * 1_000_000, mimeType = "video/hevc"
    )
    val surface = encoder.inputSurface

    try {
        while (currentTimeMs <= endTimeMs && currentCoroutineContext().isActive) {
            val canvas = surface.lockCanvas(null)

            if (canvas != null) {
                drawFrame(
                    canvas = canvas,
                    elements = elements,
                    timeMs = currentTimeMs,
                    gradientConfigs = gradientConfigs,
                    imageBitmaps = imageBitmaps,
                    scaleFactor = resolutionMultiplier,
                    canvasWidth = exportWidth.toFloat(),
                    canvasHeight = exportHeight.toFloat(),
                    canvasBackgroundColor = canvasBackgroundColor,
                    canvasBackgroundBrush = canvasBackgroundBrush,
                    fontCache = fontCache
                )
                surface.unlockCanvasAndPost(canvas)
            }

            encoder.drainEncoder {}

            val progress = (currentTimeMs - startTimeMs).toFloat() / totalDurationMs.toFloat()
            onProgress(progress.coerceIn(0f, 1f))

            currentTimeMs += frameDurationMs
        }

        encoder.signalEndOfStream()
        var eosReceived = false
        while (!eosReceived && currentCoroutineContext().isActive) {
            encoder.drainEncoder { eosReceived = true }
        }

        val savedPath = encoder.releaseAndSaveToGallery("NativeExport_${System.currentTimeMillis()}")

        withContext(Dispatchers.Main) {
            if (savedPath == null) {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (_: Exception) {
    } finally {
        encoder.release()
        imageBitmaps.values.forEach { it.recycle() }
    }
}

class OffscreenVideoEncoder(
    private val context: Context,
    private val width: Int,
    private val height: Int,
    private val frameRate: Int,
    private val bitRate: Int,
    private val mimeType: String = "video/hevc"
) {
    private lateinit var codec: MediaCodec
    private lateinit var muxer: MediaMuxer
    private var trackIndex = -1
    private var muxerStarted = false

    private var encodedFrameCount = 0L

    private val outputFile = File(context.cacheDir, "temp_native_video.mp4")
    lateinit var inputSurface: Surface
        private set

    init {
        setup()
    }

    private fun setup() {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val codecInfo = codecList.codecInfos.firstOrNull { info ->
            info.isEncoder && info.supportedTypes.contains(mimeType) &&
                    info.getCapabilitiesForType(mimeType)
                        .colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        } ?: run {
            val avc = "video/avc"
            codecList.codecInfos.first {
                it.isEncoder && it.supportedTypes.contains(avc) &&
                        it.getCapabilitiesForType(avc)
                            .colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }
        }

        val finalMime = codecInfo.supportedTypes.first { it == mimeType || it == "video/avc" }
        val format = MediaFormat.createVideoFormat(finalMime, width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
        }

        codec = MediaCodec.createByCodecName(codecInfo.name)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = codec.createInputSurface()
        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        codec.start()
    }

    fun signalEndOfStream() {
        codec.signalEndOfInputStream()
    }

    fun drainEncoder(outputDone: () -> Unit) {
        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            }
            if (outputIndex >= 0) {
                val encodedData = codec.getOutputBuffer(outputIndex)!!

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size > 0) {
                    if (!muxerStarted) {
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }

                    val frameDurationUs = 1_000_000L / frameRate
                    bufferInfo.presentationTimeUs = encodedFrameCount * frameDurationUs
                    encodedFrameCount++

                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                }

                codec.releaseOutputBuffer(outputIndex, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    outputDone()
                    return
                }
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!muxerStarted) {
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
            }
        }
    }

    fun releaseAndSaveToGallery(fileName: String): String? {
        try {
            codec.stop()
            codec.release()
            muxer.stop()
            muxer.release()

            val displayName = "$fileName.mp4"
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")

            uri.let { it ->
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputFile.inputStream().use { it.copyTo(outputStream) }
                }
            }
            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            outputFile.delete()
            return uri.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun release() {
        if (outputFile.exists()) outputFile.delete()
    }
}

private fun wrapTextToLines(text: String, paint: Paint, maxWidth: Float): List<String> {
    val result = mutableListOf<String>()
    val paragraphs = text.split("\n")

    for (paragraph in paragraphs) {
        if (paragraph.isEmpty()) {
            result.add("")               // preserve empty lines from consecutive \n
            continue
        }
        var start = 0
        val len = paragraph.length
        while (start < len) {
            val count = paint.breakText(paragraph, start, len, true, maxWidth, null)
            var end = start + count
            if (end < len) {
                // try to break at a space
                val spaceIdx = paragraph.lastIndexOf(' ', end)
                if (spaceIdx in (start + 1) until end) {
                    end = spaceIdx
                }
            }
            result.add(paragraph.substring(start, end).trim())
            start = if (end < len && paragraph[end] == ' ') end + 1 else end
        }
    }
    return result
}

@RequiresApi(Build.VERSION_CODES.O)
private fun drawElementContent(
    canvas: Canvas,
    element: CanvasElement,
    width: Float,
    height: Float,
    paint: Paint,
    gradient: GradientConfig?,
    density: Float,
    imageBitmaps: Map<String, Bitmap>,
    strokeOnly: Boolean,
    fontCache: Map<String, Typeface> = emptyMap()
) {
    val isText = !element.content.startsWith("Shape:") && !element.content.startsWith("Image:")
    if (isText) {
        val fontFamily = element.fontFamily ?: "system"
        val typeface = fontCache[fontFamily] ?: Typeface.DEFAULT

        val textPaint = Paint(paint).apply {
            textSize = 60f * density
            this.typeface = typeface
        }

        // Wrap text to fit width, respecting newlines
        val lines = wrapTextToLines(element.content, textPaint, width)
        if (lines.isEmpty()) return

        val lineHeight = textPaint.fontSpacing
        val totalTextHeight = lineHeight * lines.size
        val fm = textPaint.fontMetrics
        val startY = (height - totalTextHeight) / 2f + (-fm.ascent)

        for ((index, line) in lines.withIndex()) {
            val lineWidth = textPaint.measureText(line)

            val x: Float = when (element.textAlign) {
                "Left" -> {
                    textPaint.textAlign = Paint.Align.LEFT
                    (width - lineWidth) / 2f
                }
                "Right" -> {
                    textPaint.textAlign = Paint.Align.RIGHT
                    width - (width - lineWidth) / 2f
                }
                "Center", null -> {
                    textPaint.textAlign = Paint.Align.CENTER
                    width / 2f
                }
                else -> {
                    textPaint.textAlign = Paint.Align.CENTER
                    width / 2f
                }
            }
            val y = startY + index * lineHeight

            if (strokeOnly) {
                val strokePaint = Paint(textPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = element.borderThickness * density
                }
                canvas.drawText(line, x, y, strokePaint)
            } else {
                if (gradient != null && paint.shader == null) {
                    textPaint.shader = LinearGradient(
                        0f, 0f, width, height,
                        gradient.startColor.toArgb(),
                        gradient.endColor.toArgb(),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawText(line, x, y, textPaint)
            }
        }
        return
    }

    if (element.content.startsWith("Image: ")) {
        var bmp = imageBitmaps[element.id] ?: return
        if (bmp.config == Bitmap.Config.HARDWARE) {
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, false)
        }
        val srcRect = Rect(0, 0, bmp.width, bmp.height)
        val dstRect = RectF(0f, 0f, width, height)
        if (!strokeOnly) {
            val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(bmp, srcRect, dstRect, imagePaint)
        } else {
            canvas.drawRect(dstRect, paint)
        }
        return
    }
    if (element.content == "Shape: ThornCrown") {
        val seed = element.id.hashCode().toLong()
        val size = Size(width, height)

        val crown = generateThornCrownPaths(seed, size)

        val vineAndroid = crown.vinePath.asAndroidPath()
        val thornsAndroid = crown.thornsPath.asAndroidPath()

        val strokeWidthScale = min(width, height) / 938f

        if (strokeOnly) {
            val borderPaint = Paint(paint).apply {
                style = Paint.Style.STROKE
                strokeWidth = element.borderThickness * density
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(vineAndroid, borderPaint)
        } else {
            val vinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = paint.color
                style = Paint.Style.STROKE
                strokeWidth = 8f * strokeWidthScale
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(vineAndroid, vinePaint)
            canvas.drawPath(thornsAndroid, paint)
        }
        return
    }
    val path = createPathForShape(element.content, width, height)
    path?.let { canvas.drawPath(it, paint) }
}

private fun createPathForShape(content: String, width: Float, height: Float): Path? {
    return when {
        content == "Shape: Square" -> {
            Path().apply { addRect(0f, 0f, width, height, Path.Direction.CW) }
        }
        content == "Shape: Circle" -> {
            val cx = width / 2f
            val cy = height / 2f
            val r = min(cx, cy)
            Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        }
        content == "Shape: Triangle" -> {
            Path().apply {
                moveTo(width / 2f, 0f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
        }
        content == "Shape: Line" -> {
            Path().apply {
                moveTo(0f, height / 2f)
                lineTo(width, height / 2f)
            }
        }
        content == "Shape: Pentagon" -> createRegularPolygonPath(5, width, height)
        content == "Shape: Hexagon" -> createRegularPolygonPath(6, width, height)
        content == "Shape: Star" -> createStarPath(width, height)
        content == "Shape: Diamond" -> {
            Path().apply {
                moveTo(width / 2f, 0f)
                lineTo(width, height / 2f)
                lineTo(width / 2f, height)
                lineTo(0f, height / 2f)
                close()
            }
        }
        content == "Shape: Heart" -> {
            Path().apply {
                moveTo(width * 0.5f, height * 0.32f)
                cubicTo(width * 0.32f, height * 0.08f, width * 0.06f, height * 0.16f, width * 0.06f, height * 0.44f)
                cubicTo(width * 0.06f, height * 0.64f, width * 0.36f, height * 0.80f, width * 0.5f, height * 0.96f)
                cubicTo(width * 0.64f, height * 0.80f, width * 0.94f, height * 0.64f, width * 0.94f, height * 0.44f)
                cubicTo(width * 0.94f, height * 0.16f, width * 0.68f, height * 0.08f, width * 0.5f, height * 0.32f)
                close()
            }
        }
        content == "Shape: ArrowRight" -> {
            Path().apply {
                moveTo(0f, height * 0.3f)
                lineTo(width * 0.6f, height * 0.3f)
                lineTo(width * 0.6f, 0f)
                lineTo(width, height / 2f)
                lineTo(width * 0.6f, height)
                lineTo(width * 0.6f, height * 0.7f)
                lineTo(0f, height * 0.7f)
                close()
            }
        }
        content == "Shape: Octagon" -> createRegularPolygonPath(8, width, height)
        content == "Shape: Cross" -> {
            Path().apply {
                moveTo(width * 0.38f, height * 0.05f)
                lineTo(width * 0.50f, 0f)
                lineTo(width * 0.62f, height * 0.05f)
                lineTo(width * 0.60f, height * 0.21f)
                cubicTo(width * 0.60f, height * 0.28f, width * 0.68f, height * 0.28f, width * 0.68f, height * 0.28f)
                lineTo(width * 0.9f, height * 0.24f)
                lineTo(width * 0.98f, height * 0.35f)
                lineTo(width * 0.9f, height * 0.46f)
                lineTo(width * 0.68f, height * 0.42f)
                cubicTo(width * 0.60f, height * 0.42f, width * 0.60f, height * 0.52f, width * 0.60f, height * 0.52f)
                lineTo(width * 0.62f, height * 0.95f)
                lineTo(width * 0.50f, height)
                lineTo(width * 0.38f, height * 0.95f)
                lineTo(width * 0.40f, height * 0.52f)
                cubicTo(width * 0.40f, height * 0.42f, width * 0.32f, height * 0.42f, width * 0.32f, height * 0.42f)
                lineTo(width * 0.1f, height * 0.46f)
                lineTo(width * 0.02f, height * 0.35f)
                lineTo(width * 0.1f, height * 0.24f)
                lineTo(width * 0.32f, height * 0.28f)
                cubicTo(width * 0.40f, height * 0.28f, width * 0.40f, height * 0.21f, width * 0.40f, height * 0.21f)
                close()
            }
        }
        content == "Shape: ThornCrown" -> null
        content == "Shape: Moon" -> {
            Path().apply {
                addOval(0f, 0f, width, height, Path.Direction.CW)
                addOval(width * 0.35f, -height * 0.05f, width * 1.35f, height * 1.05f, Path.Direction.CW)
                op(Path().apply { addOval(0f, 0f, width, height, Path.Direction.CW) },
                    Path().apply { addOval(width * 0.35f, -height * 0.05f, width * 1.35f, height * 1.05f, Path.Direction.CW) },
                    Path.Op.DIFFERENCE)
            }
        }
        content == "Shape: DavidStar" -> createDavidStarPath(width, height)
        content == "Shape: Gear" -> createGearPath(width, height)
        content.startsWith("Shape:CustomPolygon:") || content.startsWith("Shape:CustomLine:") -> {
            parseBezierPath(content, width, height)
        }
        else -> null
    }
}

private fun createRegularPolygonPath(sides: Int, w: Float, h: Float): Path {
    val path = Path()
    val cx = w / 2f
    val cy = h / 2f
    val r = min(cx, cy)
    val angleOffset = -PI.toFloat() / 2f
    for (i in 0 until sides) {
        val angle = angleOffset + 2f * PI.toFloat() * i / sides
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun createStarPath(w: Float, h: Float): Path {
    val points = 5
    val path = Path()
    val cx = w / 2f
    val cy = h / 2f
    val outerR = min(cx, cy)
    val innerR = outerR * 0.4f
    val angleOffset = -PI.toFloat() / 2f
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerR else innerR
        val angle = angleOffset + PI.toFloat() * i / points
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun createDavidStarPath(w: Float, h: Float): Path {
    val cx = w / 2f
    val cy = h / 2f
    val rOuter = min(cx, cy)
    val rInner = rOuter / sqrt(3f)
    val path = Path()
    val vertices = List(12) { i ->
        val angleDeg = -90f + i * 30f
        val rad = angleDeg * PI.toFloat() / 180f
        val r = if (i % 2 == 0) rOuter else rInner
        Offset(cx + r * cos(rad), cy + r * sin(rad))
    }
    path.moveTo(vertices[0].x, vertices[0].y)
    val curveFactor = 0.85f
    for (i in 0 until 12) {
        val start = vertices[i]
        val end = vertices[(i + 1) % 12]
        val midX = (start.x + end.x) / 2f
        val midY = (start.y + end.y) / 2f
        val ctrlX = cx + (midX - cx) * curveFactor
        val ctrlY = cy + (midY - cy) * curveFactor
        path.quadTo(ctrlX, ctrlY, end.x, end.y)
    }
    path.close()
    return path
}

private fun createGearPath(w: Float, h: Float): Path {
    val teeth = 8
    val cx = w / 2f
    val cy = h / 2f
    val rOuter = min(cx, cy)
    val rInner = rOuter * 0.7f
    val rHole = rOuter * 0.25f
    val step = 2f * PI.toFloat() / teeth
    val offset = -PI.toFloat() / 2f
    val outerPath = Path()
    for (i in 0 until teeth) {
        val a1 = offset + step * (i + 0.1f)
        val a2 = offset + step * (i + 0.3f)
        val a3 = offset + step * (i + 0.7f)
        val a4 = offset + step * (i + 0.9f)
        val p1x = cx + rInner * cos(a1); val p1y = cy + rInner * sin(a1)
        val p2x = cx + rOuter * cos(a2); val p2y = cy + rOuter * sin(a2)
        val p3x = cx + rOuter * cos(a3); val p3y = cy + rOuter * sin(a3)
        val p4x = cx + rInner * cos(a4); val p4y = cy + rInner * sin(a4)
        if (i == 0) outerPath.moveTo(p1x, p1y)
        else outerPath.lineTo(p1x, p1y)
        outerPath.lineTo(p2x, p2y)
        outerPath.lineTo(p3x, p3y)
        outerPath.lineTo(p4x, p4y)
    }
    outerPath.close()
    val holePath = Path().apply { addCircle(cx, cy, rHole, Path.Direction.CW) }
    val result = Path()
    result.op(outerPath, holePath, Path.Op.DIFFERENCE)
    return result
}

private fun parseBezierPath(content: String, width: Float, height: Float): Path? {
    val prefix = if (content.startsWith("Shape:CustomPolygon:")) "Shape:CustomPolygon:"
    else "Shape:CustomLine:"
    val serialized = content.removePrefix(prefix)
    val rawNodes = serialized.split(";").mapNotNull { nodeStr ->
        val parts = nodeStr.split(":")
        if (parts.size >= 2) {
            val a = parts[0].split(",")
            val hi = parts[1].split(",")
            val ho = parts.getOrNull(2)?.split(",") ?: hi
            if (a.size == 2 && hi.size == 2 && ho.size == 2) {
                BezierNodeData(
                    anchor = Offset(a[0].toFloatOrNull() ?: 0f, a[1].toFloatOrNull() ?: 0f),
                    handleIn = Offset(hi[0].toFloatOrNull() ?: 0f, hi[1].toFloatOrNull() ?: 0f),
                    handleOut = Offset(ho[0].toFloatOrNull() ?: 0f, ho[1].toFloatOrNull() ?: 0f)
                )
            } else null
        } else {
            val coords = parts[0].split(",")
            if (coords.size == 2) {
                val pt = Offset(coords[0].toFloatOrNull() ?: 0f, coords[1].toFloatOrNull() ?: 0f)
                BezierNodeData(pt, pt, pt)
            } else null
        }
    }
    if (rawNodes.isEmpty()) return null
    val allPts = rawNodes.flatMap { listOf(it.anchor, it.handleIn, it.handleOut) }
    val minX = allPts.minOf { it.x }
    val maxX = allPts.maxOf { it.x }
    val minY = allPts.minOf { it.y }
    val maxY = allPts.maxOf { it.y }
    val polyW = maxX - minX
    val polyH = maxY - minY

    val normalized = rawNodes.map { n ->
        BezierNodeData(
            anchor = Offset(if (polyW > 0) (n.anchor.x - minX) / polyW else 0.5f,
                if (polyH > 0) (n.anchor.y - minY) / polyH else 0.5f),
            handleIn = Offset(if (polyW > 0) (n.handleIn.x - minX) / polyW else 0.5f,
                if (polyH > 0) (n.handleIn.y - minY) / polyH else 0.5f),
            handleOut = Offset(if (polyW > 0) (n.handleOut.x - minX) / polyW else 0.5f,
                if (polyH > 0) (n.handleOut.y - minY) / polyH else 0.5f)
        )
    }

    val path = Path()
    val first = normalized[0]
    path.moveTo(first.anchor.x * width, first.anchor.y * height)
    for (i in 1 until normalized.size) {
        val prev = normalized[i - 1]
        val curr = normalized[i]
        path.cubicTo(
            prev.handleOut.x * width, prev.handleOut.y * height,
            curr.handleIn.x * width, curr.handleIn.y * height,
            curr.anchor.x * width, curr.anchor.y * height
        )
    }
    if (content.startsWith("Shape:CustomPolygon:")) {
        val last = normalized.last()
        path.cubicTo(
            last.handleOut.x * width, last.handleOut.y * height,
            first.handleIn.x * width, first.handleIn.y * height,
            first.anchor.x * width, first.anchor.y * height
        )
        path.close()
    }
    return path
}