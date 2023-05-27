package vfunny.shortvideovfunnyapp.models

import androidx.annotation.DrawableRes

internal sealed class ViewEffect

internal sealed class PageEffect : ViewEffect()

internal data class AnimationEffect(@DrawableRes val drawable: Int) : PageEffect()

internal object ResetAnimationsEffect : PageEffect()

internal data class  TappedLanguageEffect(val page: Int)  : ViewEffect()

internal object TappedLanguageListEffect : ViewEffect()

internal data class  TappedUpdatesNotifyEffect(val mediaUri: String)  : ViewEffect()

internal data class PlayerErrorEffect(val throwable: Throwable) : ViewEffect()


