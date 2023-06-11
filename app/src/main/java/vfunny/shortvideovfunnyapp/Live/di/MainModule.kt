package vfunny.shortvideovfunnyapp.Live.di

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import coil.imageLoader
import com.exo.players.ExoAppPlayerFactory
import com.exo.ui.ExoAppPlayerViewFactory
import com.player.models.VideoData
import com.videopager.ui.VideoPagerFragment
import com.videopager.vm.VideoPagerViewModelFactory
import com.videopager.watermark.AddWatermarkVideoBuilder
import vfunny.shortvideovfunnyapp.Live.data.OneShotAssetVideoDataRepository
import java.io.FileInputStream
import java.io.InputStream

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
                            ),
                            addWatermarkVideoBuilder = AddWatermarkVideoBuilder(activity)
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
