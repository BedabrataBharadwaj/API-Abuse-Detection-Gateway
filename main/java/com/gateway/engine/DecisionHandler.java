package com.gateway.engine;

import java.util.List;

import org.springframework.stereotype.Component;

import com.gateway.model.AbuseEvent;
import com.gateway.model.ApiRequest;
import com.gateway.model.Decision;
import com.gateway.model.RiskLevel;
import com.gateway.rule.AbuseRule;

@Component
public class DecisionHandler {

    public Decision decide(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case HIGH -> Decision.BLOCK;
            case MEDIUM -> Decision.ALLOW_WITH_ALERT;
            default -> Decision.ALLOW;
        };
    }

    // Called only when the decision is ALLOW_WITH_ALERT or BLOCK.
    public AbuseEvent createAbuseEvent(ApiRequest request, List<AbuseRule> triggeredRules,
                                       int riskScore, RiskLevel riskLevel, Decision decision) {
        String ruleNames = joinRuleNames(triggeredRules);
        return new AbuseEvent(
                request.getIpAddress(),
                ruleNames,
                riskScore,
                riskLevel,
                request.getTimestamp(),
                decision.name());
    }

    private String joinRuleNames(List<AbuseRule> triggeredRules) {
        StringBuilder names = new StringBuilder();
        for (AbuseRule rule : triggeredRules) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(rule.getRuleName());
        }
        return names.toString();
    }
}