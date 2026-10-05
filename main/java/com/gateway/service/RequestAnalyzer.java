package com.gateway.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.gateway.model.ApiRequest;
import com.gateway.repository.RequestRepository;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class RequestAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(RequestAnalyzer.class);
    private static final String GATEWAY_PREFIX = "/gateway";

    private final RequestRepository requestRepository;
    private final int lookbackSeconds;

    public RequestAnalyzer(RequestRepository requestRepository,
                           @Value("${history.lookback-seconds}") int lookbackSeconds) {
        this.requestRepository = requestRepository;
        this.lookbackSeconds = lookbackSeconds;
    }

    // Turns the raw HTTP request into our own ApiRequest (status code not known yet).
    public ApiRequest createApiRequest(HttpServletRequest request) {
        return new ApiRequest(
                getClientIp(request),
                getTargetPath(request),
                request.getMethod(),
                LocalDateTime.now());
    }

    // Loads this client's recent history from MySQL for the rules.
    public List<ApiRequest> findRecentRequests(ApiRequest currentRequest) {
        LocalDateTime since = currentRequest.getTimestamp().minusSeconds(lookbackSeconds);
        try {
            return requestRepository.findRecentByIp(currentRequest.getIpAddress(), since);
        } catch (DataAccessException e) {
            // Fail open: if the database is down, do not punish legitimate clients.
            log.error("Could not load request history for IP {}", currentRequest.getIpAddress(), e);
            return new ArrayList<>();
        }
    }

    public String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    // "/gateway/demo/login" -> "/demo/login"
    public String getTargetPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(GATEWAY_PREFIX.length());
        return path.isEmpty() ? "/" : path;
    }

    // Same as above but keeps "?id=5" so the Target API receives the full query.
    public String getTargetPathWithQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null) {
            return getTargetPath(request);
        }
        return getTargetPath(request) + "?" + query;
    }

    public HttpHeaders getRequestHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        for (String name : Collections.list(request.getHeaderNames())) {
            for (String value : Collections.list(request.getHeaders(name))) {
                headers.add(name, value);
            }
        }
        return headers;
    }
}