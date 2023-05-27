package vfunny.shortvideovfunnyapp.models

internal sealed class ViewEvent

internal sealed class HandleIntentsEvent : ViewEvent() {
    object Start : HandleIntentsEvent()
    data class Stop(val isChangingConfigurations: Boolean) : HandleIntentsEvent()
}

internal object TappedAddPostsEvent : ViewEvent()

internal object ToggleAdsEvent : ViewEvent()

internal object TappedUpdatesNotifyEvent : ViewEvent()

internal object TappedLanguageEvent : ViewEvent()

internal object TappedLanguageListEvent : ViewEvent()

internal data class OnPageSettledEvent(val page: Int) : ViewEvent()