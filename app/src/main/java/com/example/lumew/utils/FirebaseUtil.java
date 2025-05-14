package com.example.lumew.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.lumew.models.Movie;
import com.example.lumew.models.Notification;
import com.example.lumew.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseUtil {
    private static final String TAG = "FirebaseUtil";

    // Collections
    private static final String USERS_COLLECTION = "users";
    private static final String USER_MOVIES_COLLECTION = "user_movies";
    private static final String NOTIFICATIONS_COLLECTION = "notifications";
    private static final String PROFILE_IMAGES_FOLDER = "profile_images";

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseAuth auth;

    // Singleton instance
    private static FirebaseUtil instance;

    public static synchronized FirebaseUtil getInstance() {
        if (instance == null) {
            instance = new FirebaseUtil();
        }
        return instance;
    }

    private FirebaseUtil() {
        try {
            db = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance();
            auth = FirebaseAuth.getInstance();
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    // ==================== NETWORK METHODS ====================

    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        } catch (Exception e) {
            Log.e(TAG, "Network check failed", e);
            return false;
        }
    }

// ==================== AUTHENTICATION METHODS ====================

    public static boolean isLoggedIn() {
        try {
            return FirebaseAuth.getInstance().getCurrentUser() != null;
        } catch (Exception e) {
            Log.e(TAG, "isLoggedIn check failed", e);
            return false;
        }
    }

    public static String getCurrentUserId() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            return user != null ? user.getUid() : null;
        } catch (Exception e) {
            Log.e(TAG, "getCurrentUserId failed", e);
            return null;
        }
    }

    public FirebaseUser getCurrentFirebaseUser() {
        try {
            return auth.getCurrentUser();
        } catch (Exception e) {
            Log.e(TAG, "getCurrentFirebaseUser failed", e);
            return null;
        }
    }

    public void loginUser(String email, String password, AuthResultCallback callback) {
        if (email == null || password == null) {
            callback.onComplete(Tasks.forException(new IllegalArgumentException("Email and password cannot be null")));
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Login successful for: " + email);
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null && !user.isEmailVerified()) {
                            Log.w(TAG, "Email not verified for user: " + user.getEmail());
                        }
                    }
                    callback.onComplete(task);
                });
    }

    public void registerUser(String email, String password, AuthResultCallback callback) {
        if (email == null || password == null) {
            callback.onComplete(Tasks.forException(new IllegalArgumentException("Email and password cannot be null")));
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Registration successful for: " + email);
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            Log.d(TAG, "Verification email sent");
                                        }
                                    });
                        }
                    }
                    callback.onComplete(task);
                });
    }


    public void sendPasswordResetEmail(String email, ResultCallback callback) {
        if (email == null) {
            callback.onFailure(new IllegalArgumentException("Email cannot be null"));
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent to: " + email);
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void signOut() {
        try {
            auth.signOut();
            Log.d(TAG, "User signed out successfully");
        } catch (Exception e) {
            Log.e(TAG, "Sign out failed", e);
        }
    }

    public void deleteUserAccount(ResultCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(new Exception("No user logged in"));
            return;
        }

        user.delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User account deleted");
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    // ==================== USER PROFILE METHODS ====================

    public void createUserProfile(User user, ResultCallback callback) {
        if (user == null || user.getUserId() == null) {
            callback.onFailure(new IllegalArgumentException("Invalid user data"));
            return;
        }

        // Remove attempt to set FieldValue.serverTimestamp() in the user object
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUserId());
        userData.put("email", user.getEmail());
        userData.put("username", user.getUsername());
        userData.put("displayName", user.getDisplayName());
        userData.put("bio", user.getBio());
        userData.put("profileImageUrl", user.getProfileImageUrl());
        userData.put("createdAt", FieldValue.serverTimestamp());  // Set timestamp correctly in Firestore
        userData.put("lastUpdated", FieldValue.serverTimestamp());

        db.collection(USERS_COLLECTION).document(user.getUserId())
                .set(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile created for: " + user.getUserId());
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void getUserProfile(String userId, UserProfileCallback callback) {
        if (userId == null) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be null"));
            return;
        }

        db.collection(USERS_COLLECTION).document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                user.setUserId(document.getId());
                                if (user.getCreatedAt() == null) {
                                    user.setCreatedAt(document.getTimestamp("createdAt"));
                                }
                                if (user.getLastUpdated() == null) {
                                    user.setLastUpdated(document.getTimestamp("lastUpdated"));
                                }
                                callback.onSuccess(user);
                                return;
                            }
                        }
                        callback.onFailure(new Exception("User not found"));
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void updateUserProfile(User user, ResultCallback callback) {
        if (user == null || user.getUserId() == null) {
            callback.onFailure(new IllegalArgumentException("Invalid user data"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", user.getUsername());
        updates.put("displayName", user.getDisplayName());
        updates.put("bio", user.getBio());
        updates.put("profileImageUrl", user.getProfileImageUrl());
        updates.put("lastUpdated", FieldValue.serverTimestamp());

        db.collection(USERS_COLLECTION).document(user.getUserId())
                .update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated for: " + user.getUserId());
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void uploadProfileImage(String userId, Uri imageUri, ProfileImageCallback callback) {
        if (imageUri == null) {
            callback.onFailure(new IllegalArgumentException("Image URI cannot be null"));
            return;
        }

        String imageName = UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storage.getReference()
                .child(PROFILE_IMAGES_FOLDER)
                .child(userId)
                .child(imageName);

        UploadTask uploadTask = imageRef.putFile(imageUri);

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return imageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                Log.d(TAG, "Profile image uploaded for user: " + userId);
                callback.onSuccess(downloadUri.toString());
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    // ==================== USER DATA MANAGEMENT ====================

    public void deleteUserData(String userId, ResultCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        DocumentReference userDoc = db.collection(USERS_COLLECTION).document(userId);
        DocumentReference userMoviesDoc = db.collection(USER_MOVIES_COLLECTION).document(userId);

        StorageReference userProfileImagesRef = storage.getReference()
                .child(PROFILE_IMAGES_FOLDER)
                .child(userId);

        WriteBatch batch = db.batch();
        batch.delete(userDoc);
        batch.delete(userMoviesDoc);

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                userProfileImagesRef.listAll()
                        .addOnSuccessListener(listResult -> {
                            List<StorageReference> items = listResult.getItems();
                            if (items.isEmpty()) {
                                callback.onSuccess();
                                return;
                            }

                            int[] deletedCount = {0};
                            for (StorageReference item : items) {
                                item.delete().addOnCompleteListener(deleteTask -> {
                                    deletedCount[0]++;
                                    if (deletedCount[0] == items.size()) {
                                        Log.d(TAG, "All user data deleted for: " + userId);
                                        callback.onSuccess();
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.w(TAG, "Failed to delete profile image: " + item.getName(), e);
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Failed to list user profile images", e);
                            callback.onFailure(e);
                        });
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    // ==================== MOVIE LIBRARY METHODS ====================

    /**
     * Static convenience method to add a movie to the current user's library
     *
     * @param movie The movie to add to the library
     * @param onSuccess Callback when operation succeeds
     * @param onFailure Callback when operation fails
     */
    public static void addMovieToLibrary(Movie movie, Runnable onSuccess, OnFailureCallback onFailure) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (onFailure != null) {
                onFailure.onFailure(new IllegalStateException("User not logged in"));
            }
            return;
        }

        getInstance().addMovieToLibrary(userId, movie, new ResultCallback() {
            @Override
            public void onSuccess() {
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (onFailure != null) {
                    onFailure.onFailure(e);
                }
            }
        });
    }

    /**
     * Add a movie to a user's library
     *
     * @param userId The user ID to add the movie to
     * @param movie The movie to add
     * @param callback Callback for result
     */
    public void addMovieToLibrary(String userId, Movie movie, ResultCallback callback) {
        if (userId == null || movie == null || movie.getImdbID() == null) {
            callback.onFailure(new IllegalArgumentException("User ID and Movie details cannot be null"));
            return;
        }

        Map<String, Object> movieData = new HashMap<>();
        movieData.put("imdbID", movie.getImdbID());
        movieData.put("title", movie.getTitle());
        movieData.put("year", movie.getYear());
        movieData.put("poster", movie.getPoster());
        movieData.put("addedAt", FieldValue.serverTimestamp());

        // Add more fields if needed
        if (movie.getImdbRating() != null) {
            movieData.put("imdbRating", movie.getImdbRating());
        }
        if (movie.getDirector() != null) {
            movieData.put("director", movie.getDirector());
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("movies." + movie.getImdbID(), movieData);

        // Use set with merge to ensure the document is created if it doesn't exist
        db.collection(USER_MOVIES_COLLECTION).document(userId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Movie added to library: " + movie.getTitle());
                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "Failed to add movie to library", task.getException());
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void getUserMovies(String userId, MovieListCallback callback) {
        if (userId == null) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be null"));
            return;
        }

        db.collection(USER_MOVIES_COLLECTION).document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        List<Movie> movies = new ArrayList<>();

                        if (document.exists()) {
                            Map<String, Object> moviesMap = (Map<String, Object>) document.get("movies");
                            if (moviesMap != null) {
                                for (Map.Entry<String, Object> entry : moviesMap.entrySet()) {
                                    Map<String, Object> movieData = (Map<String, Object>) entry.getValue();
                                    Movie movie = new Movie(
                                            (String) movieData.get("imdbID"),
                                            (String) movieData.get("title"),
                                            (String) movieData.get("year"),
                                            (String) movieData.get("poster")
                                    );
                                    // Add additional fields if needed
                                    movies.add(movie);
                                }
                            }
                        }
                        callback.onSuccess(movies);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void removeMovieFromLibrary(String userId, String imdbId, ResultCallback callback) {
        if (userId == null || imdbId == null) {
            callback.onFailure(new IllegalArgumentException("User ID and Movie ID cannot be null"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("movies." + imdbId, FieldValue.delete());

        db.collection(USER_MOVIES_COLLECTION).document(userId)
                .update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Movie removed from library: " + imdbId);
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void getMovieFromLibrary(String userId, String imdbId, MovieListCallback callback) {
        if (userId == null || imdbId == null) {
            callback.onFailure(new IllegalArgumentException("User ID and Movie ID cannot be null"));
            return;
        }

        db.collection(USER_MOVIES_COLLECTION).document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        List<Movie> movies = new ArrayList<>();

                        if (document.exists()) {
                            Map<String, Object> moviesMap = (Map<String, Object>) document.get("movies");
                            if (moviesMap != null && moviesMap.containsKey(imdbId)) {
                                Map<String, Object> movieData = (Map<String, Object>) moviesMap.get(imdbId);
                                Movie movie = new Movie(
                                        (String) movieData.get("imdbID"),
                                        (String) movieData.get("title"),
                                        (String) movieData.get("year"),
                                        (String) movieData.get("poster")
                                );
                                // Add additional fields if needed
                                movies.add(movie);
                            }
                        }
                        callback.onSuccess(movies);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    // ==================== NOTIFICATION METHODS ====================

    public void getNotifications(String userId, NotificationListCallback callback) {
        if (userId == null) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be null"));
            return;
        }

        db.collection(NOTIFICATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Notification> notifications = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Notification notification = document.toObject(Notification.class);
                            notification.setId(document.getId());
                            notifications.add(notification);
                        }
                        Log.d(TAG, "Retrieved " + notifications.size() + " notifications");
                        callback.onSuccess(notifications);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void createNotification(Notification notification, ResultCallback callback) {
        if (notification == null || notification.getUserId() == null) {
            callback.onFailure(new IllegalArgumentException("Invalid notification data"));
            return;
        }

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("userId", notification.getUserId());
        notificationData.put("title", notification.getTitle());
        notificationData.put("message", notification.getMessage());
        notificationData.put("imageUrl", notification.getImageUrl());
        notificationData.put("relatedMovieId", notification.getRelatedMovieId());
        notificationData.put("timestamp", notification.getTimestamp() != null ? notification.getTimestamp() : FieldValue.serverTimestamp());
        notificationData.put("read", notification.isRead());
        notificationData.put("type", notification.getType());

        db.collection(NOTIFICATIONS_COLLECTION)
                .add(notificationData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Notification created successfully");
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void markNotificationAsRead(String notificationId, ResultCallback callback) {
        if (notificationId == null) {
            callback.onFailure(new IllegalArgumentException("Notification ID cannot be null"));
            return;
        }

        db.collection(NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .update("read", true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Notification marked as read: " + notificationId);
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void toggleNotificationReadStatus(String notificationId, boolean isRead, ResultCallback callback) {
        if (notificationId == null) {
            callback.onFailure(new IllegalArgumentException("Notification ID cannot be null"));
            return;
        }

        db.collection(NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .update("read", !isRead)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Notification read status toggled: " + notificationId);
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void markAllNotificationsAsRead(String userId, ResultCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        db.collection(NOTIFICATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            callback.onSuccess();
                            return;
                        }

                        WriteBatch batch = db.batch();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            batch.update(document.getReference(), "read", true);
                        }

                        batch.commit()
                                .addOnCompleteListener(batchTask -> {
                                    if (batchTask.isSuccessful()) {
                                        Log.d(TAG, "Marked all notifications as read for user: " + userId);
                                        callback.onSuccess();
                                    } else {
                                        callback.onFailure(batchTask.getException());
                                    }
                                });
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void deleteNotification(String notificationId, ResultCallback callback) {
        if (notificationId == null) {
            callback.onFailure(new IllegalArgumentException("Notification ID cannot be null"));
            return;
        }

        db.collection(NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Notification deleted: " + notificationId);
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    // ==================== CALLBACK INTERFACES ====================
    public interface AuthResultCallback {
        void onComplete(Task<AuthResult> task);
    }

    public interface ResultCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnFailureCallback {
        void onFailure(Exception e);
    }

    public interface UserProfileCallback {
        void onSuccess(User user);
        void onFailure(Exception e);
    }

    public interface ProfileImageCallback {
        void onSuccess(String imageUrl);
        void onFailure(Exception e);
    }

    public interface NotificationListCallback {
        void onSuccess(List<Notification> notifications);
        void onFailure(Exception e);
    }

    public interface MovieListCallback {
        void onSuccess(List<Movie> movies);
        void onFailure(Exception e);
    }

    // Add this new interface for the specific movie callback
    public interface MovieCallback {
        void onSuccess(Movie movie);
        void onFailure(Exception e);
    }
}