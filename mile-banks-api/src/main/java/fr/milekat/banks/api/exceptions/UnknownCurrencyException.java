package fr.milekat.banks.api.exceptions;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Exception thrown when a currency tag is present but its value does not match
 * any of the currencies configured in the plugin.
 */
public class UnknownCurrencyException extends StorageException {
    private final String currency;
    private final List<String> availableCurrencies;

    public UnknownCurrencyException(@NotNull String currency, @NotNull List<String> availableCurrencies) {
        super(new IllegalArgumentException(),
                "Currency '" + currency + "' is not configured. Available: " + availableCurrencies);
        this.currency = currency;
        this.availableCurrencies = availableCurrencies;
    }

    /**
     * Returns the unrecognized currency value that was provided.
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Returns the list of currencies that are available in the current configuration.
     */
    public List<String> getAvailableCurrencies() {
        return availableCurrencies;
    }
}
