package vfunny.shortvideovfunnyapp.Live.di

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import coil.imageLoader
import com.exo.players.ExoAppPlayerFactory
import com.exo.ui.ExoAppPlayerViewFactory
import com.player.models.VideoData
import com.videopager.ui.VideoPagerFragment
import com.videopager.vm.VideoPagerViewModelFactory
import vfunny.shortvideovfunnyapp.Live.data.OneShotAssetVideoDataRepository

class MainModule(activity: ComponentActivity,  videoItemList: ArrayList<VideoData>) {
    val fragmentFactory: FragmentFactory = object : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            return when (loadFragmentClass(classLoader, className)) {
                VideoPagerFragment::class.java -> VideoPagerFragment(
                    viewModelFactory = { owner ->
                        VideoPagerViewModelFactory(
                            repository = OneShotAssetVideoDataRepository(videoItemList),
                            appPlayerFactory = ExoAppPlayerFactory(
                                context = activity.applicationContext
                            )
                        ).create(owner)
                    },
                    appPlayerViewFactory = ExoAppPlayerViewFactory(),
                    imageLoader = activity.imageLoader
                )
                else -> super.instantiate(classLoader, className)
            }
        }
    }
}
