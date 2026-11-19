import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    public List<String> getEvents() {
        List<String> events = new ArrayList<>();

        String sql = "SELECT event_id, event_name FROM events";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                events.add(rs.getInt("event_id") + " - "
                        + rs.getString("event_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return events;
    }

    public boolean registerStudent(String name, String regNo,
                                   String email, int eventId)
                                   throws SQLException {

        String sql = "INSERT INTO registrations "
                + "(student_name, register_number, email, event_id) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, regNo);
            ps.setString(3, email);
            ps.setInt(4, eventId);

            return ps.executeUpdate() > 0;
        }
    }

    public List<Object[]> getRegistrations(Integer eventId,
                                            int limit, int offset)
                                            throws SQLException {

        List<Object[]> rows = new ArrayList<>();

        String sql = "SELECT r.registration_id, r.student_name, "
                + "r.register_number, r.email, e.event_name "
                + "FROM registrations r JOIN events e "
                + "ON r.event_id = e.event_id "
                + (eventId == null ? "" : "WHERE r.event_id = ? ")
                + "ORDER BY r.registration_id "
                + "LIMIT ? OFFSET ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int index = 1;

            if (eventId != null) {
                ps.setInt(index++, eventId);
            }

            ps.setInt(index++, limit);
            ps.setInt(index, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getInt("registration_id"),
                        rs.getString("student_name"),
                        rs.getString("register_number"),
                        rs.getString("email"),
                        rs.getString("event_name")
                    });
                }
            }
        }

        return rows;
    }
}