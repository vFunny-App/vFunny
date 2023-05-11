package vfunny.shortvideovfunnyapp.Post.model

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import vfunny.shortvideovfunnyapp.Login.model.User

enum class Language(code: String) {
    WORLDWIDE("ww"), ENGLISH("en"), HINDI("hi"), OTHERS("ot");

    companion object {
        private const val TAG = "Language"
        fun getAllLanguages(): List<Language> {
            return enumValues<Language>().toList()
        }

        fun addWorldWideLangToDb(
            context: Context,
            user: User,
        ): List<Language> {
            if (user.id != null) {
                val userLangRef =
                    FirebaseDatabase.getInstance().getReference("users").child(user.id!!).child(Const.kLanguageKey)
                Log.e(TAG, "addWorldWideLangToDb")

                val updatedLangList = user.language.toMutableList()
                // Add the string "ww" to the mutable list
                updatedLangList.add(WORLDWIDE)
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
            return user.language + WORLDWIDE
        }
    }
}
