package fr.milekat.banks.api;

import fr.milekat.banks.api.exceptions.ApiUnavailable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The MileBanksAPI class provides access to the banks API.
 */
public class MileBanksAPI {
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
