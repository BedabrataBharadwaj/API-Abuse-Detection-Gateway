package com.gateway.model;

import java.time.LocalDateTime;

public class AbuseEvent {

    private Long id;                   // null until saved in the database
    private final String ipAddress;
    private final String triggeredRules;   // e.g. "RATE_LIMIT, BURST"
    private final int riskScore;
    private final RiskLevel riskLevel;
    private final LocalDateTime timestamp;
    private final String action;           // e.g. "ALLOW_WITH_ALERT" or "BLOCK"

    public AbuseEvent(String ipAddress, String triggeredRules, int riskScore,
                      RiskLevel riskLevel, LocalDateTime timestamp, String action) {
        this.ipAddress = ipAddress;
        this.triggeredRules = triggeredRules;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.timestamp = timestamp;
        this.action = action;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIpAddress() { return ipAddress; }
    public String getTriggeredRules() { return triggeredRules; }
    public int getRiskScore() { return riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getAction() { return action; }
}