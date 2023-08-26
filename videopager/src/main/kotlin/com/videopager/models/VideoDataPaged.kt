package com.videopager.models

import com.google.firebase.database.DataSnapshot
import vfunny.shortvideovfunnyapp.Post.model.Language


/**
 * Created on 02/05/2019.
 * Copyright by shresthasaurabh86@gmail.com
 */
data class VideoDataPaged(
    var lastIndex: Int,
    var language: Language
)