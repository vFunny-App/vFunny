package vfunny.shortvideovfunnyapp.LangUtils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import vfunny.shortvideovfunnyapp.Login.model.User
import vfunny.shortvideovfunnyapp.Post.model.Const
import vfunny.shortvideovfunnyapp.Post.model.Language

/**
 * Created by shresthasaurabh86@gmail.com 09/05/2023.
 */
class LangManager {

    // The list of languages we have
    companion object {
        private const val TAG: String = "LangManager"

        @JvmStatic
        val instance: LangManager by lazy { LangManager() }
    }

    fun addWorldWideLangToDb(
        context: Context,
        user: User,
    ): List<Language> {
        if (user.id != null) {
            val userLangRef =
                FirebaseDatabase.getInstance().getReference("users").child(user.id!!)
                    .child(Const.kLanguageKey)
            Log.e(TAG, "addWorldWideLangToDb")

            val updatedLangList = user.language.toMutableList()
            // Add the string "ww" to the mutable list
            updatedLangList.add(Language.WORLDWIDE)
            // Save the updated language array back to the database
            userLangRef.setValue(updatedLangList.toList()).addOnSuccessListener {
                Log.e(TAG, "addWorldWideLangToDb success")
            }.addOnFailureListener {
                Log.e(TAG, "addWorldWideLangToDb Failure: $it")
                Toast.makeText(context,
                    "Please contact us if this message pops regularly.(code:WW_LANG)",
                    Toast.LENGTH_LONG).show()
            }.addOnCanceledListener {
                Log.e(TAG, "addWorldWideLangToDb cancelled")
            }
        }
        return user.language + Language.WORLDWIDE
    }

}