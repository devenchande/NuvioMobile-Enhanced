package com.nuvio.app.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.nuvioTypeScale
import org.jetbrains.compose.resources.stringResource

/**
 * Text chips for the paused-state metadata row: video quality, dynamic range plus Atmos, and the
 * audio language/layout, e.g. `4K` · `Dolby Vision · Atmos` · `English 5.1`.
 */
@Composable
internal fun PlayerScreenRuntime.rememberPlayerQualityChips(): List<String> {
    val controller = playerController
    // Media info is only read while the controls are up, so a hidden player does no extra work.
    LaunchedEffect(controller, activeSourceUrl, selectedAudioIndex, controlsVisible, playbackSnapshot.isLoading) {
        if (controller != null && controlsVisible && !playbackSnapshot.isLoading) {
            streamMediaInfo = controller.getMediaInfo()
        }
    }
    val track = audioTracks.firstOrNull { it.index == selectedAudioIndex }
    val languageName = track?.language?.let(::languageLabelResForCode)?.let { stringResource(it) }
    return playerQualityChipLabels(
        info = streamMediaInfo,
        fallbackWidth = playbackSnapshot.videoWidth,
        fallbackHeight = playbackSnapshot.videoHeight,
        audioLanguage = languageName,
    )
}

internal fun playerQualityChipLabels(
    info: PlayerMediaInfo,
    fallbackWidth: Int = 0,
    fallbackHeight: Int = 0,
    audioLanguage: String? = null,
): List<String> = listOfNotNull(
    playerResolutionLabel(
        width = info.videoWidth ?: fallbackWidth.takeIf { it > 0 },
        height = info.videoHeight ?: fallbackHeight.takeIf { it > 0 },
    ),
    listOfNotNull(playerDynamicRangeLabel(info.hdr), "Atmos".takeIf { playerHasAtmos(info) })
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · "),
    playerAudioLabel(audioLanguage, info),
)

internal fun playerResolutionLabel(width: Int?, height: Int?): String? {
    if (width == null || height == null) return null
    return when {
        width >= 3800 || height >= 2100 -> "4K"
        width >= 2500 || height >= 1400 -> "1440p"
        width >= 1900 || height >= 1000 -> "1080p"
        width >= 1260 || height >= 700 -> "720p"
        else -> "SD"
    }
}

internal fun playerDynamicRangeLabel(hdr: List<String>): String? = when {
    "DV" in hdr -> "Dolby Vision"
    "HDR10" in hdr -> "HDR10"
    "HLG" in hdr -> "HLG"
    else -> null
}

internal fun playerHasAtmos(info: PlayerMediaInfo): Boolean =
    info.audioCodec.orEmpty().uppercase().endsWith("JOC") ||
        info.audioProfile.orEmpty().uppercase().contains("ATMOS")

internal fun playerAudioLabel(language: String?, info: PlayerMediaInfo): String? {
    val layout = when (info.audioChannels) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> null
    }
    // Without a language, the codec keeps the chip meaningful (e.g. "E-AC-3 5.1").
    val name = language?.takeIf { it.isNotBlank() } ?: info.audioCodec?.takeIf { it.isNotBlank() }
    return listOfNotNull(name, layout).takeIf { it.isNotEmpty() }?.joinToString(" ")
}

/** Single-line chip row; chips that don't fit are dropped instead of wrapping. */
@Composable
internal fun PlayerQualityChipRow(
    labels: List<String>,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = { labels.forEach { PlayerQualityChip(text = it, fontSize = fontSize) } },
        modifier = modifier,
    ) { measurables, constraints ->
        val gap = 6.dp.roundToPx()
        val placeables = measurables.map { it.measure(Constraints(maxWidth = constraints.maxWidth)) }
        var width = 0
        var visibleCount = 0
        for (placeable in placeables) {
            val next = if (visibleCount == 0) placeable.width else width + gap + placeable.width
            if (next > constraints.maxWidth) break
            width = next
            visibleCount++
        }
        val visible = placeables.take(visibleCount)
        val height = visible.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            var x = 0
            visible.forEach { placeable ->
                placeable.placeRelative(x, (height - placeable.height) / 2)
                x += placeable.width + gap
            }
        }
    }
}

private val PlayerQualityChipFill = Color.Black.copy(alpha = 0.28f)

private val PlayerQualityChipBorder = Color.White.copy(alpha = 0.38f)

@Composable
private fun PlayerQualityChip(text: String, fontSize: TextUnit) {
    Box(
        modifier = Modifier
            .clip(PlayerGlassCapsule)
            .background(PlayerQualityChipFill)
            .border(1.dp, PlayerQualityChipBorder, PlayerGlassCapsule)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.nuvioTypeScale.labelSm.copy(
                fontSize = fontSize,
                lineHeight = fontSize * 1.25f,
                fontWeight = FontWeight.SemiBold,
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}
