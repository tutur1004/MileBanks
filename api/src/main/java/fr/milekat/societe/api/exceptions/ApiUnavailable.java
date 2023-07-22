package fr.milekat.societe.api.exceptions;

/**
 * Exception thrown when the API is unavailable or not loaded.
 */
public class ApiUnavailable extends Exception {
    /**
     * Constructs a new ApiUnavailable exception.
     * This exception is thrown when the API is not loaded.
     */
    public ApiUnavailable() {
        super();
    }
}