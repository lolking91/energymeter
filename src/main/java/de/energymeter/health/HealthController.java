package de.energymeter.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple diagnostic endpoint exposing the scheduler's health state.
 *
 * <p>Available at {@code GET /api/health}.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthStatus healthStatus;

    public HealthController(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    /**
     * Returns a JSON object with the last successful/failed fetch timestamps
     * and cumulative run counts.
     *
     * <p>HTTP 200 when the last run was successful (or no run has completed yet),
     * HTTP 503 when the last completed run was a failure.
     *
     * @return response entity with health details
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Instant lastSuccess = healthStatus.getLastSuccess();
        Instant lastFailure = healthStatus.getLastFailure();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("lastSuccess", lastSuccess != null ? lastSuccess.toString() : "never");
        body.put("lastFailure", lastFailure != null ? lastFailure.toString() : "never");
        body.put("lastError", healthStatus.getLastError());
        body.put("successCount", healthStatus.getSuccessCount());
        body.put("failureCount", healthStatus.getFailureCount());

        boolean healthy = lastFailure == null
                || (lastSuccess != null && lastSuccess.isAfter(lastFailure));
        body.put("status", healthy ? "UP" : "DOWN");

        return healthy
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(503).body(body);
    }
}
