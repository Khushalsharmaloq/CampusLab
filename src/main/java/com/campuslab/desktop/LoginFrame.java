package com.campuslab.desktop;

import com.campuslab.config.Database;
import com.campuslab.service.CampusLabService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public class LoginFrame extends JFrame {
    private final JTextField email = new JTextField(24);
    private final JPasswordField password = new JPasswordField(24);
    private final CampusLabService service = new CampusLabService();
    private final JButton login = new JButton("Sign in");

    public LoginFrame() {
        super("CampusLab | Sign in");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(470, 420);
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(new EmptyBorder(28, 32, 24, 32));
        root.setBackground(new Color(246, 248, 252));

        JPanel brand = new JPanel(new GridLayout(0, 1, 4, 4));
        brand.setOpaque(false);
        JLabel title = new JLabel("CampusLab");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 29f));
        title.setForeground(new Color(37, 64, 117));
        JLabel subtitle = new JLabel("College Lab & Project Management System");
        subtitle.setForeground(new Color(92, 104, 126));
        brand.add(title); brand.add(subtitle);

        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.setOpaque(false);
        form.add(label("Email address")); form.add(email);
        form.add(label("Password")); form.add(password);
        login.setBackground(new Color(43, 78, 145)); login.setForeground(Color.WHITE);
        login.setFocusPainted(false);
        form.add(login);
        JLabel help = new JLabel("Sign in with an account created by your administrator.");
        help.setFont(help.getFont().deriveFont(11f)); help.setForeground(new Color(92, 104, 126));
        root.add(brand, BorderLayout.NORTH); root.add(form, BorderLayout.CENTER); root.add(help, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(login);
        login.addActionListener(e -> signIn());
        password.addActionListener(e -> signIn());
    }

    private JLabel label(String text) { JLabel label = new JLabel(text); label.setFont(label.getFont().deriveFont(Font.BOLD, 12f)); return label; }

    private void signIn() {
        login.setEnabled(false);
        try {
            Database.initialize();
            Map<String, Object> user = service.authenticate(email.getText(), new String(password.getPassword()));
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Email or password is incorrect.", "Sign in failed", JOptionPane.WARNING_MESSAGE);
                return;
            }
            dispose();
            new MainFrame(user).setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "CampusLab could not connect to MySQL. Check that MySQL Server is running and the DB_USER / DB_PASSWORD values in .env are correct.\n\nDetails: " + ex.getMessage(),
                    "Database connection failed", JOptionPane.ERROR_MESSAGE);
        } finally { login.setEnabled(true); }
    }
}
