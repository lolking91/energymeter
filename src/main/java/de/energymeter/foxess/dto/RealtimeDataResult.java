package de.energymeter.foxess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Single device entry within the Fox ESS real-time data response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RealtimeDataResult(
        // Device serial number
        String deviceSN,
        // List of variable readings for this device
        List<DeviceVariable> datas
) {
}
