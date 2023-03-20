package vfunny.shortvideovfunnyapp.Live.ui;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import vfunny.shortvideovfunnyapp.Login.data.Story;
import vfunny.shortvideovfunnyapp.Login.data.User;

/**
 * Created on 26/05/2017.
 * Copyright by 01eg.me
 */

public class MediaUtils {

    private static final int REQUEST_CODE = 1001;
    public static final int REQUEST_IMAGE_CAPTURE = REQUEST_CODE + 1;
    public static final int REQUEST_IMAGE_PICK = REQUEST_CODE + 2;
    public static final int REQUEST_PROFILE_CAPTURE = REQUEST_CODE + 3;
    public static final int REQUEST_PROFILE_PICK = REQUEST_CODE + 4;
    public static final int REQUEST_VIDEO_CAPTURE = REQUEST_CODE + 5;
    public static final int REQUEST_VIDEO_PICK = REQUEST_CODE + 6;

    private static final int PERMISSIONS_REQUEST_STORAGE = 10;

    public static CharSequence relativeTime(Long date) {
        if (date != null) {
            return DateUtils.getRelativeTimeSpanString(date,
                    System.currentTimeMillis(),
                    MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
        } else {
            return null;
        }
    }

    @NonNull
    public static boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_ANY);
    }


    public static void openCamera(Activity activity, int requestCode, File file) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            if (file != null) {
                Uri photoURI = FileProvider.getUriForFile(activity,
                        activity.getApplicationContext().getPackageName() + ".fileprovider",
                        file);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            }

            activity.startActivityForResult(takePictureIntent, requestCode);
        }
    }

    public static void openRecorder(Activity activity, int requestCode) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(intent, requestCode);
        }
    }

    public static boolean openPhotoLibrary(Activity activity, int requestCode) {
        // pick from gallery
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_PICK);
        activity.startActivityForResult(Intent.createChooser(intent, "Select Picture"), requestCode);
        return true;
    }

    public static boolean storagePermissionGrant(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // request the permission
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_STORAGE);
            return false;
        } else {
            return true;
        }
    }

    public static boolean openVideoLibrary(Activity activity, int requestCode) {
        // pick from gallery
        try {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        Log.e("TAG", "addClick: intent created");
            Log.e("TAG", "addClick: getPackageManager not null");
            activity.startActivityForResult(Intent.createChooser(intent, "Select"), requestCode);
            return true;
        } catch(Error e) {
            Log.e("TAG", "addClick: getPackageManager null");
            Log.e("TAG", "addClick: ERROR " + e);
            return false;
        }
    }

    @Nullable
    public static UploadTask handleImageCapture(Intent intent) {
        Bundle extras = intent.getExtras();
        Bitmap imageBitmap = (Bitmap) extras.get("data");

        return createUploadTask(imageBitmap);
    }

    private static UploadTask createUploadTask(Bitmap imageBitmap) {
        if (imageBitmap != null) {
            final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference imgref = storageRef.child(userId + "/ac" + System.currentTimeMillis() + ".jpg");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] bytes = baos.toByteArray();

            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpg")
                    .build();

            UploadTask uploadTask = imgref.putBytes(bytes, metadata);
            return uploadTask;
        }
        return null;
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
            ref.putFile(filePath)
                    .addOnSuccessListener(taskSnapshot -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(uri -> {
                            if (requestCode == REQUEST_VIDEO_PICK || requestCode == REQUEST_VIDEO_CAPTURE) {
                                String thumbnailPath = getAbsolutePath(filePath, context);
                                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                retriever.setDataSource(thumbnailPath);
                                Bitmap bitmap  = retriever.getFrameAtTime(0);
                                createUploadTask(bitmap).addOnSuccessListener(thumbnailSnapshot -> {
                                    thumbnailSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(thumbnailUri -> {
                                        Story.uploadVideoStory(uri.getResult().toString(), thumbnailUri.getResult().toString());
                                    });
                                });
                            }
                        });
                        Toast.makeText(context, "Uploaded", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(context, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("TAG", "Failed : " + e.getMessage());
                    })
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot
                                .getTotalByteCount());
                        progressDialog.setMessage("Uploaded " + (int) progress + "%");
                    });
        }
    }

    private static String getAbsolutePath(Uri uri, Context context) {
        String[] filePathColumn = {MediaStore.Video.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, filePathColumn,
                null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        return cursor.getString(columnIndex);
    }

    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    private static Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {

        ExifInterface ei = new ExifInterface(selectedImage.getPath());
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }
}
