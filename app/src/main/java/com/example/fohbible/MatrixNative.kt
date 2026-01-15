package com.example.fohbible

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fohbible.screens.getFontFamily
import com.example.fohbible.ui.theme.DefaultPrimaryColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

val JesusAttributes = listOf(
    "Jesus Christ", "Messiah", "Savior", "Redeemer", "Son of God", "Lamb of God", "King of Kings", "Lord of Lords",
    "Prince of Peace", "Alpha and Omega", "The Way", "The Truth", "The Life", "Good Shepherd", "Light of the World",
    "Bread of Life", "The Resurrection", "Emmanuel", "Wonderful Counselor", "Mighty God", "Everlasting Father",
    "The Word", "Son of Man", "The Door", "The Vinedresser", "True Vine", "The Amen", "Author and Finisher of Our Faith",
    "Chief Cornerstone", "Bright Morning Star", "Lion of the Tribe of Judah", "Root of David", "Holy One of Israel",
    "Bridegroom", "Head of the Church", "Mediator", "Great High Priest", "The Prophet", "The Rock", "Stone of Stumbling",
    "Captain of Our Salvation", "Chosen One", "Image of the Invisible God", "Firstborn Over All Creation",
    "Firstborn from the Dead", "The Righteous One", "I AM", "The Great I Am", "Lord of All", "Judge of the Living and the Dead",
    "Shiloh", "Sun of Righteousness", "The Branch", "Man of Sorrows", "Faithful and True Witness", "The Amen",
    "Lord of Glory", "The Power of God", "The Wisdom of God", "Our Passover Lamb", "Shepherd of Souls",
    "The Resurrection and the Life", "The Holy One", "The Just One", "The Advocate", "The Deliverer", "The Hope of Nations",
    "The Consolation of Israel", "The Desire of All Nations", "The Fountain of Living Waters", "The Rod from the Stem of Jesse",
    "The Governor Among the Nations", "The Word of Life", "The Spirit of Life", "The Beloved Son", "The Light of Men",
    "The True Light", "The Horn of Salvation", "The Dayspring from on High", "The Upholder of All Things",
    "The Apostle of Our Confession", "The Bishop of Souls", "The Christ of God", "The Holy Servant", "The Pioneer of Salvation",
    "The Author of Eternal Salvation", "The Forerunner", "The Lawgiver", "The Lord of the Harvest", "The Lord of the Sabbath",
    "The Truth of God", "The Vine", "The Living Stone", "The Chosen Stone", "The Precious Cornerstone", "The Foundation",
    "The Temple", "The Light of Heaven", "The King of the Jews", "The King of Israel", "The King of Righteousness",
    "The King of Peace", "The King of Glory", "The Lord Strong and Mighty", "The Lord Mighty in Battle", "The Lord of Hosts",
    "The Lord Our Righteousness", "The Lord Who Heals", "The Lord Who Provides", "The Lord Who Sanctifies", "The Lord Who Sees",
    "The Angel of God", "The Angel of the Lord", "Yahweh", "Jehovah", "Elohim", "El Shaddai", "Adonai", "Jehovah Jireh",
    "Jehovah Rapha", "Jehovah Nissi", "Jehovah Shalom", "Jehovah Raah", "Jehovah Tsidkenu", "Jehovah Shammah", "El Elyon",
    "El Roi", "El Olam", "Yahweh Yireh", "Yahweh Rapha", "Yahweh Nissi", "Yahweh Shalom", "Yahweh Raah", "Yahweh Tsidkenu",
    "Yahweh Shammah", "Yahweh Sabaoth",
)

const val MATRIX_HEIGHT = 612f
val FONT_SIZE = 12.sp
const val TRAIL_LENGTH = 15

data class Drop(
    val id: String,
    val headAnim: Animatable<Float, *>,
    val trailChars: List<String>,
    val x: Float
)

data class Overlay(
    val id: String,
    val text: String,
    val left: Float,
    val top: Float,
    val fontSize: Float,
    val fadeAnim: Animatable<Float, *>,
    val positionAnim: Animatable<Float, *>
)

