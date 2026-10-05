package com.gateway.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoApiController {

    // Fixed demo credentials. This is NOT a real authentication system.
    private static final String VALID_USERNAME = "demo";
    private static final String VALID_PASSWORD = "demo123";

    private static final int HIGHEST_ITEM_ID = 3;

    @GetMapping("/hello")                                   // 200
    public Map<String, String> hello() {
        return Map.of("message", "Hello from the Demo API");
    }

    @PostMapping("/login")                                  // 200 or 401
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
            return ResponseEntity.ok(Map.of("message", "Login successful"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid username or password"));
    }

    @GetMapping("/admin")                                   // always 403
    public ResponseEntity<Map<String, String>> admin() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access to this resource is forbidden"));
    }

    @GetMapping("/items/{id}")                              // 200 for ids 1-3, otherwise 404
    public ResponseEntity<Map<String, String>> getItem(@PathVariable int id) {
        if (id >= 1 && id <= HIGHEST_ITEM_ID) {
            return ResponseEntity.ok(Map.of("id", String.valueOf(id), "name", "Item " + id));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Item not found"));
    }
}