package de.energymeter.shelly;

import de.energymeter.shelly.dto.Pm1Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Low-level HTTP client for the local, unauthenticated Shelly Gen2/Gen3 RPC API.
 *
 * <p>Unlike the Fox ESS cloud API, Shelly devices are queried directly on the
 * local network and require no API key or signature.
 */
@Component
public class ShellyClient {

    private static final Logger log = LoggerFactory.getLogger(ShellyClient.class);

    private final RestClient restClient;

    public ShellyClient(RestClient shellyRestClient) {
        this.restClient = shellyRestClient;
    }

    /**
     * Fetches the single-phase power meter status from a Shelly PM Mini (Gen3).
     *
     * @param host IP address or hostname of the device on the local network
     * @return parsed status; never {@code null}
     * @throws ShellyApiException if the device is unreachable or returns an error
     */
    public Pm1Status fetchPm1Status(String host) {
        String uri = "http://%s/rpc/PM1.GetStatus?id=0".formatted(host);
        log.debug("Calling Shelly RPC API: {}", uri);
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(Pm1Status.class);
        } catch (RestClientException e) {
            throw new ShellyApiException("Failed to reach Shelly device at " + host, e);
        }
    }
}
