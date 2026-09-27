package com.school.service;

import com.school.model.FacilityKRT;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FacilservJRD {

    public FacilservJRD() {
    }

    private FacilityKRT mapRow(ResultSet rs) throws SQLException {
        FacilityKRT f = new FacilityKRT();
        f.setId(rs.getString("id"));
        f.setName(rs.getString("name"));
        try { f.setType(FacilityKRT.Type.valueOf(rs.getString("type"))); } catch (Exception e) { f.setType(FacilityKRT.Type.CLASSROOM); }
        f.setLocation(rs.getString("location"));
        f.setCapacity(rs.getInt("capacity"));
        f.setDescription(rs.getString("description"));
        // support both BOOLEAN (true/false, t/f) and INTEGER (1/0) schemas
        Object activeObj = rs.getObject("active");
        boolean isActive;
        if (activeObj instanceof Boolean) isActive = (Boolean) activeObj;
        else if (activeObj instanceof Number) isActive = ((Number) activeObj).intValue() != 0;
        else {
            String s = String.valueOf(activeObj).trim().toLowerCase();
            isActive = s.equals("t") || s.equals("true") || s.equals("1");
        }
        f.setActive(isActive);
        return f;
    }

    public List<FacilityKRT> getAll() {
        List<FacilityKRT> list = new ArrayList<>();
        try (Connection c = DatabaseStorage.getConnectionDrm(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM facilities")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<FacilityKRT> getActive() {
        List<FacilityKRT> list = new ArrayList<>();
        try (Connection c = DatabaseStorage.getConnectionDrm(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM facilities WHERE active = true")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<FacilityKRT> filterByType(FacilityKRT.Type type) {
        if (type == null) return getActive();
        List<FacilityKRT> list = new ArrayList<>();
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("SELECT * FROM facilities WHERE active = true AND type=?")) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Optional<FacilityKRT> findById(String id) {
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("SELECT * FROM facilities WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(mapRow(rs)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public FacilityKRT addFacility(String name, FacilityKRT.Type type, String location, int capacity, String description) {
        String id = UUID.randomUUID().toString();
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("INSERT INTO facilities (id,name,type,location,capacity,description,active) VALUES (?,?,?,?,?,?,true)")) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, type.name());
            ps.setString(4, location);
            ps.setInt(5, capacity);
            ps.setString(6, description);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        return findById(id).orElse(new FacilityKRT(id, name, type, location, capacity, description));
    }

    public boolean updateFacility(String id, String name, FacilityKRT.Type type, String location, int capacity, String description) {
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("UPDATE facilities SET name=?, type=?, location=?, capacity=?, description=? WHERE id=?")) {
            ps.setString(1, name);
            ps.setString(2, type.name());
            ps.setString(3, location);
            ps.setInt(4, capacity);
            ps.setString(5, description);
            ps.setString(6, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean setFacilityActive(String id, boolean active) {
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("UPDATE facilities SET active=? WHERE id=?")) {
            ps.setBoolean(1, active);
            ps.setString(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean enableFacility(String id) {
        return setFacilityActive(id, true);
    }

    public boolean disableFacility(String id) {
        return setFacilityActive(id, false);
    }

    public boolean deleteFacility(String id) {
        return disableFacility(id);
    }

    public boolean hasBookings(String facilityId) {
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM bookings WHERE facility_id=? LIMIT 1")) {
            ps.setString(1, facilityId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean hardDelete(String id) {
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("DELETE FROM facilities WHERE id=?")) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}
