package vfunny.shortvideovfunnyapp.Login.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class User {

    private String id;
    private String name;
    private String seen;
    private String photo;
    private String bio;

    public User() {
    }

    public static DatabaseReference Ads() {
        return FirebaseDatabase.getInstance().getReference(Const.kAdsKey);
    }

    public static DatabaseReference collection() {
        return FirebaseDatabase.getInstance().getReference(Const.kUsersKey);
    }

    public static DatabaseReference collection(String userId) {
        return FirebaseDatabase.getInstance().getReference(Const.kUsersKey).child(userId);
    }

    public static DatabaseReference following(String userId) {
        return collection(userId).child(Const.kFollowinsKey);
    }

    public static DatabaseReference followers(String userId) {
        return collection(userId).child(Const.kFollowersKey);
    }

    public static DatabaseReference uploads(String userId) {
        return FirebaseDatabase.getInstance().getReference(Const.kDataUploadsKey).child(userId);
    }

    public static DatabaseReference chats() {
        return FirebaseDatabase.getInstance().getReference(Const.kChatsKey).child(currentKey());
    }

    public static DatabaseReference messages(String chat) {
        return FirebaseDatabase.getInstance().getReference(Const.kMessagesKey).child(chat);
    }

    public static String currentKey() {
        return FirebaseAuth.getInstance().getUid();
    }

    @Nullable
    public static DatabaseReference current() {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            return collection(userId);
        }

        return null;
    }

    @Nullable
    public static DatabaseReference hasSeen() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            return seencollection(userId);
        }
        return null;
    }

    private static DatabaseReference seencollection(String userId) {
        return FirebaseDatabase.getInstance().getReference(Const.kUsersKey).child(userId).child("seen");
    }

    public static void updatePhoto(String image) {
        DatabaseReference reference = User.current();
        if (reference != null) {
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        if (image != null) {
                            user.setPhoto(image);
                            reference.child("photo").setValue(image);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getSeen() {
        return seen;
    }
    public void setSeen(String seen) {
        this.seen = seen;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
