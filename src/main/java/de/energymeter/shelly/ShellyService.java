package de.energymeter.shelly;

import de.energymeter.config.ShellyProperties;
import de.energymeter.shelly.dto.EmStatus;
import de.energymeter.shelly.dto.Pm1Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application-level service for Shelly data retrieval across all configured devices.
 *
 * <p>Supports two device types: {@code pm-mini} (Shelly PM Mini Gen3,
 * single-phase) and {@code pro-3em} (Shelly Pro 3EM, three-phase).
 */
@Service
public class ShellyService {

    private static final Logger log = LoggerFactory.getLogger(ShellyService.class);

    private static final String TYPE_PM_MINI = "pm-mini";
    private static final String TYPE_PRO_3EM = "pro-3em";

    private final ShellyClient client;
    private final ShellyProperties properties;

    public ShellyService(ShellyClient client, ShellyProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * Fetches the current power reading for every configured {@code pm-mini} device.
     *
     * <p>Devices of other (not-yet-supported) types are skipped with a warning.
     * A single unreachable device does not abort the others; the failure is
     * logged and that device is omitted from the result.
     *
     * @return readings for all reachable {@code pm-mini} devices
     */
    public List<Reading> fetchPmMiniReadings() {
        return properties.devices().stream()
                .filter(device -> TYPE_PM_MINI.equals(device.type()))
                .<Reading>mapMulti((device, consumer) -> {
                    try {
                        Pm1Status status = client.fetchPm1Status(device.host());
                        consumer.accept(new Reading(device.name(), status));
                    } catch (ShellyApiException e) {
                        log.warn("Skipping unreachable Shelly device '{}': {}", device.name(), e.getMessage());
                    }
                })
                .toList();
    }

    /**
     * Fetches the current power reading for every configured {@code pro-3em} device.
     *
     * <p>Devices of other (not-yet-supported) types are skipped with a warning.
     * A single unreachable device does not abort the others; the failure is
     * logged and that device is omitted from the result.
     *
     * @return readings for all reachable {@code pro-3em} devices
     */
    public List<EmReading> fetchPro3emReadings() {
        return properties.devices().stream()
                .filter(device -> TYPE_PRO_3EM.equals(device.type()))
                .<EmReading>mapMulti((device, consumer) -> {
                    try {
                        EmStatus status = client.fetchEmStatus(device.host());
                        consumer.accept(new EmReading(device.name(), status));
                    } catch (ShellyApiException e) {
                        log.warn("Skipping unreachable Shelly device '{}': {}", device.name(), e.getMessage());
                    }
                })
                .toList();
    }

    /**
     * A single PM Mini device reading paired with its configured logical name.
     *
     * @param deviceName logical name from configuration (e.g. {@code solar}), used as InfluxDB tag
     * @param status     parsed PM1 status from the device
     */
    public record Reading(String deviceName, Pm1Status status) {
    }

    /**
     * A single Pro 3EM device reading paired with its configured logical name.
     *
     * @param deviceName logical name from configuration (e.g. {@code house-total}), used as InfluxDB tag
     * @param status     parsed EM status from the device
     */
    public record EmReading(String deviceName, EmStatus status) {
    }
}
