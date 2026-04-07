package com.fountofhopedotorg.fohbible.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun VersionSelectionModal(
    currentVersionKey: String,
    onVersionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    colors: Map<String, Color>
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors["primary"] as Color)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Select Bible Version",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    val versionEntries = BibleVersionUtils.versionMap.entries.toList()
                    items(versionEntries) { (key, shortName) ->
                        val isSelected = key == currentVersionKey
                        val description = BibleVersionUtils.descriptionMap[key] ?: ""
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) (colors["primary"] as Color).copy(alpha = 0.1f) else colors["card"] as Color)
                                .clickable { onVersionSelected(key) }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = shortName,
                                color = if (isSelected) colors["primary"] as Color else colors["text"] as Color,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = description,
                                color = colors["muted"] as Color,
                                fontSize = 12.sp
                            )
                        }
                        HorizontalDivider(color = colors["border"] as Color)
                    }
                }
            }
        }
    }
}