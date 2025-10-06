package fr.milekat.banks.api;

import fr.milekat.banks.api.exceptions.ApiUnavailable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Main entry point for accessing the MileBanks API.
 * <p>
 * This class serves as a facade to retrieve the banks API instance and check its availability.
 * The API must be initialized before use, otherwise an {@link ApiUnavailable} exception will be thrown.
 * </p>
 */
public class MileBanksAPI {
    /**
     * Default constructor.
     * <p>
     * This constructor is provided for instantiation purposes, though typically
     * this class is used through its static methods.
     * </p>
     */
    public MileBanksAPI() {}

    /**
     * Indicates whether the API is ready for use.
     */
    public static boolean API_READY = false;
    /**
     * The loaded API instance.
     */
    public static MileBanksIAPI LOADED_API;

    /**
     * Retrieves the instance of the banks API.
     *
     * @return The banks API instance.
     * @throws ApiUnavailable if the API is not ready.
     */
    @Contract(value = " -> new", pure = true)
    public static @NotNull MileBanksIAPI getAPI() throws ApiUnavailable {
        if (!MileBanksAPI.API_READY) {
            throw new ApiUnavailable();
        }
        return LOADED_API;
    }
}