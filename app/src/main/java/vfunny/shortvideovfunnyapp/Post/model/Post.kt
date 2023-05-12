package vfunny.shortvideovfunnyapp.Post.model

/**
 * Created on 02/05/2019.
 * Copyright by shresthasaurabh86@gmail.com
 */
class Post {
    var image: String? = null
    var video: String? = null
    var key: String? = null
    var migrate: Boolean? = null
    var timestamp: Any? = null

    override fun equals(other: Any?): Boolean {
        if (other is Post) {
            return image == other.image &&
                    video == other.video &&
                    timestamp == other.timestamp
        }
        return false
    }

    override fun toString(): String {
        return "Post(image=$image, video=$video, key=$key, migrate=$migrate, timestamp=$timestamp)"
    }
}