package com.gateway.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.model.AbuseEvent;
import com.gateway.model.ApiRequest;
import com.gateway.repository.AbuseEventRepository;
import com.gateway.repository.RequestRepository;

@RestController
@RequestMapping("/logs")
public class LogController {

    private static final Logger log = LoggerFactory.getLogger(LogController.class);
    private static final int MAX_LIMIT = 200;

    private final RequestRepository requestRepository;
    private final AbuseEventRepository abuseEventRepository;

    public LogController(RequestRepository requestRepository,
                         AbuseEventRepository abuseEventRepository) {
        this.requestRepository = requestRepository;
        this.abuseEventRepository = abuseEventRepository;
    }

    @GetMapping("/requests")
    public List<ApiRequest> getRequests(@RequestParam(defaultValue = "50") int limit) {
        return requestRepository.findLatest(normalizeLimit(limit));
    }

    @GetMapping("/events")
    public List<AbuseEvent> getEvents(@RequestParam(defaultValue = "50") int limit) {
        return abuseEventRepository.findLatest(normalizeLimit(limit));
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDatabaseError(DataAccessException e) {
        log.error("Database error while reading logs", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Database is not available"));
    }
}