package fr.milekat.banks.api.exceptions;

import fr.milekat.banks.api.MilekatBanksAPI;

/**
 * Exception thrown when there is an issue during a storage execution.
 */
public class StorageException extends Exception {
    /**
     * Error message
     */
    private final String message;

    /**
     * Constructs a new StorageException with the specified cause and message.
     *
     * @param exception The underlying exception that caused the storage issue.
     * @param message   The error message associated with the storage issue.
     */
    public StorageException(Throwable exception, String message) {
        super(exception);
        this.message = message;
        try {
            if (MilekatBanksAPI.getAPI().isDebug()) {
                exception.printStackTrace();
            }
        } catch (ApiUnavailable ignore) {
            //  Not possible since you can't except a Storage Exception from the API... if the API is not loaded...
        }
    }

    /**
     * Retrieves the error message associated with the storage issue, if it exists.
     *
     * @return The error message, or null if no message is available.
     */
    public String getMessage() {
        return message;
    }
}