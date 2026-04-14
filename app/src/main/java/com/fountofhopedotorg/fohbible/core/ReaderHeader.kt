package com.fountofhopedotorg.fohbible.core

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fountofhopedotorg.fohbible.AnimatedIconButton
import com.fountofhopedotorg.fohbible.Screen
import com.fountofhopedotorg.fohbible.composables.ScrollSyncButton
import com.fountofhopedotorg.fohbible.data.PassageSelection
import com.fountofhopedotorg.fohbible.dropdowns.ReaderAppBarMenu
import com.fountofhopedotorg.fohbible.dropdowns.WindowsLayoutDropdown
import com.fountofhopedotorg.fohbible.models.AppViewModel

@Composable
fun ChapterHeader(
    passage: PassageSelection,
    versionAbbr: String,
    scrollSyncEnabled: Boolean,
    onBookChapterClick: () -> Unit,
    onVersionClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onColorLensClick: () -> Unit,
    onScrollSyncToggle: () -> Unit,
    viewModel: AppViewModel,
    onScreenChange: (Screen) -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
) {
    val isLandscapeVerticalMulti = isLandscape && viewModel.multiVersion && viewModel.multiViewLayout == "vertical"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onBookChapterClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(0.2f)),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier
                .height(20.dp)
                .weight(0.7f)
                .padding(end = 4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = passage.bookName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(0.5f)
                )
                Text(
                    text = passage.chapter.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
        Button(
            onClick = onVersionClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(0.2f)),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier
                .height(20.dp)
                .weight(0.5f)
        ) {
            Text(
                text = versionAbbr,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.weight(if (isLandscapeVerticalMulti) 2f else 0.25f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedIconButton(
                onClick = onThemeToggle,
                icon = if (viewModel.darkTheme) Icons.Filled.Brightness6 else Icons.Filled.Brightness2,
                contentDescription = "Theme",
                rotation = 180f,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            AnimatedIconButton(
                onClick = onColorLensClick,
                icon = Icons.Filled.ColorLens,
                contentDescription = "Color",
                rotation = 180f,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            if (viewModel.multiVersion) {
                ScrollSyncButton(
                    scrollSyncEnabled = scrollSyncEnabled,
                    onToggle = onScrollSyncToggle,
                    modifier = Modifier.size(24.dp)
                )
            }
            WindowsLayoutDropdown(
                viewModel = viewModel,
                modifier = Modifier.size(20.dp)
            )
            ReaderAppBarMenu(
                isLandscape = isLandscape,
                viewModel = viewModel,
                onScreenChange = onScreenChange,
                coroutineScope = rememberCoroutineScope(),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}