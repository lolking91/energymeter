package de.energymeter.scheduler;

import de.energymeter.config.FoxEssProperties;
import de.energymeter.foxess.FoxEssApiException;
import de.energymeter.foxess.FoxEssService;
import de.energymeter.foxess.dto.DeviceVariable;
import de.energymeter.health.HealthStatus;
import de.energymeter.influx.InfluxDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically fetches real-time data from the Fox ESS API and persists it
 * in InfluxDB.
 *
 * <p>The polling interval is configured via {@code scheduler.interval-ms}
 * (default: {@code 300000} ms = 5 minutes). Fox ESS updates device data
 * approximately every 5 minutes, so polling more frequently is not beneficial.
 *
 * <p>Disabled entirely when {@code foxess.enabled=false} (e.g. while only
 * the Shelly integration is in use).
 */
@Component
@ConditionalOnProperty(prefix = "foxess", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FoxessDataScheduler {

    private static final Logger log = LoggerFactory.getLogger(FoxessDataScheduler.class);

    private final FoxEssService foxEssService;
    private final InfluxDbService influxDbService;
    private final FoxEssProperties foxEssProperties;
    private final HealthStatus healthStatus;

    public FoxessDataScheduler(
            FoxEssService foxEssService,
            InfluxDbService influxDbService,
            FoxEssProperties foxEssProperties,
            HealthStatus healthStatus) {
        this.foxEssService = foxEssService;
        this.influxDbService = influxDbService;
        this.foxEssProperties = foxEssProperties;
        this.healthStatus = healthStatus;
    }

    /**
     * Main polling task. Runs every {@code scheduler.interval-ms} milliseconds
     * with an initial delay of 5 seconds to allow the application context to
     * fully start up before the first API call.
     */
    @Scheduled(
            fixedRateString = "${scheduler.interval-ms:300000}",
            initialDelayString = "${scheduler.initial-delay-ms:5000}"
    )
    public void collectAndStore() {
        log.info("Fetching Fox ESS data for device {}", foxEssProperties.deviceSn());
        try {
            List<DeviceVariable> readings = foxEssService.fetchCurrentReadings();
            influxDbService.writeDataPoints(foxEssProperties.deviceSn(), readings);
            healthStatus.recordSuccess(readings.size());
            log.info("Stored {} variables for device {}", readings.size(), foxEssProperties.deviceSn());
        } catch (FoxEssApiException e) {
            healthStatus.recordFailure(e.getMessage());
            log.error("Failed to fetch Fox ESS data: {}", e.getMessage());
        } catch (Exception e) {
            healthStatus.recordFailure(e.getMessage());
            log.error("Unexpected error during data collection", e);
        }
    }
}