@Composable
fun MatrixNative() {
    val viewModel: AppViewModel = viewModel()
    val safeMatrixColor = viewModel.selectedColor ?: DefaultPrimaryColor
    val bgColor = MaterialTheme.colorScheme.surface
    // Get the theme's primary color for overlays
    val overlayColor = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val typeface = remember(viewModel.selectedFontFamily) {
        when (viewModel.selectedFontFamily) {
            "system" -> Typeface.DEFAULT
            "oswald" -> Typeface.createFromAsset(context.assets, "fonts/Oswald.ttf")
            "rubik-glitch" -> Typeface.createFromAsset(context.assets, "fonts/RubikGlitch.ttf")
            "poppins" -> Typeface.createFromAsset(context.assets, "fonts/Poppins.ttf")
            else -> Typeface.DEFAULT
        }
    }
    val overlayTypeface = remember {
        Typeface.createFromAsset(context.assets, "fonts/RubikGlitch.ttf")
    }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    val drops = remember { mutableStateListOf<Drop>() }
    val coroutineScope = rememberCoroutineScope()
    val matrixChars =
        "アァカサタナハマヤャラワガザダバパイィキシチニヒミリヰギジヂビピウゥクスツヌフムユュルグズブヅプエェケセテネヘメレヱゲゼデベペオォコソトノホモヨョロヲゴゾドボポヴッン0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ~!@#$%^&*()_-+=ᜀᜁᜂᜃᜄᜅᜆᜇᜈᜉᜊᜋᜌᜎᜏᜐᜑ"
    val density = LocalDensity.current
    val fontSizePx = with(density) { FONT_SIZE.toPx() }
    val paint = remember(safeMatrixColor, typeface) {
        Paint().apply {
            textSize = fontSizePx
            color = safeMatrixColor.toArgb()
            setShadowLayer(2f, 0f, 0f, safeMatrixColor.copy(alpha = 0.5f).toArgb())
            isAntiAlias = false
            this.typeface = typeface
        }
    }
    // Remember overlayPaint with overlayColor as a key to update when theme changes
    val overlayPaint = remember(overlayColor, overlayTypeface) {
        Paint().apply {
            isAntiAlias = false
            this.typeface = overlayTypeface
            // We'll set textSize and alpha dynamically in the Canvas
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Here is the Fount of Hope",
                style = TextStyle(
                    fontSize = FONT_SIZE * 1.5f,
                    textAlign = TextAlign.Center,
                    color = safeMatrixColor,
                    fontFamily = getFontFamily("rubik-glitch")
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MATRIX_HEIGHT.dp)
            ) {
                var containerWidth by remember { mutableFloatStateOf(0f) }
                var containerHeight by remember { mutableFloatStateOf(0f) }
                LaunchedEffect(containerWidth) {
                    if (containerWidth == 0f) return@LaunchedEffect
                    val numColumns = (containerWidth / fontSizePx).toInt()

                    // CHANGED: Only create drops for 70% of the columns
                    val numDrops = (numColumns * 0.7).roundToInt()

                    drops.forEach { it.headAnim.stop() }
                    drops.clear()

                    // CHANGED: Create a list of column indices and take 70% of them
                    val columns = (0 until numColumns).shuffled().take(numDrops)

                    for (i in columns) {
                        val headAnim = Animatable(0f)
                        val trailChars = List(TRAIL_LENGTH) { matrixChars.random().toString() }
                        val x = i * fontSizePx
                        drops.add(Drop("drop-$i", headAnim, trailChars, x))
                        coroutineScope.launch {
                            startDropAnimation(headAnim, containerHeight + TRAIL_LENGTH * fontSizePx)
                        }
                    }
                }

                LaunchedEffect(containerWidth) {
                    if (containerWidth == 0f) return@LaunchedEffect
                    showRandomAttribute(
                        containerWidth,
                        containerHeight,
                        coroutineScope,
                        onNewOverlay = { newOverlay ->
                            overlay = newOverlay
                        }
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            containerWidth = size.width.toFloat()
                            containerHeight = size.height.toFloat()
                        }
                ) {
                    // Draw the matrix drops
                    drops.forEach { drop ->
                        for (j in 0 until TRAIL_LENGTH) {
                            val offset = -j * fontSizePx
                            val y = drop.headAnim.value + offset
                            if (y > 0 && y < size.height) {
                                val opacity = if (j == 0) 1f else max(0f, 1f - (j.toFloat() / TRAIL_LENGTH) * 1.2f)
                                paint.alpha = (opacity * 255).toInt()
                                drawIntoCanvas {
                                    it.nativeCanvas.drawText(drop.trailChars[j], drop.x, y, paint)
                                }
                            }
                        }
                    }
                    // Draw the overlay directly on the Canvas using theme primary color
                    overlay?.let { o ->
                        // Update overlayPaint with current values
                        overlayPaint.apply {
                            textSize = o.fontSize * density.density
                            color = overlayColor.copy(alpha = o.fadeAnim.value).toArgb()
                        }
                        drawIntoCanvas {
                            it.nativeCanvas.drawText(
                                o.text,
                                o.left,
                                o.top + o.positionAnim.value,
                                overlayPaint
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun startDropAnimation(
    anim: Animatable<Float, *>,
    totalHeight: Float
) {
    while (true) {
        anim.snapTo(0f)
        val duration = 2000 + Random.nextInt(3000)
        anim.animateTo(
            targetValue = totalHeight,
            animationSpec = tween(duration, easing = LinearEasing)
        )
        // Increased gap delay for sparser rain
        val gapDelay = 1000 + Random.nextInt(3000)  // Changed from 500-2500 to 1000-4000
        delay(gapDelay.toLong())
    }
}

private suspend fun showRandomAttribute(
    containerWidth: Float,
    height: Float,
    scope: CoroutineScope,
    onNewOverlay: (Overlay?) -> Unit
) {
    while (true) {
        val randomAttribute = JesusAttributes.random()
        val fs = Random.nextFloat() * 12 + 10
        val estimatedWidth = randomAttribute.length * (fs / 2.5f)
        var left = Random.nextFloat() * 0.8f * containerWidth
        left = min(left, containerWidth - estimatedWidth)
        val top = Random.nextFloat() * 0.8f * height
        val fadeAnim = Animatable(0f)
        val positionAnim = Animatable(20f)
        val newOverlay = Overlay(
            id = "${System.currentTimeMillis()}-${Random.nextFloat()}",
            text = randomAttribute,
            left = left,
            top = top,
            fontSize = fs,
            fadeAnim = fadeAnim,
            positionAnim = positionAnim
        )
        onNewOverlay(newOverlay)
        val fadeInJobs: List<Job> = listOf(
            scope.launch { fadeAnim.animateTo(1f, animationSpec = tween(500)) },
            scope.launch { positionAnim.animateTo(0f, animationSpec = tween(500)) }
        )
        fadeInJobs.joinAll()
        delay(2000)
        val fadeOutJobs: List<Job> = listOf(
            scope.launch { fadeAnim.animateTo(0f, animationSpec = tween(1500)) },
            scope.launch { positionAnim.animateTo(-20f, animationSpec = tween(1500)) }
        )
        fadeOutJobs.joinAll()
        onNewOverlay(null)
    }
}