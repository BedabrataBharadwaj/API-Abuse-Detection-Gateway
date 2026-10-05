package com.gateway.rule;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gateway.model.ApiRequest;

@Component
public class AuthFailureRule implements AbuseRule {

    private static final String RULE_NAME = "AUTH_FAILURE";
    private static final int RISK_POINTS = 30;
    private static final int UNAUTHORIZED = 401;
    private final String endpointSuffix;

    private final int maxAttempts;
    private final int windowSeconds;

    public AuthFailureRule(@Value("${auth-failure.max-attempts}") int maxAttempts,
                       @Value("${auth-failure.window-seconds}") int windowSeconds,
                       @Value("${auth-failure.endpoint-suffix:}") String endpointSuffix) {
    this.maxAttempts = maxAttempts;
    this.windowSeconds = windowSeconds;
    this.endpointSuffix = endpointSuffix;
}

    @Override
    public boolean check(ApiRequest currentRequest, List<ApiRequest> recentRequests) {
        LocalDateTime windowStart = currentRequest.getTimestamp().minusSeconds(windowSeconds);

        int failedLogins = 0;
        for (ApiRequest oldRequest : recentRequests) {
            boolean insideWindow = !oldRequest.getTimestamp().isBefore(windowStart);
            boolean matchesEndpoint = endpointSuffix.isEmpty() || oldRequest.getEndpoint().endsWith(endpointSuffix);
            boolean failed = oldRequest.getStatusCode() == UNAUTHORIZED;

            if (insideWindow && matchesEndpoint && failed) {
                failedLogins++;
            }
        }
        return failedLogins >= maxAttempts;
    }

    @Override
    public String getRuleName() { return RULE_NAME; }

    @Override
    public int getRiskPoints() { return RISK_POINTS; }
}