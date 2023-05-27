package vfunny.shortvideovfunnyapp.models

import vfunny.shortvideovfunnyapp.Post.model.Language
import java.text.SimpleDateFormat
import java.util.*


data class UploadData(
    val type: String? = "video",
    val language: Language? = null,
    val timestamp: Any? = null,
) {
    fun getDateString(): String {
        val simpleDateFormat = SimpleDateFormat("d/M/yy h:mm a", Locale.ENGLISH)
        return if (timestamp != null) {
            when (timestamp) {
                is Long -> {
                    // handle Long data
                    simpleDateFormat.format(timestamp)
                }
                else -> {
                    // handle other types of data
                    return  timestamp.toString()
                }
            }
        } else {
            ("Unknown DATE")
        }
    }

}
