package vfunny.shortvideovfunnyapp.Live.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import vfunny.shortvideovfunnyapp.Login.model.User
import vfunny.shortvideovfunnyapp.Post.model.Language

class LanguageRepository {
    private val userKey = User.currentKey()
    private val languageRef: DatabaseReference? =
        userKey?.let { User.getLanguage(it) }

    fun getLanguages(): Flow<MutableMap<Language, Boolean>> {
        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val languageMap = Language.getAllLanguagesMappedFalse()
                    for (childSnapshot in snapshot.children) {
                        val languageString: String =
                            childSnapshot.value as String // Get the language string from the key
                        val language =
                            Language.getLanguageFromName(languageString) // Create a Language object
                        if (language != null) {
                            languageMap[language] =
                                true // Map the Language object to the boolean value
                        }
                    }
                    trySend(languageMap)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            languageRef?.addValueEventListener(listener)

            awaitClose {
                languageRef?.removeEventListener(listener)
            }
        }
    }

    fun setLanguages(languageMap: MutableMap<Language, Boolean>): Flow<MutableMap<Language, Boolean>> {
        return channelFlow {
            if (languageMap.isNotEmpty()) {
                val languageNames: List<String> = languageMap.filterValues { it }.keys.map { it.name }
                languageRef?.setValue(languageNames)
                    ?.addOnSuccessListener {
                        trySend(languageMap).isSuccess
                    }?.addOnFailureListener { error ->
                        close(error)
                    }?.addOnCanceledListener {
                        close()
                    }
            }
            awaitClose()
        }
    }
}