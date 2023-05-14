package vfunny.shortvideovfunnyapp.LangUtils

import android.util.Log
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

    /**
     *Adds the WORLDWIDE language to the list of languages of a user in the database.
     *@param user the user object whose language list will be updated
     *@return a list of languages that includes the updated language list with WORLDWIDE added
     */
    fun addWorldWideLangToDb(user: User): List<Language> {
        if (user.id != null) {
            val userLangRef = FirebaseDatabase.getInstance().getReference("users").child(user.id!!)
                .child(Const.kLanguageKey)
            Log.e(TAG, "addWorldWideLangToDb")
            val updatedLangList = user.language.toMutableList()
            // Add the string "ww" to the mutable list
            updatedLangList.add(Language.WORLDWIDE)
            // Save the updated language array back to the database
            userLangRef.setValue(updatedLangList.toList()).addOnSuccessListener {
                Log.e(TAG, "addWorldWideLangToDb success")
            }.addOnFailureListener { exception ->
                Log.e(TAG, "addWorldWideLangToDb Failure: $exception")
            }.addOnCanceledListener {
                Log.e(TAG, "addWorldWideLangToDb cancelled")
            }
        }
        return user.language + Language.WORLDWIDE
    }

    /**
     *Adds all available languages to the user's language list in the database.
     *@param user the user object whose language list will be updated
     *@return a list of all available languages
     */
    private fun addAllLanguagesToUser(user: User?): List<Language> {
        if (user?.id != null) {
            val userLangRef = FirebaseDatabase.getInstance().getReference("users").child(user.id!!)
                .child(Const.kLanguageKey)
            // Save the updated language array back to the database
            userLangRef.setValue(Language.getAllLanguages().toList()).addOnSuccessListener {
                Log.e(TAG, "addAllLanguagesToUser success")
            }.addOnFailureListener { exception ->
                Log.e(TAG, "addAllLanguagesToUser Failure: $exception")
            }.addOnCanceledListener {
                Log.e(TAG, "addAllLanguagesToUser cancelled")
            }
        }
        return Language.getAllLanguages()
    }

    /**
     *Returns the list of languages of a user. If the user's language list is null or contains null values,
     *this method adds all available languages to the user's language list and returns them.
     *@param user the user object whose language list will be returned
     *@return a list of languages of the user
     */
    fun getUserLanguages(user: User?): List<Language> {
        return user?.language?.filterNotNull() ?: this.addAllLanguagesToUser(user)
    }
}