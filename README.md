# API Abuse Detection Gateway

A Java/Spring Boot HTTP gateway that sits in front of an API, analyzes each request with configurable rules, assigns a risk score, and either forwards or blocks the request.

## Overview

Clients call the gateway instead of the target API. For every request the gateway identifies the client by IP address, loads that client's recent request history from MySQL, runs four abuse-detection rules, calculates a risk score, and decides: allow, allow with an alert, or block with HTTP 429. Allowed requests are forwarded to the Target API and the response status code is stored so it can be used when judging future requests.

The project includes a small Demo REST API so the gateway can be demonstrated locally. The detection logic does not depend on it.

## Problem Statement

APIs can be abused through excessive requests, request bursts, repeated failed logins, and repeated error-generating requests. This consumes server resources, degrades availability, and can indicate malicious behavior. This project detects and responds to these **predefined behavioral patterns**. It does not claim to detect every possible form of API abuse.

## Objectives

- Monitor incoming requests and identify clients (by IP address).
- Track request frequency and client behavior over time.
- Detect the predefined abuse patterns and assign a risk score.
- Allow legitimate requests; alert on or block suspicious ones.
- Store request and abuse information in MySQL.
- Provide a gateway layer that can be placed in front of compatible HTTP APIs.

## Architecture

```
Client
  ↓
ApiGatewayController
  ↓
RequestAnalyzer  → builds ApiRequest, loads recent history (RequestRepository)
  ↓
RuleEngine       → runs List<AbuseRule>
  ↓
RiskEngine       → score + LOW / MEDIUM / HIGH
  ↓
DecisionHandler  → ALLOW / ALLOW_WITH_ALERT / BLOCK
  ↓
TargetApiClient  → forwards (not for BLOCK) → Target API
  ↓
RequestLogger    → stores request + abuse event (MySQL)
```

## Workflow

1. The client sends a request to `/gateway/<target path>`.
2. The gateway creates an `ApiRequest` and loads this IP's recent requests.
3. Each rule checks the current request plus history.
4. Points of triggered rules are added up; the score gives the risk level.
5. LOW: forward. MEDIUM: store an abuse event, then forward. HIGH: store an abuse event and return HTTP 429 without calling the Target API.
6. For forwarded requests the Target API's status code is captured and the request is stored.
7. Stored status codes are used by `AuthFailureRule` and `ErrorRateRule` for later requests.

## Abuse Types

| Rule | Detects | Default threshold |
|---|---|---|
| `RateLimitRule` | Too many requests in a time window | 100 requests / 60 s |
| `BurstRule` | Too many requests in a very short window | 20 requests / 5 s |
| `AuthFailureRule` | Repeated HTTP 401 responses (all endpoints, or only paths ending with `auth-failure.endpoint-suffix`) | 10 failures / 60 s |
| `ErrorRateRule` | Repeated 401, 403 or 404 responses | 20 errors / 60 s |

## Risk Scoring

| Violation | Points |
|---|---|
| Rate limit | +40 |
| Burst | +30 |
| Authentication failure | +30 |
| Excessive errors | +20 |

| Score | Level | Action |
|---|---|---|
| 0–29 | LOW | Allow |
| 30–59 | MEDIUM | Allow + store abuse event |
| 60+ | HIGH | Block (HTTP 429) + store abuse event |

Points of all rules triggered in one evaluation are added together. A single rule can reach MEDIUM at most; blocking requires two or more violations.

## Database Design

**api_requests**: `id`, `ip_address`, `endpoint`, `http_method`, `status_code`, `timestamp` (DATETIME(3)). Index: `(ip_address, timestamp)`.

**abuse_events**: `id`, `ip_address`, `triggered_rules` (e.g. `BURST, RATE_LIMIT`), `risk_score`, `risk_level`, `timestamp`, `action` (`ALLOW_WITH_ALERT` or `BLOCK`). Index: `(ip_address, timestamp)`.

The composite indexes serve the main query: "this IP's rows since time T".

## Technology Stack

Java 17, Spring Boot 3.3 (Web, JDBC), MySQL 8, JdbcTemplate, Spring `RestClient`, Maven, Postman, Git/GitHub. No JPA/Hibernate, no frontend, no ML.

## Project Structure

```
api-abuse-detection-gateway/
├── pom.xml
├── README.md
├── database/schema.sql
└── src/main/
    ├── java/com/gateway/
    │   ├── GatewayApplication.java
    │   ├── controller/   ApiGatewayController, DemoApiController, LogController
    │   ├── engine/       RuleEngine, RiskEngine, DecisionHandler
    │   ├── model/        ApiRequest, AbuseEvent, RiskLevel, Decision
    │   ├── repository/   RequestRepository, AbuseEventRepository
    │   ├── rule/         AbuseRule, RateLimitRule, BurstRule, AuthFailureRule, ErrorRateRule
    │   └── service/      RequestAnalyzer, RequestLogger, TargetApiClient
    └── resources/application.properties
```

## REST Endpoints

| Method | Path | Purpose |
|---|---|---|
| any | `/gateway/**` | Gateway: analyzes, then forwards or blocks |
| GET | `/demo/hello` | Demo API: 200 |
| POST | `/demo/login` | Demo API: 200, or 401 for wrong credentials (`demo` / `demo123`) |
| GET | `/demo/admin` | Demo API: always 403 |
| GET | `/demo/items/{id}` | Demo API: 200 for ids 1–3, otherwise 404 |
| GET | `/logs/requests?limit=50` | Latest stored requests |
| GET | `/logs/events?limit=50` | Latest stored abuse events |

`/gateway/demo/login` is forwarded to `/demo/login`, and so on.

## Setup Instructions

Requirements: JDK 17+, Maven 3.8+, MySQL 8.

## MySQL Configuration

