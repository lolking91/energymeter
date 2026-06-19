package de.energymeter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the InfluxDB 3 connection.
 */
@ConfigurationProperties(prefix = "influxdb")
public record InfluxDbProperties(
        // InfluxDB server URL, e.g. {@code http://influxdb:8181}
        String url,
        // InfluxDB authentication token
        String token,
        // Target database for raw, full-resolution readings
        String database
) {
}
