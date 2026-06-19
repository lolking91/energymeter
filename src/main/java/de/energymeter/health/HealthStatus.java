package de.energymeter.health;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory state bean that tracks the outcome of the last scheduler run.
 *
 * <p>Thread-safe via {@link AtomicReference} / {@link AtomicInteger} so the
 * HTTP health endpoint can read it concurrently with the scheduler thread.
 */
@Component
public class HealthStatus {

    private final AtomicReference<Instant> lastSuccess = new AtomicReference<>();
    private final AtomicReference<Instant> lastFailure = new AtomicReference<>();
    private final AtomicReference<String> lastError = new AtomicReference<>("none");
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);

    /**
     * Records a successful data collection run.
     *
     * @param variablesWritten number of variables persisted in InfluxDB
     */
    public void recordSuccess(int variablesWritten) {
        lastSuccess.set(Instant.now());
        successCount.incrementAndGet();
        lastError.set("none");
    }

    /**
     * Records a failed data collection run.
     *
     * @param errorMessage human-readable description of the failure
     */
    public void recordFailure(String errorMessage) {
        lastFailure.set(Instant.now());
        failureCount.incrementAndGet();
        lastError.set(errorMessage != null ? errorMessage : "unknown error");
    }

    /** @return timestamp of the last successful run, or {@code null} if none yet */
    public Instant getLastSuccess() { return lastSuccess.get(); }

    /** @return timestamp of the last failed run, or {@code null} if none yet */
    public Instant getLastFailure() { return lastFailure.get(); }

    /** @return error message from the last failure */
    public String getLastError() { return lastError.get(); }

    /** @return cumulative count of successful runs since application start */
    public int getSuccessCount() { return successCount.get(); }

    /** @return cumulative count of failed runs since application start */
    public int getFailureCount() { return failureCount.get(); }
}
