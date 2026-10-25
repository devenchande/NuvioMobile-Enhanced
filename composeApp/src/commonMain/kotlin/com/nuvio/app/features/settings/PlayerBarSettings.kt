package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.DialogButton
import com.nuvio.app.core.ui.DialogButtonStyle
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogOption
import com.nuvio.app.core.ui.DialogSurface
import com.nuvio.app.features.player.PlayerBarAction
import com.nuvio.app.features.player.PlayerBarCollapsedActionCount
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun playerBarActionLabel(action: PlayerBarAction): String = stringResource(
    when (action) {
        PlayerBarAction.Subtitles -> Res.string.compose_player_subtitles
        PlayerBarAction.Audio -> Res.string.compose_player_audio
        PlayerBarAction.Sources -> Res.string.compose_player_sources
        PlayerBarAction.Speed -> Res.string.compose_player_speed
        PlayerBarAction.NextEpisode -> Res.string.player_next_episode
        PlayerBarAction.AspectRatio -> Res.string.player_bar_action_aspect_ratio
        PlayerBarAction.Chapters -> Res.string.player_chapters
        PlayerBarAction.Quality -> Res.string.player_bar_action_quality
        PlayerBarAction.Episodes -> Res.string.compose_player_episodes
        PlayerBarAction.Rate -> Res.string.user_rating_action_rate
        PlayerBarAction.ExternalPlayer -> Res.string.streams_open_external_player
        PlayerBarAction.PlaybackInfo -> Res.string.compose_player_playback_info
        PlayerBarAction.SwitchEngine -> Res.string.cd_switch_player_engine
        PlayerBarAction.VideoSettings -> Res.string.player_action_video_settings
        PlayerBarAction.SubmitIntro -> Res.string.submit_intro_action
    },
)

/**
 * Reorders the bottom-bar actions with up/down buttons. Changes are kept in the dialog until Save,
 * so Cancel leaves the saved order untouched.
 */
@Composable
internal fun ArrangePlayerBarDialog(
    currentOrder: List<PlayerBarAction>,
    onSave: (List<PlayerBarAction>) -> Unit,
    onDismiss: () -> Unit,
) {
    var order by remember(currentOrder) { mutableStateOf(currentOrder) }

    fun move(from: Int, to: Int) {
        if (to !in order.indices) return
        order = order.toMutableList().apply { add(to, removeAt(from)) }
    }

    DialogSurface(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.settings_playback_bar_arrange),
        message = stringResource(Res.string.settings_playback_bar_arrange_hint, PlayerBarCollapsedActionCount),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            order.forEachIndexed { index, action ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "${index + 1}. ${playerBarActionLabel(action)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            // The first actions are the ones visible while the bar is collapsed.
                            fontWeight = if (index < PlayerBarCollapsedActionCount) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                    )
                    IconButton(onClick = { move(index, index - 1) }, enabled = index > 0) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(Res.string.cd_move_up))
                    }
                    IconButton(onClick = { move(index, index + 1) }, enabled = index < order.lastIndex) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(Res.string.cd_move_down))
                    }
                }
            }
        }
        DialogButtons {
            DialogButton(
                text = stringResource(Res.string.action_reset),
                onClick = { order = PlayerBarAction.defaultOrder },
            )
            DialogButton(
                text = stringResource(Res.string.action_cancel),
                onClick = onDismiss,
            )
            DialogButton(
                text = stringResource(Res.string.action_save),
                style = DialogButtonStyle.Primary,
                onClick = { onSave(order) },
            )
        }
    }
}

/** Chooses between the generated text chips and the user's imported Fusion badge images. */
@Composable
internal fun PlayerBadgeStyleDialog(
    useFusionBadges: Boolean,
    onSelected: (useFusion: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    DialogSurface(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.settings_playback_badge_style),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DialogOption(
                text = stringResource(Res.string.settings_playback_badge_style_chips),
                description = stringResource(Res.string.settings_playback_badge_style_chips_description),
                selected = !useFusionBadges,
                onClick = { onSelected(false) },
            )
            DialogOption(
                text = stringResource(Res.string.settings_playback_badge_style_fusion),
                description = stringResource(Res.string.settings_playback_badge_style_fusion_description),
                selected = useFusionBadges,
                onClick = { onSelected(true) },
            )
        }
        DialogButtons {
            DialogButton(
                text = stringResource(Res.string.action_cancel),
                onClick = onDismiss,
            )
        }
    }
}
