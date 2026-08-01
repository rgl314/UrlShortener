CREATE TABLE url_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_url TEXT NOT NULL,
    short_code VARCHAR(20) NOT NULL UNIQUE,
    click_count INT NOT NULL DEFAULT 0,
    created_by VARCHAR(45),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL
);

CREATE INDEX idx_url_data_short_code
ON url_data(short_code);

CREATE INDEX idx_url_data_created_at
ON url_data(created_at);

CREATE INDEX idx_url_data_expires_at
ON url_data(expires_at);
