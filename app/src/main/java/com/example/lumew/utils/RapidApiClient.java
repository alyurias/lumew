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
    // Updated BASE_URL to include the /api/ prefix
    private static final String BASE_URL = "https://imdb236.p.rapidapi.com/api/";
    private static final String API_HOST = "imdb236.p.rapidapi.com";
    private static final String API_KEY = "255960d57emsh5ceceafab984506p100ccdjsn4593cc4fa79a";
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    private static final AsyncHttpClient client = new AsyncHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    static {
        Log.d(TAG, "Initializing RapidApiClient with API Key: " + maskApiKey(API_KEY));
        client.addHeader("x-rapidapi-key", API_KEY);
        client.addHeader("x-rapidapi-host", API_HOST);
        client.setTimeout((int) TimeUnit.SECONDS.toMillis(30));
        client.setMaxRetriesAndTimeout(MAX_RETRIES, (int) TimeUnit.SECONDS.toMillis(30));
        client.setEnableRedirects(true);
    }

    // Helper method to mask API key in logs for security
    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
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
        // Using the correct endpoint path
        String url = getAbsoluteUrl("imdb/top250-movies");
        Log.d(TAG, "Requesting top 250 movies from URL: " + url);
        makeListRequest(url, null, listener);
    }

    public static void getMovieDetails(String imdbId, final OnMovieLoadedListener listener) {
        String url = getAbsoluteUrl("imdb/movie-details");
        RequestParams params = new RequestParams();
        params.put("movieId", imdbId);
        makeSingleRequest(url, params, listener);
    }

    public static void searchMovies(String query, String yearFilter, final OnMoviesLoadedListener listener) {
        String url = getAbsoluteUrl("imdb/search");
        RequestParams params = new RequestParams();
        params.put("primaryTitleAutocomplete", query);
        params.put("type", "movie");
        params.put("rows", "25");

        if (yearFilter != null) {
            String[] years = yearFilter.split("-");
            params.put("startYearFrom", years[0]);
            params.put("startYearTo", years[1]);
        }

        Log.d(TAG, "Searching for movies with query: " + query + ", year filter: " + yearFilter);
        makeListRequest(url, params, listener);
    }

    // Method to search movies with additional parameters
    public static void searchMoviesByGenre(String genre, String sortOrder, String sortField, int rows,
                                           final OnMoviesLoadedListener listener) {
        String url = getAbsoluteUrl("imdb/search");
        RequestParams params = new RequestParams();
        params.put("type", "movie");
        params.put("genre", genre);
        params.put("rows", String.valueOf(rows));
        params.put("sortOrder", sortOrder);
        params.put("sortField", sortField);

        Log.d(TAG, "Searching for movies by genre: " + genre);
        makeListRequest(url, params, listener);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

    private static void makeListRequest(String url, RequestParams params,
                                        final OnMoviesLoadedListener listener) {
        Log.d(TAG, "Making API request to: " + url + " with params: " + (params != null ? params.toString() : "none"));

        // Print the full request URL with parameters for debugging - safer way that doesn't rely on RequestParams.get()
        StringBuilder fullUrl = new StringBuilder(url);
        if (params != null) {
            fullUrl.append("?").append(params.toString().replace(", ", "&"));
        }
        Log.d(TAG, "Full request URL: " + fullUrl);

        client.get(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(TAG, "API request successful with status code: " + statusCode);
                // Log headers for debugging
                for (Header header : headers) {
                    Log.d(TAG, "Response header: " + header.getName() + " = " + header.getValue());
                }
                // Log response body preview for debugging
                String responsePreview = new String(responseBody);
                Log.d(TAG, "Response preview: " + responsePreview.substring(0, Math.min(200, responsePreview.length())) + "...");
                handleListResponse(responseBody, listener);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  byte[] responseBody, Throwable error) {
                Log.e(TAG, "API request failed with status code: " + statusCode);
                // Log headers for debugging
                if (headers != null) {
                    for (Header header : headers) {
                        Log.e(TAG, "Error header: " + header.getName() + " = " + header.getValue());
                    }
                }
                // Log error response body if available
                if (responseBody != null && responseBody.length > 0) {
                    Log.e(TAG, "Error response: " + new String(responseBody));
                }
                handleListFailure(statusCode, responseBody, error, listener);
            }
        });
    }

    private static void makeSingleRequest(String url, RequestParams params,
                                          final OnMovieLoadedListener listener) {
        Log.d(TAG, "Making API request to: " + url + " with params: " + (params != null ? params.toString() : "none"));

        client.get(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(TAG, "API request successful with status code: " + statusCode);
                String responsePreview = new String(responseBody);
                Log.d(TAG, "Response preview: " + responsePreview.substring(0, Math.min(200, responsePreview.length())) + "...");
                handleSingleResponse(responseBody, listener);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  byte[] responseBody, Throwable error) {
                Log.e(TAG, "API request failed with status code: " + statusCode);
                if (responseBody != null && responseBody.length > 0) {
                    Log.e(TAG, "Error response: " + new String(responseBody));
                }
                handleSingleFailure(statusCode, responseBody, error, listener);
            }
        });
    }

    private static void handleListResponse(byte[] responseBody,
                                           final OnMoviesLoadedListener listener) {
        try {
            String jsonString = new String(responseBody);
            Log.d(TAG, "API Response: " + jsonString.substring(0, Math.min(500, jsonString.length())) + "...");

            List<Movie> movies = parseMoviesResponse(jsonString);
            if (movies.isEmpty()) {
                notifyError(listener, "No movies found in response");
            } else {
                Log.d(TAG, "Successfully parsed " + movies.size() + " movies");
                notifySuccess(listener, movies);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON Parsing Error", e);
            notifyError(listener, "Invalid data format from server: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected Error", e);
            notifyError(listener, "Failed to process response: " + e.getMessage());
        }
    }

    private static void handleSingleResponse(byte[] responseBody,
                                             final OnMovieLoadedListener listener) {
        try {
            String jsonString = new String(responseBody);
            Log.d(TAG, "API Response: " + jsonString.substring(0, Math.min(500, jsonString.length())) + "...");

            Movie movie = parseMovieDetailsResponse(jsonString);
            if (movie == null) {
                notifyError(listener, "No movie details found");
            } else {
                Log.d(TAG, "Successfully parsed movie: " + movie.getTitle());
                notifySuccess(listener, movie);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON Parsing Error", e);
            notifyError(listener, "Invalid movie data format: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected Error", e);
            notifyError(listener, "Failed to process movie details: " + e.getMessage());
        }
    }

    private static void handleListFailure(int statusCode, byte[] responseBody,
                                          Throwable error, final OnMoviesLoadedListener listener) {
        String errorDetails = buildErrorDetails(statusCode, responseBody, error);
        Log.e(TAG, "API Request Failed: " + errorDetails);
        notifyError(listener, "Search request failed: " + errorDetails);
    }

    private static void handleSingleFailure(int statusCode, byte[] responseBody,
                                            Throwable error, final OnMovieLoadedListener listener) {
        String errorDetails = buildErrorDetails(statusCode, responseBody, error);
        Log.e(TAG, "API Request Failed: " + errorDetails);
        notifyError(listener, "Request failed: " + errorDetails);
    }

    private static String buildErrorDetails(int statusCode, byte[] responseBody, Throwable error) {
        StringBuilder sb = new StringBuilder("Status: ").append(statusCode);

        if (error != null) {
            sb.append(", Error: ").append(error.getMessage());
            // Log full stack trace for better debugging
            Log.e(TAG, "API Error stack trace:", error);
        }

        if (responseBody != null && responseBody.length > 0) {
            try {
                String response = new String(responseBody);
                sb.append(", Response: ").append(response);
                Log.e(TAG, "API Error Response Body: " + response);
            } catch (Exception e) {
                sb.append(", [Could not parse response body]");
            }
        }

        return sb.toString();
    }

    private static List<Movie> parseMoviesResponse(String jsonResponse) throws JSONException {
        List<Movie> movies = new ArrayList<>();
        Log.d(TAG, "Parsing response: " + jsonResponse.substring(0, Math.min(200, jsonResponse.length())) + "...");

        // First try to parse as direct array
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            Log.d(TAG, "Found direct JSON array with " + jsonArray.length() + " items");
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject item = jsonArray.getJSONObject(i);
                    Movie movie = parseMovieFromJson(item);
                    if (isValidMovie(movie)) {
                        movies.add(movie);
                        Log.d(TAG, "Added movie from array: " + movie.getTitle());
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

        // Try top250 endpoint format which often has items array
        JSONArray items = responseObj.optJSONArray("items");
        if (items != null) {
            Log.d(TAG, "Found top250 format with 'items' array of size: " + items.length());
            for (int i = 0; i < items.length(); i++) {
                try {
                    JSONObject item = items.getJSONObject(i);
                    Movie movie = parseMovieFromJson(item);
                    if (isValidMovie(movie)) {
                        movies.add(movie);
                        Log.d(TAG, "Added movie from items: " + movie.getTitle());
                    }
                } catch (JSONException e) {
                    Log.w(TAG, "Skipping invalid movie item at position " + i, e);
                }
            }
            return movies;
        }

        // Try results array which is common in search responses
        JSONArray results = responseObj.optJSONArray("results");
        if (results != null) {
            Log.d(TAG, "Found search format with 'results' array of size: " + results.length());
            for (int i = 0; i < results.length(); i++) {
                try {
                    JSONObject item = results.getJSONObject(i);
                    Movie movie = parseMovieFromJson(item);
                    if (isValidMovie(movie)) {
                        movies.add(movie);
                        Log.d(TAG, "Added movie from results: " + movie.getTitle());
                    }
                } catch (JSONException e) {
                    Log.w(TAG, "Skipping invalid movie item at position " + i, e);
                }
            }
            return movies;
        }

        // Try auto-complete endpoint format which has a 'd' property containing results
        JSONArray dResults = responseObj.optJSONArray("d");
        if (dResults != null) {
            Log.d(TAG, "Found auto-complete format with 'd' array of size: " + dResults.length());
            for (int i = 0; i < dResults.length(); i++) {
                try {
                    JSONObject item = dResults.getJSONObject(i);
                    Movie movie = parseMovieFromAutoCompleteJson(item);
                    if (isValidMovie(movie)) {
                        movies.add(movie);
                        Log.d(TAG, "Added movie from auto-complete: " + movie.getTitle());
                    }
                } catch (JSONException e) {
                    Log.w(TAG, "Error parsing auto-complete item at position " + i, e);
                }
            }
            return movies;
        }

        // Try to parse as single movie if no arrays were found
        try {
            Movie movie = parseMovieFromJson(responseObj);
            if (isValidMovie(movie)) {
                movies.add(movie);
                Log.d(TAG, "Added single movie: " + movie.getTitle());
            }
        } catch (JSONException e) {
            Log.w(TAG, "Could not parse response as single movie", e);
        }

        Log.d(TAG, "Finished parsing, found " + movies.size() + " movies");
        return movies;
    }

    private static Movie parseMovieFromAutoCompleteJson(JSONObject item) throws JSONException {
        Movie movie = new Movie();
        Log.d(TAG, "Parsing auto-complete movie item: " + item.toString());

        // Handle auto-complete format specific fields
        String id = item.optString("id", "");
        // Auto-complete IDs often have a prefix like "tt"
        movie.setImdbID(id);

        String title = item.optString("l", ""); // 'l' is title in auto-complete format
        movie.setTitle(title);

        // Year might be in 'y' field
        String year = item.optString("y", "N/A");
        movie.setYear(year);

        // Image might be in 'i' object with 'imageUrl' field
        JSONObject imageObj = item.optJSONObject("i");
        if (imageObj != null && imageObj.has("imageUrl")) {
            String imageUrl = imageObj.optString("imageUrl", "");
            movie.setPoster(imageUrl);
        }

        // Type information might be in 'q' field
        String type = item.optString("q", "");
        if (!type.isEmpty()) {
            movie.setGenre(type); // Store type info in genre for now
        }

        // Additional information like actors might be in 's' field
        String stars = item.optString("s", "");
        movie.setActors(stars);

        Log.d(TAG, "Parsed auto-complete movie: " + movie.getTitle() + " (" + movie.getYear() + ")");
        return movie;
    }

    private static Movie parseMovieFromJson(JSONObject item) throws JSONException {
        Movie movie = new Movie();
        Log.d(TAG, "Parsing movie item: " + item.toString().substring(0, Math.min(200, item.toString().length())) + "...");

        // Handle different API response formats
        String id = item.optString("id", "");
        if (id.isEmpty()) {
            id = item.optString("imdbId", "");
        }
        movie.setImdbID(id);

        // Handle different title fields
        String title = item.optString("primaryTitle", "");
        if (title.isEmpty()) {
            title = item.optString("title", "");
        }
        if (title.isEmpty()) {
            title = item.optString("name", "Unknown Title");
        }
        if (title.isEmpty()) {
            title = item.optString("fullTitle", "");
        }
        movie.setTitle(title);

        // Handle different year fields
        String year = item.optString("startYear", "");
        if (year.isEmpty()) {
            year = item.optString("year", "N/A");
        }
        movie.setYear(year);

        // Handle different image/poster fields
        String poster = item.optString("primaryImage", "");
        if (poster.isEmpty()) {
            poster = item.optString("image", "");
        }
        if (poster.isEmpty()) {
            poster = item.optString("posterUrl", "");
        }
        if (poster.isEmpty()) {
            poster = item.optString("poster", "");
        }
        movie.setPoster(poster);

        // Handle different rating fields
        String rating = item.optString("averageRating", "");
        if (rating.isEmpty()) {
            rating = item.optString("imDbRating", "0.0");
        }
        movie.setImdbRating(rating);

        // Handle different description/plot fields
        String plot = item.optString("description", "");
        if (plot.isEmpty()) {
            plot = item.optString("plot", "No plot available");
        }
        movie.setPlot(plot);

        // Optional fields
        movie.setGenre(item.optString("genres", ""));
        movie.setRuntime(item.optString("runtimeMinutes", "N/A") + " mins");
        movie.setReleased(item.optString("releaseDate", ""));

        Log.d(TAG, "Parsed movie: " + movie.getTitle() + " (" + movie.getYear() + ")");
        return movie;
    }

    private static Movie parseMovieDetailsResponse(String jsonResponse) throws JSONException {
        JSONObject response = new JSONObject(jsonResponse);
        Movie movie = parseMovieFromJson(response);

        Log.d(TAG, "Parsing movie details response: " + jsonResponse.substring(0, Math.min(200, jsonResponse.length())) + "...");

        // Additional details specific to movie details response
        movie.setBackdropUrl(response.optString("backdrop", ""));
        movie.setDirector(response.optString("directors", "Unknown"));
        movie.setActors(response.optString("stars", ""));

        Log.d(TAG, "Parsed movie details: " + movie.getTitle() + " directed by " + movie.getDirector());
        return movie;
    }

    private static boolean isValidMovie(Movie movie) {
        boolean isValid = movie.getImdbID() != null && !movie.getImdbID().isEmpty() &&
                movie.getTitle() != null && !movie.getTitle().isEmpty();

        if (!isValid) {
            Log.w(TAG, "Invalid movie object detected - missing ID or title");
        }
        return isValid;
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