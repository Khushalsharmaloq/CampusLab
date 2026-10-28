package com.campuslab.desktop;

import com.campuslab.config.Database;
import com.campuslab.service.CampusLabService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {
    private final Map<String, Object> user;
    private final CampusLabService service = new CampusLabService();
    private final boolean admin;
    private final JTabbedPane tabs = new JTabbedPane();
    private JLabel summaryLabel;
    private JTable courseTable, labTable, projectTable, submissionTable, userTable;

    public MainFrame(Map<String, Object> user) {
        super("CampusLab | Workspace");
        this.user = user;
        this.admin = "ADMIN".equals(user.get("role"));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 680));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        buildUi();
        refreshAll();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(18, 18, 18, 18));
        root.setBackground(new Color(247, 249, 253));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        JPanel labels = new JPanel(new GridLayout(0, 1, 2, 2)); labels.setOpaque(false);
        JLabel title = new JLabel("CampusLab"); title.setFont(title.getFont().deriveFont(Font.BOLD, 26f)); title.setForeground(new Color(37, 64, 117));
        JLabel welcome = new JLabel("Welcome, " + user.get("name") + "  ·  " + user.get("role")); welcome.setForeground(new Color(92, 104, 126));
        labels.add(title); labels.add(welcome);
        JButton logout = new JButton("Sign out"); logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        top.add(labels, BorderLayout.WEST); top.add(logout, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);
        tabs.addTab("Overview", buildOverview());
        tabs.addTab("Courses", buildCourses());
        tabs.addTab("Labs", buildLabs());
        tabs.addTab("Projects", buildProjects());
        tabs.addTab("Submissions", buildSubmissions());
        if (admin) tabs.addTab("Users", buildUsers());
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildOverview() {
        JPanel p = new JPanel(new BorderLayout(8, 12)); p.setBorder(new EmptyBorder(18, 8, 8, 8)); p.setOpaque(false);
        JLabel h = new JLabel("Workspace overview"); h.setFont(h.getFont().deriveFont(Font.BOLD, 20f));
        summaryLabel = new JLabel("Loading dashboard statistics…"); summaryLabel.setFont(summaryLabel.getFont().deriveFont(15f));
        JTextArea info = new JTextArea("CampusLab provides one shared workspace for courses, lab exercises, student projects and submissions.\n\n" +
                "• Add and track courses, practical lab tasks and projects.\n" +
                "• Students can submit a repository or project URL for a lab.\n" +
                "• Administrators can review submissions and provide feedback.\n" +
                "• The Swing app and web app use the same MySQL database.");
        info.setEditable(false); info.setOpaque(false); info.setFont(info.getFont().deriveFont(14f));
        p.add(h, BorderLayout.NORTH); p.add(summaryLabel, BorderLayout.CENTER); p.add(info, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildCourses() {
        courseTable = table();
        JPanel p = tablePanel(courseTable, () -> service.listCourses(), new String[]{"id", "code", "name", "semester", "lab_count", "created_at"}, "No courses found.");
        if (admin) addButton(p, "Add course", e -> safeAction(() -> addCourse()));
        return p;
    }

    private JPanel buildLabs() {
        labTable = table();
        JPanel p = tablePanel(labTable, () -> service.listLabs(), new String[]{"id", "title", "course_code", "course_name", "due_date", "status", "description"}, "No labs found.");
        if (admin) {
            addButton(p, "Add lab", e -> safeAction(() -> addLab()));
            addButton(p, "Delete selected", e -> safeAction(() -> { long id = selectedId(labTable); if (confirm("Delete lab #" + id + " and its submissions?")) { service.deleteLab(id); refreshAll(); } }));
        }
        if (!admin) addButton(p, "Submit lab", e -> safeAction(() -> submitLab()));
        return p;
    }

    private JPanel buildProjects() {
        projectTable = table();
        JPanel p = tablePanel(projectTable, () -> service.listProjects(), new String[]{"id", "title", "tech_stack", "status", "owner_name", "created_at", "description"}, "No projects found.");
        if (admin) {
            addButton(p, "Add project", e -> safeAction(() -> addProject()));
            addButton(p, "Update status", e -> safeAction(() -> updateProjectStatus()));
            addButton(p, "Delete selected", e -> safeAction(() -> { long id = selectedId(projectTable); if (confirm("Delete project #" + id + "?")) { service.deleteProject(id); refreshAll(); } }));
        }
        return p;
    }

    private JPanel buildSubmissions() {
        submissionTable = table();
        JPanel p = tablePanel(submissionTable, () -> service.listSubmissions(asLong(user.get("id")), admin),
                new String[]{"id", "lab_title", "student_name", "student_email", "submission_url", "status", "remarks", "reviewer_feedback", "submitted_at"}, "No submissions yet.");
        if (admin) addButton(p, "Review selected", e -> safeAction(() -> reviewSubmission()));
        if (!admin) addButton(p, "Submit a lab", e -> safeAction(() -> submitLab()));
        return p;
    }

    private JPanel buildUsers() {
        userTable = table();
        JPanel p = tablePanel(userTable, () -> service.listUsers(), new String[]{"id", "full_name", "email", "role", "created_at"}, "No users found.");
        addButton(p, "Add user", e -> safeAction(() -> addUser()));
        return p;
    }

    private JTable table() {
        JTable table = new JTable(new DefaultTableModel() { @Override public boolean isCellEditable(int row, int col) { return false; } });
        table.setAutoCreateRowSorter(true); table.setRowHeight(26); table.setFillsViewportHeight(true);
        return table;
    }

    private JPanel tablePanel(JTable table, ListSupplier supplier, String[] fields, String emptyMessage) {
        JPanel p = new JPanel(new BorderLayout(8, 8)); p.setOpaque(false); p.setBorder(new EmptyBorder(12, 6, 6, 6));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false);
        JButton refresh = new JButton("Refresh"); refresh.addActionListener(e -> safeAction(() -> loadTable(table, supplier, fields)));
        actions.add(refresh);
        p.add(actions, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.putClientProperty("supplier", supplier); p.putClientProperty("fields", fields); p.putClientProperty("empty", emptyMessage);
        return p;
    }

    private void addButton(JPanel panel, String title, java.awt.event.ActionListener action) {
        JPanel actions = (JPanel) panel.getComponent(0);
        JButton button = new JButton(title); button.addActionListener(action); actions.add(button);
    }

    @SuppressWarnings("unchecked")
    private void refreshTableForTab(int index) throws Exception {
        Component c = tabs.getComponentAt(index);
        if (!(c instanceof JPanel p)) return;
        ListSupplier supplier = (ListSupplier) p.getClientProperty("supplier");
        String[] fields = (String[]) p.getClientProperty("fields");
        if (supplier != null) {
            for (Component child : ((JScrollPane) p.getComponent(1)).getViewport().getComponents()) {
                if (child instanceof JTable t) loadTable(t, supplier, fields);
            }
        }
    }

    private void loadTable(JTable table, ListSupplier supplier, String[] fields) throws Exception {
        List<Map<String, Object>> rows = supplier.get();
        String[] headers = new String[fields.length];
        for (int i = 0; i < fields.length; i++) headers[i] = humanize(fields[i]);
        DefaultTableModel model = new DefaultTableModel(headers, 0) { @Override public boolean isCellEditable(int row, int col) { return false; } };
        for (Map<String, Object> row : rows) {
            Object[] values = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) values[i] = row.get(fields[i]);
            model.addRow(values);
        }
        table.setModel(model);
        if (rows.isEmpty()) table.setToolTipText("No records are available yet.");
    }

    private void refreshAll() {
        try {
            Database.initialize();
            Map<String, Long> s = service.summary(asLong(user.get("id")), admin);
            summaryLabel.setText("Courses: " + s.get("courses") + "     Labs: " + s.get("labs") + "     Projects: " + s.get("projects") +
                    "     Submissions: " + s.get("submissions") + "     Pending reviews: " + s.get("pending") + (admin ? "     Users: " + s.get("users") : ""));
            for (int i = 1; i < tabs.getTabCount(); i++) refreshTableForTab(i);
        } catch (Exception ex) { showError(ex); }
    }

    private void addCourse() throws Exception {
        JTextField code = new JTextField("CS303"), name = new JTextField(), semester = new JTextField("6");
        JPanel panel = formPanel(new String[]{"Course code", "Course name", "Semester"}, new JComponent[]{code, name, semester});
        if (JOptionPane.showConfirmDialog(this, panel, "Add course", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            service.createCourse(code.getText(), name.getText(), Integer.parseInt(semester.getText().trim())); refreshAll();
        }
    }

    private void addLab() throws Exception {
        List<Map<String, Object>> courses = service.listCourses();
        JComboBox<String> course = new JComboBox<>(); course.addItem("No course");
        for (Map<String, Object> c : courses) course.addItem(c.get("id") + " — " + c.get("code") + " | " + c.get("name"));
        JTextField title = new JTextField(), due = new JTextField(); JTextArea description = new JTextArea(4, 24); description.setLineWrap(true);
        JPanel panel = formPanel(new String[]{"Course", "Lab title", "Due date (YYYY-MM-DD, optional)", "Description"}, new JComponent[]{course, title, due, new JScrollPane(description)});
        if (JOptionPane.showConfirmDialog(this, panel, "Add lab", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            Long courseId = null;
            if (course.getSelectedIndex() > 0) courseId = Long.parseLong(course.getSelectedItem().toString().split(" — ")[0]);
            service.createLab(courseId, title.getText(), description.getText(), due.getText()); refreshAll();
        }
    }

    private void addProject() throws Exception {
        JTextField title = new JTextField(), tech = new JTextField("Java, Swing, MySQL"); JTextArea description = new JTextArea(4, 24); description.setLineWrap(true);
        List<Map<String, Object>> users = service.listUsers(); JComboBox<String> owner = new JComboBox<>(); owner.addItem("Unassigned");
        for (Map<String, Object> u : users) if ("STUDENT".equals(u.get("role"))) owner.addItem(u.get("id") + " — " + u.get("full_name"));
        JPanel panel = formPanel(new String[]{"Project title", "Description", "Technology stack", "Owner"}, new JComponent[]{title, new JScrollPane(description), tech, owner});
        if (JOptionPane.showConfirmDialog(this, panel, "Add project", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            Long ownerId = owner.getSelectedIndex() == 0 ? null : Long.parseLong(owner.getSelectedItem().toString().split(" — ")[0]);
            service.createProject(title.getText(), description.getText(), tech.getText(), ownerId); refreshAll();
        }
    }

    private void updateProjectStatus() throws Exception {
        long id = selectedId(projectTable);
        String status = (String) JOptionPane.showInputDialog(this, "Choose new status", "Update project", JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"PLANNED", "IN_PROGRESS", "COMPLETED", "ON_HOLD"}, "IN_PROGRESS");
        if (status != null) { service.updateProjectStatus(id, status); refreshAll(); }
    }

    private void submitLab() throws Exception {
        List<Map<String, Object>> labs = service.listLabs(); if (labs.isEmpty()) { info("Add a lab before submitting work."); return; }
        JComboBox<String> lab = new JComboBox<>();
        for (Map<String, Object> l : labs) lab.addItem(l.get("id") + " — " + l.get("title"));
        JTextField url = new JTextField("https://github.com/your-username/your-repository"); JTextArea remarks = new JTextArea(3, 24);
        JPanel panel = formPanel(new String[]{"Lab", "Repository / submission URL", "Remarks"}, new JComponent[]{lab, url, new JScrollPane(remarks)});
        if (JOptionPane.showConfirmDialog(this, panel, "Submit lab work", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            long labId = Long.parseLong(lab.getSelectedItem().toString().split(" — ")[0]);
            service.createSubmission(labId, asLong(user.get("id")), url.getText(), remarks.getText()); refreshAll();
        }
    }

    private void reviewSubmission() throws Exception {
        long id = selectedId(submissionTable);
        String status = (String) JOptionPane.showInputDialog(this, "Choose review status", "Review submission", JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"REVIEWED", "ACCEPTED", "REJECTED", "PENDING"}, "REVIEWED");
        if (status == null) return;
        String feedback = JOptionPane.showInputDialog(this, "Feedback for the student (optional):", "");
        service.reviewSubmission(id, status, feedback); refreshAll();
    }

    private void addUser() throws Exception {
        JTextField name = new JTextField(), email = new JTextField(); JPasswordField pass = new JPasswordField(); JComboBox<String> role = new JComboBox<>(new String[]{"STUDENT", "ADMIN"});
        JPanel panel = formPanel(new String[]{"Full name", "Email", "Temporary password (8+ chars)", "Role"}, new JComponent[]{name, email, pass, role});
        if (JOptionPane.showConfirmDialog(this, panel, "Add user", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            service.createUser(name.getText(), email.getText(), new String(pass.getPassword()), (String) role.getSelectedItem()); refreshAll();
        }
    }

    private JPanel formPanel(String[] labels, JComponent[] controls) {
        JPanel panel = new JPanel(new GridBagLayout()); GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(5, 6, 5, 6); c.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < labels.length; i++) {
            c.gridx = 0; c.gridy = i; c.weightx = 0; panel.add(new JLabel(labels[i]), c);
            c.gridx = 1; c.weightx = 1; panel.add(controls[i], c);
        }
        return panel;
    }

    private long selectedId(JTable table) {
        int selected = table.getSelectedRow(); if (selected < 0) throw new IllegalArgumentException("Select a row first.");
        int modelRow = table.convertRowIndexToModel(selected);
        return Long.parseLong(table.getModel().getValueAt(modelRow, 0).toString());
    }
    private boolean confirm(String message) { return JOptionPane.showConfirmDialog(this, message, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; }
    private void info(String message) { JOptionPane.showMessageDialog(this, message, "CampusLab", JOptionPane.INFORMATION_MESSAGE); }
    private void safeAction(ThrowingAction action) { try { action.run(); } catch (Exception ex) { showError(ex); } }
    private void showError(Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage() == null ? ex.toString() : ex.getMessage(), "CampusLab error", JOptionPane.ERROR_MESSAGE); }
    private static Long asLong(Object value) { return value == null ? null : Long.valueOf(value.toString()); }
    private static String humanize(String s) { String[] pieces = s.split("_"); StringBuilder b = new StringBuilder(); for (String piece : pieces) { if (!b.isEmpty()) b.append(' '); b.append(piece.substring(0, 1).toUpperCase()).append(piece.substring(1)); } return b.toString(); }
    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }
    @FunctionalInterface private interface ListSupplier { List<Map<String, Object>> get() throws Exception; }
}
