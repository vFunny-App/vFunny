package vfunny.shortvideovfunnyapp.Login.Loginutils;

import static vfunny.shortvideovfunnyapp.Post.model.Const.kTopicsFeed;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vfunny.shortvideovfunnyapp.Login.model.User;
import vfunny.shortvideovfunnyapp.R;

/**
 * Created by shresthasaurabh86@gmail.com 04/05/2019.
 */

public class AuthManager implements FirebaseAuth.AuthStateListener {

    public static final int REQUEST_AUTH_CODE = 123;
    private static AuthManager _shared = null;
    private final ArrayList<AuthListener> mListeners = new ArrayList();

    public static AuthManager getInstance() {
        if (_shared == null) {
            _shared = new AuthManager();
        }
        return _shared;
    }


    public void showLogin(Activity activity) {
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.addAuthStateListener(this);
        if (auth.getCurrentUser() != null) {
            // subscribe for new notifications
            FirebaseMessaging.getInstance().subscribeToTopic(kTopicsFeed + User.currentKey());
            String locale = activity.getApplicationContext().getResources().getConfiguration().locale.getLanguage();
            User.current().child("lang").setValue(locale); // update lang configuration
        } else {
            List providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build(),
                    new AuthUI.IdpConfig.PhoneBuilder().setDefaultCountryIso("IN").build()
            );

            String eula = activity.getString(R.string.eula);
            String privacy = activity.getString(R.string.privacy_policy);

            // show sign-in dialog
            Intent intent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setTosAndPrivacyPolicyUrls(eula, privacy)
                    .setLogo(R.mipmap.ic_launcher)
                    .setIsSmartLockEnabled(false, true) // disabled to let user logout
                    .build();

            activity.startActivityForResult(intent, REQUEST_AUTH_CODE);
        }
    }

    public void completeAuth(Context context) {
        FirebaseUser fbuser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(fbuser.getUid());
        // subscribe for new notifications
        FirebaseMessaging.getInstance().subscribeToTopic(kTopicsFeed + User.currentKey());
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Bundle bundle = new Bundle();
                FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
                User user;
                // store user info if user not exist yet
                if (!dataSnapshot.exists() || dataSnapshot.child("name").getValue() == null) {
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, fbuser.getUid());
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, fbuser.getDisplayName());
                    user = new User();
                    user.setName(fbuser.getDisplayName());
                    if (fbuser.getPhotoUrl() != null) {
                        user.setPhoto(fbuser.getPhotoUrl().getPath());
                    }
                    ref.setValue(user);
                } else {
                    user = dataSnapshot.getValue(User.class);
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, user.getId());
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, user.getName());
                }

                for (AuthListener listener : mListeners) {
                    listener.onAuthSuccess(user);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                for (AuthListener listener : mListeners) {
                    listener.onAuthFailed();
                }
            }
        });
    }

    public void logout() {
        // subscribe for new notifications
        FirebaseMessaging.getInstance().unsubscribeFromTopic(kTopicsFeed + User.currentKey());
        FirebaseAuth.getInstance().signOut();

        for (AuthListener listener : mListeners) {
            listener.onAuthFailed();
        }
    }

    public void addListener(final AuthListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(final AuthListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
    }

    public interface AuthListener {
        void onAuthSuccess(User user);

        void onAuthFailed();
    }
}
