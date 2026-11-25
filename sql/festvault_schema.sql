CREATE DATABASE IF NOT EXISTS festvault_db;
USE festvault_db;

CREATE TABLE clubs (
    club_id VARCHAR(15) PRIMARY KEY,
    club_name VARCHAR(60) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    user_id VARCHAR(15) PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    email VARCHAR(80) UNIQUE NOT NULL,
    role ENUM('ADMIN', 'FACULTY', 'STUDENT') NOT NULL,
    password_hash VARCHAR(64),
    club_id VARCHAR(15) NULL,
    CONSTRAINT fk_users_club FOREIGN KEY (club_id) REFERENCES clubs(club_id)
);

CREATE TABLE access_passes (
    pass_id VARCHAR(15) PRIMARY KEY,
    student_user_id VARCHAR(15) UNIQUE NOT NULL,
    pin_hash VARCHAR(64) NOT NULL,
    failed_attempts INT DEFAULT 0,
    status ENUM('ACTIVE', 'BLOCKED') DEFAULT 'ACTIVE',
    CONSTRAINT fk_pass_student FOREIGN KEY (student_user_id) REFERENCES users(user_id)
);

CREATE TABLE club_accounts (
    account_no VARCHAR(15) PRIMARY KEY,
    club_id VARCHAR(15) NOT NULL,
    account_name VARCHAR(60) NOT NULL,
    account_type ENUM('MAIN', 'EVENT') NOT NULL,
    balance DECIMAL(12,2) NOT NULL CHECK (balance >= 0),
    locked_amount DECIMAL(12,2) DEFAULT 0.00 CHECK (locked_amount >= 0),
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    CONSTRAINT fk_account_club FOREIGN KEY (club_id) REFERENCES clubs(club_id)
);

CREATE TABLE budget_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    club_id VARCHAR(15) NOT NULL,
    target_account_no VARCHAR(15) NOT NULL,
    requested_amount DECIMAL(12,2) NOT NULL CHECK (requested_amount > 0),
    reason VARCHAR(150) NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_req_club FOREIGN KEY (club_id) REFERENCES clubs(club_id),
    CONSTRAINT fk_req_account FOREIGN KEY (target_account_no) REFERENCES club_accounts(account_no)
);

CREATE TABLE milestone_locks (
    lock_id VARCHAR(15) PRIMARY KEY,
    account_no VARCHAR(15) NOT NULL,
    title VARCHAR(80) NOT NULL,
    locked_amount DECIMAL(12,2) NOT NULL CHECK (locked_amount > 0),
    unlock_date DATE NOT NULL,
    status ENUM('LOCKED', 'RELEASED', 'EARLY_BROKEN') DEFAULT 'LOCKED',
    CONSTRAINT fk_lock_account FOREIGN KEY (account_no) REFERENCES club_accounts(account_no)
);

CREATE TABLE transactions (
    txn_id VARCHAR(25) PRIMARY KEY,
    from_account VARCHAR(15) NULL,
    to_account VARCHAR(15) NULL,
    txn_type ENUM('SPONSOR_INFLOW', 'SUBTEAM_TRANSFER', 'VENDOR_EXPENSE', 'PRIZE_LOCK', 'PRIZE_UNLOCK') NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    sponsor_name VARCHAR(60) NULL,
    bank_ref_id VARCHAR(40) UNIQUE NULL,
    purpose VARCHAR(150) NOT NULL,
    status ENUM('SUCCESS', 'FAILED') NOT NULL,
    txn_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE activity_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    actor_id VARCHAR(15) NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    details VARCHAR(200) NOT NULL,
    logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
