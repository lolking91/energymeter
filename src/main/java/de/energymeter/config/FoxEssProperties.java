package de.energymeter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Fox ESS Open API.
 *
 * <p>All values are injected via environment variables or {@code application.yml}.
 * Never hard-code secrets in source code.
 */
@ConfigurationProperties(prefix = "foxess")
public record FoxEssProperties(
        // Fox ESS Open API key (generated in the Fox ESS app)
        String apiKey,
        // Serial number of the inverter device
        String deviceSn,
        // Base URL of the Fox ESS Open API
        String baseUrl
) {
}
