package vfunny.shortvideovfunnyapp.Live.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import vfunny.shortvideovfunnyapp.Post.model.Story;

/**
 * Created on 26/05/2017.
 * Copyright by Shresthasaurabh86@gmail.com
 */

public class MediaUtils {

    private static final int REQUEST_CODE = 1001;
    public static final int REQUEST_VIDEO_PICK = REQUEST_CODE + 6;

    private static final int PERMISSIONS_REQUEST_STORAGE = 10;

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


    public static void uploadPhoto(StorageReference storageReference, Uri filePath, Context context, int requestCode) {
        if (filePath != null) {
            final ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();
            final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Log.e("TAG", "uploadPhoto: userId : " + userId);
            Log.e("TAG", "uploadPhoto: filePath : " + filePath);
            StorageReference ref = storageReference.child(userId + "/av" + System.currentTimeMillis());
            ref.putFile(filePath).addOnSuccessListener(taskSnapshot -> {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(uri -> {
                    if (requestCode == REQUEST_VIDEO_PICK) {
                        createThumbnailUploadTask(filePath, context).addOnSuccessListener(thumbnailSnapshot -> thumbnailSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(thumbnailUri -> Story.uploadVideoStory(uri.getResult().toString(), thumbnailUri.getResult().toString())));
                    }
                });
                Toast.makeText(context, "Uploaded", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(context, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("TAG", "Failed : " + e.getMessage());
            }).addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                progressDialog.setMessage("Uploaded " + (int) progress + "%");
            });
        }
    }

    public static void uploadMultiplePhoto(StorageReference storageReference, List<Uri> uriList, Context context, int requestCode) {
        List<Uri> failedUris = new ArrayList<>(); // To store failed files
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Uploading...");
        progressDialog.show();
        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.e("TAG", "uploadPhoto: userId : " + userId);
        int itemCount = uriList.size();
        AtomicInteger uploadedCount = new AtomicInteger();
        for (int i = 0; i < itemCount; i++) {
            Uri filePath = uriList.get(i);
            if (filePath != null) {
                StorageReference ref = storageReference.child(userId + "/av" + System.currentTimeMillis());
                int finalI = i;
                int finalI1 = i;
                ref.putFile(filePath).addOnSuccessListener(taskSnapshot -> {
                    uploadedCount.getAndIncrement();
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(uri -> {
                        if (requestCode == REQUEST_VIDEO_PICK) {
                            createThumbnailUploadTask(filePath, context).addOnSuccessListener(thumbnailSnapshot -> thumbnailSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(thumbnailUri -> Story.uploadVideoStory(uri.getResult().toString(), thumbnailUri.getResult().toString())));
                        }
                    });
                    Toast.makeText(context, "Uploaded file " + (finalI + 1) + " of " + itemCount, Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "uploadMultiplePhoto: uploadedCount" + uploadedCount.get());
                    Log.e("TAG", "uploadMultiplePhoto: itemCount" + itemCount);
                    if (uploadedCount.get() == itemCount && !failedUris.isEmpty()) {
                        Log.e("TAG", "uploadMultiplePhoto: Starting showRetryDialog");
                        showRetryDialog(context, failedUris, storageReference, requestCode);
                    }
                }).addOnFailureListener(e -> {
                    uploadedCount.getAndIncrement();
                    failedUris.add(filePath);
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(context, "Failed to upload file " + (finalI1 + 1) + " of " + itemCount + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "uploadMultiplePhoto: uploadedCount" + uploadedCount.get());
                    Log.e("TAG", "uploadMultiplePhoto: itemCount" + itemCount);
                    if (uploadedCount.get() == itemCount && !failedUris.isEmpty()) {
                        Log.e("TAG", "uploadMultiplePhoto: Starting showRetryDialog");
                        showRetryDialog(context, failedUris, storageReference, requestCode);
                    }
                }).addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    progressDialog.setMessage("Uploading file " + (finalI1 + 1) + " of " + itemCount + ": " + (int) progress + "%");
                });
            }
        }
    }

    private static void showRetryDialog(Context context, List<Uri> failedUris, StorageReference storageReference, int requestCode) {
        Log.e("TAG", "Failed to upload file: showRetryDialog starting");
        new MaterialAlertDialogBuilder(context)
                .setTitle("Retry Failed Uploads?")
                .setMessage("Some files failed to upload. Do you want to retry uploading them?")
                .setPositiveButton("OK", (dialog, which) -> {
                    uploadMultiplePhoto(storageReference, failedUris, context, requestCode);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    showFileNamesDialog(context, failedUris);
                    dialog.dismiss();
                })
                .show();
    }

    public static void showFileNamesDialog(Context context, List<Uri> uriList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Uri uri : uriList) {
            stringBuilder.append(getAbsolutePath(uri, context)).append("\n");
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle("Failed File Names")
                .setMessage(stringBuilder.toString())
                .setPositiveButton("COPY TO CLIPBOARD", (dialog, which) -> {
                    // Copy all the file names to clipboard
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("File Names", stringBuilder.toString());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "File names copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Exit", (dialog, which) -> dialog.dismiss())
                .show();
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