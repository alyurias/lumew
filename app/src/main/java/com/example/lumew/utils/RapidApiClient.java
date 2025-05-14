package com.example.lumew.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.lumew.models.Movie;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

public class RapidApiClient {
    private static final String TAG = "RapidApiClient";
    private static final String BASE_URL = "https://imdb236.p.rapidapi.com/";
    private static final String API_HOST = "imdb236.p.rapidapi.com";
    private static final String API_KEY = "7fec5c329emshd63ec62f6c53adap14ae3bjsne77908f7c737";
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    private static final AsyncHttpClient client = new AsyncHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    static {
        client.addHeader("x-rapidapi-key", API_KEY);
        client.addHeader("x-rapidapi-host", API_HOST);
        client.setTimeout((int) TimeUnit.SECONDS.toMillis(30));
        client.setMaxRetriesAndTimeout(MAX_RETRIES, (int) TimeUnit.SECONDS.toMillis(30));
        client.setEnableRedirects(true);
    }

    public interface OnMoviesLoadedListener {
        void onMoviesLoaded(List<Movie> movies);
        void onError(String message);
    }

    public interface OnMovieLoadedListener {
        void onMovieLoaded(Movie movie);
        void onError(String message);
    }

    public static void getTop250Movies(final OnMoviesLoadedListener listener) {
        String url = getAbsoluteUrl("imdb/top250-movies");
        makeListRequest(url, null, listener);
    }

    public static void getMovieDetails(String imdbId, final OnMovieLoadedListener listener) {
        String url = getAbsoluteUrl("imdb/movie-details");
        RequestParams params = new RequestParams();
        params.put("movieId", imdbId);
        makeSingleRequest(url, params, listener);
    }

