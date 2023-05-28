package vfunny.shortvideovfunnyapp.models

import vfunny.shortvideovfunnyapp.Post.model.Language

internal data class ViewState(
    val uploadData: List<UploadData>? = null,
    val isLoggedIn: Boolean = false,
    val adsEnabled: Boolean = false,
    val languagesMap: MutableMap<Language, Boolean> = mutableMapOf()
)
