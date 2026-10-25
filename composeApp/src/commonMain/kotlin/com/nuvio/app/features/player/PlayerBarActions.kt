package com.nuvio.app.features.player

/**
 * Actions that can appear in the modern player's bottom bar. [key] is persisted, so keep it stable
 * when renaming entries.
 */
enum class PlayerBarAction(val key: String) {
    Subtitles("subtitles"),
    Audio("audio"),
    Sources("sources"),
    Speed("speed"),
    NextEpisode("next_episode"),
    AspectRatio("aspect_ratio"),
    Chapters("chapters"),
    Quality("quality"),
    Episodes("episodes"),
    Rate("rate"),
    ExternalPlayer("external_player"),
    PlaybackInfo("playback_info"),
    SwitchEngine("switch_engine"),
    VideoSettings("video_settings"),
    SubmitIntro("submit_intro"),
    ;

    companion object {
        val defaultOrder: List<PlayerBarAction> = entries.toList()

        /** Reads a saved order; unknown keys are dropped and actions added later go to the end. */
        fun decodeOrder(raw: String?): List<PlayerBarAction> {
            val saved = raw.orEmpty()
                .split(',')
                .mapNotNull { key -> entries.firstOrNull { it.key == key.trim() } }
                .distinct()
            return saved + defaultOrder.filterNot { it in saved }
        }

        fun encodeOrder(order: List<PlayerBarAction>): String = order.joinToString(",") { it.key }
    }
}

/** Actions shown before the overflow arrow while the bar is collapsed. */
internal const val PlayerBarCollapsedActionCount = 3
