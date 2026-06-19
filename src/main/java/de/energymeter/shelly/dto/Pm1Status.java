package de.energymeter.shelly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response of the Shelly {@code PM1.GetStatus} RPC method, as returned by the
 * Shelly PM Mini (Gen3) single-phase power meter.
 *
 * <p>Endpoint: {@code GET http://<host>/rpc/PM1.GetStatus?id=0}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Pm1Status(
        // Component instance id (always 0 on the PM Mini, single channel)
        int id,
        // Active power in watts
        Double apower,
        // Voltage in volts
        Double voltage,
        // Current in amperes
        Double current,
        // Grid frequency in Hz
        Double freq,
        // Cumulative energy counters (total/by-minute), see Pm1Energy
        Pm1Energy aenergy
) {

    /**
     * Cumulative energy sub-object of {@link Pm1Status}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pm1Energy(
            // Total energy consumed since device reset, in watt-hours
            Double total
    ) {
    }
}
