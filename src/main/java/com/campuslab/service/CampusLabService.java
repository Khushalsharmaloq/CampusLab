package com.campuslab.service;

import com.campuslab.config.Database;
import com.campuslab.security.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Application service shared by the Swing and web interfaces. */
public class CampusLabService {

    public Map<String, Object> authenticate(String email, String password) throws Exception {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) return null;
        try (Connection con = Database.connect(); PreparedStatement ps = con.prepareStatement(
                "SELECT id,full_name,email,password_hash,password_salt,role FROM app_users WHERE LOWER(email)=LOWER(?)")) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !PasswordUtil.verify(password, rs.getString("password_salt"), rs.getString("password_hash"))) return null;
                Map<String, Object> user = new LinkedHashMap<>();
                user.put("id", rs.getLong("id")); user.put("name", rs.getString("full_name"));
                user.put("email", rs.getString("email")); user.put("role", rs.getString("role"));
                return user;
            }
        }
    }

    public List<Map<String, Object>> listCourses() throws Exception {
        return query("SELECT c.id,c.code,c.name,c.semester,c.created_at,COUNT(l.id) AS lab_count " +
                "FROM courses c LEFT JOIN labs l ON l.course_id=c.id GROUP BY c.id,c.code,c.name,c.semester,c.created_at ORDER BY c.code");
    }

    public void createCourse(String code, String name, int semester) throws Exception {
        requireText(code, "Course code"); requireText(name, "Course name");
        if (semester < 1 || semester > 12) throw new IllegalArgumentException("Semester must be between 1 and 12.");
        execute("INSERT INTO courses(code,name,semester) VALUES(?,?,?)", code.trim().toUpperCase(), name.trim(), semester);
    }

    public List<Map<String, Object>> listLabs() throws Exception {
        return query("SELECT l.id,l.title,l.description,l.due_date,l.status,l.course_id,c.code AS course_code,c.name AS course_name, " +
                "l.created_at FROM labs l LEFT JOIN courses c ON c.id=l.course_id ORDER BY l.id DESC");
    }

    public void createLab(Long courseId, String title, String description, String dueDate) throws Exception {
        requireText(title, "Lab title");
        java.sql.Date date = null;
        if (dueDate != null && !dueDate.isBlank()) {
            try { date = java.sql.Date.valueOf(dueDate.trim()); }
            catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Due date must use YYYY-MM-DD format."); }
        }
        try (Connection con = Database.connect(); PreparedStatement ps = con.prepareStatement(
                "INSERT INTO labs(course_id,title,description,due_date,status) VALUES(?,?,?,?, 'OPEN')")) {
            if (courseId == null || courseId <= 0) ps.setNull(1, Types.BIGINT); else ps.setLong(1, courseId);
            ps.setString(2, title.trim()); ps.setString(3, safe(description));
            if (date == null) ps.setNull(4, Types.DATE); else ps.setDate(4, date);
            ps.executeUpdate();
        }
    }

    public void deleteLab(long id) throws Exception { execute("DELETE FROM labs WHERE id=?", id); }

    public List<Map<String, Object>> listProjects() throws Exception {
        return query("SELECT p.id,p.title,p.description,p.tech_stack,p.status,p.owner_id,u.full_name AS owner_name,p.created_at " +
                "FROM projects p LEFT JOIN app_users u ON u.id=p.owner_id ORDER BY p.id DESC");
    }

    public void createProject(String title, String description, String techStack, Long ownerId) throws Exception {
        requireText(title, "Project title");
        try (Connection con = Database.connect(); PreparedStatement ps = con.prepareStatement(
                "INSERT INTO projects(title,description,tech_stack,status,owner_id) VALUES(?,?,?,'PLANNED',?)")) {
            ps.setString(1, title.trim()); ps.setString(2, safe(description)); ps.setString(3, safe(techStack));
            if (ownerId == null || ownerId <= 0) ps.setNull(4, Types.BIGINT); else ps.setLong(4, ownerId);
            ps.executeUpdate();
        }
    }

    public void deleteProject(long id) throws Exception { execute("DELETE FROM projects WHERE id=?", id); }

    public void updateProjectStatus(long id, String status) throws Exception {
        if (!List.of("PLANNED", "IN_PROGRESS", "COMPLETED", "ON_HOLD").contains(status)) throw new IllegalArgumentException("Select a valid project status.");
        execute("UPDATE projects SET status=? WHERE id=?", status, id);
    }

    public void createSubmission(long labId, long studentId, String url, String remarks) throws Exception {
        requireText(url, "Submission URL");
        if (!(url.trim().startsWith("https://") || url.trim().startsWith("http://"))) {
            throw new IllegalArgumentException("Submission URL must start with http:// or https://.");
        }
        execute("INSERT INTO submissions(lab_id,student_id,submission_url,remarks,status,reviewer_feedback,submitted_at,reviewed_at) " +
                        "VALUES(?,?,?,?,'PENDING',NULL,CURRENT_TIMESTAMP,NULL) " +
                        "ON DUPLICATE KEY UPDATE submission_url=VALUES(submission_url),remarks=VALUES(remarks),status='PENDING',reviewer_feedback=NULL,submitted_at=CURRENT_TIMESTAMP,reviewed_at=NULL",
                labId, studentId, url.trim(), safe(remarks));
    }

    public List<Map<String, Object>> listSubmissions(Long studentId, boolean admin) throws Exception {
        String sql = "SELECT s.id,s.lab_id,s.student_id,s.submission_url,s.remarks,s.status,s.reviewer_feedback,s.submitted_at,s.reviewed_at, " +
                "l.title AS lab_title,u.full_name AS student_name,u.email AS student_email " +
                "FROM submissions s JOIN labs l ON l.id=s.lab_id JOIN app_users u ON u.id=s.student_id " +
                (admin ? "" : "WHERE s.student_id=? ") + "ORDER BY s.submitted_at DESC";
        if (admin) return query(sql);
        return query(sql, studentId);
    }

    public void reviewSubmission(long id, String status, String feedback) throws Exception {
        if (!List.of("PENDING", "REVIEWED", "ACCEPTED", "REJECTED").contains(status)) throw new IllegalArgumentException("Select a valid submission status.");
        execute("UPDATE submissions SET status=?,reviewer_feedback=?,reviewed_at=CURRENT_TIMESTAMP WHERE id=?", status, safe(feedback), id);
    }

    public List<Map<String, Object>> listUsers() throws Exception {
        return query("SELECT id,full_name,email,role,created_at FROM app_users ORDER BY id");
    }

    public void createUser(String name, String email, String password, String role) throws Exception {
        requireText(name, "Full name"); requireText(email, "Email");
        if (!email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Enter a valid email address.");
        if (password == null || password.length() < 8) throw new IllegalArgumentException("Password must contain at least 8 characters.");
        if (!List.of("ADMIN", "STUDENT").contains(role)) throw new IllegalArgumentException("Select a valid role.");
        PasswordUtil.PasswordRecord record = PasswordUtil.hash(password);
        execute("INSERT INTO app_users(full_name,email,password_hash,password_salt,role) VALUES(?,?,?,?,?)",
                name.trim(), email.trim().toLowerCase(), record.hash(), record.salt(), role);
    }

    public Map<String, Long> summary(Long userId, boolean admin) throws Exception {
        Map<String, Long> out = new LinkedHashMap<>();
        out.put("courses", scalar("SELECT COUNT(*) FROM courses"));
        out.put("labs", scalar("SELECT COUNT(*) FROM labs"));
        out.put("projects", scalar("SELECT COUNT(*) FROM projects"));
        out.put("submissions", admin ? scalar("SELECT COUNT(*) FROM submissions") : scalar("SELECT COUNT(*) FROM submissions WHERE student_id=?", userId));
        out.put("pending", admin ? scalar("SELECT COUNT(*) FROM submissions WHERE status='PENDING'") : scalar("SELECT COUNT(*) FROM submissions WHERE student_id=? AND status='PENDING'", userId));
        if (admin) out.put("users", scalar("SELECT COUNT(*) FROM app_users"));
        return out;
    }

    private List<Map<String, Object>> query(String sql, Object... params) throws Exception {
        try (Connection con = Database.connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                int count = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= count; i++) row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    private long scalar(String sql, Object... params) throws Exception {
        try (Connection con = Database.connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    private void execute(String sql, Object... params) throws Exception {
        try (Connection con = Database.connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params); ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws Exception {
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
    }
    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
