package com.example.lumew.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.example.lumew.R;
import com.example.lumew.models.Movie;

public class MovieDetailActivity extends AppCompatActivity {

    // All views should be properly declared
    private ImageView moviePoster;
    private TextView movieTitle, movieYear, movieRating;
    private TextView moviePlot, movieGenre, movieRuntime;
    private TextView movieActors, movieDirector, movieReleased;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private NestedScrollView contentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail); // This must come first!

        // Initialize all views with null checks
        initializeViews();

        // Setup toolbar after views are initialized
        setupToolbar();

        Movie movie = getIntent().getParcelableExtra("movie");
        if (movie != null) {
            displayMovieDetails(movie);
        } else {
            showErrorAndFinish("No movie data provided");
        }
    }

    private void initializeViews() {
        try {
            // Main container
            contentContainer = findViewById(R.id.content_container);
            progressBar = findViewById(R.id.progress_bar);

            // Toolbar
            toolbar = findViewById(R.id.toolbar);

            // Movie info views
            moviePoster = findViewById(R.id.movie_poster);
            movieTitle = findViewById(R.id.movie_title);
            movieYear = findViewById(R.id.movie_year);
            movieRating = findViewById(R.id.movie_rating);

            // Detailed info
            moviePlot = findViewById(R.id.movie_plot);
            movieGenre = findViewById(R.id.movie_genre);
            movieRuntime = findViewById(R.id.movie_runtime);

            // Additional info
            movieActors = findViewById(R.id.movie_actors);
            movieDirector = findViewById(R.id.movie_director);
            movieReleased = findViewById(R.id.movie_released);

        } catch (Exception e) {
            Toast.makeText(this, "Error initializing views: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void displayMovieDetails(Movie movie) {
        // First check if views are initialized
        if (movieTitle == null || moviePlot == null /* check other critical views */) {
            showErrorAndFinish("UI not properly initialized");
            return;
        }

        // Set all text with null checks
        safeSetText(movieTitle, movie.getTitle());
        safeSetText(movieYear, movie.getYear());
        safeSetText(movieRating, formatRating(movie.getImdbRating()));
        safeSetText(moviePlot, movie.getPlot());
        safeSetText(movieGenre, movie.getGenre());
        safeSetText(movieRuntime, movie.getRuntime());
        safeSetText(movieActors, formatList("Cast", movie.getActors()));
        safeSetText(movieDirector, formatDetail("Director", movie.getDirector()));
        safeSetText(movieReleased, formatDetail("Released", movie.getReleased()));

        // Load image safely
        if (moviePoster != null) {
            Glide.with(this)
                    .load(movie.getPoster())
                    .placeholder(R.drawable.logo2)
                    .error(R.drawable.logo)
                    .into(moviePoster);
        }

        if (contentContainer != null) {
            contentContainer.setVisibility(View.VISIBLE);
        }
    }

    // Helper method to safely set text
    private void safeSetText(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text != null ? text : "N/A");
        }
    }

    private String formatRating(String rating) {
        return (rating != null && !rating.equals("N/A")) ? "⭐ " + rating + "/10" : "Rating: N/A";
    }

    private String formatList(String label, String items) {
        if (items == null || items.isEmpty()) return label + ": N/A";
        return label + ":\n" + items.replace(", ", "\n• ");
    }

    private String formatDetail(String label, String value) {
        if (value == null || value.equals("N/A")) return label + ": N/A";
        return label + ": " + value;
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}