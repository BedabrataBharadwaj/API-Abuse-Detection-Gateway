CREATE DATABASE IF NOT EXISTS api_gateway_db;
USE api_gateway_db;

CREATE TABLE IF NOT EXISTS api_requests (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip_address  VARCHAR(45)  NOT NULL,
    endpoint    VARCHAR(255) NOT NULL,
    http_method VARCHAR(10)  NOT NULL,
    status_code INT          NOT NULL,
    timestamp   DATETIME(3)  NOT NULL,
    INDEX idx_requests_ip_time (ip_address, timestamp)
);

CREATE TABLE IF NOT EXISTS abuse_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip_address      VARCHAR(45)  NOT NULL,
    triggered_rules VARCHAR(255) NOT NULL,
    risk_score      INT          NOT NULL,
    risk_level      VARCHAR(10)  NOT NULL,
    timestamp       DATETIME(3)  NOT NULL,
    action          VARCHAR(20)  NOT NULL,
    INDEX idx_events_ip_time (ip_address, timestamp)
);