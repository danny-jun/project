package com.example.emergencysystem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// EmergencyReport Model Class
public class EmergencyReport {
    private String id;
    private String userId;
    private String userName;
    private String userRegNo;
    private String emergencyType;
    private String description;
    private double latitude;
    private double longitude;
    private List<String> imageUrls;
    private List<String> videoUrls; // ADDED: For video uploads
    private String severity;
    private String status;
    private boolean isPanicAlert;
    private String timestamp;
    private String respondedAt;
    private String completedAt;
    private String responderName;
    private String notes;

    // Required empty constructor for Firebase
    public EmergencyReport() {}

    // Constructor
    public EmergencyReport(String userId, String userName, String emergencyType,
                           String description, double latitude, double longitude,
                           String severity, boolean isPanicAlert) {
        this.userId = userId;
        this.userName = userName;
        this.emergencyType = emergencyType;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.severity = severity;
        this.isPanicAlert = isPanicAlert;
        this.status = "pending";
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserRegNo() { return userRegNo; }
    public void setUserRegNo(String userRegNo) { this.userRegNo = userRegNo; }

    public String getEmergencyType() { return emergencyType; }
    public void setEmergencyType(String emergencyType) { this.emergencyType = emergencyType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    // ADDED: Video URLs getter and setter
    public List<String> getVideoUrls() { return videoUrls; }
    public void setVideoUrls(List<String> videoUrls) { this.videoUrls = videoUrls; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPanicAlert() { return isPanicAlert; }
    public void setPanicAlert(boolean panicAlert) { isPanicAlert = panicAlert; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getRespondedAt() { return respondedAt; }
    public void setRespondedAt(String respondedAt) { this.respondedAt = respondedAt; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public String getResponderName() { return responderName; }
    public void setResponderName(String responderName) { this.responderName = responderName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // Helper methods
    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return timestamp;
        }
    }

    public String getStatusEmoji() {
        switch (status) {
            case "pending": return "⏳";
            case "responded": return "🚑";
            case "completed": return "✅";
            default: return "📋";
        }
    }

    public String getSeverityColor() {
        switch (severity) {
            case "Low": return "#4CAF50";
            case "Medium": return "#FF9800";
            case "High": return "#F44336";
            case "Critical": return "#D32F2F";
            default: return "#757575";
        }
    }

    // ADDED: Helper method to check if report has media (images or videos)
    public boolean hasMedia() {
        boolean hasImages = imageUrls != null && !imageUrls.isEmpty();
        boolean hasVideos = videoUrls != null && !videoUrls.isEmpty();
        return hasImages || hasVideos;
    }

    // ADDED: Helper method to get total media count
    public int getTotalMediaCount() {
        int imageCount = (imageUrls != null) ? imageUrls.size() : 0;
        int videoCount = (videoUrls != null) ? videoUrls.size() : 0;
        return imageCount + videoCount;
    }

    // ADDED: Helper method to check if report has videos
    public boolean hasVideos() {
        return videoUrls != null && !videoUrls.isEmpty();
    }

    // ADDED: Helper method to check if report has images
    public boolean hasImages() {
        return imageUrls != null && !imageUrls.isEmpty();
    }
}