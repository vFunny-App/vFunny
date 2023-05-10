package vfunny.shortvideovfunnyapp.Login.data

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

enum class Language(code: String) {
    WORLDWIDE("ww"), ENGLISH("en"), HINDI("hi"), OTHERS("ot");

    companion object {
        private const val TAG = "Language"
        fun getAllLanguages(): List<Language> {
            return enumValues<Language>().toList()
        }

        fun addWorldWideLangToDb(
            context: Context,
            currentLangList: List<Language>,
        ): List<Language> {
            FirebaseAuth.getInstance().currentUser?.let {
                val userLangRef = FirebaseDatabase.getInstance().getReference("users").child(it.uid)
                    .child(Const.kLanguageKey)
                // Update and insert a new language to the language array of the user
                userLangRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val currentLangArray = dataSnapshot.getValue(Array<String>::class.java)
                        if (currentLangArray != null) {
                            // Convert the language array to a mutable list so that we can modify it
                            val updatedLangList = currentLangArray.toMutableList()
                            // Add the string "ww" to the mutable list
                            updatedLangList.add(WORLDWIDE.name)
                            // Save the updated language array back to the database
                            userLangRef.setValue(updatedLangList.toTypedArray())
                        } else {
                            userLangRef.setValue(getAllLanguages())
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e(TAG, "onCancelled: $databaseError")
                        Toast.makeText(context,
                            "Please contact us if this message pops regularly.(code:WW_LANG)",
                            Toast.LENGTH_LONG).show()
                    }
                })
            }
            return currentLangList + WORLDWIDE
        }
    }
}
