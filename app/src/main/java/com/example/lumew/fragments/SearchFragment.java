package com.example.lumew.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lumew.R;
import com.example.lumew.activities.MovieDetailActivity;
import com.example.lumew.adapters.MovieGridAdapter;
import com.example.lumew.adapters.OnMovieClickListener;
import com.example.lumew.models.Movie;
import com.example.lumew.utils.RapidApiClient;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment implements OnMovieClickListener {
    private EditText searchInput;
    private RecyclerView moviesRecyclerView;
    private MovieGridAdapter adapter;
    private List<Movie> moviesList = new ArrayList<>();
    private static final int SEARCH_DELAY_MS = 500;
    private final Handler searchHandler = new Handler();
    private final Runnable searchRunnable = new Runnable() {
        @Override
        public void run() {
            performSearch(searchInput.getText().toString().trim());
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
        setupSearchInput();
    }

    private void setupViews(View view) {
        searchInput = view.findViewById(R.id.search_edit_text);
        moviesRecyclerView = view.findViewById(R.id.movies_recycler_view);
        adapter = new MovieGridAdapter(requireContext(), moviesList, this);
        moviesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        moviesRecyclerView.setAdapter(adapter);
    }

    private void setupSearchInput() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                searchHandler.removeCallbacks(searchRunnable);
                String query = s.toString().trim();
                if (query.length() >= 3) {
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                } else {
                    clearResults();
                }
            }
        });
    }

    private void performSearch(String query) {
        RapidApiClient.searchMovies(query, new RapidApiClient.OnMoviesLoadedListener() {
            @Override
            public void onMoviesLoaded(List<Movie> movies) {
                updateMoviesList(movies);
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void updateMoviesList(List<Movie> movies) {
        requireActivity().runOnUiThread(() -> {
            moviesList.clear();
            if (movies != null) {
                moviesList.addAll(movies);
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void clearResults() {
        requireActivity().runOnUiThread(() -> {
            moviesList.clear();
            adapter.notifyDataSetChanged();
        });
    }

    private void showError(String message) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), "Search error: " + message, Toast.LENGTH_SHORT).show();
            clearResults();
        });
    }

    @Override
    public void onMovieClick(Movie movie) {
        if (getActivity() == null || isDetached()) return;

        Intent intent = new Intent(getActivity(), MovieDetailActivity.class);
        intent.putExtra("movie", movie); // Simplified key name
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchHandler.removeCallbacksAndMessages(null);
        RapidApiClient.cancelAllRequests();
    }
}