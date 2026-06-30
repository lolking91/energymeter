package de.energymeter.influx;

import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.Point;
import de.energymeter.foxess.dto.DeviceVariable;
import de.energymeter.shelly.ShellyService;
import de.energymeter.shelly.dto.Pm1Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Writes energy measurement data points to InfluxDB 3.
 *
 * <p>Each Fox ESS variable becomes a field under the {@code energy} measurement,
 * tagged with the device serial number for multi-device support. Points are
 * written at full resolution; downsampling to long-term storage runs
 * server-side via the InfluxDB 3 Processing Engine.
 */
@Service
public class InfluxDbService {

    private static final Logger log = LoggerFactory.getLogger(InfluxDbService.class);

    // InfluxDB measurement name
    private static final String MEASUREMENT = "energy";

    private final InfluxDBClient influxDBClient;

    public InfluxDbService(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    // Fixed device tag used for all Fox ESS writes — single-inverter setup
    private static final String FOXESS_DEVICE_TAG = "foxess-inverter";

    /**
     * Writes a snapshot of Fox ESS device variables to InfluxDB.
     *
     * <p>All variables from {@code variables} are written as fields on a single
     * point with the current timestamp. Variables with a {@code null} value are
     * skipped to avoid gaps in Grafana panels.
     *
     * @param variables list of readings returned by the Fox ESS API
     */
    public void writeDataPoints(List<DeviceVariable> variables) {
        if (variables.isEmpty()) {
            log.warn("No Fox ESS variables to write");
            return;
        }

        Point point = Point.measurement(MEASUREMENT)
                .setTag("device", FOXESS_DEVICE_TAG)
                .setTimestamp(Instant.now());

        for (DeviceVariable variable : variables) {
            if (variable.value() != null) {
                point.setField(variable.variable(), variable.value());
            }
        }

        influxDBClient.writePoint(point);

        log.debug("Wrote {} fields for device '{}' to InfluxDB", variables.size(), FOXESS_DEVICE_TAG);
    }

    /**
     * Writes a single Shelly PM Mini reading to InfluxDB.
     *
     * <p>Uses the same {@code energy} measurement as Fox ESS so that all power
     * sources/consumers can be compared in a single Grafana panel, distinguished
     * by the {@code device} tag.
     *
     * @param reading Shelly reading, tagged with its configured logical device name
     */
    public void writeShellyReading(ShellyService.Reading reading) {
        Pm1Status status = reading.status();

        Point point = Point.measurement(MEASUREMENT)
                .setTag("device", reading.deviceName())
                .setTimestamp(Instant.now());

        if (status.apower() != null) {
            point.setField("power", status.apower());
        }
        if (status.voltage() != null) {
            point.setField("voltage", status.voltage());
        }
        if (status.current() != null) {
            point.setField("current", status.current());
        }
        if (status.freq() != null) {
            point.setField("frequency", status.freq());
        }
        if (status.aenergy() != null && status.aenergy().total() != null) {
            point.setField("energyTotal", status.aenergy().total());
        }

        influxDBClient.writePoint(point);

        log.debug("Wrote Shelly reading for device {} to InfluxDB", reading.deviceName());
    }
}
