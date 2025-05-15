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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
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
    private ProgressBar loadingProgressBar;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateText;
    private ImageButton filterButton;
    private String currentYearFilter = null; // null means no filter

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
        setupSearchInput();
        setupFilterButton(); // Add this line
    }

    private void setupViews(View view) {
        searchInput = view.findViewById(R.id.search_edit_text);
        moviesRecyclerView = view.findViewById(R.id.movies_recycler_view);
        loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        emptyStateText = view.findViewById(R.id.empty_state_text);

        // If these views don't exist in your layout, you'll need to add them

        adapter = new MovieGridAdapter(requireContext(), moviesList, this);
        moviesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        moviesRecyclerView.setAdapter(adapter);

        // Initially hide results and show empty state
        showEmptyState("Search for movies");
    }

    private void setupFilterButton() {
        filterButton = getView().findViewById(R.id.filter_button);
        filterButton.setOnClickListener(v -> showFilterMenu());
    }

    private void showFilterMenu() {
        PopupMenu popup = new PopupMenu(requireContext(), filterButton);
        popup.getMenuInflater().inflate(R.menu.search_filters, popup.getMenu());

        // Set current selection
        if (currentYearFilter != null) {
            if (currentYearFilter.equals("1980-1989")) {
                popup.getMenu().findItem(R.id.filter_80s).setChecked(true);
            } else if (currentYearFilter.equals("1990-1999")) {
                popup.getMenu().findItem(R.id.filter_90s).setChecked(true);
            } else if (currentYearFilter.equals("2000-2009")) {
                popup.getMenu().findItem(R.id.filter_2000s).setChecked(true);
            } else if (currentYearFilter.equals("2010-2019")) {
                popup.getMenu().findItem(R.id.filter_2010s).setChecked(true);
            } else {
                popup.getMenu().findItem(R.id.filter_all).setChecked(true);
            }
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.filter_80s) {
                currentYearFilter = "1980-1989";
            } else if (id == R.id.filter_90s) {
                currentYearFilter = "1990-1999";
            } else if (id == R.id.filter_2000s) {
                currentYearFilter = "2000-2009";
            } else if (id == R.id.filter_2010s) {
                currentYearFilter = "2010-2019";
            } else {
                currentYearFilter = null;
            }
            performSearchWithFilters();
            return true;
        });

        popup.show();
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
                    showLoading();
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                } else {
                    clearResults();
                    if (query.isEmpty()) {
                        showEmptyState("Search for movies");
                    } else {
                        showEmptyState("Type at least 3 characters to search");
                    }
                }
            }
        });
    }

    private void performSearchWithFilters() {
        String query = searchInput.getText().toString().trim();
        if (query.length() >= 3) {
            showLoading();
            performSearch(query);
        }
    }

    private void performSearch(String query) {
        RapidApiClient.searchMovies(query, currentYearFilter, new RapidApiClient.OnMoviesLoadedListener() {
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
            hideLoading();
            moviesList.clear();
            if (movies != null && !movies.isEmpty()) {
                moviesList.addAll(movies);
                adapter.notifyDataSetChanged();
                showResults();
            } else {
                showEmptyState("No movies found");
            }
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
            hideLoading();
            Toast.makeText(getContext(), "Search error: " + message, Toast.LENGTH_SHORT).show();
            showEmptyState("Error searching for movies");
            clearResults();
        });
    }

    private void showLoading() {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.GONE);
        }
        moviesRecyclerView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.GONE);
        }
    }

    private void showResults() {
        moviesRecyclerView.setVisibility(View.VISIBLE);
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message) {
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            if (emptyStateText != null) {
                emptyStateText.setText(message);
            }
        }
        moviesRecyclerView.setVisibility(View.GONE);
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.GONE);
        }
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