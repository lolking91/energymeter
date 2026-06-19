package de.energymeter.shelly;

/**
 * Thrown when a Shelly device's local RPC API is unreachable or returns an error.
 */
public class ShellyApiException extends RuntimeException {

    public ShellyApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
