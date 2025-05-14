package com.example.lumew.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

@IgnoreExtraProperties
public class User {
    private String userId;
    private String email;
    private String username;
    private String displayName;
    private String bio;
    private String profileImageUrl;

    @ServerTimestamp
    private Timestamp createdAt;

    private Timestamp lastUpdated;

    // Required empty constructor for Firestore
    public User() {}

    // Constructor for new users
    public User(String userId, String email, String username) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        this.userId = userId;
        this.email = email;
        this.username = username != null ? username : "User" + System.currentTimeMillis();
        this.displayName = this.username;
        this.bio = "Hello! I'm new here.";
        this.createdAt = Timestamp.now();
        this.lastUpdated = this.createdAt;
    }

    // Getters and setters with validation
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName != null ? displayName : this.username;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio != null ? bio : "";
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt != null ? createdAt : Timestamp.now();
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt != null ? createdAt : Timestamp.now();
    }

    public Timestamp getLastUpdated() {
        return lastUpdated != null ? lastUpdated : Timestamp.now();
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated != null ? lastUpdated : Timestamp.now();
    }

    @Exclude
    public long getCreatedAtMillis() {
        return getCreatedAt().toDate().getTime();
    }

    @Exclude
    public long getLastUpdatedMillis() {
        return getLastUpdated().toDate().getTime();
    }
}