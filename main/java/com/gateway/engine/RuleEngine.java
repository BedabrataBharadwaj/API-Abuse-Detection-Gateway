package com.gateway.engine;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.gateway.model.ApiRequest;
import com.gateway.rule.AbuseRule;

@Component
public class RuleEngine {

    private final List<AbuseRule> rules;

    public RuleEngine(List<AbuseRule> rules) {
        this.rules = rules;
    }

    public List<AbuseRule> evaluate(ApiRequest currentRequest, List<ApiRequest> recentRequests) {
        List<AbuseRule> triggeredRules = new ArrayList<>();

        for (AbuseRule rule : rules) {
            if (rule.check(currentRequest, recentRequests)) {
                triggeredRules.add(rule);
            }
        }
        return triggeredRules;
    }
}