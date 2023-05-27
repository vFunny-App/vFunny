package vfunny.shortvideovfunnyapp.vm

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import com.player.players.AppPlayer
import com.videopager.data.VideoDataRepository

class MainActivityViewModelFactory(
    private val repository: VideoDataRepository,
    private val appPlayerFactory: AppPlayer.Factory,
) {
    fun create(owner: SavedStateRegistryOwner): ViewModelProvider.Factory {
        return object : AbstractSavedStateViewModelFactory(owner, null) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T {
                @Suppress("UNCHECKED_CAST")
                return MainActivityViewModelFactory(
                    repository = repository,
                    appPlayerFactory = appPlayerFactory,
                ) as T
            }
        }
    }
}
