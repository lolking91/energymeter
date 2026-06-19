package de.energymeter.foxess;

import de.energymeter.foxess.dto.DeviceVariable;
import de.energymeter.foxess.dto.RealtimeDataResult;
import de.energymeter.foxess.dto.RealtimeDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Application-level service for Fox ESS data retrieval.
 *
 * <p>Delegates low-level HTTP calls to {@link FoxEssClient} and exposes a
 * clean interface consumed by the scheduler.
 */
@Service
public class FoxEssService {

    private static final Logger log = LoggerFactory.getLogger(FoxEssService.class);

    private final FoxEssClient client;

    public FoxEssService(FoxEssClient client) {
        this.client = client;
    }

    /**
     * Returns the list of real-time variable readings for the configured device.
     *
     * <p>Returns an empty list if the API response contains no device results
     * (rather than throwing) so the scheduler can record the gap gracefully.
     *
     * @return list of {@link DeviceVariable} instances; never {@code null}
     */
    public List<DeviceVariable> fetchCurrentReadings() {
        RealtimeDataResponse response = client.fetchRealtimeData();

        if (response.result() == null || response.result().isEmpty()) {
            log.warn("Fox ESS API returned no device results");
            return Collections.emptyList();
        }

        RealtimeDataResult first = response.result().getFirst();
        log.debug("Received {} variables from device {}", first.datas().size(), first.deviceSN());
        return first.datas() != null ? first.datas() : Collections.emptyList();
    }
}
