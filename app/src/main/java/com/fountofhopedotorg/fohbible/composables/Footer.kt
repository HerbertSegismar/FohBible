package com.fountofhopedotorg.fohbible.composables

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.fountofhopedotorg.fohbible.R
import java.util.Calendar

@Composable
fun Footer() {
    val context = LocalContext.current
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Made with ❤️ For All Believers Worldwide",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "© Copyright $currentYear",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "App Created By: Herbert Segismar",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Contact: ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(
                text = "fountofhopedevotionals@gmail.com",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:fountofhopedevotionals@gmail.com".toUri()
                        putExtra(Intent.EXTRA_SUBJECT, "FoH Bible App Feedback")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .height(45.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = R.drawable.christisrisen2,
                contentDescription = "Christ Is Risen vector text image",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Special thanks to the ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "MyBible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val url = "https://mybible.zone/us/"
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        }
                    )
                    Text(
                        text = " team",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "for making their modules available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Visit our Daily Devotionals Website ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(
                text = "FountOfHope.Org",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val url = "https://fountofhope.org/"
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
            )
        }
    }
}