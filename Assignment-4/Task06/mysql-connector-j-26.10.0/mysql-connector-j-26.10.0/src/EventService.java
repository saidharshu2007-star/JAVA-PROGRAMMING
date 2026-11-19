import java.sql.SQLException;
import java.util.List;

public class EventService {

    private final EventDAO dao = new EventDAO();

    public List<String> getEvents() {
        return dao.getEvents();
    }

    public String registerStudent(String name, String regNo,
                                  String email, String event) {

        try {
            int eventId = Integer.parseInt(event.split(" - ")[0]);

            boolean success = dao.registerStudent(
                name, regNo, email, eventId
            );

            if (success) {
                return "Registration successful!";
            } else {
                return "Registration failed!";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}