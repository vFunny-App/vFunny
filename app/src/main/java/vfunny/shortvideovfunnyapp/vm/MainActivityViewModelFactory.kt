package vfunny.shortvideovfunnyapp.vm

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import vfunny.shortvideovfunnyapp.Live.data.LanguageRepository
import vfunny.shortvideovfunnyapp.models.ViewState

class MainActivityViewModelFactory(
    private val languageRepository: LanguageRepository
) {
    fun create(owner: SavedStateRegistryOwner): ViewModelProvider.Factory {
        return object : AbstractSavedStateViewModelFactory(owner, null) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T {
                @Suppress("UNCHECKED_CAST")
                return MainActivityViewModel(
                    initialState = ViewState(
                        uploadData = null,
                        isLoggedIn = false,
                        adsEnabled = false,
                        languagesMap = mutableMapOf()
                    ),
                    languageRepository
                ) as T
            }
        }
    }
}
