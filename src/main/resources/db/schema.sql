-- Bank Mega Auth Service - Database Schema
-- Database: iam_db
-- Description: IAM (Identity and Access Management) schema for authentication and authorization

-- Drop tables if exist (for clean setup)
DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ============================================================================
-- 1. USERS TABLE
-- ============================================================================
-- Core user information for authentication
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);

-- ============================================================================
-- 2. ROLES TABLE
-- ============================================================================
-- Role definitions for RBAC (Role-Based Access Control)
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
);

-- Insert default roles for loan system
INSERT INTO roles (role_name, description) VALUES
    ('CUSTOMER', 'Regular customer applying for loans'),
    ('LOAN_OFFICER', 'Staff who process loan applications'),
    ('APPROVER', 'Manager who approves/rejects loans'),
    ('ADMIN', 'System administrator with full access');

-- ============================================================================
-- 3. USER_ROLES TABLE
-- ============================================================================
-- Many-to-many relationship between users and roles
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id INTEGER NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Indexes for JOIN performance
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- ============================================================================
-- 4. REFRESH_TOKENS TABLE
-- ============================================================================
-- Stores refresh tokens for JWT authentication
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_revoked BOOLEAN DEFAULT false,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for token lookup and cleanup
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_is_revoked ON refresh_tokens(is_revoked);

-- ============================================================================
-- 5. AUDIT_LOG TABLE
-- ============================================================================
-- Security audit trail for authentication events
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    success BOOLEAN NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for audit queries
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_success ON audit_log(success);

-- ============================================================================
-- 6. TEST DATA
-- ============================================================================
-- Insert test admin user
-- Username: admin
-- Password: admin123
-- Password hash generated using BCrypt with strength 12
INSERT INTO users (username, email, password_hash, full_name, phone, is_active)
VALUES (
    'admin',
    'admin@bankmega.com',
    '$2a$12$VmmA9Qx415eEjzu2Qz5O3OljhYwcpIu8f8mQTQzAQgJ4Dcnca6MUG',
    'System Administrator',
    '081234567890',
    true
);

-- Assign ADMIN role to test admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.role_name = 'ADMIN';