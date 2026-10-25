package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSNotificationCenter
import platform.MediaPlayer.MPVolumeView
import platform.UIKit.UIApplication
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPhone
import platform.UIKit.UIScreen
import platform.UIKit.UISlider

private const val lockPlayerToLandscapeNotification = "NuvioPlayerLockLandscape"
private const val unlockPlayerOrientationNotification = "NuvioPlayerUnlockOrientation"
private const val preferPlayerPortraitNotification = "NuvioPlayerPreferPortrait"
private const val preferPlayerLandscapeNotification = "NuvioPlayerPreferLandscape"
private var playerLandscapeLockCount = 0

@Composable
actual fun LockPlayerToLandscape() {
    DisposableEffect(Unit) {
        playerLandscapeLockCount += 1
        if (playerLandscapeLockCount == 1) {
            NSNotificationCenter.defaultCenter.postNotificationName(
                lockPlayerToLandscapeNotification,
                null,
            )
        }

        onDispose {
            playerLandscapeLockCount -= 1
            if (playerLandscapeLockCount == 0) {
                PlayerOrientationPreference.reset()
                NSNotificationCenter.defaultCenter.postNotificationName(
                    unlockPlayerOrientationNotification,
                    null,
                )
            }
        }
    }
    val portrait by PlayerOrientationPreference.portrait.collectAsState()
    LaunchedEffect(portrait) {
        NSNotificationCenter.defaultCenter.postNotificationName(
            if (portrait) preferPlayerPortraitNotification else preferPlayerLandscapeNotification,
            null,
        )
    }
}

@Composable
actual fun rememberPlayerOrientationToggleSupported(): Boolean =
    remember { UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPhone }

@Composable
actual fun FullscreenPlayerDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            usePlatformInsets = false,
            useSoftwareKeyboardInset = false,
        ),
        content = content,
    )
}

@Composable
actual fun HidePlayerSystemBars() = Unit

@Composable
actual fun EnterImmersivePlayerMode(keepScreenAwake: Boolean) {
    SideEffect {
        UIApplication.sharedApplication.setIdleTimerDisabled(keepScreenAwake)
    }

    DisposableEffect(Unit) {
        onDispose {
            UIApplication.sharedApplication.setIdleTimerDisabled(false)
        }
    }
}

@Composable
actual fun ManagePlayerPictureInPicture(
    isPlaying: Boolean,
    videoSize: IntSize,
) = Unit

@Composable
actual fun rememberIsInPictureInPicture(): Boolean = false

@Composable
internal actual fun rememberPlatformPlayerGestureController(): PlayerGestureController? {
    val controller = remember { IOSPlayerGestureController() }

    DisposableEffect(controller) {
        controller.attachVolumeView()
        onDispose {
            controller.detachVolumeView()
            controller.restoreBrightness()
        }
    }

    return controller
}

@OptIn(ExperimentalForeignApi::class)
private class IOSPlayerGestureController : PlayerGestureController {
    // iOS hides its own volume HUD only while an MPVolumeView is in the window and not hidden,
    // so the view is attached off-screen and nearly transparent instead of `hidden = true`.
    private val volumeView = MPVolumeView(frame = CGRectMake(-2000.0, -2000.0, 1.0, 1.0)).apply {
        alpha = 0.01
        userInteractionEnabled = false
    }
    private val originalBrightness = UIScreen.mainScreen.brightness
    private var brightnessRestored = false

    override fun currentBrightness(): Float =
        UIScreen.mainScreen.brightness.toFloat().coerceIn(0f, 1f)

    override fun setBrightness(level: Float): Float {
        val target = level.coerceIn(0f, 1f)
        UIScreen.mainScreen.brightness = target.toDouble()
        return target
    }

    override fun currentVolume(): PlayerAudioLevel {
        val current = (volumeView.subviews.filterIsInstance<UISlider>().firstOrNull()?.value ?: 0f)
            .coerceIn(0f, 1f)
        return PlayerAudioLevel(
            fraction = current,
            isMuted = current <= 0.001f,
        )
    }

    override fun setVolume(level: Float): PlayerAudioLevel {
        val target = level.coerceIn(0f, 1f)
        val slider = volumeView.subviews.filterIsInstance<UISlider>().firstOrNull()
            ?: return currentVolume()
        slider.value = target
        slider.sendActionsForControlEvents(UIControlEventValueChanged)
        return PlayerAudioLevel(
            fraction = target,
            isMuted = target <= 0.001f,
        )
    }

    fun attachVolumeView() {
        if (volumeView.superview != null) return
        UIApplication.sharedApplication.keyWindow?.addSubview(volumeView)
    }

    fun detachVolumeView() {
        volumeView.removeFromSuperview()
    }

    fun restoreBrightness() {
        if (brightnessRestored) return
        brightnessRestored = true
        UIScreen.mainScreen.brightness = originalBrightness
    }
}
