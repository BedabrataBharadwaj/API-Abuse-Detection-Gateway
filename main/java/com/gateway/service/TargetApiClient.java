package com.gateway.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TargetApiClient {

    private static final Logger log = LoggerFactory.getLogger(TargetApiClient.class);

    // Never copied in either direction: connection-level headers, plus Host and Content-Length,
    // which must be recalculated for the new connection.
    private static final Set<String> NOT_COPIED = Set.of(
            "host", "content-length", "connection", "keep-alive", "proxy-authenticate",
            "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade", "expect");

    private final RestClient restClient;
    private final String baseUrl;

    public TargetApiClient(@Value("${target-api.base-url}") String baseUrl,
                           @Value("${target-api.timeout-seconds}") int timeoutSeconds) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    // Sends the request to the Target API and returns whatever it answered, even 401/404.
    public ResponseEntity<byte[]> forward(String method, String pathAndQuery,
                                          HttpHeaders requestHeaders, byte[] body) {
        URI targetUri = URI.create(baseUrl + pathAndQuery);
        log.info("Forwarding {} {} to Target API", method, targetUri);

        RestClient.RequestBodySpec request = restClient
                .method(HttpMethod.valueOf(method))
                .uri(targetUri);

        request.headers(headers -> copyHeaders(requestHeaders, headers));
        if (body != null && body.length > 0) {
            request.body(body);
        }

        return request.exchange((clientRequest, clientResponse) -> {
            byte[] responseBody = clientResponse.getBody().readAllBytes();

            HttpHeaders responseHeaders = new HttpHeaders();
            copyHeaders(clientResponse.getHeaders(), responseHeaders);

            return ResponseEntity.status(clientResponse.getStatusCode())
                    .headers(responseHeaders)
                    .body(responseBody);
        });
    }

    private void copyHeaders(HttpHeaders from, HttpHeaders to) {
        from.forEach((name, values) -> {
            if (!NOT_COPIED.contains(name.toLowerCase(Locale.ROOT))) {
                to.addAll(name, values);
            }
        });
    }
}