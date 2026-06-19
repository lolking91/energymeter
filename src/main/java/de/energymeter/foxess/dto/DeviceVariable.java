package de.energymeter.foxess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single variable returned by the Fox ESS real-time data endpoint.
 *
 * <p>Example: {@code {"variable":"pvPower","unit":"kW","value":0.82}}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeviceVariable(
        // Variable name as defined by Fox ESS (e.g. {@code pvPower}, {@code feedInPower})
        String variable,
        // Physical unit of the value (e.g. {@code kW}, {@code kWh})
        String unit,
        // Current numeric value
        Double value
) {
}