```
mysql -u root -p < database/schema.sql
```

Credentials are read from environment variables (never hard-coded):

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/api_gateway_db` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | empty |

Rule thresholds are in `application.properties` (`rate-limit.*`, `burst.*`, `auth-failure.*`, `error-rate.*`). `history.lookback-seconds` must be at least as large as the largest rule window.

## How to Run

```
mvn clean compile
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. Call the Demo API through the gateway, for example `GET http://localhost:8080/gateway/demo/hello`.

## Using the Gateway with a Real API

The target is set by the `TARGET_API_URL` environment variable (default: the built-in Demo API on `http://localhost:8080`).

```
# Windows PowerShell
$env:TARGET_API_URL="https://jsonplaceholder.typicode.com"; mvn spring-boot:run

# macOS / Linux
TARGET_API_URL=https://jsonplaceholder.typicode.com mvn spring-boot:run
```

A request to `/gateway/<path>` is forwarded to `<TARGET_API_URL>/<path>` with the same method, query string, body and headers (including `Authorization`). Hop-by-hop headers and `Host` are not copied. The target's status, body and headers are returned unchanged. One gateway instance protects one target API; start a second instance on another port (`SERVER_PORT`) for another API.

`auth-failure.endpoint-suffix` is empty by default, so any HTTP 401 counts as an authentication failure. Set it to `/login` to count only login endpoints.

## Postman Tests

Before each scenario clear the history: `DELETE FROM api_requests; DELETE FROM abuse_events;`. Multi-request scenarios use Postman's Collection Runner (iterations + delay). Expected results below assume the default thresholds.

| # | Scenario | Request | Expected |
|---|---|---|---|
| 1 | Normal request | `GET /gateway/demo/hello` once | 200, LOW, row in `api_requests`, no abuse event |
| 2 | Rate limit | Same GET, 100 iterations, 500 ms delay | 100th request: `RATE_LIMIT`, score 40, MEDIUM, still 200 |
| 3 | Burst | Same GET, 25 iterations, 0 ms delay | From the 20th: `BURST`, score 30, MEDIUM, still 200 |
| 4 | Auth failures | `POST /gateway/demo/login` with wrong password, 12 iterations, 1000 ms delay | From the 11th: `AUTH_FAILURE`, score 30, MEDIUM; responses stay 401 |
| 5 | Error rate | `GET /gateway/demo/items/99`, 25 iterations, 0 ms delay | From the 21st: `BURST` + `ERROR_RATE`, score 50, MEDIUM (error rate alone scores 20 = LOW, so it creates no event on its own) |
| 6 | Multiple rules, HIGH | Wrong-password login, 30 iterations, 0 ms delay | Requests 1–10 LOW, 11–19 MEDIUM (`AUTH_FAILURE`), 20+ HTTP 429 with `AUTH_FAILURE` + `BURST`, score 60 |
| 7 | Blocked requests do not reach the Target API | Scenario 6 | Console shows 19 `Forwarding ...` lines for the 30 requests; the Demo API never returns 429, so every 429 row was produced by the gateway |
| 8 | Allowed requests reach the Target API | Scenario 1 | Body is the Demo API's JSON and a `Forwarding ...` line is logged |
| 9 | Requests stored | `GET /logs/requests` or SQL below | One row per request with status codes |
| 10 | Abuse events stored | `GET /logs/events` or SQL below | Rows with rules, score, level, action |

```sql
SELECT status_code, COUNT(*) FROM api_requests GROUP BY status_code;
SELECT * FROM api_requests ORDER BY id DESC LIMIT 10;
SELECT * FROM abuse_events ORDER BY id DESC LIMIT 10;
```

## OOP Concepts

- **Encapsulation**: models have private fields with getters, and `final` where values never change.
- **Abstraction**: `AbuseRule` defines what a rule offers, not how it works.
- **Polymorphism**: `RuleEngine` calls `rule.check(...)` on a `List<AbuseRule>`; each rule runs its own logic.
- **Composition**: `RuleEngine` has rules; the controller has its collaborators.
- **Exception handling**: database failures, target-API failures and unexpected errors are handled separately.

## JDBC Concepts

- `JdbcTemplate` handles connections, statements and cleanup.
- Parameterized queries (`?`) prevent SQL injection.
- `RowMapper` methods convert one `ResultSet` row into one Java object.
- `SQLException` is translated to the unchecked `DataAccessException`.
- Composite indexes support per-IP time-window queries.

## Limitations

- Detects only the four predefined behavioral patterns, not all forms of API abuse.
- Clients are identified by IP only: clients behind one NAT share a score, and an attacker with many IPs is not linked.
- History is counted from MySQL on each request: cost grows with traffic, and truly simultaneous requests may not see each other.
- `AuthFailureRule` and `ErrorRateRule` react one request late (status is only known after the response).
- A request is stored after the response, so MySQL being down means missing history (fail open).
- One abuse event is stored per evaluated MEDIUM/HIGH request, not one per attack.
- No data retention: tables grow without cleanup.
- One gateway instance fronts one target API (one base URL). Headers are forwarded except hop-by-hop ones; redirects, cookies and bodies are not rewritten, and request/response bodies are held in memory (no streaming or WebSockets).
- `/logs/*` endpoints are not protected.
- The Demo API shares the application with the gateway for convenience; thresholds are global, not per endpoint.

## Future Improvements

- Route to several target APIs by path prefix, and add an `X-Forwarded-For` header.
- Per-endpoint thresholds and an allow/deny list.
- Retry-After header on 429 and a temporary block duration.
- Scheduled cleanup of old rows, and summarizing events per incident.
- Cache recent counts in memory to avoid a query per request.
- Authentication for the `/logs/*` endpoints.
- Unit tests for each rule and integration tests for the gateway flow.
