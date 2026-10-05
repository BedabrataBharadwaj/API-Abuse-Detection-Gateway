package com.gateway.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.gateway.engine.DecisionHandler;
import com.gateway.engine.RiskEngine;
import com.gateway.engine.RuleEngine;
import com.gateway.model.AbuseEvent;
import com.gateway.model.ApiRequest;
import com.gateway.model.Decision;
import com.gateway.model.RiskLevel;
import com.gateway.rule.AbuseRule;
import com.gateway.service.RequestAnalyzer;
import com.gateway.service.RequestLogger;
import com.gateway.service.TargetApiClient;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ApiGatewayController {

    private static final Logger log = LoggerFactory.getLogger(ApiGatewayController.class);

    private final RequestAnalyzer requestAnalyzer;
    private final RuleEngine ruleEngine;
    private final RiskEngine riskEngine;
    private final DecisionHandler decisionHandler;
    private final RequestLogger requestLogger;
    private final TargetApiClient targetApiClient;

    public ApiGatewayController(RequestAnalyzer requestAnalyzer,
                                RuleEngine ruleEngine,
                                RiskEngine riskEngine,
                                DecisionHandler decisionHandler,
                                RequestLogger requestLogger,
                                TargetApiClient targetApiClient) {
        this.requestAnalyzer = requestAnalyzer;
        this.ruleEngine = ruleEngine;
        this.riskEngine = riskEngine;
        this.decisionHandler = decisionHandler;
        this.requestLogger = requestLogger;
        this.targetApiClient = targetApiClient;
    }

    @RequestMapping("/gateway/**")   // every HTTP method, every path under /gateway
    public ResponseEntity<?> handleRequest(HttpServletRequest request,
                                           @RequestBody(required = false) byte[] body) {
        // 1. Analyze the request and load recent history
        ApiRequest apiRequest = requestAnalyzer.createApiRequest(request);
        List<ApiRequest> recentRequests = requestAnalyzer.findRecentRequests(apiRequest);

        // 2. Rules -> risk -> decision
        List<AbuseRule> triggeredRules = ruleEngine.evaluate(apiRequest, recentRequests);
        int riskScore = riskEngine.calculateScore(triggeredRules);
        RiskLevel riskLevel = riskEngine.calculateRiskLevel(riskScore);
        Decision decision = decisionHandler.decide(riskLevel);

        // 3. MEDIUM and HIGH: store an abuse event
        AbuseEvent abuseEvent = null;
        if (decision != Decision.ALLOW) {
            abuseEvent = decisionHandler.createAbuseEvent(
                    apiRequest, triggeredRules, riskScore, riskLevel, decision);
            requestLogger.logAbuseEvent(abuseEvent);
        }

        // 4. HIGH: block. LOW/MEDIUM: forward.
        if (decision == Decision.BLOCK) {
            return buildBlockedResponse(apiRequest, abuseEvent);
        }
        return forwardToTargetApi(apiRequest, request, body);
    }

    private ResponseEntity<?> buildBlockedResponse(ApiRequest apiRequest, AbuseEvent abuseEvent) {
        apiRequest.setStatusCode(HttpStatus.TOO_MANY_REQUESTS.value());   // 429
        requestLogger.logRequest(apiRequest);

        Map<String, Object> responseBody = Map.of(
                "error", "Too Many Requests",
                "riskScore", abuseEvent.getRiskScore(),
                "triggeredRules", abuseEvent.getTriggeredRules());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(responseBody);
    }

    private ResponseEntity<?> forwardToTargetApi(ApiRequest apiRequest,
                                                 HttpServletRequest request, byte[] body) {
        try {
            ResponseEntity<byte[]> targetResponse = targetApiClient.forward(
            request.getMethod(),
            requestAnalyzer.getTargetPathWithQuery(request),
            requestAnalyzer.getRequestHeaders(request),
            body);

            apiRequest.setStatusCode(targetResponse.getStatusCode().value());
            requestLogger.logRequest(apiRequest);
            return targetResponse;

        } catch (RestClientException e) {
            log.error("Target API call failed for {} {}", apiRequest.getHttpMethod(), apiRequest.getEndpoint(), e);
            apiRequest.setStatusCode(HttpStatus.BAD_GATEWAY.value());     // 502
            requestLogger.logRequest(apiRequest);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Target API is not reachable"));
        }
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedError(RuntimeException e) {
        log.error("Unexpected gateway error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Gateway error"));
    }
}