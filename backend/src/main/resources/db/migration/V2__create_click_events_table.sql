CREATE TABLE click_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url_data_id BIGINT NOT NULL,
    clicked_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    referer VARCHAR(512),
    country VARCHAR(100),
    city VARCHAR(100),
    CONSTRAINT fk_click_events_url_data
            FOREIGN KEY (url_data_id)
            REFERENCES url_data(id)
            ON DELETE CASCADE
);

CREATE INDEX idx_click_events_url_data_id
ON click_events(url_data_id);

CREATE INDEX idx_click_events_clicked_at
ON click_events(clicked_at);
