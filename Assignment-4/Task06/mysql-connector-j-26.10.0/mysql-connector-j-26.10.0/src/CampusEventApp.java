import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.sql.SQLException;
import java.util.List;

public class CampusEventApp extends JFrame {

    private final EventService service = new EventService();
    private final EventDAO dao = new EventDAO();

    private JTextField nameField, regField, emailField;
    private JComboBox<String> eventBox, filterBox;
    private JTable registrationTable;
    private DefaultTableModel tableModel;

    private int currentPage = 0;
    private final int PAGE_SIZE = 20;

    public CampusEventApp() {
        setTitle("Campus Event Registration System");
        setSize(850, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        createMenuBar();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Student Registration", createRegistrationPanel());
        tabs.addTab("Committee Dashboard", createCommitteePanel());

        add(tabs);
        setVisible(true);

        loadEvents();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");

        JMenuItem refreshItem = new JMenuItem("Refresh Events");
        refreshItem.addActionListener(e -> loadEvents());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());

        menu.add(refreshItem);
        menu.add(exitItem);
        menuBar.add(menu);

        setJMenuBar(menuBar);
    }

    private JPanel createRegistrationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.insets = new Insets(12, 12, 12, 12);
        c.fill = GridBagConstraints.HORIZONTAL;

        nameField = new JTextField(22);
        regField = new JTextField(22);
        emailField = new JTextField(22);
        eventBox = new JComboBox<>();

        addField(panel, c, 0, "Student Name:", nameField);
        addField(panel, c, 1, "Register Number:", regField);
        addField(panel, c, 2, "Email:", emailField);
        addField(panel, c, 3, "Select Event:", eventBox);

        JButton registerButton = new JButton("Register");
        c.gridx = 1;
        c.gridy = 4;
        panel.add(registerButton, c);

        registerButton.addActionListener(e -> registerStudent());

        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints c,
                          int row, String label, JComponent field) {

        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel(label), c);

        c.gridx = 1;
        panel.add(field, c);
    }

    private JPanel createCommitteePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        filterBox = new JComboBox<>();
        JButton filterButton = new JButton("Filter");
        JButton refreshButton = new JButton("Refresh");
        JButton exportButton = new JButton("Export CSV");

        topPanel.add(new JLabel("Filter by Event:"));
        topPanel.add(filterBox);
        topPanel.add(filterButton);
        topPanel.add(refreshButton);
        topPanel.add(exportButton);

        panel.add(topPanel, BorderLayout.NORTH);

        String[] columns = {
            "ID", "Student Name", "Register Number", "Email", "Event"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        registrationTable = new JTable(tableModel);
        panel.add(new JScrollPane(registrationTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();

        JButton previousButton = new JButton("Previous");
        JButton nextButton = new JButton("Next");

        bottomPanel.add(previousButton);
        bottomPanel.add(nextButton);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        filterButton.addActionListener(e -> {
            currentPage = 0;
            loadRegistrations();
        });

        refreshButton.addActionListener(e -> {
            currentPage = 0;
            loadRegistrations();
        });

        previousButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadRegistrations();
            }
        });

        nextButton.addActionListener(e -> {
            currentPage++;
            loadRegistrations();
        });

        exportButton.addActionListener(e -> exportCSV());

        return panel;
    }

    private void loadEvents() {
        new SwingWorker<List<String>, Void>() {
            protected List<String> doInBackground() {
                return service.getEvents();
            }

            protected void done() {
                try {
                    List<String> events = get();

                    eventBox.removeAllItems();
                    filterBox.removeAllItems();

                    filterBox.addItem("All Events");

                    for (String event : events) {
                        eventBox.addItem(event);
                        filterBox.addItem(event);
                    }

                    if (events.isEmpty()) {
                        JOptionPane.showMessageDialog(
                            CampusEventApp.this,
                            "No events found. Check the database connection."
                        );
                    }

                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }.execute();
    }

    private void registerStudent() {
        String name = nameField.getText();
        String regNo = regField.getText();
        String email = emailField.getText();
        String event = (String) eventBox.getSelectedItem();

        if (name.trim().isEmpty() || regNo.trim().isEmpty()
                || email.trim().isEmpty() || event == null) {
            JOptionPane.showMessageDialog(
                this, "Please fill all fields and select an event."
            );
            return;
        }

        new SwingWorker<String, Void>() {
            protected String doInBackground() {
                return service.registerStudent(name, regNo, email, event);
            }

            protected void done() {
                try {
                    String message = get();

                    JOptionPane.showMessageDialog(
                        CampusEventApp.this, message
                    );

                    if (message.equals("Registration successful!")) {
                        nameField.setText("");
                        regField.setText("");
                        emailField.setText("");
                        currentPage = 0;
                        loadRegistrations();
                    }

                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }.execute();
    }

    private Integer getSelectedEventId() {
        String selected = (String) filterBox.getSelectedItem();

        if (selected == null || selected.equals("All Events")) {
            return null;
        }

        return Integer.parseInt(selected.split(" - ")[0]);
    }

    private void loadRegistrations() {
        Integer eventId;

        try {
            eventId = getSelectedEventId();
        } catch (Exception ex) {
            showError(ex);
            return;
        }

        int offset = currentPage * PAGE_SIZE;

        new SwingWorker<List<Object[]>, Void>() {
            protected List<Object[]> doInBackground() throws SQLException {
                return dao.getRegistrations(
                    eventId, PAGE_SIZE, offset
                );
            }

            protected void done() {
                try {
                    List<Object[]> rows = get();

                    tableModel.setRowCount(0);

                    for (Object[] row : rows) {
                        tableModel.addRow(row);
                    }

                    if (rows.isEmpty() && currentPage > 0) {
                        currentPage--;
                        JOptionPane.showMessageDialog(
                            CampusEventApp.this,
                            "No more registrations."
                        );
                    }

                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }.execute();
    }

    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(
            new java.io.File("event_registrations.csv")
        );

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try (FileWriter writer =
                     new FileWriter(chooser.getSelectedFile())) {

            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                writer.write(tableModel.getColumnName(col) + ",");
            }

            writer.write("\n");

            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    String value = String.valueOf(
                        tableModel.getValueAt(row, col)
                    );

                    writer.write("\"" + value.replace("\"", "\"\"") + "\"");

                    if (col < tableModel.getColumnCount() - 1) {
                        writer.write(",");
                    }
                }

                writer.write("\n");
            }

            JOptionPane.showMessageDialog(
                this, "CSV exported successfully!"
            );

        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CampusEventApp());
    }
}