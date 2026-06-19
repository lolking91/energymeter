package de.energymeter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * Entry point for the Energy Meter application.
 *
 * <p>Periodically fetches real-time data from the Fox ESS Open API and writes
 * it to InfluxDB for visualization in Grafana.
 */
@SpringBootApplication
@EnableScheduling
public class EnergyMeterApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnergyMeterApplication.class, args);
    }
}
