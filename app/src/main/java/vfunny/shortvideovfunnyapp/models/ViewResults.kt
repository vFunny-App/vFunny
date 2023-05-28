package vfunny.shortvideovfunnyapp.models

import vfunny.shortvideovfunnyapp.Post.model.Language

internal sealed class ViewResult

internal object NoOpResult : ViewResult()


internal sealed class LanguageViewResult : ViewResult(){
    data class SelectLanguage(val languagesMap: MutableMap<Language, Boolean>) : LanguageViewResult()
    data class ConfirmSelection(val languagesMap: MutableMap<Language, Boolean>) : LanguageViewResult()
    object CancelSelection : LanguageViewResult()
}

internal data class TappedAddPostsResult(val uploadData: List<UploadData>) : ViewResult()

internal data class ToggleAdsResult(val isAdsEnabled: Boolean) : ViewResult()

internal data class TappedUpdatesNotifyResult(val mediaUri: String) : ViewResult()

internal data class LoadLanguageResult(val languagesMap: MutableMap<Language, Boolean>) : ViewResult()

internal object TappedLanguageListResult : ViewResult()

internal data class PlayerErrorResult(val throwable: Throwable) : ViewResult()

