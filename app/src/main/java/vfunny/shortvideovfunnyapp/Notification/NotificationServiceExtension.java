package vfunny.shortvideovfunnyapp.Notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import com.onesignal.OSNotification;
import com.onesignal.OSMutableNotification;
import com.onesignal.OSNotificationReceivedEvent;
import com.onesignal.OneSignal.OSRemoteNotificationReceivedHandler;

import java.math.BigInteger;

import vfunny.shortvideovfunnyapp.R;

@SuppressWarnings("unused")
public class NotificationServiceExtension implements OSRemoteNotificationReceivedHandler {

    @Override
    public void remoteNotificationReceived(Context context, OSNotificationReceivedEvent notificationReceivedEvent) {
        OSNotification notification = notificationReceivedEvent.getNotification();

        JSONObject data = notification.getAdditionalData();
        Log.i("OneSignalExample", "Received Notification Data: " + data);
        // Check if the notification is an app update notification
        if (data != null && data.has("app_update_notification")) {
            // Handle the app update notification here
            // You can use the notification's data to customize the message or action
            // Modify the notification's title and body to display the app update message
            OSMutableNotification mutableNotification = notification.mutableCopy();
            mutableNotification.setExtender(builder -> {
                builder.setContentTitle("New update available!");
                builder.setContentText("A new version of the app is available. Tap here to update.");
                // Add a button to take the user to the app store to download the update
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=vfunny.shortvideovfunnyapp"));
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                builder.addAction(R.drawable.play, "Update", pendingIntent);
                // Remove the notification from the notification center after it is clicked
                builder.setAutoCancel(true);
                return builder;
            });

            // Call complete() to finish processing the notification
            notificationReceivedEvent.complete(mutableNotification);
        } else {
            // Handle other types of notifications here

            // Example of modifying the notification's accent color
            OSMutableNotification mutableNotification = notification.mutableCopy();
            mutableNotification.setExtender(builder -> { // Modify the notification's appearance and behavior here
                // Sets the accent color to Green on Android 5+ devices.
                // Accent color controls icon and action buttons on Android 5+. Accent color does not change app title on Android 10+
                builder.setColor(new BigInteger("FF00FF00", 16).intValue());
                // Sets the notification Title to Red
                Spannable spannableTitle = new SpannableString(notification.getTitle());
                spannableTitle.setSpan(new ForegroundColorSpan(Color.RED), 0, notification.getTitle().length(), 0);
                builder.setContentTitle(spannableTitle);
                // Sets the notification Body to Blue
                Spannable spannableBody = new SpannableString(notification.getBody());
                spannableBody.setSpan(new ForegroundColorSpan(Color.BLUE), 0, notification.getBody().length(), 0);
                builder.setContentText(spannableBody);
                //Force remove push from Notification Center after 30 seconds
                builder.setTimeoutAfter(30000);
                return builder;
            });

            // If complete isn't call within a time period of 25 seconds, OneSignal internal logic will show the original notification
            // To omit displaying a notification, pass `null` to complete()
            notificationReceivedEvent.complete(mutableNotification);
        }

    }
}