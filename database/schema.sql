-- CampusLab schema reference. The application also creates these tables automatically at first start.
-- Run in MySQL Workbench only if you want to create the database manually first.
CREATE DATABASE IF NOT EXISTS campuslab CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campuslab;

CREATE TABLE IF NOT EXISTS app_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN','STUDENT'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(24) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    semester INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS labs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT,
    due_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lab_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL,
    CONSTRAINT fk_lab_creator FOREIGN KEY (created_by) REFERENCES app_users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(180) NOT NULL,
    description TEXT,
    tech_stack VARCHAR(240),
    status VARCHAR(24) NOT NULL DEFAULT 'PLANNED',
    owner_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES app_users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    submission_url VARCHAR(500) NOT NULL,
    remarks TEXT,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    reviewer_feedback TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    CONSTRAINT fk_submission_lab FOREIGN KEY (lab_id) REFERENCES labs(id) ON DELETE CASCADE,
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT uq_lab_student UNIQUE (lab_id, student_id)
) ENGINE=InnoDB;

-- Demo user records are created by the Java bootstrap so passwords are hashed, never stored as plain text.
