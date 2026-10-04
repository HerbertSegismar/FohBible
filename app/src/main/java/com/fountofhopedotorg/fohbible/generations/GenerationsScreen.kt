package com.fountofhopedotorg.fohbible.generations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private data class Generation(
    val name: String,
    val birthYear: Int,
    val lifespan: Int,
) {
    val deathYear: Int get() = birthYear + lifespan
}

private val NAME_COLUMN_WIDTH = 90.dp
private val ROW_HEIGHT = 18.dp
private const val MIN_DP_PER_YEAR = 0.05f
private const val MAX_DP_PER_YEAR = 2.0f

private val adamToJesus = listOf(
    Generation("Adam",        0, 930),
    Generation("Seth",      130, 912),
    Generation("Enosh",     235, 905),
    Generation("Kenan",     325, 910),
    Generation("Mahalalel", 395, 895),
    Generation("Jared",     460, 962),
    Generation("Enoch",     622, 365),
    Generation("Methuselah",687, 969),
    Generation("Lamech",    874, 777),
    Generation("Noah",     1056, 950),
    Generation("Shem",     1558, 600),
    Generation("Arphaxad", 1658, 438),
    Generation("Cainan",   1693, 460),
    Generation("Shelah",   1728, 433),
    Generation("Eber",     1758, 464),
    Generation("Peleg",    1792, 239),
    Generation("Reu",      1822, 239),
    Generation("Serug",    1854, 230),
    Generation("Nahor",    1884, 148),
    Generation("Terah",    1913, 205),
    Generation("Abraham",  1983, 175),
    Generation("Isaac",    2108, 180),
    Generation("Jacob",    2168, 147),
    Generation("Judah",    2308, 119),
    Generation("Perez",    2338,  70),
    Generation("Hezron",   2368,  70),
    Generation("Ram",      2398,  70),
    Generation("Amminadab",2428,  70),
    Generation("Nahshon",  2458,  70),
    Generation("Salmon",   2488,  70),
    Generation("Boaz",     2518,  70),
    Generation("Obed",     2548,  70),
    Generation("Jesse",    2578,  70),
    Generation("David",      2608, 70),
    Generation("Nathan",     2638, 72),
    Generation("Mattatha",   2673, 65),
    Generation("Menan",      2698, 70),
    Generation("Melea",      2730, 68),
    Generation("Eliakim",    2758, 75),
    Generation("Jonan",      2793, 62),
    Generation("Joseph",     2818, 70),
    Generation("Juda",       2850, 65),
    Generation("Simeon",     2878, 72),
    Generation("Levi",       2913, 60),
    Generation("Matthat",    2938, 68),
    Generation("Jorim",      2970, 65),
    Generation("Eliezer",    2998, 75),
    Generation("Jose",       3033, 62),
    Generation("Er",         3058, 70),
    Generation("Elmodam",    3090, 65),
    Generation("Cosam",      3118, 72),
    Generation("Addi",       3153, 60),
    Generation("Melchi",     3178, 68),
    Generation("Neri",       3210, 65),
    Generation("Salathiel",  3238, 75),
    Generation("Zorobabel",  3273, 62),
    Generation("Rhesa",      3298, 70),
    Generation("Joanan",     3330, 65),
    Generation("Joda",       3358, 72),
    Generation("Josech",     3393, 60),
    Generation("Semein",     3418, 68),
    Generation("Mattathias", 3450, 65),
    Generation("Maath",      3478, 75),
    Generation("Naggai",     3513, 62),
    Generation("Esli",       3538, 70),
    Generation("Nahum",      3570, 65),
    Generation("Amos",       3598, 72),
    Generation("Mattathias", 3633, 60),
    Generation("Joseph",     3658, 68),
    Generation("Janna",      3688, 65),
    Generation("Melchi",     3721, 70),
    Generation("Levi",       3748, 75),
    Generation("Matthat",    3783, 62),
    Generation("Heli",       3808, 70),
    Generation("Joseph",     3840, 65),
    Generation("Jesus",      3868, 33),
)

private fun niceStep(minYears: Float): Int {
    val candidates = intArrayOf(25, 50, 100, 200, 250, 500, 1000, 2000, 2500, 5000, 10000)
    return candidates.firstOrNull { it >= minYears } ?: 10000
}

