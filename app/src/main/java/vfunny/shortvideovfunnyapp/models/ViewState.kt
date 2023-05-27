package vfunny.shortvideovfunnyapp.models

import com.player.models.VideoData
import com.player.players.AppPlayer

internal data class ViewState(
    val adsEnabled: Boolean = false,
    val uploadData: List<UploadData>? = null,
)
