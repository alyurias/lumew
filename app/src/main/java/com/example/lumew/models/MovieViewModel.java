package com.example.lumew.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MovieViewModel extends ViewModel {
    private final MutableLiveData<List<Movie>> movies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Movie>> favoriteMovies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Movie>> skippedMovies = new MutableLiveData<>(new ArrayList<>());
    private final Set<String> favoriteIds = new HashSet<>();
    private final Set<String> skippedIds = new HashSet<>();

    public LiveData<List<Movie>> getMovies() {
        return movies;
    }

    public LiveData<List<Movie>> getFavoriteMovies() {
        return favoriteMovies;
    }

    public LiveData<List<Movie>> getSkippedMovies() {
        return skippedMovies;
    }

    public void setMovies(List<Movie> newMovies) {
        List<Movie> filteredMovies = new ArrayList<>();
        for (Movie movie : newMovies) {
            if (!favoriteIds.contains(movie.getImdbID()) && !skippedIds.contains(movie.getImdbID())) {
                filteredMovies.add(movie);
            }
        }
        movies.postValue(filteredMovies);
    }

    public void addToFavorites(Movie movie) {
        if (movie == null || favoriteIds.contains(movie.getImdbID())) {
            return;
        }

        List<Movie> currentFavorites = favoriteMovies.getValue() != null ?
                new ArrayList<>(favoriteMovies.getValue()) : new ArrayList<>();
        currentFavorites.add(movie);
        favoriteIds.add(movie.getImdbID());
        favoriteMovies.postValue(currentFavorites);
        removeFromCurrentMovies(movie.getImdbID());
    }

    public void addToSkipped(Movie movie) {
        if (movie == null || skippedIds.contains(movie.getImdbID())) {
            return;
        }

        List<Movie> currentSkipped = skippedMovies.getValue() != null ?
                new ArrayList<>(skippedMovies.getValue()) : new ArrayList<>();
        currentSkipped.add(movie);
        skippedIds.add(movie.getImdbID());
        skippedMovies.postValue(currentSkipped);
        removeFromCurrentMovies(movie.getImdbID());
    }

    private void removeFromCurrentMovies(String imdbId) {
        List<Movie> currentMovies = movies.getValue();
        if (currentMovies == null) return;

        List<Movie> updatedMovies = new ArrayList<>();
        for (Movie movie : currentMovies) {
            if (!movie.getImdbID().equals(imdbId)) {
                updatedMovies.add(movie);
            }
        }

        if (updatedMovies.size() != currentMovies.size()) {
            movies.postValue(updatedMovies);
        }
    }

    public void clearFavorites() {
        favoriteMovies.postValue(new ArrayList<>());
        favoriteIds.clear();
    }

    public void clearSkipped() {
        skippedMovies.postValue(new ArrayList<>());
        skippedIds.clear();
    }
}