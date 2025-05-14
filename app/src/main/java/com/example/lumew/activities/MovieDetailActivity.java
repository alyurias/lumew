package com.example.lumew.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.lumew.R;
import com.example.lumew.models.Movie;
import com.example.lumew.utils.RapidApiClient;
import com.google.android.material.appbar.MaterialToolbar;

public class MovieDetailActivity extends AppCompatActivity {
    private static final String TAG = "MovieDetailActivity";
    public static final String EXTRA_MOVIE_ID = "movie_id";
    public static final String EXTRA_MOVIE_OBJECT = "movie_object";

    private ImageView moviePoster;
    private ImageView movieBackdrop;
    private TextView movieTitle;
    private TextView movieYear;
    private TextView movieRating;
    private TextView moviePlot;
    private TextView movieGenre;
    private TextView movieRuntime;
    private TextView movieActors;
    private TextView movieDirector;
    private TextView movieReleased;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;
    private ConstraintLayout contentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        initializeViews();
        setupToolbar();

        // Try to get complete movie object first
        Movie movie = getIntent().getParcelableExtra(EXTRA_MOVIE_OBJECT);
        if (movie != null && movie.getImdbID() != null && !movie.getImdbID().isEmpty()) {
            displayMovieDetails(movie);
            // Load fresh details in background
            loadMovieDetails(movie.getImdbID());
        } else {
            // Fall back to loading by ID
            String movieId = getIntent().getStringExtra(EXTRA_MOVIE_ID);
            if (movieId != null && !movieId.isEmpty()) {
                loadMovieDetails(movieId);
            } else {
                showErrorAndFinish("No valid movie data provided");
            }
        }
    }

    @SuppressLint("WrongViewCast")
    private void initializeViews() {
        moviePoster = findViewById(R.id.movie_poster);
        movieBackdrop = findViewById(R.id.movie_backdrop);
        movieTitle = findViewById(R.id.movie_title);
        movieYear = findViewById(R.id.movie_year);
        movieRating = findViewById(R.id.movie_rating);
        moviePlot = findViewById(R.id.movie_plot);
        movieGenre = findViewById(R.id.movie_genre);
        movieRuntime = findViewById(R.id.movie_runtime);
        movieActors = findViewById(R.id.movie_actors);
        movieDirector = findViewById(R.id.movie_director);
        movieReleased = findViewById(R.id.movie_released);
        progressBar = findViewById(R.id.progress_bar);
        toolbar = findViewById(R.id.toolbar);

    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadMovieDetails(String movieId) {
        showLoading(true);
        RapidApiClient.getMovieDetails(movieId, new RapidApiClient.OnMovieLoadedListener() {
            @Override
            public void onMovieLoaded(Movie movie) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (movie != null) {
                        displayMovieDetails(movie);
                    } else {
                        showErrorAndFinish("Movie data is null");
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (contentLayout.getVisibility() == View.VISIBLE) {
                        Toast.makeText(MovieDetailActivity.this,
                                "Failed to update: " + message,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        showErrorAndFinish("Failed to load movie: " + message);
                    }
                });
            }
        });
    }

    private void displayMovieDetails(Movie movie) {
        // Load images with Glide
        loadImageWithGlide(movie.getPoster(), moviePoster, R.drawable.logo);

        if (movie.getBackdropUrl() != null && !movie.getBackdropUrl().isEmpty()) {
            loadImageWithGlide(movie.getBackdropUrl(), movieBackdrop, R.drawable.logo2);
        }

        // Set text values
        movieTitle.setText(movie.getTitle());
        movieYear.setText(movie.getYear());

        if (movie.getImdbRating() != null && !movie.getImdbRating().isEmpty() && !movie.getImdbRating().equals("N/A")) {
            movieRating.setText(getString(R.string.imdb_rating_format, movie.getImdbRating()));
            movieRating.setVisibility(View.VISIBLE);
        } else {
            movieRating.setVisibility(View.GONE);
        }

        moviePlot.setText(movie.getPlot());
        movieGenre.setText(movie.getGenre());
        movieRuntime.setText(movie.getRuntime());
        movieActors.setText(movie.getActors());
        movieDirector.setText(movie.getDirector());
        movieReleased.setText(movie.getReleased());

        // Update toolbar
        toolbar.setTitle(movie.getTitle());
        contentLayout.setVisibility(View.VISIBLE);
    }

    private void loadImageWithGlide(String url, ImageView imageView, int placeholder) {
        Glide.with(this)
                .load(url)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .placeholder(placeholder)
                .error(placeholder)
                .into(imageView);
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    public static Bundle createIntentExtras(Movie movie) {
        Bundle extras = new Bundle();
        extras.putParcelable(EXTRA_MOVIE_OBJECT, movie);
        return extras;
    }

    public static Bundle createIntentExtras(String movieId) {
        Bundle extras = new Bundle();
        extras.putString(EXTRA_MOVIE_ID, movieId);
        return extras;
    }
}