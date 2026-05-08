package com.fountofhopedotorg.fohbible.composables

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fountofhopedotorg.fohbible.models.AppViewModel

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun ImageSection() {
    val viewModel = viewModel<AppViewModel>()
    val context = LocalContext.current

    var imageSrc by remember { mutableStateOf<String?>(null) }
    var currentImageFile by remember { mutableStateOf<String?>(null) }
    var randomText by remember { mutableStateOf("") }
    var imageError by remember { mutableStateOf(false) }
    var imageLoaded by remember { mutableStateOf(false) }
    var isMobile by remember { mutableStateOf(false) }
    var isConverting by remember { mutableStateOf(false) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration.screenWidthDp) {
        isMobile = configuration.screenWidthDp < 768
    }

    LaunchedEffect(isMobile) {
        val imageFiles = if (isMobile) imageFilesSm else imageFilesMd
        val assetPath = if (isMobile) "images/" else "images-md/"
        val randomImageFile = imageFiles.random()
        imageSrc = "file:///android_asset/$assetPath$randomImageFile"
        currentImageFile = randomImageFile
        randomText = inspirationalTexts.random()
        imageError = false
        imageLoaded = false
    }

    // ----- Error fallback (unchanged) -----
    if (imageError) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (viewModel.darkTheme) {
                                listOf(Color(0xFF2D2D2D), Color(0xFF1E1E1E))
                            } else {
                                listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0))
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Daily Inspiration", fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                        color = if (viewModel.darkTheme) Color.White else Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "“${randomText.substringBefore(" - ")}”",
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        color = if (viewModel.darkTheme) Color.White.copy(alpha = 0.85f)
                        else Color.Black.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "- ${randomText.substringAfter(" - ")}",
                        color = if (viewModel.darkTheme) Color.White.copy(alpha = 0.65f)
                        else Color.Black.copy(alpha = 0.55f)
                    )
                }
            }
        }
        return
    }

    // ----- Main image section -----
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        // Share / Download buttons
        Row(
            modifier = Modifier
                .padding(12.dp)
                .zIndex(10f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { /* share placeholder */ },
                enabled = !isConverting,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }
            IconButton(
                onClick = { /* download placeholder */ },
                enabled = !isConverting,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
            }
        }

        // Card holding the image and overlay
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Loading placeholder
                if (!imageLoaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(256.dp)
                            .background(
                                if (viewModel.darkTheme) Color(0xFF2D2D2D)
                                else Color(0xFFE0E0E0)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading image...", color = Color.Gray)
                    }
                }

                // Actual image
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageSrc)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .onGloballyPositioned { coordinates ->
                            imageSize = coordinates.size
                        },
                    contentScale = ContentScale.Crop,
                    onSuccess = { imageLoaded = true },
                    onError = { imageError = true }
                )

                // Effects overlay – exact same size as the image
                if (imageLoaded && currentImageFile != null && imageSize.height > 0) {
                    val effect = when {
                        currentImageFile!!.contains("w") -> EffectType.SNOW
                        currentImageFile!!.contains("o") -> EffectType.ORBS
                        else -> EffectType.METEORS
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(LocalDensity.current) { imageSize.height.toDp() })
                    ) {
                        when (effect) {
                            EffectType.SNOW -> FallingSnow(number = 50)
                            EffectType.ORBS -> FloatingOrbs(number = 3)
                            EffectType.METEORS -> SimpleMeteors(number = 5)
                        }
                    }
                }

                // Text overlay
                if (randomText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.8f),
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent
                                    ),
                                    startY = Float.POSITIVE_INFINITY,
                                    endY = 0f
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("Daily Inspiration", fontSize = 20.sp,
                                color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "“${randomText.substringBefore(" - ")}”",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "- ${randomText.substringAfter(" - ")}",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
private val imageFilesSm = listOf(
    "w1.jpg", "w2.jpg", "w3.jpg", "w4.jpg", "w5.jpg", "w6.jpg", "w7.jpg", "w8.jpg",
    "n1.jpg", "n2.jpg", "n3.jpg", "n4.jpg", "n5.jpg", "n6.jpg", "n7.jpg", "n8.jpg",
    "n9.jpg", "n10.jpg", "n11.jpg", "n12.jpg", "n13.jpg", "n14.jpg", "n15.jpg", "n16.jpg",
    "n17.jpg", "n18.jpg", "n19.jpg", "n20.jpg", "n21.jpg", "n22.jpg", "n23.jpg", "n24.jpg",
    "n25.jpg", "n26.jpg", "n27.jpg", "n28.jpg", "n29.jpg", "n30.jpg", "n31.jpg", "n32.jpg",
    "n33.jpg", "n34.jpg", "o1.jpg", "o2.jpg", "o3.jpg", "o4.jpg", "o5.jpg", "o6.jpg",
    "o7.jpg", "o8.jpg", "o9.jpg", "o10.jpg"
)

private val imageFilesMd = listOf(
    "wm1.jpg", "wm2.jpg", "wm3.jpg", "wm4.jpg", "wm5.jpg", "wm6.jpg", "wm7.jpg",
    "nm1.jpg", "nm2.jpg", "nm3.jpg", "nm4.jpg", "nm5.jpg", "nm6.jpg", "nm7.jpg", "nm8.jpg",
    "nm9.jpg", "nm10.jpg", "nm11.jpg", "nm12.jpg", "nm13.jpg", "nm14.jpg",
    "om1.jpg", "om2.jpg", "om3.jpg", "om4.jpg", "om5.jpg"
)

private val inspirationalTexts = listOf(
    "Be still and know that I am God. - Psalm 46:10",
    "I can do all things through Christ who strengthens me. - Philippians 4:13",
    "The Lord is my shepherd; I shall not want. - Psalm 23:1",
    "For I know the plans I have for you, declares the Lord. - Jeremiah 29:11",
    "Trust in the Lord with all your heart. - Proverbs 3:5",
    "The joy of the Lord is your strength. - Nehemiah 8:10",
    "Cast all your anxiety on him because he cares for you. - 1 Peter 5:7",
    "Be strong and courageous. Do not be afraid. - Joshua 1:9",
    "In all things God works for the good of those who love him. - Romans 8:28",
    "The peace of God, which transcends all understanding, will guard your hearts and your minds. - Philippians 4:7",
    "Let your light shine before others. - Matthew 5:16",
    "With God all things are possible. - Matthew 19:26",
    "God is our refuge and strength, an ever-present help in trouble. - Psalm 46:1",
    "The Lord is my light and my salvation—whom shall I fear? - Psalm 27:1",
    "Do not be anxious about anything, but in every situation, by prayer and petition, with thanksgiving, present your requests to God. - Philippians 4:6",
    "But those who hope in the Lord will renew their strength. They will soar on wings like eagles. - Isaiah 40:31",
    "The Lord is close to the brokenhearted and saves those who are crushed in spirit. - Psalm 34:18",
    "For God so loved the world that he gave his one and only Son, that whoever believes in him shall not perish but have eternal life. - John 3:16",
    "Therefore, if anyone is in Christ, the new creation has come: The old has gone, the new is here! - 2 Corinthians 5:17",
    "And we know that in all things God works for the good of those who love him, who have been called according to his purpose. - Romans 8:28",
    "But the fruit of the Spirit is love, joy, peace, forbearance, kindness, goodness, faithfulness, gentleness and self-control. - Galatians 5:22-23",
    "Your word is a lamp for my feet, a light on my path. - Psalm 119:105",
    "The Lord will fight for you; you need only to be still. - Exodus 14:14",
    "Come to me, all you who are weary and burdened, and I will give you rest. - Matthew 11:28",
    "But seek first his kingdom and his righteousness, and all these things will be given to you as well. - Matthew 6:33",
    "I have told you these things, so that in me you may have peace. In this world you will have trouble. But take heart! I have overcome the world. - John 16:33",
    "And now these three remain: faith, hope and love. But the greatest of these is love. - 1 Corinthians 13:13",
    "For where two or three gather in my name, there am I with them. - Matthew 18:20",
    "The Lord is my strength and my shield; my heart trusts in him, and he helps me. - Psalm 28:7",
    "But you are a chosen people, a royal priesthood, a holy nation, God's special possession, that you may declare the praises of him who called you out of darkness into his wonderful light. - 1 Peter 2:9",
    "Rejoice always, pray continually, give thanks in all circumstances; for this is God's will for you in Christ Jesus. - 1 Thessalonians 5:16-18",
    "So do not fear, for I am with you; do not be dismayed, for I am your God. I will strengthen you and help you; I will uphold you with my righteous right hand. - Isaiah 41:10",
    "Jesus looked at them and said, 'With man this is impossible, but with God all things are possible.' - Matthew 19:26",
    "The name of the Lord is a fortified tower; the righteous run to it and are safe. - Proverbs 18:10",
    "He gives strength to the weary and increases the power of the weak. - Isaiah 40:29",
    "But the Lord is faithful, and he will strengthen you and protect you from the evil one. - 2 Thessalonians 3:3",
    "Taste and see that the Lord is good; blessed is the one who takes refuge in him. - Psalm 34:8",
    "Therefore, since we have been justified through faith, we have peace with God through our Lord Jesus Christ. - Romans 5:1",
    "And my God will meet all your needs according to the riches of his glory in Christ Jesus. - Philippians 4:19",
    "For the Spirit God gave us does not make us timid, but gives us power, love and self-discipline. - 2 Timothy 1:7"
)

private enum class EffectType { SNOW, METEORS, ORBS }