package de.energymeter.foxess;

/**
 * Thrown when the Fox ESS Open API returns an error response or is unreachable.
 */
public class FoxEssApiException extends RuntimeException {

    public FoxEssApiException(String message) {
        super(message);
    }

    public FoxEssApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
