package com.gateway.model;

import java.time.LocalDateTime;

public class ApiRequest {

    private Long id;                   // null until saved in the database
    private final String ipAddress;
    private final String endpoint;
    private final String httpMethod;
    private final LocalDateTime timestamp;
    private int statusCode;            // 0 = Target API has not responded yet

    public ApiRequest(String ipAddress, String endpoint, String httpMethod, LocalDateTime timestamp) {
        this.ipAddress = ipAddress;
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIpAddress() { return ipAddress; }
    public String getEndpoint() { return endpoint; }
    public String getHttpMethod() { return httpMethod; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
}