@Composable
fun GenerationsScreen() {
    val generations = remember { adamToJesus }

    val axisMax = remember(generations) {
        ((generations.maxOf { it.deathYear } + 499) / 500) * 500
    }

    var visibleCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        visibleCount = 0
        generations.indices.forEach { i ->
            delay(60.milliseconds)
            visibleCount = i + 1
        }
    }

    // Zoom: dp of chart width per year of timeline.
    var dpPerYear by remember { mutableFloatStateOf(0.15f) }
    val chartWidth: Dp = (axisMax * dpPerYear).dp

    // Adaptive tick step so axis labels never crowd.
    val density = LocalDensity.current
    val tickStep = remember(dpPerYear, density, axisMax) {
        val pxPerYear = with(density) { dpPerYear.dp.toPx() }
        val minPx = with(density) { 60.dp.toPx() }
        niceStep(minPx / pxPerYear)
    }

    val vScroll = rememberScrollState()
    // Shared horizontal scroll state: header and chart body stay in sync.
    val hScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Generations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Adam → Jesus · each bar is one lifetime",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Zoom scrubber
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Zoom",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = dpPerYear,
                onValueChange = { dpPerYear = it },
                valueRange = MIN_DP_PER_YEAR..MAX_DP_PER_YEAR,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
        }

        // Axis header: fixed name spacer + horizontally-scrolling axis.
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(NAME_COLUMN_WIDTH))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(hScroll),
            ) {
                AxisHeader(
                    axisMax = axisMax,
                    chartWidth = chartWidth,
                    tickStep = tickStep,
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // Body: single vertical scroll over both columns.
        // Names column is fixed width; chart column scrolls horizontally
        // using the same shared state as the header.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(vScroll),
        ) {
            Row(Modifier.fillMaxWidth()) {
                // Fixed names column — never scrolls sideways.
                Column(Modifier.width(NAME_COLUMN_WIDTH)) {
                    generations.take(visibleCount).forEach { person ->
                        key(person.name + person.birthYear) {
                            NameCell(person)
                        }
                    }
                }

                // Horizontally scrolling chart column.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(hScroll),
                ) {
                    Column(Modifier.width(chartWidth)) {
                        generations.take(visibleCount).forEach { person ->
                            key(person.name + person.birthYear) {
                                ChartCell(
                                    person = person,
                                    axisMax = axisMax,
                                    chartWidth = chartWidth,
                                    tickStep = tickStep,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AxisHeader(axisMax: Int, chartWidth: Dp, tickStep: Int) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Canvas(
        modifier = Modifier
            .width(chartWidth)
            .height(14.dp),
    ) {
        val total = axisMax.toFloat()
        val w = size.width
        val pad = 2.dp.toPx()
        var year = 0
        while (year <= axisMax) {
            val x = (year / total) * w
            val layout = textMeasurer.measure(
                text = AnnotatedString(year.toString()),
                style = labelStyle,
            )
            val lx = if (x + layout.size.width + pad > w) {
                (w - layout.size.width - pad).coerceAtLeast(0f)
            } else {
                x + pad
            }
            drawText(layout, topLeft = Offset(lx, 0f))
            year += tickStep
        }
    }
}

/** Fixed column cell: name and lifespan on a single compact line. */
@Composable
private fun NameCell(person: Generation) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = person.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = person.lifespan.toString(),
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Scrolling column cell: draws the grid + animated lifetime bar. */
@Composable
private fun ChartCell(
    person: Generation,
    axisMax: Int,
    chartWidth: Dp,
    tickStep: Int,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(person.name, person.birthYear) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        )
    }

    val barColor = MaterialTheme.colorScheme.primary
    val headColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = Modifier
            .width(chartWidth)
            .height(ROW_HEIGHT),
    ) {
        val total = axisMax.toFloat()
        val width = size.width
        val centerY = size.height / 2f
        val stroke = 5.dp.toPx()

        var year = 0
        while (year <= axisMax) {
            val x = (year / total) * width
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            year += tickStep
        }

        val birthX = (person.birthYear / total) * width
        val lifeWidth = (person.lifespan / total) * width * progress.value

        drawLine(
            color = barColor,
            start = Offset(birthX, centerY),
            end = Offset(birthX + lifeWidth, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = barColor,
            radius = stroke / 2f,
            center = Offset(birthX, centerY),
        )
        if (progress.value > 0.02f) {
            drawCircle(
                color = headColor,
                radius = stroke * 0.7f,
                center = Offset(birthX + lifeWidth, centerY),
            )
        }
    }
}