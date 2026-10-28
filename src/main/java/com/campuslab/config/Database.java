package com.campuslab.config;

import com.campuslab.security.PasswordUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** Central JDBC connection factory and first-run schema/seed initialisation. */
public final class Database {
    private static volatile boolean initialized = false;
    private static final Object INIT_LOCK = new Object();

    private Database() {}

    public static Connection connect() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = EnvConfig.get("DB_URL", "jdbc:mysql://localhost:3306/campuslab?createDatabaseIfNotExist=true&serverTimezone=UTC");
        String user = EnvConfig.get("DB_USER", "").trim();
        if (user.isEmpty()) {
            throw new IllegalStateException("DB_USER is required. Configure your local .env file or set the DB_USER environment variable.");
        }
        String password = EnvConfig.get("DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }

    public static void initialize() throws Exception {
        if (initialized) return;
        synchronized (INIT_LOCK) {
            if (initialized) return;
            try (Connection connection = connect(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS app_users (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT, full_name VARCHAR(120) NOT NULL, " +
                        "email VARCHAR(190) NOT NULL UNIQUE, password_hash VARCHAR(128) NOT NULL, " +
                        "password_salt VARCHAR(64) NOT NULL, role VARCHAR(20) NOT NULL DEFAULT 'STUDENT', " +
                        "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "CONSTRAINT chk_user_role CHECK (role IN ('ADMIN','STUDENT'))) ENGINE=InnoDB");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS courses (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT, code VARCHAR(24) NOT NULL UNIQUE, " +
                        "name VARCHAR(160) NOT NULL, semester INT NOT NULL, " +
                        "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS labs (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT, course_id BIGINT NULL, title VARCHAR(180) NOT NULL, " +
                        "description TEXT, due_date DATE NULL, status VARCHAR(20) NOT NULL DEFAULT 'OPEN', " +
                        "created_by BIGINT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "CONSTRAINT fk_lab_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL, " +
                        "CONSTRAINT fk_lab_creator FOREIGN KEY (created_by) REFERENCES app_users(id) ON DELETE SET NULL) ENGINE=InnoDB");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS projects (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(180) NOT NULL, description TEXT, " +
                        "tech_stack VARCHAR(240), status VARCHAR(24) NOT NULL DEFAULT 'PLANNED', " +
                        "owner_id BIGINT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES app_users(id) ON DELETE SET NULL) ENGINE=InnoDB");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS submissions (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT, lab_id BIGINT NOT NULL, student_id BIGINT NOT NULL, " +
                        "submission_url VARCHAR(500) NOT NULL, remarks TEXT, status VARCHAR(24) NOT NULL DEFAULT 'PENDING', " +
                        "reviewer_feedback TEXT, submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, reviewed_at TIMESTAMP NULL, " +
                        "CONSTRAINT fk_submission_lab FOREIGN KEY (lab_id) REFERENCES labs(id) ON DELETE CASCADE, " +
                        "CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES app_users(id) ON DELETE CASCADE, " +
                        "CONSTRAINT uq_lab_student UNIQUE (lab_id, student_id)) ENGINE=InnoDB");
                bootstrapAdministrator(connection);
                seedDemoData(connection);
                initialized = true;
            }
        }
    }

    /** Creates the first administrator only when explicitly configured by the operator. */
    private static void bootstrapAdministrator(Connection connection) throws Exception {
        String email = EnvConfig.get("BOOTSTRAP_ADMIN_EMAIL", "").trim();
        String password = EnvConfig.get("BOOTSTRAP_ADMIN_PASSWORD", "");
        String name = EnvConfig.get("BOOTSTRAP_ADMIN_NAME", "CampusLab Administrator").trim();
        if (email.isEmpty() && password.isEmpty()) return;
        if (email.isEmpty() || password.isEmpty()) {
            throw new IllegalStateException("Set both BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD to create the initial administrator.");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters long.");
        }
        try (var check = connection.prepareStatement("SELECT id FROM app_users WHERE LOWER(email)=LOWER(?)")) {
            check.setString(1, email);
            try (ResultSet rs = check.executeQuery()) { if (rs.next()) return; }
        }
        PasswordUtil.PasswordRecord record = PasswordUtil.hash(password);
        try (var insert = connection.prepareStatement("INSERT INTO app_users(full_name,email,password_hash,password_salt,role) VALUES(?,?,?,?,'ADMIN')")) {
            insert.setString(1, name.isEmpty() ? "CampusLab Administrator" : name);
            insert.setString(2, email.toLowerCase(java.util.Locale.ROOT));
            insert.setString(3, record.hash());
            insert.setString(4, record.salt());
            insert.executeUpdate();
        }
    }

    private static void seedDemoData(Connection connection) throws Exception {
        long courseCount = count(connection, "courses");
        if (courseCount == 0) {
            try (var insert = connection.prepareStatement("INSERT INTO courses(code,name,semester) VALUES(?,?,?)")) {
                insert.setString(1, "CS301"); insert.setString(2, "Advanced Java Lab"); insert.setInt(3, 5); insert.executeUpdate();
                insert.setString(1, "CS302"); insert.setString(2, "Database Management Lab"); insert.setInt(3, 5); insert.executeUpdate();
            }
        }
        if (count(connection, "labs") == 0) {
            long courseId = firstId(connection, "SELECT id FROM courses ORDER BY id LIMIT 1");
            try (var insert = connection.prepareStatement("INSERT INTO labs(course_id,title,description,status) VALUES(?,?,?,'OPEN')")) {
                insert.setLong(1, courseId); insert.setString(2, "JDBC CRUD Application"); insert.setString(3, "Build a Java application that creates, reads, updates and deletes MySQL records using JDBC."); insert.executeUpdate();
                insert.setLong(1, courseId); insert.setString(2, "Servlet Form Handling"); insert.setString(3, "Create a servlet and JSP form with server-side validation and a clear result page."); insert.executeUpdate();
            }
        }
        if (count(connection, "projects") == 0) {
            try (var insert = connection.prepareStatement("INSERT INTO projects(title,description,tech_stack,status,owner_id) VALUES(?,?,?,'IN_PROGRESS',NULL)")) {
                insert.setString(1, "CampusLab — College Lab & Project Management System");
                insert.setString(2, "A unified desktop and web system for managing lab exercises, student projects and submissions.");
                insert.setString(3, "Java, Swing, JSP, Servlets, JDBC, MySQL, Maven, Tomcat");
                insert.executeUpdate();
            }
        }
    }

    private static long count(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next(); return rs.getLong(1);
        }
    }

    private static long firstId(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
            throw new IllegalStateException("Unable to initialise sample data: required database record is missing.");
        }
    }
}
