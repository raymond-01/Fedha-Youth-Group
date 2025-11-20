import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;

public class MembersSection {
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private DefaultTableModel tableModel;
    private JLabel totalSharesLabel;
    private JLabel totalRegistrationFeesLabel;

    public MembersSection() {
        frame = new JFrame("Members Section");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        tabbedPane.add("Main State", createMainStatePanel());
        tabbedPane.add("Form View", createFormViewPanel());
        tabbedPane.add("Table View", createTableViewPanel());

        tabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tabPlacement, int runCount, int maxTabHeight) {
                return 0; // Hides the tab area
            }
        });

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 2) {
                loadMembersIntoTable();
                updateSummaryLabels();
            }
        });

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private JPanel createMainStatePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Member Management", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton formButton = new JButton("Add A Member");
        JButton tableButton = new JButton("View All Members");

        formButton.setToolTipText("Navigate to the form view to add a new member.");
        tableButton.setToolTipText("Navigate to the table view to view and manage members.");

        formButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        tableButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        formButton.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        tableButton.addActionListener(e -> tabbedPane.setSelectedIndex(2));

        panel.add(Box.createVerticalGlue());
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(formButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(tableButton);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createFormViewPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel title = new JLabel("Form View - Add Member");
        title.setFont(new Font("Arial", Font.BOLD, 18));

        JLabel nameLabel = new JLabel("Full Name:");
        JTextField nameField = new JTextField(15);
        JLabel ageLabel = new JLabel("Age:");
        JTextField ageField = new JTextField(15);
        JLabel sharesLabel = new JLabel("Shares:");
        JTextField sharesField = new JTextField(15);

        JButton addButton = new JButton("Add Member");
        JButton backButton = new JButton("Back");

        addButton.setToolTipText("Add the member to the database.");
        backButton.setToolTipText("Return to the main state.");

        addButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter a valid name.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int age;
            try {
                age = Integer.parseInt(ageField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Age must be a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (age < 18 || age > 35) {
                JOptionPane.showMessageDialog(frame, "Age should be between 18 and 35.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double shares;
            try {
                shares = Double.parseDouble(sharesField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Shares must be a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (shares < 1000) {
                JOptionPane.showMessageDialog(frame, "Shares should be above 1000.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                addMemberToDatabase(name, age, shares);
                JOptionPane.showMessageDialog(frame, "Member added successfully!");
                nameField.setText("");
                ageField.setText("");
                sharesField.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "An error occurred while adding the member.", "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        backButton.addActionListener(e -> tabbedPane.setSelectedIndex(0));

        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        panel.add(nameLabel, gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(ageLabel, gbc);
        gbc.gridx = 1;
        panel.add(ageField, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        panel.add(sharesLabel, gbc);
        gbc.gridx = 1;
        panel.add(sharesField, gbc);

        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(addButton, gbc);

        gbc.gridy = 5;
        panel.add(backButton, gbc);

        return panel;
    }

    private JPanel createTableViewPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Table View - View and Manage Members", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JLabel searchLabel = new JLabel("Search by Name:");
        JTextField searchField = new JTextField(15);
        JButton searchButton = new JButton("Search");

        totalSharesLabel = new JLabel("Total Shares: Ksh 0.00");
        totalRegistrationFeesLabel = new JLabel("Total Registration Fees: Ksh 0.00");

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(totalSharesLabel);
        searchPanel.add(totalRegistrationFeesLabel);
        panel.add(searchPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"Member ID", "Name", "Age", "Shares", "Registration Fee", "Outstanding Loan", "Exit Notice", "Max Loan Amount"}, 0
        );
        JTable membersTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(membersTable);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton refreshButton = new JButton("Refresh");
        JButton backButton = new JButton("Back");

        refreshButton.setToolTipText("Refresh the members table.");
        backButton.setToolTipText("Return to the main state.");

        refreshButton.addActionListener(e -> {
            loadMembersIntoTable();
            updateSummaryLabels();
        });
        searchButton.addActionListener(e -> searchMembersByName(searchField.getText().trim()));

        backButton.addActionListener(e -> tabbedPane.setSelectedIndex(0));

        buttonPanel.add(refreshButton);
        buttonPanel.add(backButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadMembersIntoTable() {
        tableModel.setRowCount(0);
        ArrayList<Object[]> members = fetchAllMembersFromDatabase();
        for (Object[] member : members) {
            tableModel.addRow(member);
        }
    }

    private ArrayList<Object[]> fetchAllMembersFromDatabase() {
        ArrayList<Object[]> members = new ArrayList<>();
        String query = "SELECT MemberID, FullName, Age, Shares, RegistrationFee, OutstandingLoan, ExitNoticeGiven FROM Members";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                double shares = rs.getDouble("Shares");
                Object[] loanData = calculateLoanEligibility(shares);
                members.add(new Object[]{
                        rs.getInt("MemberID"),
                        rs.getString("FullName"),
                        rs.getInt("Age"),
                        shares,
                        rs.getDouble("RegistrationFee"),
                        rs.getDouble("OutstandingLoan"),
                        rs.getBoolean("ExitNoticeGiven"),
                        // loanData[0], // Loan Type
                        loanData[1]  // Max Loan Amount
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error fetching members.", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return members;
    }

    private Object[] calculateLoanEligibility(double shares) {
        String loanType = "";
        double maxLoanAmount = 0;

        if (shares < 6000) {
            loanType = "Emergency Loan";
            maxLoanAmount = shares;
        } else if (shares < 12000) {
            loanType = "Short Loan";
            maxLoanAmount = 2 * shares;
        } else if (shares < 18000) {
            loanType = "Normal Loan";
            maxLoanAmount = 3 * shares;
        } else {
            loanType = "Development Loan";
            maxLoanAmount = 5 * shares;
        }

        return new Object[]{loanType, maxLoanAmount};
    }

    private void addMemberToDatabase(String name, int age, double shares) throws SQLException {
        String query = "INSERT INTO Members (FullName, Age, Shares, RegistrationFee, OutstandingLoan, ExitNoticeGiven) VALUES (?, ?, ?, 1000, 0, false)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, age);
            pstmt.setDouble(3, shares);
            pstmt.executeUpdate();
        }
    }

    private void searchMembersByName(String name) {
        tableModel.setRowCount(0);
        String query = "SELECT MemberID, FullName, Age, Shares, RegistrationFee, OutstandingLoan, ExitNoticeGiven FROM Members WHERE FullName LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                double shares = rs.getDouble("Shares");
                Object[] loanData = calculateLoanEligibility(shares);
                tableModel.addRow(new Object[]{
                        rs.getInt("MemberID"),
                        rs.getString("FullName"),
                        rs.getInt("Age"),
                        shares,
                        rs.getDouble("RegistrationFee"),
                        rs.getDouble("OutstandingLoan"),
                        rs.getBoolean("ExitNoticeGiven"),
                        loanData[0], // Loan Type
                        loanData[1]  // Max Loan Amount
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error searching members.", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void updateSummaryLabels() {
        String query = "SELECT SUM(Shares) AS TotalShares, SUM(RegistrationFee) AS TotalRegistrationFees FROM Members";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                totalSharesLabel.setText("Total Shares: Ksh " + rs.getDouble("TotalShares"));
                totalRegistrationFeesLabel.setText("Total Registration Fees: Ksh " + rs.getDouble("TotalRegistrationFees"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error fetching summary data.", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MembersSection();
    }
}
