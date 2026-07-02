package de.energymeter.shelly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response of the Shelly {@code EM.GetStatus} RPC method, as returned by the
 * Shelly Pro 3EM (Gen3) three-phase power meter.
 *
 * <p>Endpoint: {@code GET http://<host>/rpc/EM.GetStatus?id=0}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EmStatus(
        // Component instance id (always 0, single EM component)
        int id,
        // Phase A: current (A), voltage (V), active power (W), apparent power (VA), power factor
        @JsonProperty("a_current") Double aCurrent,
        @JsonProperty("a_voltage") Double aVoltage,
        @JsonProperty("a_act_power") Double aActivePower,
        @JsonProperty("a_aprt_power") Double aApparentPower,
        @JsonProperty("a_pf") Double aPowerFactor,
        // Phase B
        @JsonProperty("b_current") Double bCurrent,
        @JsonProperty("b_voltage") Double bVoltage,
        @JsonProperty("b_act_power") Double bActivePower,
        @JsonProperty("b_aprt_power") Double bApparentPower,
        @JsonProperty("b_pf") Double bPowerFactor,
        // Phase C
        @JsonProperty("c_current") Double cCurrent,
        @JsonProperty("c_voltage") Double cVoltage,
        @JsonProperty("c_act_power") Double cActivePower,
        @JsonProperty("c_aprt_power") Double cApparentPower,
        @JsonProperty("c_pf") Double cPowerFactor,
        // Totals across all three phases
        @JsonProperty("total_current") Double totalCurrent,
        @JsonProperty("total_act_power") Double totalActivePower,
        @JsonProperty("total_aprt_power") Double totalApparentPower
) {
}
