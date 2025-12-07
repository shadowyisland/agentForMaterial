-- V1__initial_schema.sql

-- User Table
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY, -- Using UUID for primary key
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Storing hashed password
    role VARCHAR(50) NOT NULL, -- e.g., 'ADMIN', 'USER'
    email VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- e.g., 'ACTIVE', 'PENDING_APPROVAL', 'INACTIVE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- File Table
CREATE TABLE files (
    id VARCHAR(36) PRIMARY KEY, -- Using UUID for primary key
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL, -- Actual name on disk/MinIO
    storage_path VARCHAR(1024) NOT NULL, -- Path on disk/MinIO
    file_size BIGINT NOT NULL, -- File size in bytes
    mime_type VARCHAR(255) NOT NULL,
    uploader_id VARCHAR(36) NOT NULL,
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    md5_hash VARCHAR(32), -- MD5 hash for deduplication
    is_deleted BOOLEAN DEFAULT FALSE, -- Logical deletion
    is_indexed BOOLEAN DEFAULT FALSE, -- For P2-01 Search Reservation
    convert_status VARCHAR(50) DEFAULT 'PENDING', -- For P2-02 Preview Reservation, e.g., 'PENDING', 'CONVERTED', 'FAILED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (uploader_id) REFERENCES users(id)
);