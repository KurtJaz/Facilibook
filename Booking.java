package com.school.model;

public class Booking {
    public enum Status { PENDING, APPROVED, REJECTED, CANCELLED }

    private String id;
    private String userId;
    private String userName;
    private String facilityId;
    private String facilityName;
    private String date;        // yyyy-MM-dd
    private String startTime;   // HH:mm
    private String endTime;     // HH:mm
    private String purpose;
    private Status status;

    public Booking() {}

    public Booking(String id, String userId, String userName, String facilityId, String facilityName,
                   String date, String startTime, String endTime, String purpose, Status status) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.facilityId = facilityId;
        this.facilityName = facilityName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getFacilityId() { return facilityId; }
    public void setFacilityId(String facilityId) { this.facilityId = facilityId; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
