package com.example.lumew.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.example.lumew.R;
import com.example.lumew.models.Movie;
import com.example.lumew.utils.SwipeHelper;
import java.util.List;

public class SingleCardAdapter {
    private final Context context;
    private final List<Movie> movies;
    private final SwipeListener swipeListener;
    private final ViewGroup container;
    private int currentPosition = 0;
    private boolean isClickEnabled = true;

    public interface SwipeListener {
        void onSwipeRight(Movie movie);
        void onSwipeLeft(Movie movie);
        void onCardClicked(Movie movie);
        void onNoMoreCards();
    }

    public SingleCardAdapter(Context context, List<Movie> movies,
                             ViewGroup container, SwipeListener listener) {
        this.context = context;
        this.movies = movies;
        this.container = container;
        this.swipeListener = listener;
        showCurrentCard();
    }

    private void showCurrentCard() {
        container.removeAllViews();
        container.setBackgroundColor(context.getResources().getColor(android.R.color.black));

        if (currentPosition >= movies.size()) {
            swipeListener.onNoMoreCards();
            return;
        }

        Movie movie = movies.get(currentPosition);
        View cardView = LayoutInflater.from(context)
                .inflate(R.layout.item_movie_card, container, false);

        ImageView posterImage = cardView.findViewById(R.id.movie_poster);
        TextView titleText = cardView.findViewById(R.id.movie_title);
        TextView yearText = cardView.findViewById(R.id.movie_year);
        TextView likeIndicator = cardView.findViewById(R.id.like_indicator);
        TextView dislikeIndicator = cardView.findViewById(R.id.dislike_indicator);
        ImageButton infoButton = cardView.findViewById(R.id.info_button);

        // Load movie data
        titleText.setText(movie.getTitle());
        yearText.setText(movie.getYear());

        Glide.with(context)
                .load(movie.getPoster())
                .placeholder(R.drawable.logo2)
                .error(R.drawable.logo)
                .centerCrop()
                .into(posterImage);

        // Info Button Click
        infoButton.setOnClickListener(v -> showMovieDetails(movie));

        // Swipe Handling
        new SwipeHelper(cardView, likeIndicator, dislikeIndicator, new SwipeHelper.SwipeCallback() {
            @Override
            public void onSwipedRight() {
                handleSwipe(true, movie);
            }

            @Override
            public void onSwipedLeft() {
                handleSwipe(false, movie);
            }

            @Override
            public void onCardReset() {
                isClickEnabled = true;
            }
        });

        // Card Click
        cardView.setOnClickListener(v -> {
            if (isClickEnabled && swipeListener != null) {
                swipeListener.onCardClicked(movie);
            }
        });

        container.addView(cardView);
    }

    private void showMovieDetails(Movie movie) {
        String details = String.format(
                "Director: %s\n\nActors: %s\n\nGenre: %s\n\nRating: %s\n\nPlot: %s",
                movie.getDirector(),
                movie.getActors(),
                movie.getGenre(),
                movie.getImdbRating(),
                movie.getPlot()
        );

        new AlertDialog.Builder(context)
                .setTitle(movie.getTitle() + " (" + movie.getYear() + ")")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    private void handleSwipe(boolean isRight, Movie movie) {
        isClickEnabled = false;
        if (isRight) {
            swipeListener.onSwipeRight(movie);
        } else {
            swipeListener.onSwipeLeft(movie);
        }
        currentPosition++;
        showCurrentCard();
    }

    public boolean hasMoreCards() {
        return currentPosition < movies.size();
    }

    public void reloadWithNewMovies(List<Movie> newMovies) {
        movies.clear();
        movies.addAll(newMovies);
        currentPosition = 0;
        showCurrentCard();
    }
}