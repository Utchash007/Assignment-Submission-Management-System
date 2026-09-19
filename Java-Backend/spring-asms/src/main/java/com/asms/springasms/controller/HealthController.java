package com.asms.springasms.controller;

import com.asms.springasms.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deployment diagnostic: answers without any log access whether the JVM is up
 * and whether Supabase is reachable. Always returns 200 so a dead backend
 * (proxy 500) is distinguishable from a live backend with a broken database.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", "UP");
        try {
            long users = userRepository.count();
            body.put("database", "UP");
            body.put("users", users);
        } catch (Exception ex) {
            body.put("database", "DOWN");
            body.put("error", ex.getClass().getSimpleName());
        }
        return ResponseEntity.ok(body);
    }
}
