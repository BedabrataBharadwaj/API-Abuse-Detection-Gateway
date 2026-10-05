package com.gateway.rule;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gateway.model.ApiRequest;

@Component
public class ErrorRateRule implements AbuseRule {

    private static final String RULE_NAME = "ERROR_RATE";
    private static final int RISK_POINTS = 20;

    private final int maxErrors;
    private final int windowSeconds;

    public ErrorRateRule(@Value("${error-rate.max-errors}") int maxErrors,
                         @Value("${error-rate.window-seconds}") int windowSeconds) {
        this.maxErrors = maxErrors;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public boolean check(ApiRequest currentRequest, List<ApiRequest> recentRequests) {
        LocalDateTime windowStart = currentRequest.getTimestamp().minusSeconds(windowSeconds);

        int errorCount = 0;
        for (ApiRequest oldRequest : recentRequests) {
            boolean insideWindow = !oldRequest.getTimestamp().isBefore(windowStart);
            if (insideWindow && isCountedError(oldRequest.getStatusCode())) {
                errorCount++;
            }
        }
        return errorCount >= maxErrors;
    }

    private boolean isCountedError(int statusCode) {
        return statusCode == 401 || statusCode == 403 || statusCode == 404;
    }

    @Override
    public String getRuleName() { return RULE_NAME; }

    @Override
    public int getRiskPoints() { return RISK_POINTS; }
}