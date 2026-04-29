package com.fountofhopedotorg.fohbible.modals
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fountofhopedotorg.fohbible.utils.BibleVersionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionSelectionModal(
    currentVersionKey: String,
    onVersionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    colors: Map<String, Color> = emptyMap()
) {
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val primaryColor = colors["primary"] ?: MaterialTheme.colorScheme.primary
    val surfaceColor = colors["card"] ?: MaterialTheme.colorScheme.surface
    val textColor = colors["text"] ?: MaterialTheme.colorScheme.onSurface
    val mutedColor = colors["muted"] ?: MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = colors["border"] ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val groupedVersions = remember(searchQuery) {
        derivedStateOf {
            val all = BibleVersionUtils.getVersionsGroupedByLanguage()
            if (searchQuery.isBlank()) {
                all
            } else {
                all.mapValues { (_, list) ->
                    list.filter { (key, shortName) ->
                        val description = BibleVersionUtils.descriptionMap[key] ?: ""
                        shortName.contains(searchQuery, ignoreCase = true) ||
                                description.contains(searchQuery, ignoreCase = true) ||
                                key.contains(searchQuery, ignoreCase = true)
                    }
                }.filterValues { it.isNotEmpty() }
            }
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(200)) +
                    scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150)) +
                    scaleOut(targetScale = 0.9f, animationSpec = tween(150))
        ) {

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            Card(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.4f else 0.8f)
                    .fillMaxHeight(if (isLandscape) 0.97f else 0.75f)
                    .shadow(24.dp, RoundedCornerShape(10.dp), clip = false),
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(primaryColor)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 25.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Select Bible Version",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onDismiss()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        placeholder = {
                            Text(
                                "Search version",
                                color = mutedColor,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = mutedColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            focusedIndicatorColor = primaryColor,
                            unfocusedIndicatorColor = borderColor,
                            focusedLeadingIconColor = primaryColor,
                            unfocusedLeadingIconColor = mutedColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedPlaceholderColor = mutedColor,
                            unfocusedPlaceholderColor = mutedColor
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        groupedVersions.value.forEach { (language, versions) ->
                            stickyHeader {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceColor)
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = language,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor,
                                        letterSpacing = 0.3.sp
                                    )
                                }
                            }
                            items(versions) { (key, shortName) ->
                                val isSelected = key == currentVersionKey
                                val description = BibleVersionUtils.descriptionMap[key] ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) primaryColor.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            keyboardController?.hide()
                                            onVersionSelected(key)
                                        }
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = shortName,
                                            fontSize = 16.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isSelected) primaryColor else textColor
                                        )
                                        Text(
                                            text = description,
                                            fontSize = 13.sp,
                                            color = mutedColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = primaryColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                                if (versions.lastOrNull() != (key to shortName)) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 20.dp),
                                        color = borderColor,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                        if (groupedVersions.value.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✨ No matching versions found\nTry a different search term",
                                        color = mutedColor,
                                        fontSize = 14.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}