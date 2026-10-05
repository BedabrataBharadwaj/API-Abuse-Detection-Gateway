package com.gateway.rule;

import java.util.List;

import com.gateway.model.ApiRequest;

public interface AbuseRule {

    /**
     * Returns true if this rule is violated.
     *
     * @param currentRequest the request being analyzed right now (not yet stored)
     * @param recentRequests this IP's stored requests from the recent past
     */
    boolean check(ApiRequest currentRequest, List<ApiRequest> recentRequests);

    /** Short name saved in abuse_events, e.g. "RATE_LIMIT". */
    String getRuleName();

    /** Points added to the risk score when this rule is violated. */
    int getRiskPoints();
}