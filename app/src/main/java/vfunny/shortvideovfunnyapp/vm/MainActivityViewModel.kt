package vfunny.shortvideovfunnyapp.vm

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator
import vfunny.shortvideovfunnyapp.Live.data.LanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import vfunny.shortvideovfunnyapp.Login.model.User
import vfunny.shortvideovfunnyapp.Post.model.Language
import vfunny.shortvideovfunnyapp.models.*
import vfunny.shortvideovfunnyapp.models.ViewEvent
import vfunny.shortvideovfunnyapp.models.ViewResult
import vfunny.shortvideovfunnyapp.models.ViewState
import vfunny.shortvideovfunnyapp.models.ViewEffect

internal class MainActivityViewModel(
    initialState: ViewState,
    private val languageRepository: LanguageRepository
) : MviViewModel<ViewEvent, ViewResult, ViewState, ViewEffect>(initialState) {

    override fun onStart() {
        viewModelScope.launch {
            languageRepository.getLanguages()
                .collect { languages ->
                    processEvent(LoadLanguageEvent(languages))
                }
        }
    }

    override fun Flow<ViewEvent>.toResults(): Flow<ViewResult> {
        // MVI boilerplate
        return merge(
            filterIsInstance<LoadLanguageEvent>().toLoadLanguageResults(),
            filterIsInstance<LanguageViewEvent.SelectLanguage>().toLoadLanguages(),
            filterIsInstance<LanguageViewEvent.ConfirmSelection>().toConfirmSelectionResults(),
            filterIsInstance<LanguageViewEvent.CancelSelection>().toCancelSelectionResults(),

            filterIsInstance<TappedAddPostsEvent>().toTappedAddPostsResults(),
            filterIsInstance<ToggleAdsEvent>().toToggleAdsResults(),
            filterIsInstance<TappedUpdatesNotifyEvent>().toTappedUpdatesNotifyResults(),
            filterIsInstance<TappedLanguageListEvent>().toTappedLanguageListResults(),
        )
    }

    private fun Flow<LanguageViewEvent.SelectLanguage>.toLoadLanguages(): Flow<ViewResult> {
        return mapLatest {
            User.currentKey()?.let { userId ->
                User.getLanguage(userId).get().addOnSuccessListener { dataSnapshot: DataSnapshot ->
                    val allLanguage = Language.getAllLanguages().filter { it != Language.WORLDWIDE }
                    // Get the selected languages from dataSnapshot
                    val selectedLanguages =
                        (dataSnapshot.getValue(object : GenericTypeIndicator<List<Language>>() {})
                            ?.filter { it != Language.WORLDWIDE } ?: emptyList()).toMutableList()
                    val languageMap = mutableMapOf<Language, Boolean>()
                    for (language in allLanguage) {
                        languageMap[language] = selectedLanguages.any { it.name == language.name }
                    }
                }
            }
            val languagesMap = requireNotNull(states.value.languagesMap)
            LanguageViewResult.SelectLanguage(languagesMap)
        }
    }

    private fun Flow<LanguageViewEvent.ConfirmSelection>.toConfirmSelectionResults(): Flow<ViewResult> {
        return flatMapLatest {
            it.languageMap.forEach {
                Log.e("TAG", "confirm :  ${it.key}: ${it.value} \n",)
            }
            Log.e("TAG", "toConfirmSelectionResults: ConfirmSelection " , )
            flow {
                viewModelScope.launch {
                    languageRepository.setLanguages(it.languageMap)
                        .collect { languages ->
                            processEvent(LoadLanguageEvent(languages))
                        }
                }
            }
        }
    }

    private fun Flow<LanguageViewEvent.CancelSelection>.toCancelSelectionResults(): Flow<ViewResult> {
        return mapLatest {
            LanguageViewResult.CancelSelection
        }
    }

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

    private fun Flow<LoadLanguageEvent>.toLoadLanguageResults(): Flow<ViewResult> {
        return mapLatest { result ->
            LoadLanguageResult(result.languagesMap)
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
            is LoadLanguageResult -> state.copy(languagesMap = languagesMap)
            is TappedAddPostsResult -> state.copy(uploadData = uploadData)
            is ToggleAdsResult -> state.copy(adsEnabled = isAdsEnabled)
            else -> state
        }
    }

    override fun Flow<ViewResult>.toEffects(): Flow<ViewEffect> {
        return merge(
            filterIsInstance<LanguageViewResult.SelectLanguage>().toLanguageLoadEffects(),
            filterIsInstance<TappedLanguageListResult>().toTappedLanguageListEffects(),
            filterIsInstance<TappedUpdatesNotifyResult>().toTappedUpdatesNotifyEffects(),
            filterIsInstance<PlayerErrorResult>().toPlayerErrorEffects()
        )
    }

    private fun Flow<LanguageViewResult.SelectLanguage>.toLanguageLoadEffects(): Flow<ViewEffect> {
        return mapLatest { result ->
            LanguageViewEffect.SelectLanguage(
                result.languagesMap,
            )
        }
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
