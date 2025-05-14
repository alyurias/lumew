package com.example.lumew.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lumew.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.Theme_LumeW_Splash);
        super.onCreate(savedInstanceState);



        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Class<?> destination = (currentUser == null)
                ? AuthActivity.class
                : MainActivity.class;

        startActivity(new Intent(this, destination));
        finish();

        // Uklonite sve animacije za prelaz između aktivnosti
        overridePendingTransition(0, 0);
    }
}