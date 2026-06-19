package de.energymeter.config;

import com.influxdb.v3.client.InfluxDBClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


/**
 * Central Spring configuration: wires up the Fox ESS REST client
 * and the InfluxDB client as managed beans.
 */
@Configuration
@EnableConfigurationProperties({FoxEssProperties.class, InfluxDbProperties.class, ShellyProperties.class})
public class AppConfig {

    /**
     * Pre-configured {@link RestClient} pointing at the Fox ESS Open API base URL.
     *
     * @param props Fox ESS connection properties
     * @return ready-to-use RestClient instance
     */
    @Bean
    public RestClient foxEssRestClient(FoxEssProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }

    /**
     * Generic {@link RestClient} used to call local Shelly device RPC APIs.
     *
     * <p>No base URL is set here since each device has its own host; callers
     * supply the full URI per request.
     *
     * @return ready-to-use RestClient instance
     */
    @Bean
    public RestClient shellyRestClient() {
        return RestClient.builder().build();
    }

    /**
     * InfluxDB 3 client configured from application properties.
     *
     * <p>Writes go to the raw, full-resolution database. Downsampling to the
     * long-term database is handled server-side by the InfluxDB 3 Processing
     * Engine, not by this application.
     *
     * @param props InfluxDB connection properties
     * @return connected InfluxDBClient
     */
    @Bean
    public InfluxDBClient influxDBClient(InfluxDbProperties props) {
        return InfluxDBClient.getInstance(props.url(), props.token().toCharArray(), props.database());
    }
}
