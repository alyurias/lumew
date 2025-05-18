package com.example.lumew.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.lumew.R;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    public static final int PICK_IMAGE_REQUEST = 101;
    public static final int MAX_IMAGE_SIZE = 1024; // Max dimension for images

    /**
     * Create an intent to pick an image from the gallery
     * @return Intent for image picking
     */
    public static Intent getImagePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        return intent;
    }

    /**
     * Load an image into an ImageView using Glide
     * @param context Application context
     * @param imageUri URI of the image to load
     * @param imageView ImageView to display the image in
     */
    public static void loadImage(Context context, Uri imageUri, ImageView imageView) {
        try {
            Glide.with(context)
                    .load(imageUri)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .apply(RequestOptions.circleCropTransform())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show();
            imageView.setImageResource(R.drawable.default_profile);
        }
    }

    /**
     * Load an image from a URL into an ImageView using Glide
     * @param context Application context
     * @param imageUrl URL of the image to load
     * @param imageView ImageView to display the image in
     */
    public static void loadImageFromUrl(Context context, String imageUrl, ImageView imageView) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageView.setImageResource(R.drawable.default_profile);
                return;
            }

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .apply(RequestOptions.circleCropTransform())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Error loading image from URL", e);
            imageView.setImageResource(R.drawable.default_profile);
        }
    }

    /**
     * Get the file extension from a URI
     * @param context Application context
     * @param uri URI to get extension from
     * @return File extension or default jpg if not found
     */
    public static String getFileExtension(Context context, Uri uri) {
        String extension;

        if (uri.getScheme().equals("content")) {
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));
        } else {
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new java.io.File(uri.getPath())).toString());
        }

        return extension != null ? extension : "jpg";
    }

    /**
     * Compress and resize an image from URI to reduce upload size
     * @param context Application context
     * @param imageUri URI of the image to compress
     * @return Compressed image as byte array or null if failed
     */
    @Nullable
    public static byte[] compressImage(Context context, Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI");
                return null;
            }

            // Resize if necessary
            Bitmap resizedBitmap = originalBitmap;
            if (originalBitmap.getWidth() > MAX_IMAGE_SIZE || originalBitmap.getHeight() > MAX_IMAGE_SIZE) {
                if (originalBitmap.getWidth() >= originalBitmap.getHeight()) {
                    resizedBitmap = Bitmap.createScaledBitmap(
                            originalBitmap,
                            MAX_IMAGE_SIZE,
                            (int) (originalBitmap.getHeight() * ((float) MAX_IMAGE_SIZE / originalBitmap.getWidth())),
                            true);
                } else {
                    resizedBitmap = Bitmap.createScaledBitmap(
                            originalBitmap,
                            (int) (originalBitmap.getWidth() * ((float) MAX_IMAGE_SIZE / originalBitmap.getHeight())),
                            MAX_IMAGE_SIZE,
                            true);
                }

                // Recycle original bitmap if we created a new one
                if (resizedBitmap != originalBitmap) {
                    originalBitmap.recycle();
                }
            }

            // Compress to JPEG
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
            resizedBitmap.recycle();

            return outputStream.toByteArray();

        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found", e);
        } catch (IOException e) {
            Log.e(TAG, "IO Exception", e);
        } catch (Exception e) {
            Log.e(TAG, "Error compressing image", e);
        }

        return null;
    }
}