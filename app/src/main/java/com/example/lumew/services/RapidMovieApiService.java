package com.example.lumew.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.lumew.models.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RapidMovieApiService {
    private static final String TAG = "RapidMovieApiService";
    private static final String API_KEY = "7fec5c329emshd63ec62f6c53adap14ae3bjsne77908f7c737";
    private static final String API_HOST = "imdb236.p.rapidapi.com";
    private static final String BASE_URL = "https://imdb236.p.rapidapi.com/";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnMoviesLoadedListener {
        void onMoviesLoaded(List<Movie> movies);
        void onError(String message);
    }

    public interface OnMovieLoadedListener {
        void onMovieLoaded(Movie movie);
        void onError(String message);
    }

    public static void getTop250Movies(final OnMoviesLoadedListener listener) {
        String url = BASE_URL + "imdb/top250-movies";
        makeListRequest(url, listener);
    }

    public static void searchMovies(String query, final OnMoviesLoadedListener listener) {
        String url = BASE_URL + "imdb/search?query=" + query + "&limit=20";
        makeListRequest(url, listener);
    }

    public static void getMovieDetails(String imdbId, final OnMovieLoadedListener listener) {
        String url = BASE_URL + "imdb/movie-details?movieId=" + imdbId;
        makeSingleRequest(url, listener);
    }

    private static void makeListRequest(String url, final OnMoviesLoadedListener listener) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-key", API_KEY)
                .addHeader("x-rapidapi-host", API_HOST)
                .build();

        Log.d(TAG, "Making request to: " + url);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed", e);
                notifyError(listener, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "empty body";
                        Log.e(TAG, "HTTP error: " + response.code() + " - " + errorBody);
                        notifyError(listener, "Server error: " + response.code());
                        return;
                    }

                    String jsonString = response.body().string();
                    Log.d(TAG, "Raw response: " + jsonString);

                    try {
                        List<Movie> movies = parseMoviesResponse(jsonString);
                        if (movies.isEmpty()) {
                            notifyError(listener, "No movies found in response");
                        } else {
                            notifySuccess(listener, movies);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error", e);
                        notifyError(listener, "Invalid data format from server");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Response handling error", e);
                    notifyError(listener, "Error processing response");
                }
            }
        });
    }

    private static void makeSingleRequest(String url, final OnMovieLoadedListener listener) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-key", API_KEY)
                .addHeader("x-rapidapi-host", API_HOST)
                .build();

        Log.d(TAG, "Making request to: " + url);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed", e);
                notifyError(listener, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "empty body";
                        Log.e(TAG, "HTTP error: " + response.code() + " - " + errorBody);
                        notifyError(listener, "Server error: " + response.code());
                        return;
                    }

                    String jsonString = response.body().string();
                    Log.d(TAG, "Raw response: " + jsonString);

                    try {
                        Movie movie = parseMovieDetailsResponse(jsonString);
                        if (movie == null) {
                            notifyError(listener, "No movie details found");
                        } else {
                            notifySuccess(listener, movie);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error", e);
                        notifyError(listener, "Invalid data format from server");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Response handling error", e);
                    notifyError(listener, "Error processing response");
                }
            }
        });
    }

    private static List<Movie> parseMoviesResponse(String jsonString) throws JSONException {
        List<Movie> movies = new ArrayList<>();

        // Try to parse as JSON array first
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject movieObj = jsonArray.getJSONObject(i);
                movies.add(parseMovieFromJson(movieObj));
            }
            return movies;
        } catch (JSONException e) {
            Log.d(TAG, "Response is not a direct array, trying object format");
        }

        // If not array, try to parse as JSON object with items array
        JSONObject responseObj = new JSONObject(jsonString);
        if (responseObj.has("items")) {
            JSONArray itemsArray = responseObj.getJSONArray("items");
            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject movieObj = itemsArray.getJSONObject(i);
                movies.add(parseMovieFromJson(movieObj));
            }
        } else if (responseObj.has("results")) {
            JSONArray resultsArray = responseObj.getJSONArray("results");
            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject movieObj = resultsArray.getJSONObject(i);
                movies.add(parseMovieFromJson(movieObj));
            }
        } else {
            throw new JSONException("Response doesn't contain 'items' or 'results' array");
        }

        return movies;
    }

    private static Movie parseMovieFromJson(JSONObject movieObj) throws JSONException {
        Movie movie = new Movie(
                movieObj.optString("id", ""),
                movieObj.optString("primaryTitle", movieObj.optString("title", "Unknown Title")),
                movieObj.optString("startYear", movieObj.optString("year", "N/A")),
                movieObj.optString("primaryImage", movieObj.optString("image", ""))
        );

        // Set additional fields that might be present
        movie.setImdbRating(movieObj.optString("averageRating", movieObj.optString("imDbRating", "N/A")));
        movie.setPlot(movieObj.optString("description", movieObj.optString("plot", "No plot available")));
        movie.setGenre(movieObj.optString("genres", ""));
        movie.setRuntime(movieObj.optString("runtimeMinutes", "N/A") + " mins");
        movie.setReleased(movieObj.optString("releaseDate", "N/A"));

        return movie;
    }

    private static Movie parseMovieDetailsResponse(String jsonString) throws JSONException {
        JSONObject response = new JSONObject(jsonString);

        Movie movie = parseMovieFromJson(response);

        // Set additional details specific to movie details response
        movie.setBackdropUrl(response.optString("backdrop", ""));
        movie.setDirector(response.optString("directors", "Unknown"));
        movie.setActors(response.optString("stars", ""));

        return movie;
    }

    private static void notifySuccess(final OnMoviesLoadedListener listener, final List<Movie> movies) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onMoviesLoaded(movies);
            }
        });
    }

    private static void notifySuccess(final OnMovieLoadedListener listener, final Movie movie) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onMovieLoaded(movie);
            }
        });
    }

    private static void notifyError(final OnMoviesLoadedListener listener, final String message) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onError(message);
            }
        });
    }

    private static void notifyError(final OnMovieLoadedListener listener, final String message) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onError(message);
            }
        });
    }
}