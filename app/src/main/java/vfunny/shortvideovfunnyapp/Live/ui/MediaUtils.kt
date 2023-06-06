package vfunny.shortvideovfunnyapp.Live.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.videopager.PostManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import vfunny.shortvideovfunnyapp.Post.PostUtils.PostsManager;
import vfunny.shortvideovfunnyapp.Post.model.Language;

/**
 * Created on 26/05/2017.
 * Copyright by Shresthasaurabh86@gmail.com
 */

public class MediaUtils {

    public static final int REQUEST_VIDEO_PICK = 1001;
    private static final int PERMISSIONS_REQUEST_STORAGE = 1002;

    public static boolean storagePermissionGrant(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // request the permission
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_STORAGE);
            return false;
        } else {
            return true;
        }
    }

    public static boolean openVideoLibrary(Activity activity, int requestCode) {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivityForResult(Intent.createChooser(intent, "Select video(s)"), requestCode);
            return true;
        } catch (Error e) {
            Log.e("TAG", "addClick: getPackageManager null");
            Log.e("TAG", "addClick: ERROR " + e);
            return false;
        }
    }

    public static void uploadPhoto(Uri filePath, Language language, Context context) {
        if (filePath != null) {
            encodeHLS(context, filePath, language, 0);
        }
    }

    public static void uploadMultiplePhoto(List<Uri> uriList, Language language, Context context) {
        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.e("TAG", "uploadPhoto: userId : " + userId);
        int itemCount = uriList.size();
        for (int i = 0; i < itemCount; i++) {
            Uri filePath = uriList.get(i);
            encodeHLS(context, filePath, language, i);
        }
    }

    private static File createTempOutputDirectory() {
        try {
            File tempDir = File.createTempFile("temp_", Long.toString(System.nanoTime()));
            if (tempDir.delete() && tempDir.mkdirs()) {
                return tempDir;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void encodeHLS(Context context, Uri videoUri, Language language, int videoIndex) {
        // Upload the HLS output files to Firebase Storage
        String videoPath = getFilePathFromContentUri(videoUri, context.getContentResolver());
        if (videoPath == null) {
            return;
        }
        PostManager.getInstance().execute(context, videoPath, language, videoIndex);
    }

    public static String getFilePathFromContentUri(Uri contentUri, ContentResolver contentResolver) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = contentResolver.query(contentUri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        }
        return null;
    }

    private static void showRetryDialog(Context context, List<Uri> failedUris, Language language) {
        Log.e("TAG", "Failed to upload file: showRetryDialog starting");
        new MaterialAlertDialogBuilder(context).setTitle("Retry Failed Uploads?").setMessage("Some files failed to upload. Do you want to retry uploading them?").setPositiveButton("OK", (dialog, which) -> {

            uploadMultiplePhoto(failedUris, language, context);
            dialog.dismiss();
        }).setNegativeButton("Cancel", (dialog, which) -> {
            showFileNamesDialog(context, failedUris);
            dialog.dismiss();
        }).show();
    }

    public static void showFileNamesDialog(Context context, List<Uri> uriList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Uri uri : uriList) {
            stringBuilder.append(getAbsolutePath(uri, context)).append("\n");
        }
        new MaterialAlertDialogBuilder(context).setTitle("Failed File Names").setMessage(stringBuilder.toString()).setPositiveButton("COPY TO CLIPBOARD", (dialog, which) -> {
            // Copy all the file names to clipboard
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("File Names", stringBuilder.toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "File names copied to clipboard", Toast.LENGTH_SHORT).show();
        }).setNegativeButton("Exit", (dialog, which) -> dialog.dismiss()).show();
    }

    private static UploadTask createThumbnailUploadTask(Uri filePath, Context context) {
        if (filePath == null) {
            Log.e("TAG", "createThumbnailUploadTask: null filepath");
        }
        if (context == null) {
            Log.e("TAG", "createThumbnailUploadTask: null context");
        }
        String thumbnailPath = getAbsolutePath(filePath, context);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(thumbnailPath);
        Bitmap imageBitmap = retriever.getFrameAtTime(0);
        if (imageBitmap != null) {
            final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference imgref = storageRef.child(userId + "/ac" + System.currentTimeMillis() + ".jpg");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] bytes = baos.toByteArray();
            StorageMetadata metadata = new StorageMetadata.Builder().setContentType("image/jpg").build();
            UploadTask uploadTask = imgref.putBytes(bytes, metadata);
            return uploadTask;
        }
        return null;
    }

    private static String getAbsolutePath(Uri uri, Context context) {
        String[] filePathColumn = {MediaStore.Video.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, filePathColumn, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        return cursor.getString(columnIndex);
    }
}

/**
 * Created on 26/05/2017.
 * IF WE NEED TO CHANGE THE WHOLE DATABASE
 */

/*
 val query2 = FirebaseDatabase.getInstance().reference.child("posts")
        query2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get the posts and reverse the order
                val posts = ArrayList<Post2>()

                // Loop through all the fetched posts
                for (postSnapshot in snapshot.children) {
                    // Convert the post data to a Post object
                    val post: Post2? = postSnapshot.getValue(Post2::class.java)
                    post?.key = postSnapshot.key
                    if (post != null) {
                        posts.add(post)
                    }
                }
                Log.e(TAG, "onDataChange: posts.size ${posts.size}")
                posts.reverse()
                // Set the starting timestamp value
                var timestamp = 1682848556490L

                // Iterate through the posts and add the timestamp field
                for (post in posts) {
                    val p = Post()
                    p.comments = post.comments
                    p.image = post.image
                    p.likes = post.likes
                    p.user = post.user
                    p.video = post.video
                    p.timestamp = timestamp
                    timestamp--
                    // Update the post in the database with the timestamp field
                    Log.e(TAG, "onDataChange: ${post.key}", )
                    val postRef = post.key?.let { query2.child(it) }
                    postRef?.setValue(p)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to retrieve posts: $error")
            }
        })
 */