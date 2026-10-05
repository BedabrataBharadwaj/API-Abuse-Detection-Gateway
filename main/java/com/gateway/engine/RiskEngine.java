package com.gateway.engine;

import java.util.List;

import org.springframework.stereotype.Component;

import com.gateway.model.RiskLevel;
import com.gateway.rule.AbuseRule;

@Component
public class RiskEngine {

    private static final int MEDIUM_THRESHOLD = 30;
    private static final int HIGH_THRESHOLD = 60;

    public int calculateScore(List<AbuseRule> triggeredRules) {
        int totalScore = 0;
        for (AbuseRule rule : triggeredRules) {
            totalScore += rule.getRiskPoints();
        }
        return totalScore;
    }

    public RiskLevel calculateRiskLevel(int score) {
        if (score >= HIGH_THRESHOLD) {
            return RiskLevel.HIGH;
        }
        if (score >= MEDIUM_THRESHOLD) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}