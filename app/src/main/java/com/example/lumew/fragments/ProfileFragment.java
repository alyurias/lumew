package com.example.lumew.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.lumew.R;
import com.example.lumew.activities.AuthActivity;
import com.example.lumew.activities.EditProfileActivity;
import com.example.lumew.activities.SettingsActivity;
import com.example.lumew.adapters.LibraryAdapter;
import com.example.lumew.models.Movie;
import com.example.lumew.models.User;
import com.example.lumew.utils.FirebaseUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int REQUEST_EDIT_PROFILE = 100;
    private static final String USER_MOVIES_COLLECTION = "user_movies";

    // Views
    private ImageView profileImage;
    private TextView nameText, usernameText, bioText, movieCountText;
    private Button editProfileButton, signInButton;
    private RecyclerView libraryRecyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton settingsFab;

    // Adapter and Data
    private LibraryAdapter libraryAdapter;
    private List<Movie> libraryMovies = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupClickListeners();
        checkAuthState();
    }

    private void initializeViews(View view) {
        profileImage = view.findViewById(R.id.profile_image);
        nameText = view.findViewById(R.id.profile_name);
        usernameText = view.findViewById(R.id.profile_username);
        bioText = view.findViewById(R.id.profile_bio);
        movieCountText = view.findViewById(R.id.profile_movie_count);
        editProfileButton = view.findViewById(R.id.btn_edit_profile);
        signInButton = view.findViewById(R.id.btn_sign_in);
        libraryRecyclerView = view.findViewById(R.id.profile_library_recycler_view);
        progressBar = view.findViewById(R.id.profile_progress_bar);
        settingsFab = view.findViewById(R.id.fab_settings);
    }

    private void setupRecyclerView() {
        String userId = FirebaseUtil.getCurrentUserId();
        libraryAdapter = new LibraryAdapter(requireContext(), libraryMovies, userId);
        libraryRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        libraryRecyclerView.setAdapter(libraryAdapter);

        // Set item click listener
        libraryAdapter.setOnLibraryItemListener(new LibraryAdapter.OnLibraryItemListener() {
            @Override
            public void onMovieRemoved(Movie movie, int position) {
                libraryMovies.remove(position);
                libraryAdapter.notifyItemRemoved(position);
                updateMovieCount();
            }

            @Override
            public void onMovieClicked(Movie movie) {
                // Handle movie click if needed
            }

            @Override
            public void onRemoveError(Exception e) {
                Toast.makeText(requireContext(), "Failed to remove movie: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        editProfileButton.setOnClickListener(v ->
                startActivityForResult(
                        new Intent(getActivity(), EditProfileActivity.class),
                        REQUEST_EDIT_PROFILE));

        settingsFab.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SettingsActivity.class)));

        signInButton.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), AuthActivity.class)));
    }

    private void checkAuthState() {
        if (FirebaseUtil.isLoggedIn()) {
            showAuthenticatedUI();
            loadUserData();
        } else {
            showUnauthenticatedUI();
        }
    }

    private void showAuthenticatedUI() {
        profileImage.setVisibility(View.VISIBLE);
        nameText.setVisibility(View.VISIBLE);
        usernameText.setVisibility(View.VISIBLE);
        bioText.setVisibility(View.VISIBLE);
        movieCountText.setVisibility(View.VISIBLE);
        editProfileButton.setVisibility(View.VISIBLE);
        settingsFab.setVisibility(View.VISIBLE);
        libraryRecyclerView.setVisibility(View.VISIBLE);
        signInButton.setVisibility(View.GONE);
    }

    private void showUnauthenticatedUI() {
        profileImage.setVisibility(View.INVISIBLE);
        nameText.setVisibility(View.GONE);
        usernameText.setVisibility(View.GONE);
        bioText.setVisibility(View.GONE);
        movieCountText.setVisibility(View.GONE);
        editProfileButton.setVisibility(View.GONE);
        settingsFab.setVisibility(View.GONE);
        libraryRecyclerView.setVisibility(View.GONE);
        signInButton.setVisibility(View.VISIBLE);
    }

    private void loadUserData() {
        showLoading(true);

        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null) {
            showError("User ID not found");
            showLoading(false);
            return;
        }

        // Update adapter with current user ID
        if (libraryAdapter != null) {
            libraryAdapter.setCurrentUserId(userId);
        }

        // Load user profile from Firestore
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && isAdded()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                user.setUserId(document.getId());
                                displayUserProfile(user);
                                loadUserLibrary(userId);
                            } else {
                                showError("Failed to parse user data");
                                showLoading(false);
                            }
                        } else {
                            showError("User profile not found");
                            showLoading(false);
                        }
                    } else if (isAdded()) {
                        Log.e(TAG, "Failed to load user profile", task.getException());
                        showError("Failed to load profile");
                        showLoading(false);
                    }
                });
    }

    private void displayUserProfile(User user) {
        String displayName = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName()
                : FirebaseAuth.getInstance().getCurrentUser().getEmail();
        nameText.setText(displayName);

        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            usernameText.setText("@" + user.getUsername());
            usernameText.setVisibility(View.VISIBLE);
        } else {
            usernameText.setVisibility(View.GONE);
        }

        if (user.getBio() != null && !user.getBio().isEmpty()) {
            bioText.setText(user.getBio());
            bioText.setVisibility(View.VISIBLE);
        } else {
            bioText.setVisibility(View.GONE);
        }

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.default_profile);
        }
    }

    private void loadUserLibrary(String userId) {
        libraryMovies.clear();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(USER_MOVIES_COLLECTION).document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && isAdded()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Map<String, Object> moviesMap = (Map<String, Object>) document.get("movies");

                            if (moviesMap != null) {
                                for (Map.Entry<String, Object> entry : moviesMap.entrySet()) {
                                    Map<String, Object> movieData = (HashMap<String, Object>) entry.getValue();

                                    Movie movie = new Movie();
                                    movie.setImdbID((String) movieData.get("imdbID"));
                                    movie.setTitle((String) movieData.get("title"));
                                    movie.setYear((String) movieData.get("year"));
                                    movie.setPoster((String) movieData.get("poster"));

                                    libraryMovies.add(movie);
                                }

                                libraryAdapter.notifyDataSetChanged();
                            }
                        } else {
                            Log.d(TAG, "No movies found for user");
                        }
                    } else if (isAdded()) {
                        Log.e(TAG, "Failed to load user library", task.getException());
                        showError("Failed to load movie library");
                    }

                    updateMovieCount();
                    showLoading(false);
                });
    }

    private void updateMovieCount() {
        int count = libraryMovies.size();
        movieCountText.setText(getResources().getQuantityString(
                R.plurals.movie_count, count, count));
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        libraryRecyclerView.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_PROFILE && resultCode == getActivity().RESULT_OK) {
            checkAuthState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAuthState();
    }
}