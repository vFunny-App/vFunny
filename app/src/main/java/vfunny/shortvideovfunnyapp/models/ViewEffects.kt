package vfunny.shortvideovfunnyapp.models

import androidx.annotation.DrawableRes
import vfunny.shortvideovfunnyapp.Post.model.Language

internal sealed class ViewEffect

internal sealed class PageEffect : ViewEffect()

internal data class AnimationEffect(@DrawableRes val drawable: Int) : PageEffect()

internal object ResetAnimationsEffect : PageEffect()

internal sealed class LanguageViewEffect : ViewEffect(){
    data class SelectLanguage(val languagesMap: MutableMap<Language, Boolean>) : LanguageViewEffect()
    object ConfirmSelection : LanguageViewEffect()
    object CancelSelection : LanguageViewEffect()
}
internal object TappedLanguageListEffect : ViewEffect()

internal data class  TappedUpdatesNotifyEffect(val mediaUri: String)  : ViewEffect()

internal data class PlayerErrorEffect(val throwable: Throwable) : ViewEffect()


