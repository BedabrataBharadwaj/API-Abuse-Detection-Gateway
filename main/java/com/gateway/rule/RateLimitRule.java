package com.gateway.rule;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gateway.model.ApiRequest;

@Component
public class RateLimitRule implements AbuseRule {

    private static final String RULE_NAME = "RATE_LIMIT";
    private static final int RISK_POINTS = 40;

    private final int maxRequests;
    private final int windowSeconds;

    public RateLimitRule(@Value("${rate-limit.max-requests}") int maxRequests,
                         @Value("${rate-limit.window-seconds}") int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public boolean check(ApiRequest currentRequest, List<ApiRequest> recentRequests) {
        LocalDateTime windowStart = currentRequest.getTimestamp().minusSeconds(windowSeconds);

        int requestCount = 1; // the current request counts too
        for (ApiRequest oldRequest : recentRequests) {
            if (!oldRequest.getTimestamp().isBefore(windowStart)) {
                requestCount++;
            }
        }
        return requestCount >= maxRequests;
    }

    @Override
    public String getRuleName() { return RULE_NAME; }

    @Override
    public int getRiskPoints() { return RISK_POINTS; }
}