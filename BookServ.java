package com.school.service;

import com.school.model.Booking;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BookServ {

    public BookServ() {
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setId(rs.getString("id"));
        b.setUserId(rs.getString("user_id"));
        b.setUserName(rs.getString("user_name"));
        b.setFacilityId(rs.getString("facility_id"));
        b.setFacilityName(rs.getString("facility_name"));
        b.setDate(rs.getString("date"));
        b.setStartTime(rs.getString("start_time"));
        b.setEndTime(rs.getString("end_time"));
        b.setPurpose(rs.getString("purpose"));
        try { b.setStatus(Booking.Status.valueOf(rs.getString("status"))); } catch (Exception e) { b.setStatus(Booking.Status.PENDING); }
        return b;
    }

    public List<Booking> getAll() {
        List<Booking> list = new ArrayList<>();
        try (Connection c = DatabaseStorage.getConnectionDrm(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM bookings")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Booking> getByUser(String userId) {
        List<Booking> list = new ArrayList<>();
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("SELECT * FROM bookings WHERE user_id=?")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Booking> getByFacility(String facilityId) {
        List<Booking> list = new ArrayList<>();
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("SELECT * FROM bookings WHERE facility_id=?")) {
            ps.setString(1, facilityId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Booking> getByDate(String facilityId, String date) {
        List<Booking> list = new ArrayList<>();
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("SELECT * FROM bookings WHERE facility_id=? AND date=? AND status NOT IN ('CANCELLED','REJECTED')")) {
            ps.setString(1, facilityId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Booking> getBlockingByDate(String date) {
        List<Booking> list = new ArrayList<>();
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("SELECT * FROM bookings WHERE date=? AND status NOT IN ('CANCELLED','REJECTED')")) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean isAvailable(String facilityId, String date, String startTime, String endTime) {
        LocalTime newStart = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime newEnd = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"));
        if (!newStart.isBefore(newEnd)) return false;
        for (Booking b : getByDate(facilityId, date)) {
            LocalTime bStart = LocalTime.parse(b.getStartTime());
            LocalTime bEnd = LocalTime.parse(b.getEndTime());
            if (newStart.isBefore(bEnd) && newEnd.isAfter(bStart)) return false;
        }
        return true;
    }

    public boolean isCurrentlyInUse(String facilityId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("SELECT * FROM bookings WHERE facility_id=? AND status='APPROVED' AND date=?")) {
            ps.setString(1, facilityId);
            ps.setString(2, todayStr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalTime s = LocalTime.parse(rs.getString("start_time"));
                    LocalTime e = LocalTime.parse(rs.getString("end_time"));
                    if (!now.isBefore(s) && now.isBefore(e)) return true;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public Optional<Booking> createBooking(String userId, String userName, String facilityId, String facilityName,
                                           String date, String startTime, String endTime, String purpose) {
        if (!isAvailable(facilityId, date, startTime, endTime)) return Optional.empty();
        String id = UUID.randomUUID().toString();
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("INSERT INTO bookings (id,user_id,user_name,facility_id,facility_name,date,start_time,end_time,purpose,status) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, id);
            ps.setString(2, userId);
            ps.setString(3, userName);
            ps.setString(4, facilityId);
            ps.setString(5, facilityName);
            ps.setString(6, date);
            ps.setString(7, startTime);
            ps.setString(8, endTime);
            ps.setString(9, purpose);
            ps.setString(10, Booking.Status.PENDING.name());
            ps.executeUpdate();
            Booking b = new Booking(id, userId, userName, facilityId, facilityName, date, startTime, endTime, purpose, Booking.Status.PENDING);
            return Optional.of(b);
        } catch (SQLException e) { e.printStackTrace(); return Optional.empty(); }
    }

    public boolean cancelBooking(String bookingId, String requesterId, boolean isAdmin) {
        String sql = isAdmin ? "UPDATE bookings SET status='CANCELLED' WHERE id=?" : "UPDATE bookings SET status='CANCELLED' WHERE id=? AND user_id=?";
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bookingId);
            if (!isAdmin) ps.setString(2, requesterId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateStatus(String bookingId, Booking.Status status) {
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("UPDATE bookings SET status=? WHERE id=?")) {
            ps.setString(1, status.name());
            ps.setString(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deleteBooking(String bookingId) {
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("DELETE FROM bookings WHERE id=?")) {
            ps.setString(1, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public List<String> getAvailableSlots(String facilityId, String date) {
        List<String> slots = new ArrayList<>();
        for (int h = 7; h < 20; h++) {
            String s = String.format("%02d:00", h);
            String e = String.format("%02d:00", h + 1);
            boolean avail = isAvailable(facilityId, date, s, e);
            slots.add(s + " - " + e + (avail ? " (Available)" : " (Booked)"));
        }
        return slots;
    }
}
