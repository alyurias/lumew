package com.example.lumew.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lumew.R;
import com.example.lumew.models.User;
import com.example.lumew.utils.FirebaseUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private EditText displayNameEditText;
    private EditText usernameEditText;
    private EditText bioEditText;
    private Button saveButton;
    private ImageButton backButton;
    private ProgressBar progressBar;

    private FirebaseUser currentUser;
    private User userProfile;
    private FirebaseUtil firebaseUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseUtil = FirebaseUtil.getInstance();

        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        loadUserProfile();
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.edit_profile_image);
        displayNameEditText = findViewById(R.id.edit_display_name);
        usernameEditText = findViewById(R.id.edit_username);
        bioEditText = findViewById(R.id.edit_bio);
        saveButton = findViewById(R.id.btn_save_profile);
        backButton = findViewById(R.id.edit_profile_back_button);
        progressBar = findViewById(R.id.edit_profile_progress);

        // Set default profile image
        profileImageView.setImageResource(R.drawable.default_profile);
    }

    private void setupClickListeners() {
        profileImageView.setOnClickListener(v -> {
            // Optional: Can remove or keep for future implementation
            Toast.makeText(this, "Profile image upload is not available", Toast.LENGTH_SHORT).show();
        });

        saveButton.setOnClickListener(v -> saveProfile());
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void loadUserProfile() {
        showLoading(true);

        // Use getUserProfile method from FirebaseUtil instance
        firebaseUtil.getUserProfile(currentUser.getUid(), new FirebaseUtil.UserProfileCallback() {
            @Override
            public void onSuccess(User user) {
                showLoading(false);
                userProfile = user;
                displayUserProfile();
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(EditProfileActivity.this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                createNewUserProfile();
            }
        });
    }

    private void createNewUserProfile() {
        userProfile = new User();
        userProfile.setUserId(currentUser.getUid());
        userProfile.setEmail(currentUser.getEmail());

        // Pre-fill with display name from Firebase Auth if available
        if (currentUser.getDisplayName() != null) {
            userProfile.setDisplayName(currentUser.getDisplayName());
        }

        displayUserProfile();
    }

    private void displayUserProfile() {
        if (userProfile.getDisplayName() != null) {
            displayNameEditText.setText(userProfile.getDisplayName());
        }

        if (userProfile.getUsername() != null) {
            usernameEditText.setText(userProfile.getUsername());
        }

        if (userProfile.getBio() != null) {
            bioEditText.setText(userProfile.getBio());
        }
    }

    private void saveProfile() {
        String displayName = displayNameEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String bio = bioEditText.getText().toString().trim();

        // Validate input
        if (displayName.isEmpty()) {
            displayNameEditText.setError("Display name is required");
            return;
        }

        if (username.isEmpty()) {
            usernameEditText.setError("Username is required");
            return;
        }

        // Update user profile object
        userProfile.setDisplayName(displayName);
        userProfile.setUsername(username);
        userProfile.setBio(bio);

        showLoading(true);

        // Use updateUserProfile method from FirebaseUtil instance
        firebaseUtil.updateUserProfile(userProfile, new FirebaseUtil.ResultCallback() {
            @Override
            public void onSuccess() {
                showLoading(false);
                Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(EditProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!isLoading);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}