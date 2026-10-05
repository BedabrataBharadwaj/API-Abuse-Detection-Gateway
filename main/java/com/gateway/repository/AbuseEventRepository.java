package com.gateway.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.gateway.model.AbuseEvent;
import com.gateway.model.RiskLevel;

@Repository
public class AbuseEventRepository {

    private static final String INSERT_SQL =
            "INSERT INTO abuse_events (ip_address, triggered_rules, risk_score, risk_level, timestamp, action) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_LATEST_SQL =
            "SELECT id, ip_address, triggered_rules, risk_score, risk_level, timestamp, action " +
            "FROM abuse_events ORDER BY id DESC LIMIT ?";

    private final JdbcTemplate jdbcTemplate;

    public AbuseEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(AbuseEvent event) {
        jdbcTemplate.update(INSERT_SQL,
                event.getIpAddress(),
                event.getTriggeredRules(),
                event.getRiskScore(),
                event.getRiskLevel().name(),   // enum -> "HIGH" text
                event.getTimestamp(),
                event.getAction());
    }

    public List<AbuseEvent> findLatest(int limit) {
        return jdbcTemplate.query(SELECT_LATEST_SQL, this::mapRow, limit);
    }

    private AbuseEvent mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNumber) throws SQLException {
        AbuseEvent event = new AbuseEvent(
                rs.getString("ip_address"),
                rs.getString("triggered_rules"),
                rs.getInt("risk_score"),
                RiskLevel.valueOf(rs.getString("risk_level")),   // "HIGH" text -> enum
                rs.getObject("timestamp", LocalDateTime.class),
                rs.getString("action"));
        event.setId(rs.getLong("id"));
        return event;
    }
}