    public static void searchMovies(String query, final OnMoviesLoadedListener listener) {
        String url = getAbsoluteUrl("imdb/search");
        RequestParams params = new RequestParams();
        params.put("query", query);
        params.put("limit", "20");
        makeListRequest(url, params, listener);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

    private static void makeListRequest(String url, RequestParams params,
                                        final OnMoviesLoadedListener listener) {
        client.get(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                handleListResponse(responseBody, listener);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  byte[] responseBody, Throwable error) {
                handleFailure(statusCode, responseBody, error, listener);
            }
        });
    }

    private static void makeSingleRequest(String url, RequestParams params,
                                          final OnMovieLoadedListener listener) {
        client.get(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                handleSingleResponse(responseBody, listener);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  byte[] responseBody, Throwable error) {
                handleFailure(statusCode, responseBody, error, listener);
            }
        });
    }

    private static void handleListResponse(byte[] responseBody,
                                           final OnMoviesLoadedListener listener) {
        try {
            String jsonString = new String(responseBody);
            Log.d(TAG, "API Response: " + jsonString);

            List<Movie> movies = parseMoviesResponse(jsonString);
            if (movies.isEmpty()) {
                notifyError(listener, "No movies found in response");
            } else {
                notifySuccess(listener, movies);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON Parsing Error", e);
            notifyError(listener, "Invalid data format from server");
        } catch (Exception e) {
            Log.e(TAG, "Unexpected Error", e);
            notifyError(listener, "Failed to process response");
        }
    }

    private static void handleSingleResponse(byte[] responseBody,
                                             final OnMovieLoadedListener listener) {
        try {
            String jsonString = new String(responseBody);
            Log.d(TAG, "API Response: " + jsonString);

            Movie movie = parseMovieDetailsResponse(jsonString);
            if (movie == null) {
                notifyError(listener, "No movie details found");
            } else {
                notifySuccess(listener, movie);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON Parsing Error", e);
            notifyError(listener, "Invalid movie data format");
        } catch (Exception e) {
            Log.e(TAG, "Unexpected Error", e);
            notifyError(listener, "Failed to process movie details");
        }
    }

    private static void handleFailure(int statusCode, byte[] responseBody,
                                      Throwable error, final OnMoviesLoadedListener listener) {
        String errorDetails = buildErrorDetails(statusCode, responseBody, error);
        Log.e(TAG, "API Request Failed: " + errorDetails);
        notifyError(listener, "Request failed: " + errorDetails);
    }

    private static void handleFailure(int statusCode, byte[] responseBody,
                                      Throwable error, final OnMovieLoadedListener listener) {
        String errorDetails = buildErrorDetails(statusCode, responseBody, error);
        Log.e(TAG, "API Request Failed: " + errorDetails);
        notifyError(listener, "Request failed: " + errorDetails);
    }

    private static String buildErrorDetails(int statusCode, byte[] responseBody, Throwable error) {
        StringBuilder sb = new StringBuilder("Status: ").append(statusCode);

        if (error != null) {
            sb.append(", Error: ").append(error.getMessage());
        }

        if (responseBody != null && responseBody.length > 0) {
            try {
                String response = new String(responseBody);
                sb.append(", Response: ").append(response);
            } catch (Exception e) {
                sb.append(", [Could not parse response body]");
            }
        }

        return sb.toString();
    }

    private static List<Movie> parseMoviesResponse(String jsonResponse) throws JSONException {
        List<Movie> movies = new ArrayList<>();

        // First try to parse as array
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject item = jsonArray.getJSONObject(i);
                    Movie movie = parseMovieFromJson(item);
                    if (isValidMovie(movie)) {
                        movies.add(movie);
                    }
                } catch (JSONException e) {
                    Log.w(TAG, "Skipping invalid movie item at position " + i, e);
                }
            }
            return movies;
        } catch (JSONException e) {
            Log.d(TAG, "Response is not a direct array, trying object format");
        }

        // If not array, try to parse as object
        JSONObject responseObj = new JSONObject(jsonResponse);
        JSONArray items = responseObj.optJSONArray("items");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                try {
                    JSONObject item = items.getJSONObject(i);
                    Movie movie = parseMovieFromJson(item);
                    if (isValidMovie(movie)) {
                        movies.add(movie);
                    }
                } catch (JSONException e) {
                    Log.w(TAG, "Skipping invalid movie item at position " + i, e);
                }
            }
        } else {
            // Try to parse as single movie if no items array
            try {
                Movie movie = parseMovieFromJson(responseObj);
                if (isValidMovie(movie)) {
                    movies.add(movie);
                }
            } catch (JSONException e) {
                Log.w(TAG, "Could not parse response as single movie", e);
            }
        }

        return movies;
    }

    private static Movie parseMovieFromJson(JSONObject item) throws JSONException {
        Movie movie = new Movie();

        // Required fields
        movie.setImdbID(item.optString("id", ""));
        movie.setTitle(item.optString("primaryTitle", item.optString("title", "Unknown Title")));

        // Optional fields
        movie.setYear(item.optString("startYear", item.optString("year", "N/A")));
        movie.setPoster(item.optString("primaryImage", item.optString("image", "")));
        movie.setImdbRating(item.optString("averageRating", item.optString("imDbRating", "0.0")));
        movie.setPlot(item.optString("description", item.optString("plot", "No plot available")));
        movie.setGenre(item.optString("genres", ""));
        movie.setRuntime(item.optString("runtimeMinutes", "N/A") + " mins");
        movie.setReleased(item.optString("releaseDate", ""));

        return movie;
    }

    private static Movie parseMovieDetailsResponse(String jsonResponse) throws JSONException {
        JSONObject response = new JSONObject(jsonResponse);
        Movie movie = parseMovieFromJson(response);

        // Additional details specific to movie details response
        movie.setBackdropUrl(response.optString("backdrop", ""));
        movie.setDirector(response.optString("directors", "Unknown"));
        movie.setActors(response.optString("stars", ""));

        return movie;
    }

    private static boolean isValidMovie(Movie movie) {
        return movie.getImdbID() != null && !movie.getImdbID().isEmpty() &&
                movie.getTitle() != null && !movie.getTitle().isEmpty();
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

    public static void cancelAllRequests() {
        client.cancelAllRequests(true);
    }
}