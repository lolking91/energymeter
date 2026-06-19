package de.energymeter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: verifies the Spring application context loads without errors.
 *
 * <p>Property overrides prevent the context from requiring real credentials
 * or a live InfluxDB instance during CI.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "foxess.api-key=test-key",
        "foxess.device-sn=test-sn",
        "foxess.base-url=http://localhost:9999",
        "influxdb.url=http://localhost:8181",
        "influxdb.token=test-token",
        "influxdb.database=test-db",
        "shelly.devices[0].name=solar",
        "shelly.devices[0].host=192.0.2.1",
        "shelly.devices[0].type=pm-mini",
        "scheduler.initial-delay-ms=999999999"
})
class EnergyMeterApplicationTests {

    @Test
    void contextLoads() {
        // verifies context starts without errors
    }
}
