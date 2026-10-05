package com.gateway.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.gateway.model.ApiRequest;

@Repository
public class RequestRepository {

    private static final String INSERT_SQL =
            "INSERT INTO api_requests (ip_address, endpoint, http_method, status_code, timestamp) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_SQL =
            "SELECT id, ip_address, endpoint, http_method, status_code, timestamp FROM api_requests ";

    private final JdbcTemplate jdbcTemplate;

    public RequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(ApiRequest request) {
        jdbcTemplate.update(INSERT_SQL,
                request.getIpAddress(),
                request.getEndpoint(),
                request.getHttpMethod(),
                request.getStatusCode(),
                request.getTimestamp());
    }

    // Used by the rules: this IP's requests since a given time.
    public List<ApiRequest> findRecentByIp(String ipAddress, LocalDateTime since) {
        String sql = SELECT_SQL + "WHERE ip_address = ? AND timestamp >= ? ORDER BY timestamp DESC";
        return jdbcTemplate.query(sql, this::mapRow, ipAddress, since);
    }

    // Used by the inspection endpoint: newest requests first.
    public List<ApiRequest> findLatest(int limit) {
        String sql = SELECT_SQL + "ORDER BY id DESC LIMIT ?";
        return jdbcTemplate.query(sql, this::mapRow, limit);
    }

    // RowMapper: converts ONE database row into ONE ApiRequest object.
    private ApiRequest mapRow(ResultSet rs, int rowNumber) throws SQLException {
        if (rowNumber < 0) {
            throw new SQLException("Invalid result set row number: " + rowNumber);
        }

        ApiRequest request = new ApiRequest(
                rs.getString("ip_address"),
                rs.getString("endpoint"),
                rs.getString("http_method"),
                rs.getObject("timestamp", LocalDateTime.class));
        request.setId(rs.getLong("id"));
        request.setStatusCode(rs.getInt("status_code"));
        return request;
    }
}