package com.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.gateway.model.AbuseEvent;
import com.gateway.model.ApiRequest;
import com.gateway.repository.AbuseEventRepository;
import com.gateway.repository.RequestRepository;

@Component
public class RequestLogger {

    private static final Logger log = LoggerFactory.getLogger(RequestLogger.class);

    private final RequestRepository requestRepository;
    private final AbuseEventRepository abuseEventRepository;

    public RequestLogger(RequestRepository requestRepository,
                         AbuseEventRepository abuseEventRepository) {
        this.requestRepository = requestRepository;
        this.abuseEventRepository = abuseEventRepository;
    }

    public void logRequest(ApiRequest request) {
        try {
            requestRepository.save(request);
        } catch (DataAccessException e) {
            log.error("Could not save request from IP {}", request.getIpAddress(), e);
        }
    }

    public void logAbuseEvent(AbuseEvent event) {
        try {
            abuseEventRepository.save(event);
        } catch (DataAccessException e) {
            log.error("Could not save abuse event for IP {}", event.getIpAddress(), e);
        }
    }
}