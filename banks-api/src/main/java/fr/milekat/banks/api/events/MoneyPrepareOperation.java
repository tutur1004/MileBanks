package fr.milekat.banks.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a Bukkit event when a new transaction is prepared.
 * This event is called before the transaction is saved to the database.
 * This event is cancellable.
 */
@SuppressWarnings("unused")
public class MoneyPrepareOperation extends Event implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean cancelled;
    private final UUID transactionId;
    private final UUID accountId;
    private final Map<String, Object> tags;
    private final double amount;
    private final String reason;

    /**
     * Constructs a new MoneyPrepareOperation with the specified transaction ID, account ID, tags, amount, and reason.
     * @param transactionId The transaction ID.
     * @param accountId The account ID.
     * @param tags The tags.
     * @param amount The amount.
     * @param reason The reason.
     */
    public MoneyPrepareOperation(UUID transactionId, UUID accountId, Map<String, Object> tags,
                                 double amount, String reason) {
        super();
        this.transactionId = transactionId;
        this.accountId = accountId;
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
     * Gets the account ID associated with the transaction.
     * @return account ID (Usually player uuid).
     */
    public UUID getAccountId() {
        return accountId;
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

    /**
     * Checks if the event is cancelled.
     *
     * @return true if the event is cancelled, false otherwise.
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the cancelled state of the event.
     *
     * @param cancel true to cancel the event, false otherwise.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
