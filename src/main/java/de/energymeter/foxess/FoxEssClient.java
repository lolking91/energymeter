package de.energymeter.foxess;

import de.energymeter.config.FoxEssProperties;
import de.energymeter.foxess.dto.RealtimeDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * Low-level HTTP client for the Fox ESS Open API.
 *
 * <p>Handles request signing: Fox ESS requires an MD5 signature built from
 * the API key, request path and a millisecond timestamp.
 *
 * <p>Signature formula (Fox ESS Open API docs):
 * {@code MD5("token={apiKey}&path={path}&timestamp={timestamp}&lang=en")}
 */
@Component
public class FoxEssClient {

    private static final Logger log = LoggerFactory.getLogger(FoxEssClient.class);

    private static final String REALTIME_PATH = "/op/v0/device/real/query";
    private static final String LANG = "en";

    private final RestClient restClient;
    private final FoxEssProperties props;

    public FoxEssClient(RestClient foxEssRestClient, FoxEssProperties props) {
        this.restClient = foxEssRestClient;
        this.props = props;
    }

    /**
     * Fetches real-time device data for the configured inverter.
     *
     * <p>Variables requested cover PV generation, grid feed-in/draw and
     * load consumption — the core metrics of a Balkonkraftwerk setup.
     *
     * @return parsed response; never {@code null}
     * @throws FoxEssApiException if the API returns a non-zero errno or the call fails
     */
    public RealtimeDataResponse fetchRealtimeData() {
        long timestamp = System.currentTimeMillis();
        String signature = buildSignature(REALTIME_PATH, timestamp);

        Map<String, Object> body = Map.of(
                "sn", props.deviceSn(),
                "variables", List.of(
                        "pvPower",
                        "generationPower",
                        "feedInPower",
                        "gridConsumptionPower",
                        "loadsPower",
                        "SoC"           // battery state-of-charge (N/A on M1-800-E, returns 0)
                )
        );

        log.debug("Calling Fox ESS API: {}", REALTIME_PATH);

        RealtimeDataResponse response = restClient.post()
                .uri(REALTIME_PATH)
                .header("token", props.apiKey())
                .header("timestamp", String.valueOf(timestamp))
                .header("signature", signature)
                .header("lang", LANG)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(RealtimeDataResponse.class);

        if (response == null) {
            throw new FoxEssApiException("Fox ESS API returned an empty response");
        }
        if (response.errno() != 0) {
            throw new FoxEssApiException(
                    "Fox ESS API error %d: %s".formatted(response.errno(), response.msg()));
        }
        return response;
    }

    /**
     * Builds the HMAC-style MD5 signature required by the Fox ESS Open API.
     *
     * @param path      API path (e.g. {@code /op/v0/device/real/query})
     * @param timestamp current Unix epoch in milliseconds
     * @return lowercase hex MD5 digest
     */
    private String buildSignature(String path, long timestamp) {
        String payload = "token=%s&path=%s&timestamp=%d&lang=%s"
                .formatted(props.apiKey(), path, timestamp, LANG);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append("%02x".formatted(b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 is guaranteed by the JVM spec
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
