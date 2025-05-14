package com.example.lumew.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.example.lumew.R;
import com.example.lumew.utils.FirebaseUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private static final String DARK_MODE_PREF = "dark_mode";
    private static final String NOTIFICATIONS_PREF = "notifications_enabled";

    private Switch darkModeSwitch;
    private Switch notificationsSwitch;
    private LinearLayout accountSettingsLayout;
    private LinearLayout logoutLayout;
    private LinearLayout deleteAccountLayout;

    private FirebaseAuth firebaseAuth;
    private SharedPreferences sharedPreferences;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeComponents();
        setupUI();
        setupListeners();
    }

    private void initializeComponents() {
        firebaseAuth = FirebaseAuth.getInstance();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        darkModeSwitch = findViewById(R.id.dark_mode_switch);
        notificationsSwitch = findViewById(R.id.notifications_switch);
        accountSettingsLayout = findViewById(R.id.account_settings_layout);
        logoutLayout = findViewById(R.id.logout_layout);
        deleteAccountLayout = findViewById(R.id.delete_account_layout);

        TextView versionTextView = findViewById(R.id.version_text);
        versionTextView.setText(getString(R.string.version_format, getAppVersion()));

        ImageButton backButton = findViewById(R.id.settings_back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupUI() {
        darkModeSwitch.setChecked(sharedPreferences.getBoolean(DARK_MODE_PREF, false));
        notificationsSwitch.setChecked(sharedPreferences.getBoolean(NOTIFICATIONS_PREF, true));

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        boolean isLoggedIn = currentUser != null;
        accountSettingsLayout.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        logoutLayout.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        deleteAccountLayout.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
    }

    private void setupListeners() {
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(DARK_MODE_PREF, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(NOTIFICATIONS_PREF, isChecked).apply();
        });

        accountSettingsLayout.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        logoutLayout.setOnClickListener(v -> showConfirmationDialog(
                getString(R.string.logout_title),
                getString(R.string.logout_message),
                this::performLogout
        ));

        deleteAccountLayout.setOnClickListener(v -> showConfirmationDialog(
                getString(R.string.delete_account_title),
                getString(R.string.delete_account_message),
                this::initiateAccountDeletion
        ));

        findViewById(R.id.privacy_policy_layout).setOnClickListener(v ->
                showInfoDialog(getString(R.string.privacy_policy_title), getString(R.string.privacy_policy_text)));

        findViewById(R.id.terms_of_service_layout).setOnClickListener(v ->
                showInfoDialog(getString(R.string.terms_of_service_title), getString(R.string.terms_of_service_text)));

        findViewById(R.id.about_layout).setOnClickListener(v ->
                showInfoDialog(getString(R.string.about_title), getString(R.string.about_text)));
    }

    private void initiateAccountDeletion() {
        showConfirmationDialog(
                getString(R.string.delete_account_title),
                getString(R.string.delete_account_confirmation),
                this::performAccountDeletion
        );
    }

    private void performAccountDeletion() {
        showProgressDialog(getString(R.string.deleting_account));

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            hideProgressDialog();
            showError(R.string.no_user_logged_in);
            return;
        }

        FirebaseUtil.getInstance().deleteUserData(currentUser.getUid(), new FirebaseUtil.ResultCallback() {
            @Override
            public void onSuccess() {
                // After data is deleted, delete the auth account
                FirebaseUtil.getInstance().deleteUserAccount(new FirebaseUtil.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        hideProgressDialog();
                        Toast.makeText(SettingsActivity.this,
                                R.string.account_deleted_success,
                                Toast.LENGTH_SHORT).show();
                        restartApp();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        hideProgressDialog();
                        showError(R.string.account_deletion_failed);
                        Log.e("AccountDeletion", "Failed to delete auth account", e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                hideProgressDialog();
                showError(R.string.data_deletion_failed);
                Log.e("AccountDeletion", "Failed to delete user data", e);
            }
        });
    }

    private void performLogout() {
        FirebaseUtil.getInstance().signOut();
        Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
        restartApp();
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showConfirmationDialog(String title, String message, Runnable positiveAction) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> positiveAction.run())
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showError(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}