package com.example.lumew.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.lumew.R;
import com.example.lumew.activities.MovieDetailActivity;
import com.example.lumew.adapters.SingleCardAdapter;
import com.example.lumew.models.Movie;
import com.example.lumew.utils.RapidApiClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment implements SingleCardAdapter.SwipeListener {
    private SingleCardAdapter cardAdapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FrameLayout cardContainer;
    private boolean isDestroyed = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.view_empty_state);
        cardContainer = view.findViewById(R.id.card_container);

        loadPopularMovies();
    }

    private void loadPopularMovies() {
        if (isDestroyed) return;

        showLoading(true);
        emptyView.setText("Loading movies...");

        RapidApiClient.getTop250Movies(new RapidApiClient.OnMoviesLoadedListener() {
            @Override
            public void onMoviesLoaded(List<Movie> movies) {
                if (isDestroyed || getActivity() == null) return;

                requireActivity().runOnUiThread(() -> {
                    if (isDestroyed || getActivity() == null) return;

                    showLoading(false);

                    if (movies == null || movies.isEmpty()) {
                        showEmptyView(true, "No movies found");
                        return;
                    }

                    Collections.shuffle(movies);
                    showEmptyView(false, "");

                    List<Movie> initialMovies = new ArrayList<>(movies.subList(0, Math.min(20, movies.size())));
                    initializeCardAdapter(initialMovies);
                });
            }

            @Override
            public void onError(String message) {
                if (isDestroyed || getActivity() == null) return;

                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showEmptyView(true, "Error loading movies");
                    showToast("Failed to load movies");
                });
            }
        });
    }

    private void initializeCardAdapter(List<Movie> movies) {
        cardAdapter = new SingleCardAdapter(requireContext(), movies, cardContainer, this);
    }

    @Override
    public void onSwipeRight(Movie movie) {
        showToast("✓ Saved: " + movie.getTitle());
        // Save to database
    }

    @Override
    public void onSwipeLeft(Movie movie) {
        showToast("✗ Passed: " + movie.getTitle());
        // Handle skip
    }

    @Override
    public void onCardClicked(Movie movie) {
        Intent intent = new Intent(getActivity(), MovieDetailActivity.class);
        intent.putExtra("movie", movie);
        startActivity(intent);
        requireActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void onNoMoreCards() {
        showEmptyView(true, "No more movies");
        showToast("You've seen all movies!");
    }

    private void showLoading(boolean isLoading) {
        if (isDestroyed) return;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        emptyView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        cardContainer.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showEmptyView(boolean show, String message) {
        if (isDestroyed) return;
        emptyView.setText(message);
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showToast(String message) {
        if (isDestroyed || getActivity() == null) return;
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isDestroyed = true;
        RapidApiClient.cancelAllRequests();
    }
}