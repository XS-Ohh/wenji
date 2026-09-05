DROP TABLE IF EXISTS ai_route_logs;
DROP TABLE IF EXISTS checkins;
DROP TABLE IF EXISTS plan_items;
DROP TABLE IF EXISTS study_plans;
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS culture_resources;
DROP TABLE IF EXISTS culture_categories;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    points INT NOT NULL DEFAULT 0,
    level INT NOT NULL DEFAULT 1,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE culture_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(100),
    default_cover VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE culture_resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50),
    address VARCHAR(255) NOT NULL,
    longitude DECIMAL(10,6),
    latitude DECIMAL(10,6),
    summary VARCHAR(500),
    description CLOB,
    opening_hours VARCHAR(255),
    ticket_info VARCHAR(255),
    recommended_minutes INT NOT NULL DEFAULT 90,
    cover_image VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    view_count INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    average_rating DECIMAL(3,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_favorite_user_resource UNIQUE (user_id, resource_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_favorite_resource FOREIGN KEY (resource_id) REFERENCES culture_resources(id)
);

CREATE TABLE study_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE plan_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    visit_date DATE,
    start_time TIME,
    end_time TIME,
    sort_order INT NOT NULL,
    transportation VARCHAR(50),
    reason VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_plan FOREIGN KEY (plan_id) REFERENCES study_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_resource FOREIGN KEY (resource_id) REFERENCES culture_resources(id)
);

CREATE TABLE checkins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT,
    resource_id BIGINT NOT NULL,
    checkin_time TIMESTAMP NOT NULL,
    image_url VARCHAR(255),
    content VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    audit_comment VARCHAR(255),
    audited_by BIGINT,
    audited_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_checkin_user_resource UNIQUE (user_id, resource_id),
    CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_checkin_plan FOREIGN KEY (plan_id) REFERENCES study_plans(id),
    CONSTRAINT fk_checkin_resource FOREIGN KEY (resource_id) REFERENCES culture_resources(id),
    CONSTRAINT fk_checkin_auditor FOREIGN KEY (audited_by) REFERENCES users(id)
);

CREATE TABLE ai_route_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT,
    request_json CLOB NOT NULL,
    response_json CLOB,
    provider VARCHAR(50),
    model_name VARCHAR(100),
    status VARCHAR(20),
    error_message VARCHAR(500),
    duration_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_log_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ai_log_plan FOREIGN KEY (plan_id) REFERENCES study_plans(id)
);
