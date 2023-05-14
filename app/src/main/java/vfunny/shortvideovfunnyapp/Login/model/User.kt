package vfunny.shortvideovfunnyapp.Login.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import vfunny.shortvideovfunnyapp.Post.model.Const
import vfunny.shortvideovfunnyapp.Post.model.Language

/**
 * Created on 02/05/2019.
 * Copyright by shresthasaurabh86@gmail.com
 */
class User {
    var id: String? = null
    var name: String? = null
    var photo: String? = null
    var language: List<Language> = Language.getAllLanguages()


    companion object {

        private val TAG: String = "User"

        fun adsDatabaseReference(): DatabaseReference {
            return FirebaseDatabase.getInstance().getReference(Const.kAdsKey)
        }

        fun collection(): DatabaseReference {
            return FirebaseDatabase.getInstance().getReference(Const.kUsersKey)
        }

        fun collection(userId: String?): DatabaseReference {
            return FirebaseDatabase.getInstance().getReference(Const.kUsersKey).child(
                userId!!
            )
        }

        fun following(userId: String?): DatabaseReference {
            return collection(userId).child(Const.kFollowinsKey)
        }

        fun followers(userId: String?): DatabaseReference {
            return collection(userId).child(Const.kFollowersKey)
        }

        fun uploads(userId: String?): DatabaseReference {
            return FirebaseDatabase.getInstance().getReference(Const.kDataUploadsKey).child(
                userId!!
            )
        }

        fun chats(): DatabaseReference {
            return FirebaseDatabase.getInstance().getReference(Const.kChatsKey).child(
                currentKey()!!
            )
        }

        fun messages(chat: String?): DatabaseReference {
            return FirebaseDatabase.getInstance().getReference(Const.kMessagesKey).child(
                chat!!
            )
        }

        @JvmStatic
        fun currentKey(): String? {
            return FirebaseAuth.getInstance().uid
        }

        @JvmStatic
        fun current(): DatabaseReference? {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                val userId = auth.currentUser!!.uid
                return collection(userId)
            }
            return null
        }

        fun hasSeen(): DatabaseReference? {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                val userId = auth.currentUser!!.uid
                return seencollection(userId)
            }
            return null
        }

        private fun seencollection(userId: String): DatabaseReference {
            return FirebaseDatabase.getInstance().getReference(Const.kUsersKey).child(userId)
                .child("seen")
        }

        fun getLanguage(userId: String): DatabaseReference {
            return FirebaseDatabase.getInstance().getReference(Const.kUsersKey).child(userId)
                .child("language")
        }

        fun updatePhoto(image: String?) {
            val reference = current()
            reference?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val user = dataSnapshot.getValue(
                        User::class.java
                    )
                    if (user != null) {
                        if (image != null) {
                            user.photo = image
                            reference.child("photo").setValue(image)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
    }
}