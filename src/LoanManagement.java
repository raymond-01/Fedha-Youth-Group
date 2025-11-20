import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class LoanManagement {
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private DefaultTableModel loanTableModel;

    // Loan Types and their rules
    private final Map<String, Double> loanInterestRates = new HashMap<>();
    private final Map<String, Integer> loanRepaymentPeriods = new HashMap<>();
    private final Map<String, Double> loanMultipliers = new HashMap<>();

    public LoanManagement() {
        // Initialize loan rules
        loanInterestRates.put("Emergency", 0.3);
        loanInterestRates.put("Short", 0.6);
        loanInterestRates.put("Normal", 1.0);
        loanInterestRates.put("Development", 1.4);

        loanRepaymentPeriods.put("Emergency", 12);
        loanRepaymentPeriods.put("Short", 24);
        loanRepaymentPeriods.put("Normal", 36);
        loanRepaymentPeriods.put("Development", 48);

        loanMultipliers.put("Emergency", 1.0);
        loanMultipliers.put("Short", 2.0);
        loanMultipliers.put("Normal", 3.0);
        loanMultipliers.put("Development", 5.0);

        // Initialize frame and UI
        frame = new JFrame("Loans Section");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        loanTableModel = new DefaultTableModel(new String[]{
                "Loan ID", "Member ID", "Loan Amount", "Loan Type", "Interest Rate",
                "Repayment Period", "Monthly Repayment", "Outstanding Balance", "Guarantors", "Status"
        }, 0);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        tabbedPane.add("Main State", createMainStatePanel());
        tabbedPane.add("Loan Application Form", createLoanFormPanel());
        tabbedPane.add("View Loans", createLoansTablePanel());
        tabbedPane.add("Repay Loan", createRepayLoanPanel());

        tabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tabPlacement, int runCount, int maxTabHeight) {
                return 0; // Hides the tab area
            }
        });

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private JPanel createLoansTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("View and Manage Loans", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        JTable loansTable = new JTable(loanTableModel);
        JScrollPane scrollPane = new JScrollPane(loansTable);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton refreshButton = new JButton("Refresh Loans");
        JButton backButton = new JButton("Back");

        refreshButton.addActionListener(e -> loadLoansIntoTable());
        backButton.addActionListener(e -> tabbedPane.setSelectedIndex(0));

        buttonPanel.add(refreshButton);
        buttonPanel.add(backButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createMainStatePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Loan Management", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton applyLoanButton = new JButton("Apply for a Loan");
        JButton viewLoansButton = new JButton("View All Loans");
        JButton repayLoanButton = new JButton("Repay Loan");

        applyLoanButton.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        viewLoansButton.addActionListener(e -> {
            tabbedPane.setSelectedIndex(2);
            loadLoansIntoTable();
        });
        repayLoanButton.addActionListener(e -> tabbedPane.setSelectedIndex(3));

        panel.add(Box.createVerticalGlue());
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(applyLoanButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(viewLoansButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(repayLoanButton);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createLoanFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel title = new JLabel("Loan Application Form");
        title.setFont(new Font("Arial", Font.BOLD, 18));

        JLabel memberIdLabel = new JLabel("Member ID:");
        JTextField memberIdField = new JTextField(15);
        JLabel loanAmountLabel = new JLabel("Loan Amount:");
        JTextField loanAmountField = new JTextField(15);
        JLabel loanTypeLabel = new JLabel("Loan Type:");
        JComboBox<String> loanTypeComboBox = new JComboBox<>(new String[]{"Emergency", "Short", "Normal", "Development"});
        JLabel guarantorLabel = new JLabel("Guarantor IDs (comma-separated):");
        JTextField guarantorField = new JTextField(15);

        JButton applyButton = new JButton("Apply Loan");
        JButton backButton = new JButton("Back");

        applyButton.addActionListener(e -> {
            try {
                int memberId = Integer.parseInt(memberIdField.getText().trim());
                double loanAmount = Double.parseDouble(loanAmountField.getText().trim());
                String loanType = (String) loanTypeComboBox.getSelectedItem();
                String[] guarantorIds = guarantorField.getText().split(",");

                if (!isEligibleForLoan(memberId)) {
                    JOptionPane.showMessageDialog(frame, "Member is not eligible for any loan. Shares must exceed Ksh 4,000.");
                    return;
                }

                double maxLoanAmount = getMaxLoanAmount(memberId, loanType);
                if (loanAmount > maxLoanAmount) {
                    JOptionPane.showMessageDialog(frame, "Loan amount exceeds your eligible limit for " + loanType + ".");
                    return;
                }

                if (applyForLoan(memberId, loanAmount, loanType, guarantorIds)) {
                    JOptionPane.showMessageDialog(frame, "Loan applied successfully and automatically accepted!");
                } else {
                    JOptionPane.showMessageDialog(frame, "Loan application failed. Check the guarantors or loan limit.");
                }

                memberIdField.setText("");
                loanAmountField.setText("");
                guarantorField.setText("");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter valid numeric values for Member ID and Loan Amount.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "An error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        gbc.gridy++;
        panel.add(memberIdLabel, gbc);
        gbc.gridx = 1;
        panel.add(memberIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(loanAmountLabel, gbc);
        gbc.gridx = 1;
        panel.add(loanAmountField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(loanTypeLabel, gbc);
        gbc.gridx = 1;
        panel.add(loanTypeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(guarantorLabel, gbc);
        gbc.gridx = 1;
        panel.add(guarantorField, gbc);

        gbc.gridwidth = 2;
        gbc.gridy++;
        panel.add(applyButton, gbc);

        gbc.gridy++;
        panel.add(backButton, gbc);

        return panel;
    }

    private JPanel createRepayLoanPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel title = new JLabel("Repay Loan");
        title.setFont(new Font("Arial", Font.BOLD, 18));

        JLabel loanIdLabel = new JLabel("Loan ID:");
        JTextField loanIdField = new JTextField(15);
        JLabel loanDetailsLabel = new JLabel("Loan Details:");
        JTextArea loanDetailsArea = new JTextArea(5, 30);
        loanDetailsArea.setEditable(false);
        JLabel amountLabel = new JLabel("Repayment Amount:");
        JTextField amountField = new JTextField(15);

        JButton repayButton = new JButton("Repay");
        JButton backButton = new JButton("Back");

        repayButton.addActionListener(e -> {
            try {
                int loanId = Integer.parseInt(loanIdField.getText().trim());
                double repaymentAmount = Double.parseDouble(amountField.getText().trim());

                if (repayLoan(loanId, repaymentAmount)) {
                    JOptionPane.showMessageDialog(frame, "Repayment successful!");
                    loanDetailsArea.setText(getLoanDetails(loanId));
                } else {
                    JOptionPane.showMessageDialog(frame, "Repayment failed. Check loan ID or amount.");
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter valid numeric values.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "An error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        gbc.gridy++;
        panel.add(loanIdLabel, gbc);
        gbc.gridx = 1;
        panel.add(loanIdField, gbc);


        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(amountLabel, gbc);
        gbc.gridx = 1;
        panel.add(amountField, gbc);

        gbc.gridwidth = 2;
        gbc.gridy++;
        panel.add(repayButton, gbc);

        // Loan details label
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(loanDetailsLabel, gbc);

        // Loan details text area spanning two columns
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(loanDetailsArea), gbc);

        // Back button centered below
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(backButton, gbc);

        return panel;
    }

    private boolean repayLoan(int loanId, double repaymentAmount) {
        String querySelect = "SELECT OutstandingBalance, LoanAmount FROM loans WHERE LoanID = ?";
        String queryUpdate = "UPDATE loans SET OutstandingBalance = ?, LoanStatus = ? WHERE LoanID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmtSelect = conn.prepareStatement(querySelect);
             PreparedStatement pstmtUpdate = conn.prepareStatement(queryUpdate)) {

            pstmtSelect.setInt(1, loanId);
            ResultSet rs = pstmtSelect.executeQuery();

            if (rs.next()) {
                double outstandingBalance = rs.getDouble("OutstandingBalance");
                double newBalance = outstandingBalance - repaymentAmount;

                String newStatus = newBalance <= 0 ? "Cleared" : "Active";

                pstmtUpdate.setDouble(1, Math.max(newBalance, 0));
                pstmtUpdate.setString(2, newStatus);
                pstmtUpdate.setInt(3, loanId);

                return pstmtUpdate.executeUpdate() > 0;
            } else {
                JOptionPane.showMessageDialog(frame, "Loan ID not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private String getLoanDetails(int loanId) {
        String query = "SELECT * FROM loans WHERE LoanID = ?";
        StringBuilder details = new StringBuilder();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, loanId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                details.append("Loan ID: ").append(rs.getInt("LoanID")).append("\n");
                details.append("Member ID: ").append(rs.getInt("MemberID")).append("\n");
                details.append("Loan Amount: ").append(rs.getDouble("LoanAmount")).append("\n");
                details.append("Outstanding Balance: ").append(rs.getDouble("OutstandingBalance")).append("\n");
                details.append("Status: ").append(rs.getString("LoanStatus"));
            } else {
                details.append("No loan found with ID: ").append(loanId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return details.toString();
    }

    private void loadLoansIntoTable() {
        loanTableModel.setRowCount(0);

        String query = "SELECT LoanID, MemberID, LoanAmount, LoanType, InterestRate, RepaymentPeriod, " +
                "MonthlyRepayment, OutstandingBalance, GuarantorIDs, LoanStatus FROM loans";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                loanTableModel.addRow(new Object[]{
                        rs.getInt("LoanID"),
                        rs.getInt("MemberID"),
                        rs.getDouble("LoanAmount"),
                        rs.getString("LoanType"),
                        rs.getDouble("InterestRate"),
                        rs.getInt("RepaymentPeriod"),
                        rs.getDouble("MonthlyRepayment"),
                        rs.getDouble("OutstandingBalance"),
                        rs.getString("GuarantorIDs"),
                        rs.getString("LoanStatus")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isEligibleForLoan(int memberId) {
        String query = "SELECT SUM(ContributionAmount) AS TotalShares FROM contributions WHERE MemberID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, memberId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double totalShares = rs.getDouble("TotalShares");
                return totalShares >= 4000;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private double getMaxLoanAmount(int memberId, String loanType) {
        double shares = getTotalShares(memberId);
        double multiplier = loanMultipliers.getOrDefault(loanType, 1.0);
        return shares * multiplier;
    }

    private double getTotalShares(int memberId) {
        String query = "SELECT SUM(ContributionAmount) AS TotalShares FROM contributions WHERE MemberID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, memberId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("TotalShares");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private boolean applyForLoan(int memberId, double loanAmount, String loanType, String[] guarantorIds) {
        double rateFactor = loanInterestRates.getOrDefault(loanType, 0.0);
        double interestRatePercent = rateFactor * 100; // store as percentage for reporting
        int repaymentPeriod = loanRepaymentPeriods.getOrDefault(loanType, 12);
        double totalRepayable = loanAmount * (1 + rateFactor);
        double monthlyRepayment = totalRepayable / repaymentPeriod;
        String guarantors = String.join(",", guarantorIds);

        String query = "INSERT INTO loans (MemberID, LoanAmount, LoanType, InterestRate, RepaymentPeriod, MonthlyRepayment, LoanStatus, GuarantorIDs, OutstandingBalance) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'Active', ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, memberId);
            pstmt.setDouble(2, loanAmount);
            pstmt.setString(3, loanType);
            pstmt.setDouble(4, interestRatePercent);
            pstmt.setInt(5, repaymentPeriod);
            pstmt.setDouble(6, monthlyRepayment);
            pstmt.setString(7, guarantors);
            pstmt.setDouble(8, loanAmount); // OutstandingBalance starts as full amount

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoanManagement());
    }
}
