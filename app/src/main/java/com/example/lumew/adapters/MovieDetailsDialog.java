package com.example.lumew.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.example.lumew.R;
import com.example.lumew.models.Movie;

public class MovieDetailsDialog extends android.app.Dialog {

    public MovieDetailsDialog(@NonNull Context context, Movie movie) {
        super(context, R.style.Theme_LumeW_Dialog); // Use custom dialog theme

        // Initialize window features
        initializeWindow();
        setContentView(R.layout.dialog_movie_details);

        // Initialize views
        ImageView poster = findViewById(R.id.dialog_poster);
        TextView title = findViewById(R.id.dialog_title);
        TextView year = findViewById(R.id.dialog_year);
        TextView plot = findViewById(R.id.dialog_plot);
        TextView director = findViewById(R.id.dialog_director);

        // Set movie data with null checks
        if (movie != null) {
            // Load poster image
            if (movie.getPoster() != null && !movie.getPoster().isEmpty()) {
                Glide.with(context)
                        .load(movie.getPoster())
                        .placeholder(R.drawable.logo)
                        .into(poster);
            }


        }
    }

    private void initializeWindow() {
        Window window = getWindow();
        if (window != null) {
            // Set transparent background
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Remove title bar (with version check)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                window.requestFeature(Window.FEATURE_NO_TITLE);
            }

            // Set animations if available
            try {
                window.setWindowAnimations(R.style.DialogAnimation);
            } catch (Exception e) {
                // Use default animations if custom ones aren't available
            }
        }
    }
}