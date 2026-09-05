SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS wenji CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE wenji;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ai_route_logs;
DROP TABLE IF EXISTS user_achievements;
DROP TABLE IF EXISTS achievements;
DROP TABLE IF EXISTS study_notes;
DROP TABLE IF EXISTS checkins;
DROP TABLE IF EXISTS plan_items;
DROP TABLE IF EXISTS study_plans;
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS culture_resources;
DROP TABLE IF EXISTS culture_categories;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    points INT NOT NULL DEFAULT 0,
    level INT NOT NULL DEFAULT 1,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE culture_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(100),
    default_cover VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE culture_resources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50),
    address VARCHAR(255) NOT NULL,
    longitude DECIMAL(10,6),
    latitude DECIMAL(10,6),
    summary VARCHAR(500),
    description TEXT,
    opening_hours VARCHAR(255),
    ticket_info VARCHAR(255),
    recommended_minutes INT NOT NULL DEFAULT 90,
    cover_image VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    view_count INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    average_rating DECIMAL(3,2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_resource_category FOREIGN KEY (category_id) REFERENCES culture_categories(id),
    CONSTRAINT chk_resource_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
    CONSTRAINT chk_resource_minutes CHECK (recommended_minutes > 0),
    INDEX idx_resource_search (status, city, category_id),
    INDEX idx_resource_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_favorite_user_resource UNIQUE (user_id, resource_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_favorite_resource FOREIGN KEY (resource_id) REFERENCES culture_resources(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE study_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    budget DECIMAL(10,2),
    interests VARCHAR(255),
    start_location VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    progress INT NOT NULL DEFAULT 0,
    ai_generated TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_plan_dates CHECK (start_date <= end_date),
    CONSTRAINT chk_plan_budget CHECK (budget IS NULL OR budget >= 0),
    CONSTRAINT chk_plan_progress CHECK (progress BETWEEN 0 AND 100),
    CONSTRAINT chk_plan_status CHECK (status IN ('DRAFT', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    INDEX idx_plan_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE plan_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    visit_date DATE,
    start_time TIME,
    end_time TIME,
    sort_order INT NOT NULL,
    transportation VARCHAR(50),
    reason VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_plan FOREIGN KEY (plan_id) REFERENCES study_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_resource FOREIGN KEY (resource_id) REFERENCES culture_resources(id),
    CONSTRAINT chk_item_times CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time),
    INDEX idx_item_plan_order (plan_id, visit_date, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE checkins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plan_id BIGINT,
    resource_id BIGINT NOT NULL,
    checkin_time DATETIME NOT NULL,
    image_url VARCHAR(255),
    content VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    audit_comment VARCHAR(255),
    audited_by BIGINT,
    audited_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_checkin_user_resource UNIQUE (user_id, resource_id),
    CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_checkin_plan FOREIGN KEY (plan_id) REFERENCES study_plans(id),
    CONSTRAINT fk_checkin_resource FOREIGN KEY (resource_id) REFERENCES culture_resources(id),
    CONSTRAINT fk_checkin_auditor FOREIGN KEY (audited_by) REFERENCES users(id),
    CONSTRAINT chk_checkin_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    INDEX idx_checkin_user_status (user_id, status),
    INDEX idx_checkin_review (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE study_notes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plan_id BIGINT,
    resource_id BIGINT,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    image_urls TEXT,
    tags VARCHAR(255),
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_note_plan FOREIGN KEY (plan_id) REFERENCES study_plans(id),
    CONSTRAINT fk_note_resource FOREIGN KEY (resource_id) REFERENCES culture_resources(id),
    CONSTRAINT chk_note_visibility CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    INDEX idx_note_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE achievements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    icon_url VARCHAR(255),
    condition_type VARCHAR(50) NOT NULL,
    condition_value INT NOT NULL,
    reward_points INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_achievements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    achievement_id BIGINT NOT NULL,
    unlocked_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_achievement UNIQUE (user_id, achievement_id),
    CONSTRAINT fk_user_achievement_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_achievement_achievement FOREIGN KEY (achievement_id) REFERENCES achievements(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_route_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plan_id BIGINT,
    request_json TEXT NOT NULL,
    response_json LONGTEXT,
    provider VARCHAR(50),
    model_name VARCHAR(100),
    status VARCHAR(20),
    error_message VARCHAR(500),
    duration_ms INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_log_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ai_log_plan FOREIGN KEY (plan_id) REFERENCES study_plans(id),
    INDEX idx_ai_log_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
