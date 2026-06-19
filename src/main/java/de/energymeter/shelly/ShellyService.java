package de.energymeter.shelly;

import de.energymeter.config.ShellyProperties;
import de.energymeter.shelly.dto.Pm1Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application-level service for Shelly data retrieval across all configured devices.
 *
 * <p>Currently supports the {@code pm-mini} device type (Shelly PM Mini Gen3,
 * single-phase). The Shelly Pro 3EM (three-phase) devices will be added as a
 * separate device type once the PM Mini integration is verified end-to-end.
 */
@Service
public class ShellyService {

    private static final Logger log = LoggerFactory.getLogger(ShellyService.class);

    private static final String TYPE_PM_MINI = "pm-mini";

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
     * A single device reading paired with its configured logical name.
     *
     * @param deviceName logical name from configuration (e.g. {@code solar}), used as InfluxDB tag
     * @param status     parsed PM1 status from the device
     */
    public record Reading(String deviceName, Pm1Status status) {
    }
}
