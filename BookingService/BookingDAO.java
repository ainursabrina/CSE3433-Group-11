package dao;

import config.DBConnection;
import model.Booking;
import java.sql.*;
import java.util.*;

public class BookingDAO {

    public List<Booking> getAll() throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookings ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        // Dapatkan nama user dari UserService
        for (Booking b : list) {
            b.setUserName(getUserName(b.getUserId()));
        }
        return list;
    }

    private String getUserName(int userId) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(
                    "http://localhost:8080/UserService/api/users/" + userId))
                .GET().build();
            java.net.http.HttpResponse<String> res = client.send(
                req, java.net.http.HttpResponse.BodyHandlers.ofString());
            String body = res.body();
            int start = body.indexOf("\"name\":\"") + 8;
            int end = body.indexOf("\"", start);
            return start > 7 ? body.substring(start, end) : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public List<Booking> getByUser(int userId) throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE user_id=? ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public int create(Booking b) throws SQLException {
        String sql = "INSERT INTO bookings (user_id, facility_id, user_name, " +
                     "facility_name, booking_date, start_time, end_time, purpose) " +
                     "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, b.getUserId());
            ps.setInt(2, b.getFacilityId());
            ps.setString(3, b.getUserName());
            ps.setString(4, b.getFacilityName());
            ps.setString(5, b.getBookingDate());
            ps.setString(6, b.getStartTime());
            ps.setString(7, b.getEndTime());
            ps.setString(8, b.getPurpose());
            return ps.executeUpdate();
        }
    }

    public int updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE bookings SET status=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate();
        }
    }

    public int cancel(int id) throws SQLException {
        return updateStatus(id, "cancelled");
    }

    public boolean checkConflict(int facilityId, String date,
                                  String start, String end) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookings WHERE facility_id=? " +
                     "AND booking_date=? AND status NOT IN ('cancelled','rejected') " +
                     "AND NOT (end_time <= ? OR start_time >= ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            ps.setString(2, date);
            ps.setString(3, start);
            ps.setString(4, end);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setId(rs.getInt("id"));
        b.setUserId(rs.getInt("user_id"));
        b.setFacilityId(rs.getInt("facility_id"));
        b.setUserName(rs.getString("user_name"));
        b.setFacilityName(rs.getString("facility_name"));
        b.setBookingDate(rs.getString("booking_date"));
        b.setStartTime(rs.getString("start_time"));
        b.setEndTime(rs.getString("end_time"));
        b.setPurpose(rs.getString("purpose"));
        b.setStatus(rs.getString("status"));
        b.setCreatedAt(rs.getString("created_at"));
        return b;
    }
}