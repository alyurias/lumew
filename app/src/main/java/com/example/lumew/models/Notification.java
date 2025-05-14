package com.example.lumew.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Notification {
    private String id;
    private String userId;
    private String title;
    private String message;
    private String imageUrl;
    private String relatedMovieId;
    private Date timestamp;
    private boolean isRead;
    private String type;
    private transient boolean isUpdating; // Won't be saved to Firebase

    // Constructors
    public Notification() {}

    public Notification(String id, String userId, String title, String message,
                        String imageUrl, String relatedMovieId, Date timestamp,
                        boolean isRead, String type) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.imageUrl = imageUrl;
        this.relatedMovieId = relatedMovieId;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.type = type;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getRelatedMovieId() { return relatedMovieId; }
    public void setRelatedMovieId(String relatedMovieId) { this.relatedMovieId = relatedMovieId; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isUpdating() { return isUpdating; }
    public void setUpdating(boolean updating) { isUpdating = updating; }

    // Update Methods
    public void prepareForUpdate() {
        this.isUpdating = true;
    }

    public void completeUpdate(boolean success, boolean newReadStatus) {
        this.isUpdating = false;
        if (success) {
            this.isRead = newReadStatus;
        }
    }

    public Map<String, Object> toUpdateMap() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isRead", isRead);
        updates.put("timestamp", timestamp != null ? timestamp : new Date());
        return updates;
    }

    // Time formatting
    public String getTimeAgo() {
        if (timestamp == null) return "Just now";

        long timeDiff = new Date().getTime() - timestamp.getTime();
        long seconds = timeDiff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + (days == 1 ? " day ago" : " days ago");
        if (hours > 0) return hours + (hours == 1 ? " hour ago" : " hours ago");
        if (minutes > 0) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        return "Just now";
    }
}