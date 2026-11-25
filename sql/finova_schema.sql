CREATE DATABASE IF NOT EXISTS finova;
USE finova;

CREATE TABLE clubs (
    club_id VARCHAR(15) PRIMARY KEY,
    club_name VARCHAR(60) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(60) NOT NULL,
    role ENUM('ADMIN', 'FACULTY', 'STUDENT') NOT NULL,
    password_hash VARCHAR(64) NOT NULL,
    club_id VARCHAR(15) NULL,
    failed_attempts INT DEFAULT 0,
    is_blocked BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_users_club FOREIGN KEY (club_id) REFERENCES clubs(club_id)
);

CREATE TABLE club_accounts (
    account_no VARCHAR(15) PRIMARY KEY,
    club_id VARCHAR(15) NOT NULL,
    account_name VARCHAR(60) NOT NULL,
    account_type ENUM('MAIN', 'EVENT') NOT NULL,
    balance DECIMAL(12,2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    CONSTRAINT fk_account_club FOREIGN KEY (club_id) REFERENCES clubs(club_id)
);

CREATE TABLE transactions (
    txn_id VARCHAR(25) PRIMARY KEY,
    from_account VARCHAR(15) NULL,
    to_account VARCHAR(15) NULL,
    txn_type ENUM('SPONSOR_INFLOW', 'SUBTEAM_TRANSFER', 'VENDOR_EXPENSE') NOT NULL,
    amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    sponsor_name VARCHAR(60) NULL,
    bank_ref_id VARCHAR(40) UNIQUE NULL,
    purpose VARCHAR(150) NOT NULL,
    status ENUM('SUCCESS', 'FAILED') NOT NULL,
    txn_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_from_account FOREIGN KEY (from_account) REFERENCES club_accounts(account_no),
    CONSTRAINT fk_transaction_to_account FOREIGN KEY (to_account) REFERENCES club_accounts(account_no)
);

INSERT INTO users (username, full_name, role, password_hash, club_id)
VALUES ('admin', 'Central Admin', 'ADMIN', 'admin123', NULL);
