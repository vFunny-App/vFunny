package com.videopager.models

import androidx.annotation.DrawableRes
import java.io.File

internal sealed class ViewEffect

internal sealed class PageEffect : ViewEffect()

internal data class AnimationEffect(@DrawableRes val drawable: Int) : PageEffect()

internal object ResetAnimationsEffect : PageEffect()

internal data class PlayerErrorEffect(val throwable: Throwable) : ViewEffect()

internal data class  ShareWhatsappEffect(val mediaUri: String)  : ViewEffect()
internal data class  SaveVideoDataEffect(val outputFilePath: File)  : ViewEffect()
internal data class  TappedShareEffect(val mediaUri: String)  : ViewEffect()