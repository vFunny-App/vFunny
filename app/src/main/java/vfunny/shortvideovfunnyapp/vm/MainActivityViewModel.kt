package vfunny.shortvideovfunnyapp.vm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapLatest
import vfunny.shortvideovfunnyapp.models.*
import vfunny.shortvideovfunnyapp.models.ViewEvent
import vfunny.shortvideovfunnyapp.models.ViewResult
import vfunny.shortvideovfunnyapp.models.ViewState
import vfunny.shortvideovfunnyapp.models.ViewEffect

internal class MainActivityViewModel(
    initialState: ViewState,
) : MviViewModel<ViewEvent, ViewResult, ViewState, ViewEffect>(initialState) {

    override fun onStart() {

    }

    override fun Flow<ViewEvent>.toResults(): Flow<ViewResult> {
        // MVI boilerplate
        return merge(
            filterIsInstance<TappedAddPostsEvent>().toTappedAddPostsResults(),
            filterIsInstance<ToggleAdsEvent>().toToggleAdsResults(),
            filterIsInstance<TappedUpdatesNotifyEvent>().toTappedUpdatesNotifyResults(),
            filterIsInstance<TappedLanguageEvent>().toTappedLanguageResults(),
            filterIsInstance<TappedLanguageListEvent>().toTappedLanguageListResults(),
        )
    }

    //TODO
    private fun Flow<TappedAddPostsEvent>.toTappedAddPostsResults(): Flow<ViewResult> {
        return mapLatest {
            TappedAddPostsResult(listOf(UploadData()))
        }
    }

    //TODO
    private fun Flow<ToggleAdsEvent>.toToggleAdsResults(): Flow<ViewResult> {
        return mapLatest {
            ToggleAdsResult(false)
        }
    }

    //TODO
    private fun Flow<TappedUpdatesNotifyEvent>.toTappedUpdatesNotifyResults(): Flow<ViewResult> {
        return mapLatest {
            TappedUpdatesNotifyResult("TODO")
        }
    }

    //TODO
    private fun Flow<TappedLanguageEvent>.toTappedLanguageResults(): Flow<ViewResult> {
        return mapLatest {
            TappedLanguageResult(0)
        }
    }

    //TODO
    private fun Flow<TappedLanguageListEvent>.toTappedLanguageListResults(): Flow<ViewResult> {
        return mapLatest {
            TappedLanguageListResult
        }
    }

    override fun ViewResult.reduce(state: ViewState): ViewState {
        // MVI reducer boilerplate
        return when (this) {
            is TappedAddPostsResult -> state.copy(uploadData = uploadData)
            is ToggleAdsResult -> state.copy(adsEnabled = isAdsEnabled)
            else -> state
        }
    }

    override fun Flow<ViewResult>.toEffects(): Flow<ViewEffect> {
        return merge(
            filterIsInstance<TappedLanguageResult>().toTappedLanguageEffects(),
            filterIsInstance<TappedLanguageListResult>().toTappedLanguageListEffects(),
            filterIsInstance<TappedUpdatesNotifyResult>().toTappedUpdatesNotifyEffects(),
            filterIsInstance<PlayerErrorResult>().toPlayerErrorEffects()
        )
    }

    private fun Flow<TappedLanguageResult>.toTappedLanguageEffects(): Flow<ViewEffect> {
        return mapLatest { result -> TappedLanguageEffect(result.page) }
    }

    private fun Flow<TappedLanguageListResult>.toTappedLanguageListEffects(): Flow<ViewEffect> {
        return mapLatest { result -> TappedLanguageListEffect }
    }

    private fun Flow<TappedUpdatesNotifyResult>.toTappedUpdatesNotifyEffects(): Flow<ViewEffect> {
        return mapLatest { result -> TappedUpdatesNotifyEffect(result.mediaUri) }
    }

    private fun Flow<PlayerErrorResult>.toPlayerErrorEffects(): Flow<ViewEffect> {
        return mapLatest { result -> PlayerErrorEffect(result.throwable) }
    }
}
