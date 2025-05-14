package com.example.lumew.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.lumew.R;

public class ImageUtils {
    public static final int PICK_IMAGE_REQUEST = 101;

    public static Intent getImagePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        return intent;
    }

    public static void loadImage(Context context, Uri imageUri, ImageView imageView) {
        try {
            Glide.with(context)
                    .load(imageUri)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .circleCrop()
                    .into(imageView);
        } catch (Exception e) {
            Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show();
            imageView.setImageResource(R.drawable.default_profile);
        }
    }
}