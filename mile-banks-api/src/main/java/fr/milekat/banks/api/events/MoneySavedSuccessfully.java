package fr.milekat.banks.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a Bukkit event when a new transaction has been saved to the database.
 */
@SuppressWarnings("unused")
public class MoneySavedSuccessfully extends Event {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private final UUID transactionId;
    private final Map<String, Object> tags;
    private final double amount;
    private final String reason;

    /**
     * Constructs a new MoneySavedSuccessfully event with the specified transaction ID, account ID, tags, amount, and
     * @param transactionId The transaction ID.
     * @param tags The tags.
     * @param amount The amount.
     * @param reason The reason.
     */
    public MoneySavedSuccessfully(UUID transactionId, Map<String, Object> tags,
                                  double amount, String reason) {
        super();
        this.transactionId = transactionId;
        this.tags = tags;
        this.amount = amount;
        this.reason = reason;
    }

    /**
     * Gets the transaction ID associated with the transaction.
     * @return transaction ID (A new UUID).
     */
    public UUID getTransactionId() {
        return transactionId;
    }

    /**
     * Gets the tags associated with the transaction.
     * @return transaction tags.
     */
    public Map<String, Object> getTags() {
        return tags;
    }

    /**
     * Gets the amount of money involved in the transaction.
     * @return The amount.
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Gets the reason for the transaction.
     * @return The reason.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Gets the list of event handlers for this event.
     *
     * @return The handler list.
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list.
     */
    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
