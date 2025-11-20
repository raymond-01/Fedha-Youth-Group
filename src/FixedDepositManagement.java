import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class FixedDepositManagement {
    private JFrame frame;

    public FixedDepositManagement() {
        frame = new JFrame("Fixed Deposits");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        // Title Section
        JLabel titleLabel = new JLabel("Fixed Deposit Management", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        frame.add(titleLabel, BorderLayout.NORTH);

        // Data Display Section
        JPanel dataPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        dataPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel totalSavingsLabel = new JLabel("Total Savings:");
        JLabel totalSavingsValue = new JLabel();
        JLabel monthlyInterestLabel = new JLabel("Monthly Interest:");
        JLabel monthlyInterestValue = new JLabel();
        JLabel accumulatedInterestLabel = new JLabel("Accumulated Interest:");
        JLabel accumulatedInterestValue = new JLabel();

        totalSavingsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        monthlyInterestLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        accumulatedInterestLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        totalSavingsValue.setHorizontalAlignment(SwingConstants.LEFT);
        monthlyInterestValue.setHorizontalAlignment(SwingConstants.LEFT);
        accumulatedInterestValue.setHorizontalAlignment(SwingConstants.LEFT);

        dataPanel.add(totalSavingsLabel);
        dataPanel.add(totalSavingsValue);
        dataPanel.add(monthlyInterestLabel);
        dataPanel.add(monthlyInterestValue);
        dataPanel.add(accumulatedInterestLabel);
        dataPanel.add(accumulatedInterestValue);

        frame.add(dataPanel, BorderLayout.CENTER);

        // Button Section
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton updateButton = new JButton("Update Fixed Deposit");
        JButton exportButton = new JButton("Export to CSV");
        JButton backButton = new JButton("Close");

        updateButton.setToolTipText("Update the fixed deposit with the latest calculations.");
        exportButton.setToolTipText("Export fixed deposit details to a CSV file.");
        backButton.setToolTipText("Close this window.");

        updateButton.addActionListener(e -> {
            try {
                updateFixedDeposit();
                loadFixedDepositData(totalSavingsValue, monthlyInterestValue, accumulatedInterestValue);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error updating fixed deposit: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        exportButton.addActionListener(e -> {
            try {
                exportToCSV();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error exporting data: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        backButton.addActionListener(e -> frame.dispose());

        buttonPanel.add(updateButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(backButton);

        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Load initial data
        try {
            loadFixedDepositData(totalSavingsValue, monthlyInterestValue, accumulatedInterestValue);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error initializing fixed deposit data: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        frame.setVisible(true);
    }

    private void loadFixedDepositData(JLabel totalSavingsLabel, JLabel monthlyInterestLabel, JLabel accumulatedInterestLabel) throws SQLException {
        String query = "SELECT TotalSavings, MonthlyInterest, AccumulatedInterest, LastUpdated FROM fixed_deposits ORDER BY DepositID DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                totalSavingsLabel.setText(String.format("Ksh %.2f", rs.getDouble("TotalSavings")));
                monthlyInterestLabel.setText(String.format("Ksh %.2f", rs.getDouble("MonthlyInterest")));
                accumulatedInterestLabel.setText(String.format("Ksh %.2f", rs.getDouble("AccumulatedInterest")));
            } else {
                totalSavingsLabel.setText("Ksh 0.00");
                monthlyInterestLabel.setText("Ksh 0.00");
                accumulatedInterestLabel.setText("Ksh 0.00");
            }
        }
    }

    private void updateFixedDeposit() throws SQLException {
        String selectQuery = "SELECT SUM(Shares) AS TotalSavings FROM members WHERE MemberID NOT IN (SELECT MemberID FROM loans WHERE LoanStatus = 'Active')";
        String lastUpdateQuery = "SELECT MAX(LastUpdated) AS LastUpdated FROM fixed_deposits";
        String insertQuery = "INSERT INTO fixed_deposits (TotalSavings, MonthlyInterest, AccumulatedInterest, LastUpdated) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement selectStmt = conn.createStatement();
             ResultSet rs = selectStmt.executeQuery(selectQuery);
             PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {

            LocalDate lastUpdated = LocalDate.now().minusMonths(1); // Default to one month ago
            try (Statement lastUpdateStmt = conn.createStatement();
                 ResultSet lastUpdateRs = lastUpdateStmt.executeQuery(lastUpdateQuery)) {
                if (lastUpdateRs.next() && lastUpdateRs.getDate("LastUpdated") != null) {
                    lastUpdated = lastUpdateRs.getDate("LastUpdated").toLocalDate();
                }
            }

            if (rs.next()) {
                double totalSavings = rs.getDouble("TotalSavings");
                double monthlyInterest = totalSavings * 0.006;

                long monthsSinceLastUpdate = ChronoUnit.MONTHS.between(lastUpdated, LocalDate.now());
                double accumulatedInterest = monthlyInterest * monthsSinceLastUpdate;

                insertStmt.setDouble(1, totalSavings);
                insertStmt.setDouble(2, monthlyInterest);
                insertStmt.setDouble(3, accumulatedInterest);
                insertStmt.setDate(4, Date.valueOf(LocalDate.now()));

                insertStmt.executeUpdate();
                JOptionPane.showMessageDialog(frame, "Fixed deposit updated successfully!");
            } else {
                JOptionPane.showMessageDialog(frame, "No savings available for fixed deposit.");
            }
        }
    }

    private void exportToCSV() throws IOException {
        String query = "SELECT * FROM fixed_deposits";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query);
             FileWriter csvWriter = new FileWriter("FixedDeposits.csv")) {

            csvWriter.append("DepositID,TotalSavings,MonthlyInterest,AccumulatedInterest,LastUpdated\n");

            while (rs.next()) {
                csvWriter.append(rs.getInt("DepositID") + ",")
                        .append(rs.getDouble("TotalSavings") + ",")
                        .append(rs.getDouble("MonthlyInterest") + ",")
                        .append(rs.getDouble("AccumulatedInterest") + ",")
                        .append(rs.getDate("LastUpdated").toString())
                        .append("\n");
            }

            JOptionPane.showMessageDialog(frame, "Data exported successfully to FixedDeposits.csv");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new FixedDepositManagement();
    }
}
