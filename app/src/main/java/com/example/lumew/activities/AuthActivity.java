package com.example.lumew.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lumew.R;
import com.example.lumew.models.User;
import com.example.lumew.utils.FirebaseUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;

public class AuthActivity extends AppCompatActivity {

    // UI Components
    private EditText etEmail, etPassword, etRegisterEmail, etRegisterPassword, etUsername;
    private Button btnLogin, btnRegister;
    private TextView tvSwitchToRegister, tvSwitchToLogin, tvForgotPassword;
    private ProgressBar progressBar;
    private View loginLayout, registerLayout;

    private FirebaseUtil firebaseUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check auth state before showing UI
        if (FirebaseUtil.isLoggedIn()) {
            navigateToMainActivity();
            return;
        }

        setContentView(R.layout.activity_auth);
        firebaseUtil = FirebaseUtil.getInstance();

        initializeViews();
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Double-check auth state when activity resumes
        if (FirebaseUtil.isLoggedIn()) {
            navigateToMainActivity();
        }
    }

    private void initializeViews() {
        loginLayout = findViewById(R.id.login_layout);
        registerLayout = findViewById(R.id.register_layout);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etRegisterEmail = findViewById(R.id.et_register_email);
        etRegisterPassword = findViewById(R.id.et_register_password);
        etUsername = findViewById(R.id.et_username);

        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);

        tvSwitchToLogin = findViewById(R.id.tv_switch_to_login);
        tvSwitchToRegister = findViewById(R.id.tv_switch_to_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);

        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> attemptRegistration());
        tvSwitchToRegister.setOnClickListener(v -> showRegisterView());
        tvSwitchToLogin.setOnClickListener(v -> showLoginView());
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    private void showLoginView() {
        loginLayout.setVisibility(View.VISIBLE);
        registerLayout.setVisibility(View.GONE);
        clearInputFields();
    }

    private void showRegisterView() {
        loginLayout.setVisibility(View.GONE);
        registerLayout.setVisibility(View.VISIBLE);
        clearInputFields();
    }

    private void clearInputFields() {
        etEmail.setText("");
        etPassword.setText("");
        etRegisterEmail.setText("");
        etRegisterPassword.setText("");
        if (etUsername != null) etUsername.setText("");
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateLoginInput(email, password)) {
            return;
        }

        setLoadingState(true);
        firebaseUtil.loginUser(email, password, task -> {
            setLoadingState(false);
            if (task.isSuccessful()) {
                checkUserProfileExists();
            } else {
                showError(task.getException() != null ?
                        getErrorMessage(task.getException()) : "Login failed. Please try again.");
            }
        });
    }

    private void checkUserProfileExists() {
        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null) {
            showError("User authentication failed");
            return;
        }

        firebaseUtil.getUserProfile(userId, new FirebaseUtil.UserProfileCallback() {
            @Override
            public void onSuccess(User user) {
                navigateToMainActivity();
            }

            @Override
            public void onFailure(Exception e) {
                // Profile doesn't exist, create one
                FirebaseUser firebaseUser = firebaseUtil.getCurrentFirebaseUser();
                if (firebaseUser != null) {
                    createUserProfile(firebaseUser.getEmail(),
                            firebaseUser.getDisplayName() != null ?
                                    firebaseUser.getDisplayName() :
                                    "User" + System.currentTimeMillis());
                } else {
                    showError("Failed to create user profile");
                }
            }
        });
    }

    private void attemptRegistration() {
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String username = etUsername.getText().toString().trim();

        if (!validateRegistrationInput(email, password, username)) {
            return;
        }

        setLoadingState(true);
        firebaseUtil.registerUser(email, password, task -> {
            if (task.isSuccessful()) {
                createUserProfile(email, username);
            } else {
                setLoadingState(false);
                showError(task.getException() != null ?
                        getErrorMessage(task.getException()) : "Registration failed. Please try again.");
            }
        });
    }

    private void createUserProfile(String email, String username) {
        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null) {
            setLoadingState(false);
            showError("User authentication failed");
            return;
        }

        // Use the proper User constructor
        User user = new User(userId, email, username);
        user.setBio("Hello! I'm new here.");

        firebaseUtil.createUserProfile(user, new FirebaseUtil.ResultCallback() {
            @Override
            public void onSuccess() {
                setLoadingState(false);
                navigateToMainActivity();
            }

            @Override
            public void onFailure(Exception e) {
                setLoadingState(false);
                showError(e != null ?
                        getErrorMessage(e) : "Profile creation failed. Please try again.");
                // Optional: Delete the user account if profile creation fails
                firebaseUtil.deleteUserAccount(new FirebaseUtil.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        showMessage("Account creation failed. Please try again.");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        showError("Failed to clean up incomplete registration");
                    }
                });
            }
        });
    }

    private void handleForgotPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your email first");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            return;
        }

        setLoadingState(true);
        firebaseUtil.sendPasswordResetEmail(email, new FirebaseUtil.ResultCallback() {
            @Override
            public void onSuccess() {
                setLoadingState(false);
                showMessage("Password reset email sent to " + email);
            }

            @Override
            public void onFailure(Exception e) {
                setLoadingState(false);
                showError(e != null ?
                        getErrorMessage(e) : "Failed to send reset email. Please try again.");
            }
        });
    }

    private boolean validateLoginInput(String email, String password) {
        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateRegistrationInput(String email, String password, String username) {
        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            etRegisterEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegisterEmail.setError("Enter a valid email");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            etRegisterPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            etRegisterPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            isValid = false;
        } else if (username.length() < 3) {
            etUsername.setError("Username must be at least 3 characters");
            isValid = false;
        } else if (username.contains(" ")) {
            etUsername.setError("Username cannot contain spaces");
            isValid = false;
        }

        return isValid;
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        btnRegister.setEnabled(!isLoading);
        tvSwitchToLogin.setEnabled(!isLoading);
        tvSwitchToRegister.setEnabled(!isLoading);
        tvForgotPassword.setEnabled(!isLoading);
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private String getErrorMessage(Exception exception) {
        String error = exception != null ? exception.getMessage() : "Unknown error";
        if (error == null) return "An error occurred";

        // Handle common Firebase Auth errors
        if (error.contains("email address is badly formatted")) {
            return "Invalid email format";
        } else if (error.contains("password is invalid")) {
            return "Invalid password";
        } else if (error.contains("no user record")) {
            return "Account not found";
        } else if (error.contains("email address is already in use")) {
            return "Email already in use";
        } else if (error.contains("network error")) {
            return "Network error. Please check your connection";
        } else if (error.contains("WEAK_PASSWORD")) {
            return "Password is too weak";
        }
        return error;
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}