import javax.swing.*;
import java.awt.*;

public class HomePage {
    public static void main(String[] args) {
        // Display welcome message
        JOptionPane.showMessageDialog(null,
                "Fedha Youth Group System!\n" ,
                "Welcome", JOptionPane.INFORMATION_MESSAGE);

        // Create the frame
        JFrame frame = new JFrame("Fedha Youth Group System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        // Title Panel
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("Fedha Youth Group System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titlePanel.add(titleLabel);

        // Menu Buttons Panel
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));

        // Buttons
        JButton membersButton = new JButton("Members");
        JButton loansButton = new JButton("Loans");
        JButton fixedDepositsButton = new JButton("Fixed Deposits");
        JButton reportsButton = new JButton("Reports");
        JButton revenueButton = new JButton("View Revenue");
        JButton exitButton = new JButton("Exit");

        // Add tooltips
        membersButton.setToolTipText("Manage members' details, add new members, and view all members.");
        loansButton.setToolTipText("Apply for loans and view/manage existing loans.");
        fixedDepositsButton.setToolTipText("View and manage fixed deposits.");
        reportsButton.setToolTipText("Generate and export reports for members, loans, fixed deposits, dividends, and revenue.");
        revenueButton.setToolTipText("View the organization's total revenue from loans and fixed deposits.");
        exitButton.setToolTipText("Exit the application.");

        // Button styling and alignment
        Font buttonFont = new Font("Arial", Font.BOLD, 16);
        Dimension buttonSize = new Dimension(250, 50);

        for (JButton button : new JButton[]{membersButton, loansButton, fixedDepositsButton, reportsButton, revenueButton, exitButton}) {
            button.setFont(buttonFont);
            button.setMaximumSize(buttonSize);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        // Add buttons to the menuPanel with spacing
        menuPanel.add(Box.createVerticalGlue());
        menuPanel.add(membersButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        menuPanel.add(loansButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        menuPanel.add(fixedDepositsButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        menuPanel.add(reportsButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        menuPanel.add(revenueButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 50)));
        menuPanel.add(exitButton);
        menuPanel.add(Box.createVerticalGlue());

        // Add action listeners to buttons
        membersButton.addActionListener(e -> new MembersSection());
        loansButton.addActionListener(e -> new LoanManagement());
        fixedDepositsButton.addActionListener(e -> new FixedDepositManagement());
        reportsButton.addActionListener(e -> new ReportsForm());
        revenueButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(frame,
                    "coming soon!", "Revenue", JOptionPane.INFORMATION_MESSAGE);
        });
        exitButton.addActionListener(e -> {
            int confirmExit = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to exit?", "Confirm Exit",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirmExit == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        // Add panels to frame
        frame.add(titlePanel, BorderLayout.NORTH);
        frame.add(menuPanel, BorderLayout.CENTER);

        // Display the frame
        frame.setVisible(true);
    }
}
