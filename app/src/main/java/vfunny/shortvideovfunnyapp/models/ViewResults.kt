package vfunny.shortvideovfunnyapp.models

import androidx.annotation.DrawableRes
import com.player.models.VideoData
import com.player.players.AppPlayer

internal sealed class ViewResult

internal object NoOpResult : ViewResult()


internal data class TappedAddPostsResult(val uploadData: List<UploadData>) : ViewResult()

internal data class ToggleAdsResult(val isAdsEnabled: Boolean) : ViewResult()

internal data class TappedUpdatesNotifyResult(val mediaUri: String) : ViewResult()

internal data class TappedLanguageResult(val page: Int) : ViewResult()

internal object TappedLanguageListResult : ViewResult()

internal data class PlayerErrorResult(val throwable: Throwable) : ViewResult()

