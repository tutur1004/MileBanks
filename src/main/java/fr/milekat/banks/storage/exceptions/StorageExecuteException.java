package fr.milekat.banks.storage.exceptions;

import fr.milekat.banks.Main;

public class StorageExecuteException extends Exception {
    private final String message;

    /**
     * Issue during a storage execution
     */
    public StorageExecuteException(Throwable exception, String message) {
        super(exception);
        this.message = message;
        if (Main.DEBUG) {
            Main.stack(exception.getStackTrace());
        }
    }

    /**
     * Get error message (If exist)
     */
    public String getMessage() {
        return message;
    }
}
