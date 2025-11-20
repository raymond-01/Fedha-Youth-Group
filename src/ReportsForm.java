import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public class ReportsForm {
    private JFrame frame;
    private DefaultTableModel tableModel;

    public ReportsForm() {
        try {
            frame = new JFrame("Reports");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLayout(new BorderLayout());
            frame.setLocationRelativeTo(null);

            // Title Section
            JLabel titleLabel = new JLabel("Reports", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            frame.add(titleLabel, BorderLayout.NORTH);

            // Report Selection Section
            JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
            JLabel reportLabel = new JLabel("Select Report:");
            JComboBox<String> reportComboBox = new JComboBox<>(new String[]{
                    "Members Report", "Loans Report", "Fixed Deposit Report",
                    "Dividends Report", "Revenue Report", "Exiting Members Report"
            });
            JButton generateButton = new JButton("Generate Report");

            selectionPanel.add(reportLabel);
            selectionPanel.add(reportComboBox);
            selectionPanel.add(generateButton);
            frame.add(selectionPanel, BorderLayout.NORTH);

            // Table Section
            tableModel = new DefaultTableModel();
            JTable reportTable = new JTable(tableModel);
            JScrollPane tableScrollPane = new JScrollPane(reportTable);
            frame.add(tableScrollPane, BorderLayout.CENTER);

            // Button Section
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            JButton exportButton = new JButton("Export to CSV");
            JButton backButton = new JButton("Back");

            buttonPanel.add(exportButton);
            buttonPanel.add(backButton);
            frame.add(buttonPanel, BorderLayout.SOUTH);

            // Action Listeners
            generateButton.addActionListener(e -> {
                try {
                    String selectedReport = (String) reportComboBox.getSelectedItem();
                    generateReport(selectedReport);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error generating report: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });

            exportButton.addActionListener(e -> {
                try {
                    if (tableModel.getRowCount() == 0) {
                        JOptionPane.showMessageDialog(frame, "No data to export. Generate a report first.",
                                "Information", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        exportToCSV();
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Error exporting to CSV: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });

            backButton.addActionListener(e -> frame.dispose());

            frame.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error initializing reports form: " + e.getMessage(),
                    "Critical Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Generate the selected report
    private void generateReport(String reportType) {
        try {
            tableModel.setRowCount(0); // Clear the table
            tableModel.setColumnCount(0); // Clear the columns

            switch (reportType) {
                case "Members Report":
                    generateMembersReport();
                    break;
                case "Loans Report":
                    generateLoansReport();
                    break;
                case "Fixed Deposit Report":
                    generateFixedDepositReport();
                    break;
                case "Dividends Report":
                    generateDividendsReport();
                    break;
                case "Revenue Report":
                    generateRevenueReport();
                    break;
                case "Exiting Members Report":
                    generateExitingMembersReport();
                    break;
                default:
                    JOptionPane.showMessageDialog(frame, "Invalid report selection.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error generating report: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Generate Members Report
    private void generateMembersReport() {
        String query = "SELECT MemberID, FullName, Age, Shares FROM members";
        populateTableFromQuery(query, new String[]{"Member ID", "Name", "Age", "Shares"});
    }

    // Generate Loans Report
    private void generateLoansReport() {
        String query = "SELECT LoanID, MemberID, LoanAmount, InterestRate, RepaymentPeriod, OutstandingBalance FROM loans";
        populateTableFromQuery(query, new String[]{"Loan ID", "Member ID", "Loan Amount", "Interest Rate", "Repayment Period", "Outstanding Balance"});
    }

    // Generate Fixed Deposit Report
    private void generateFixedDepositReport() {
        String query = "SELECT DepositID, TotalSavings, MonthlyInterest, AccumulatedInterest, LastUpdated FROM fixed_deposits";
        populateTableFromQuery(query, new String[]{"Deposit ID", "Total Savings", "Monthly Interest", "Accumulated Interest", "Last Updated"});
    }

    // Generate Dividends Report
    private void generateDividendsReport() {
        String query = "SELECT MemberID, FullName, Shares, Dividends FROM members";
        populateTableFromQuery(query, new String[]{"Member ID", "Name", "Shares", "Dividends"});
    }

    // Generate Revenue Report
    private void generateRevenueReport() {
        String query = "SELECT SUM(LoanAmount * InterestRate / 100) AS LoanRevenue, " +
                "SUM(MonthlyInterest) AS FixedDepositRevenue FROM loans, fixed_deposits";
        populateTableFromQuery(query, new String[]{"Loan Revenue", "Fixed Deposit Revenue"});
    }

    // Generate Exiting Members Report
    private void generateExitingMembersReport() {
        String query = "SELECT MemberID, FullName, Shares, OutstandingLoan, ExitNoticeGiven FROM members WHERE ExitNoticeGiven = true";
        populateTableFromQuery(query, new String[]{"Member ID", "Name", "Shares", "Outstanding Loan", "Exit Notice Given"});
    }

    // Populate the table from a database query
    private void populateTableFromQuery(String query, String[] columnNames) {
        try {
            tableModel.setColumnIdentifiers(columnNames);

            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    ArrayList<Object> row = new ArrayList<>();
                    for (int i = 1; i <= columnNames.length; i++) {
                        row.add(rs.getObject(i));
                    }
                    tableModel.addRow(row.toArray());
                }

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(frame, "Error populating report table: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Unexpected error while populating table: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Export the table to a CSV file
    private void exportToCSV() throws IOException {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Report as CSV");

        try {
            int userSelection = fileChooser.showSaveDialog(frame);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();

                try (FileWriter writer = new FileWriter(filePath + ".csv")) {
                    for (int i = 0; i < tableModel.getColumnCount(); i++) {
                        writer.write(tableModel.getColumnName(i) + (i < tableModel.getColumnCount() - 1 ? "," : "\n"));
                    }

                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        for (int j = 0; j < tableModel.getColumnCount(); j++) {
                            writer.write(tableModel.getValueAt(i, j).toString() + (j < tableModel.getColumnCount() - 1 ? "," : "\n"));
                        }
                    }

                    JOptionPane.showMessageDialog(frame, "Report exported successfully!");
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error exporting file: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
    }

    public static void main(String[] args) {
        new ReportsForm();
    }
}
