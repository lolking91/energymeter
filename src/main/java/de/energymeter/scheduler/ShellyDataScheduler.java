package de.energymeter.scheduler;

import de.energymeter.health.HealthStatus;
import de.energymeter.influx.InfluxDbService;
import de.energymeter.shelly.ShellyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically fetches readings from all configured Shelly devices and
 * persists them in InfluxDB.
 *
 * <p>Reuses the same {@code scheduler.interval-ms} configuration as the Fox
 * ESS scheduler since Shelly devices can be polled at the same cadence
 * (unlike Fox ESS, they have no inherent refresh-rate limit, but 5 minutes
 * matches Grafana's typical panel resolution).
 */
@Component
public class ShellyDataScheduler {

    private static final Logger log = LoggerFactory.getLogger(ShellyDataScheduler.class);

    private final ShellyService shellyService;
    private final InfluxDbService influxDbService;
    private final HealthStatus healthStatus;

    public ShellyDataScheduler(
            ShellyService shellyService,
            InfluxDbService influxDbService,
            HealthStatus healthStatus) {
        this.shellyService = shellyService;
        this.influxDbService = influxDbService;
        this.healthStatus = healthStatus;
    }

    /**
     * Main polling task for all configured {@code pm-mini} Shelly devices.
     */
    @Scheduled(
            fixedRateString = "${scheduler.interval-ms:300000}",
            initialDelayString = "${scheduler.initial-delay-ms:5000}"
    )
    public void collectAndStore() {
        try {
            List<ShellyService.Reading> pmReadings = shellyService.fetchPmMiniReadings();
            for (ShellyService.Reading reading : pmReadings) {
                influxDbService.writeShellyReading(reading);
            }

            List<ShellyService.EmReading> emReadings = shellyService.fetchPro3emReadings();
            for (ShellyService.EmReading reading : emReadings) {
                influxDbService.writeShellyEmReading(reading);
            }

            int total = pmReadings.size() + emReadings.size();
            healthStatus.recordSuccess(total);
            log.info("Stored readings for {} Shelly device(s)", total);
        } catch (Exception e) {
            healthStatus.recordFailure(e.getMessage());
            log.error("Unexpected error during Shelly data collection", e);
        }
    }
}
