package dev.sam.countri.ui.share

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.hairline
import dev.sam.countri.ui.theme.pressScale
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Pick a card style, see it, send it. Previews render off the main thread
 * at full export size and are reused for the actual share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportShareSheet(
    stamps: List<CountryWithState>,
    onDismiss: () -> Unit,
) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    val context = LocalContext.current
    var selected by remember { mutableStateOf(if (palette.isDark) ShareStyle.Dark else ShareStyle.Light) }
    var previews by remember { mutableStateOf<Map<ShareStyle, ImageBitmap>>(emptyMap()) }

    LaunchedEffect(stamps) {
        withContext(Dispatchers.Default) {
            val rendered = ShareStyle.entries.associateWith { style ->
                PassportCardRenderer.render(context, stamps, style).asImageBitmap()
            }
            previews = rendered
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface2,
        contentWindowInsets = { WindowInsets(0) },
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp)
                .navigationBarsPadding()
        ) {
            Text("Share your passport", style = CountriType.subtitle, color = palette.textPrimary)
            Text(
                "Pick a style. It exports as an image.",
                style = CountriType.bodySmall,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShareStyle.entries.forEach { style ->
                    val preview = previews[style]
                    Column(
                        Modifier
                            .weight(1f)
                            .pressScale(0.96f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                haptics.tick()
                                selected = style
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1080f / 1350f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.surface1)
                                .hairline(
                                    RoundedCornerShape(12.dp),
                                    if (selected == style) palette.visited
                                    else palette.hairline,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (preview != null) {
                                Image(
                                    bitmap = preview,
                                    contentDescription = style.displayName,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                CircularProgressIndicator(
                                    color = palette.visited,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.height(22.dp).aspectRatio(1f),
                                )
                            }
                        }
                        Text(
                            style.displayName,
                            style = CountriType.monoSmall,
                            color = if (selected == style) palette.visited else palette.textFaint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .pressScale(0.97f)
                    .clip(CircleShape)
                    .background(palette.visited)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        haptics.confirm()
                        shareCard(context, stamps, selected)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Share", style = CountriType.subtitle, color = palette.onVisited)
            }
        }
    }
}

private fun shareCard(
    context: android.content.Context,
    stamps: List<CountryWithState>,
    style: ShareStyle,
) {
    val bitmap = PassportCardRenderer.render(context, stamps, style)
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "countri-passport.png")
    file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "dev.sam.countri.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share your passport"))
}
