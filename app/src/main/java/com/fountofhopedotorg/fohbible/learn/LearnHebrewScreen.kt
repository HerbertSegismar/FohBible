package com.fountofhopedotorg.fohbible.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.data.HebrewLetter

@Composable
fun LearnHebrewScreen() {
    val letters = listOf(
        HebrewLetter("Aleph", "A") { progress -> drawAleph(progress) },
        HebrewLetter("Beth", "B/V") { progress -> drawBeth(progress) },
        HebrewLetter("Gimel", "G") { progress -> drawGimel(progress) },
        HebrewLetter("Dalet", "D") { progress -> drawDalet(progress) },
        HebrewLetter("He", "H") { progress -> drawHe(progress) },
        HebrewLetter("Vav", "V") { progress -> drawVav(progress) },
        HebrewLetter("Zayin", "Z") { progress -> drawZayin(progress) },
        HebrewLetter("Chet", "Ch") { progress -> drawChet(progress) }
    )

    var currentIndex by remember { mutableIntStateOf(0) }
    var replayTrigger by remember { mutableIntStateOf(0) }
    var showEnglish by remember { mutableStateOf(false) }
    val progressAnimatable = remember { Animatable(0f) }

    LaunchedEffect(currentIndex, replayTrigger) {
        showEnglish = false
        progressAnimatable.snapTo(0f)
        progressAnimatable.animateTo(1f, animationSpec = tween(4000))
        showEnglish = true
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val currentLetter = letters[currentIndex]

        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                currentLetter.draw(this, progressAnimatable.value)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(currentLetter.name, style = MaterialTheme.typography.bodyLarge, fontSize = 24.sp)
        Text("English: ${currentLetter.english}", color = MaterialTheme.colorScheme.primary, modifier = Modifier.alpha(if (showEnglish) 1f else 0f))

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Button(onClick = { replayTrigger++ }) { Text("Replay", color = Color.White) }
            Spacer(Modifier.width(16.dp))
            Button(onClick = {
                if (currentIndex < letters.size - 1) currentIndex++ else currentIndex = 0
            }) {
                Text(if (currentIndex < letters.size - 1) "Next" else "Restart", color = Color.White)
            }
        }
    }
}