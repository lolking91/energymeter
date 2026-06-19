package de.energymeter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for locally polled Shelly devices.
 *
 * <p>Shelly Gen2/Gen3 devices expose an unauthenticated local RPC API, so no
 * secrets are required here beyond the device's IP/hostname on the local network.
 */
@ConfigurationProperties(prefix = "shelly")
public record ShellyProperties(
        // List of configured Shelly devices to poll
        List<Device> devices
) {

    /**
     * A single Shelly device entry.
     *
     * @param name logical name used as the InfluxDB {@code device} tag (e.g. {@code solar})
     * @param host IP address or hostname of the device on the local network
     * @param type device type, determines which RPC component is queried (e.g. {@code pm-mini})
     */
    public record Device(String name, String host, String type) {
    }
}
