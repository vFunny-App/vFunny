package vfunny.shortvideovfunnyapp.models

import vfunny.shortvideovfunnyapp.Post.model.Language

internal sealed class ViewEvent

internal sealed class HandleIntentsEvent : ViewEvent() {
    object Start : HandleIntentsEvent()
    data class Stop(val isChangingConfigurations: Boolean) : HandleIntentsEvent()
}
internal sealed class LanguageViewEvent : ViewEvent(){
    object SelectLanguage : LanguageViewEvent()
    internal data class ConfirmSelection(val languageMap :  MutableMap<Language, Boolean>) : LanguageViewEvent()
    object CancelSelection : LanguageViewEvent()
}
internal object TappedAddPostsEvent : ViewEvent()

internal object ToggleAdsEvent : ViewEvent()

internal object TappedUpdatesNotifyEvent : ViewEvent()

internal data class LoadLanguageEvent(val languagesMap: MutableMap<Language, Boolean>) : ViewEvent()

internal object TappedLanguageListEvent : ViewEvent()

internal data class OnPageSettledEvent(val page: Int) : ViewEvent()