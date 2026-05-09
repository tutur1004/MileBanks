package fr.milekat.banks.api.exceptions;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Exception thrown when a currency tag is required but absent from the provided tags map.
 * This happens when the plugin is configured with multiple currencies and an operation
 * is attempted without specifying which currency to use.
 */
public class MissingCurrencyException extends StorageException {
    private final List<String> availableCurrencies;

    public MissingCurrencyException(@NotNull List<String> availableCurrencies) {
        super(new IllegalArgumentException(),
                "Tag 'currency' is required when multiple currencies are configured. " +
                "Available: " + availableCurrencies);
        this.availableCurrencies = availableCurrencies;
    }

    /**
     * Returns the list of currencies that are available in the current configuration.
     */
    public List<String> getAvailableCurrencies() {
        return availableCurrencies;
    }
}
