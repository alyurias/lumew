package com.example.lumew.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.lumew.R;
import com.example.lumew.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView profileImageView;
    private EditText usernameEditText, bioEditText;
    private Button updateProfileBtn, logoutBtn;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private FloatingActionButton settingsFab;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageReference;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("profile_images");

        // Initialize views
        initializeViews();

        // Set up toolbar
        setupToolbar();

        // Load user data
        loadUserData();

        // Set click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.profile_image);
        usernameEditText = findViewById(R.id.username_edit);
        bioEditText = findViewById(R.id.bio_edit);
        updateProfileBtn = findViewById(R.id.update_profile_btn);
        logoutBtn = findViewById(R.id.logout_btn);
        progressBar = findViewById(R.id.profile_progress_bar);
        settingsFab = findViewById(R.id.fab_settings);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupClickListeners() {
        profileImageView.setOnClickListener(v -> openFileChooser());
        updateProfileBtn.setOnClickListener(v -> updateProfile());
        logoutBtn.setOnClickListener(v -> logout());
        settingsFab.setOnClickListener(v -> openSettings());
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            progressBar.setVisibility(View.VISIBLE);

            db.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                User userProfile = document.toObject(User.class);
                                if (userProfile != null) {
                                    populateUserData(userProfile);
                                }
                            } else {
                                createNewUserDocument(user);
                            }
                        } else {
                            showError("Failed to load profile", task.getException());
                        }
                    });
        }
    }

    private void populateUserData(User userProfile) {
        usernameEditText.setText(userProfile.getUsername());
        bioEditText.setText(userProfile.getBio());

        if (userProfile.getProfileImageUrl() != null && !userProfile.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(userProfile.getProfileImageUrl())
                    .placeholder(R.drawable.default_profile)
                    .circleCrop()
                    .into(profileImageView);
        }
    }

    private void createNewUserDocument(FirebaseUser firebaseUser) {
        User newUser = new User(
                firebaseUser.getUid(),
                firebaseUser.getEmail(),
                firebaseUser.getDisplayName() != null ?
                        firebaseUser.getDisplayName() :
                        "User" + System.currentTimeMillis()
        );

        newUser.setBio("Hello! I'm new here.");

        if (firebaseUser.getPhotoUrl() != null) {
            newUser.setProfileImageUrl(firebaseUser.getPhotoUrl().toString());
        }

        db.collection("users").document(firebaseUser.getUid())
                .set(newUser)
                .addOnSuccessListener(aVoid -> loadUserData())
                .addOnFailureListener(e -> showError("Failed to create profile", e));
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this)
                    .load(imageUri)
                    .circleCrop()
                    .into(profileImageView);
        }
    }

    private void updateProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            showError("User not authenticated", null);
            return;
        }

        String username = usernameEditText.getText().toString().trim();
        String bio = bioEditText.getText().toString().trim();

        if (!validateProfileInput(username)) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        if (imageUri != null) {
            uploadImageAndUpdateProfile(user.getUid(), username, bio);
        } else {
            updateUserData(user.getUid(), username, bio, null);
        }
    }

    private boolean validateProfileInput(String username) {
        if (username.isEmpty()) {
            usernameEditText.setError("Username is required");
            usernameEditText.requestFocus();
            return false;
        }
        if (username.length() < 3) {
            usernameEditText.setError("Username must be at least 3 characters");
            usernameEditText.requestFocus();
            return false;
        }
        return true;
    }

    private void uploadImageAndUpdateProfile(String userId, String username, String bio) {
        StorageReference fileReference = storageReference.child(
                userId + "_" + Timestamp.now().getSeconds() + "." + getFileExtension(imageUri));

        fileReference.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateUserData(userId, username, bio, task.getResult().toString());
                    } else {
                        progressBar.setVisibility(View.GONE);
                        showError("Upload failed", task.getException());
                    }
                });
    }

    private void updateUserData(String userId, String username, String bio, String imageUrl) {
        db.collection("users").document(userId)
                .update(
                        "username", username,
                        "bio", bio,
                        "profileImageUrl", imageUrl != null ? imageUrl : FieldValue.delete(),
                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    showSuccess("Profile updated successfully");
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showError("Update failed", e);
                });
    }

    private String getFileExtension(Uri uri) {
        return getContentResolver().getType(uri).split("/")[1];
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, AuthActivity.class));
        finishAffinity();
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void showError(String message, Exception e) {
        String error = e != null ? e.getMessage() : "";
        Toast.makeText(this, message + (error.isEmpty() ? "" : ": " + error),
                Toast.LENGTH_LONG).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}