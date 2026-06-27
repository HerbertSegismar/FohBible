package com.fountofhopedotorg.fohbible.gfx_creator

import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fountofhopedotorg.fohbible.data.BezierNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

enum class ActiveControl { ANCHOR, HANDLE_IN, HANDLE_OUT }

private val BezierNodeListSaver = listSaver<List<BezierNode>, String>(
    save = { list ->
        list.map { "${it.anchor.x},${it.anchor.y}:${it.handleIn.x},${it.handleIn.y}:${it.handleOut.x},${it.handleOut.y}" }
    },
    restore = { strings ->
        strings.map { s ->
            val parts = s.split(":")
            val a = parts[0].split(",")
            val hi = parts[1].split(",")
            val ho = parts[2].split(",")
            BezierNode(
                Offset(a[0].toFloat(), a[1].toFloat()),
                Offset(hi[0].toFloat(), hi[1].toFloat()),
                Offset(ho[0].toFloat(), ho[1].toFloat())
            )
        }
    }
)

@Composable
fun CustomPolygonDialog(
    initialSerializedPoints: String? = null,
    isLineMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (List<BezierNode>, Boolean) -> Unit
) {
    var isCloseState by rememberSaveable { mutableStateOf(!isLineMode) }

    var points by rememberSaveable(stateSaver = BezierNodeListSaver) {
        mutableStateOf(
            if (!initialSerializedPoints.isNullOrBlank()) {
                try {
                    initialSerializedPoints.split(";").map { nodeStr ->
                        val parts = nodeStr.split(":")
                        val anchorParts = parts[0].split(",")
                        val handleInParts = parts[1].split(",")
                        val handleOutParts = parts[2].split(",")
                        BezierNode(
                            anchor = Offset(anchorParts[0].toFloat(), anchorParts[1].toFloat()),
                            handleIn = Offset(handleInParts[0].toFloat(), handleInParts[1].toFloat()),
                            handleOut = Offset(handleOutParts[0].toFloat(), handleOutParts[1].toFloat())
                        )
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        )
    }

    var redoStack by rememberSaveable(stateSaver = BezierNodeListSaver) { mutableStateOf(emptyList()) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(if (points.isNotEmpty()) 0 else -1) }
    var nudgeAmountIndex by rememberSaveable { mutableIntStateOf(0) }
    var activeControl by rememberSaveable { mutableStateOf(ActiveControl.ANCHOR) }
    var referenceImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var referenceImageOpacity by remember { mutableFloatStateOf(1f) }
    var showOpacitySlider by remember { mutableStateOf(false) }

    LaunchedEffect(referenceImageBitmap, referenceImageOpacity) {
        if (referenceImageBitmap != null) {
            showOpacitySlider = true
            delay(3000.milliseconds)
            showOpacitySlider = false
        } else {
            showOpacitySlider = false
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val bitmap = context.contentResolver.openInputStream(it)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                    withContext(Dispatchers.Main) {
                        referenceImageBitmap = bitmap
                        if (bitmap != null) {
                            referenceImageOpacity = 1f
                            showOpacitySlider = true
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    var drawingAreaSize by remember { mutableStateOf(IntSize.Zero) }
    val nudgeAmounts = listOf(0.01f, 0.05f, 0.1f)
    val currentNudgeAmount = nudgeAmounts[nudgeAmountIndex]

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isEditing = !initialSerializedPoints.isNullOrBlank()

    val minPointsRequired = if (!isCloseState) 2 else 3
    val titleText = if (!isCloseState) {
        if (isEditing) "Edit Line Points" else "Tap to Add Line Points"
    } else {
        if (isEditing) "Edit Polygon Points" else "Tap to Add Polygon Points"
    }

    val onConfirmAction = {
        if (points.size >= minPointsRequired) {
            val minX = points.minOf { minOf(it.anchor.x, it.handleIn.x, it.handleOut.x) }
            val maxX = points.maxOf { maxOf(it.anchor.x, it.handleIn.x, it.handleOut.x) }
            val minY = points.minOf { minOf(it.anchor.y, it.handleIn.y, it.handleOut.y) }
            val maxY = points.maxOf { maxOf(it.anchor.y, it.handleIn.y, it.handleOut.y) }

            val boundsWidth = (maxX - minX).takeIf { it > 0f } ?: 1f
            val boundsHeight = (maxY - minY).takeIf { it > 0f } ?: 1f

            val normalizedPoints = points.map { p ->
                BezierNode(
                    anchor = Offset((p.anchor.x - minX) / boundsWidth, (p.anchor.y - minY) / boundsHeight),
                    handleIn = Offset((p.handleIn.x - minX) / boundsWidth, (p.handleIn.y - minY) / boundsHeight),
                    handleOut = Offset((p.handleOut.x - minX) / boundsWidth, (p.handleOut.y - minY) / boundsHeight)
                )
            }
            onConfirm(normalizedPoints, !isCloseState)
        }
    }

    val nudgePoint = { dx: Float, dy: Float ->
        if (selectedIndex in points.indices) {
            val p = points[selectedIndex]
            points = points.toMutableList().also { list ->
                when (activeControl) {
                    ActiveControl.ANCHOR -> {
                        val newAnchor = Offset(
                            (p.anchor.x + dx).coerceIn(0f, 1f),
                            (p.anchor.y + dy).coerceIn(0f, 1f)
                        )
                        val moveX = newAnchor.x - p.anchor.x
                        val moveY = newAnchor.y - p.anchor.y
                        list[selectedIndex] = p.copy(
                            anchor = newAnchor,
                            handleIn = Offset((p.handleIn.x + moveX).coerceIn(0f, 1f), (p.handleIn.y + moveY).coerceIn(0f, 1f)),
                            handleOut = Offset((p.handleOut.x + moveX).coerceIn(0f, 1f), (p.handleOut.y + moveY).coerceIn(0f, 1f))
                        )
                    }
                    ActiveControl.HANDLE_IN -> {
                        list[selectedIndex] = p.copy(
                            handleIn = Offset(
                                (p.handleIn.x + dx).coerceIn(0f, 1f),
                                (p.handleIn.y + dy).coerceIn(0f, 1f)
                            )
                        )
                    }
                    ActiveControl.HANDLE_OUT -> {
                        list[selectedIndex] = p.copy(
                            handleOut = Offset(
                                (p.handleOut.x + dx).coerceIn(0f, 1f),
                                (p.handleOut.y + dy).coerceIn(0f, 1f)
                            )
                        )
                    }
                }
            }
        }
    }

    val canvasArea: @Composable (Modifier) -> Unit = { modifier ->
        Box(
            modifier = modifier
                .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
                .onSizeChanged { drawingAreaSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        if (drawingAreaSize.width > 0 && drawingAreaSize.height > 0) {
                            val normX = tapOffset.x / drawingAreaSize.width
                            val normY = tapOffset.y / drawingAreaSize.height
                            val clamped = Offset(
                                normX.coerceIn(0f, 1f),
                                normY.coerceIn(0f, 1f)
                            )
                            points = points + BezierNode(clamped, clamped, clamped)
                            selectedIndex = points.lastIndex
                            activeControl = ActiveControl.ANCHOR
                            redoStack = emptyList()
                        }
                    }
                }
        ) {
            if (referenceImageBitmap != null) {
                Image(
                    bitmap = referenceImageBitmap!!,
                    contentDescription = "Reference image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    alpha = referenceImageOpacity
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasSize = this.size

                if (points.size >= minPointsRequired) {
                    val path = Path().apply {
                        moveTo(
                            points[0].anchor.x * canvasSize.width,
                            points[0].anchor.y * canvasSize.height
                        )
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            cubicTo(
                                prev.handleOut.x * canvasSize.width, prev.handleOut.y * canvasSize.height,
                                curr.handleIn.x * canvasSize.width, curr.handleIn.y * canvasSize.height,
                                curr.anchor.x * canvasSize.width, curr.anchor.y * canvasSize.height
                            )
                        }

                        if (isCloseState) {
                            val last = points.last()
                            val first = points.first()
                            cubicTo(
                                last.handleOut.x * canvasSize.width, last.handleOut.y * canvasSize.height,
                                first.handleIn.x * canvasSize.width, first.handleIn.y * canvasSize.height,
                                first.anchor.x * canvasSize.width, first.anchor.y * canvasSize.height
                            )
                            close()
                        }
                    }

                    if (isCloseState) {
                        drawPath(path, color = Color(0x4000BCD4), style = Fill)
                    }
                    drawPath(path, color = Color(0xFF00BCD4), style = Stroke(width = 3f))
                }

                if (isCloseState && points.size == 2) {
                    val curr = points[0]
                    val next = points[1]
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(curr.anchor.x * canvasSize.width, curr.anchor.y * canvasSize.height),
                        end = Offset(next.anchor.x * canvasSize.width, next.anchor.y * canvasSize.height),
                        strokeWidth = 2f
                    )
                }

                points.forEachIndexed { index, point ->
                    val isSelected = index == selectedIndex
                    val anchorPos = Offset(point.anchor.x * canvasSize.width, point.anchor.y * canvasSize.height)

                    drawCircle(
                        color = if (isSelected && activeControl == ActiveControl.ANCHOR) Color.Blue
                        else if (isSelected) Color.Cyan
                        else Color.Red,
                        radius = if (isSelected && activeControl == ActiveControl.ANCHOR) 12f else 8f,
                        center = anchorPos
                    )
                    if (isSelected) {
                        val hiPos = Offset(point.handleIn.x * canvasSize.width, point.handleIn.y * canvasSize.height)
                        val hoPos = Offset(point.handleOut.x * canvasSize.width, point.handleOut.y * canvasSize.height)

                        drawLine(Color.Gray, anchorPos, hiPos, 2f)
                        drawCircle(
                            color = if (activeControl == ActiveControl.HANDLE_IN) Color.Green else Color.DarkGray,
                            radius = if (activeControl == ActiveControl.HANDLE_IN) 10f else 6f,
                            center = hiPos
                        )

                        drawLine(Color.Gray, anchorPos, hoPos, 2f)
                        drawCircle(
                            color = if (activeControl == ActiveControl.HANDLE_OUT) Color.Magenta else Color.DarkGray,
                            radius = if (activeControl == ActiveControl.HANDLE_OUT) 10f else 6f,
                            center = hoPos
                        )
                    }
                }
            }
        }
    }

    val controlsArea: @Composable (Modifier) -> Unit = { modifier ->
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLandscape) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newPoint = if (selectedIndex in points.indices) {
                            val p = points[selectedIndex]
                            BezierNode(
                                Offset((p.anchor.x + 0.05f).coerceIn(0f, 1f), (p.anchor.y + 0.05f).coerceIn(0f, 1f)),
                                Offset((p.handleIn.x + 0.05f).coerceIn(0f, 1f), (p.handleIn.y + 0.05f).coerceIn(0f, 1f)),
                                Offset((p.handleOut.x + 0.05f).coerceIn(0f, 1f), (p.handleOut.y + 0.05f).coerceIn(0f, 1f))
                            )
                        } else {
                            BezierNode(Offset(0.5f, 0.5f), Offset(0.5f, 0.5f), Offset(0.5f, 0.5f))
                        }
                        val insertIndex = if (selectedIndex in points.indices) selectedIndex + 1 else points.size
                        points = points.toMutableList().apply { add(insertIndex, newPoint) }
                        selectedIndex = insertIndex
                        activeControl = ActiveControl.ANCHOR
                        redoStack = emptyList()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Point", tint = MaterialTheme.colorScheme.secondary)
                }

                IconButton(
                    onClick = {
                        if (points.isNotEmpty() && selectedIndex in points.indices) {
                            points = points.toMutableList().apply { removeAt(selectedIndex) }
                            if (points.isEmpty()) {
                                selectedIndex = -1
                            } else if (selectedIndex >= points.size) {
                                selectedIndex = points.lastIndex
                            }
                            redoStack = emptyList()
                        }
                    },
                    enabled = points.isNotEmpty()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Point", tint = MaterialTheme.colorScheme.secondary)
                }

                IconButton(
                    onClick = {
                        if (points.isNotEmpty()) {
                            selectedIndex = (selectedIndex - 1 + points.size) % points.size
                        }
                    },
                    enabled = points.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cycle Selected Point", tint = MaterialTheme.colorScheme.secondary)
                }

                IconButton(
                    onClick = {
                        if (points.isNotEmpty()) {
                            selectedIndex = (selectedIndex + 1) % points.size
                        }
                    },
                    enabled = points.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Cycle Selected Point", tint = MaterialTheme.colorScheme.secondary)
                }

                IconButton(
                    onClick = {
                        if (points.isNotEmpty()) {
                            val removedPoint = points.last()
                            points = points.dropLast(1)
                            redoStack = redoStack + removedPoint

                            if (selectedIndex >= points.size) {
                                selectedIndex = points.lastIndex
                            }
                        }
                    },
                    enabled = points.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.secondary)
                }

                IconButton(
                    onClick = {
                        if (redoStack.isNotEmpty()) {
                            val restoredPoint = redoStack.last()
                            redoStack = redoStack.dropLast(1)
                            points = points + restoredPoint
                            selectedIndex = points.lastIndex
                        }
                    },
                    enabled = redoStack.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = MaterialTheme.colorScheme.secondary)
                }
            }

            BezierModeSelector(
                activeControl = activeControl,
                onControlSelected = { activeControl = it },
                modifier = Modifier.padding(vertical = 4.dp),
            )

            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HybridJoystick(
                    nudgeAmount = currentNudgeAmount,
                    enabled = points.isNotEmpty(),
                    onNudgeAmountClick = {
                        nudgeAmountIndex = (nudgeAmountIndex + 1) % nudgeAmounts.size
                    },
                    onDirectionClick = { dx, dy ->
                        nudgePoint(dx, dy)
                    }
                )

                if (referenceImageBitmap != null) {
                    Box(
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            if (showOpacitySlider) {
                                Box(
                                    modifier = Modifier.size(width = 120.dp, height = 24.dp).padding(top = 70.dp)
                                ) {
                                    Slider(
                                        value = referenceImageOpacity,
                                        onValueChange = { referenceImageOpacity = it },
                                        valueRange = 0f..1f,
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(24.dp)
                                            .rotate(-90f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(110.dp))
                                Text(
                                    text = "Opacity",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            else {
                                IconButton(
                                    onClick = { showOpacitySlider = true }
                                ) {
                                    Icon(
                                        Icons.Default.Opacity,
                                        contentDescription = "Show opacity slider",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(if (isLandscape) 0.97f else 0.95f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                if (!isLandscape) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.95f)
                                    .fillMaxHeight()
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                canvasArea(Modifier)
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    controlsArea(Modifier)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 36.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp).weight(1f)
                                    ) {
                                        Text("Fill")
                                        Checkbox(
                                            checked = isCloseState,
                                            onCheckedChange = { isCloseState = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary,
                                                checkmarkColor = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { imagePickerLauncher.launch("image/*") }
                                        ) {
                                            Icon(
                                                Icons.Default.Image,
                                                modifier = Modifier.size(26.dp),
                                                contentDescription = "Load Reference Image",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    TextButton(onClick = onDismiss) { Text("Cancel") }

                                    TextButton(
                                        onClick = {
                                            points = emptyList()
                                            redoStack = emptyList()
                                            selectedIndex = -1
                                        },
                                        enabled = points.isNotEmpty()
                                    ) {
                                        Text("Clear")
                                    }

                                    TextButton(
                                        onClick = onConfirmAction,
                                        enabled = points.size >= minPointsRequired
                                    ) {
                                        Text(if (isEditing) "Save Changes" else "Add to Canvas")
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                canvasArea(Modifier.fillMaxSize())
                            }

                            Spacer(Modifier.height(16.dp))

                            controlsArea(Modifier.fillMaxWidth())
                        }
                    }
                }

                if (!isLandscape) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp).weight(1f)
                            ) {
                                Text("Fill")
                                Checkbox(
                                    checked = isCloseState,
                                    onCheckedChange = { isCloseState = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { imagePickerLauncher.launch("image/*") }
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        modifier = Modifier.size(26.dp),
                                        contentDescription = "Load Reference Image",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            TextButton(onClick = onDismiss) { Text("Cancel") }

                            TextButton(
                                onClick = {
                                    points = emptyList()
                                    redoStack = emptyList()
                                    selectedIndex = -1
                                },
                                enabled = points.isNotEmpty()
                            ) {
                                Text("Clear")
                            }

                            TextButton(
                                onClick = onConfirmAction,
                                enabled = points.size >= minPointsRequired
                            ) {
                                Text(if (isEditing) "Save Changes" else "Add to Canvas")
                            }
                        }
                    }
                }
            }
        }
    }
}
