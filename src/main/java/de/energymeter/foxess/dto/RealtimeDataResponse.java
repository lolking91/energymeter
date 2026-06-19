package de.energymeter.foxess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Top-level response wrapper from the Fox ESS Open API
 * {@code /op/v0/device/real/query} endpoint.
 *
 * <pre>
 * {
 *   "errno": 0,
 *   "result": [ { "deviceSN": "...", "datas": [...] } ]
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RealtimeDataResponse(
        // Error code; {@code 0} means success
        int errno,
        // Error message (only populated on failure)
        String msg,
        // List of device results
        List<RealtimeDataResult> result
) {
}
