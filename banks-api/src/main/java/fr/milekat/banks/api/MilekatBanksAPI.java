package fr.milekat.banks.api;

import fr.milekat.banks.api.exceptions.ApiUnavailable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The SocieteMileKatAPI class provides access to the societe milekat API.
 */
public class MilekatBanksAPI {
    /**
     * Indicates whether the API is ready for use.
     */
    public static boolean API_READY = false;
    /**
     * The loaded API instance.
     */
    public static MilekatBanksIAPI LOADED_API;

    /**
     * Retrieves the instance of the custom shops API.
     *
     * @return The custom shops API instance.
     * @throws ApiUnavailable if the API is not ready.
     */
    @Contract(value = " -> new", pure = true)
    public static @NotNull MilekatBanksIAPI getAPI() throws ApiUnavailable {
        if (!MilekatBanksAPI.API_READY) {
            throw new ApiUnavailable();
        }
        return LOADED_API;
    }
}
