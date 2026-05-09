package fr.milekat.banks.api;

import fr.milekat.banks.api.exceptions.ApiUnavailable;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jspecify.annotations.NonNull;

/**
 * Main entry point for accessing the MileBanks API.
 * <p>
 * This class serves as a facade to retrieve the banks API instance and check its availability.
 * The API must be initialized before use, otherwise an {@link ApiUnavailable} exception will be thrown.
 * </p>
 */
public class MileBanksAPI {

    public static boolean isDebug() {
        try {
            return getLoadBankAPI().isDebug();
        } catch (ApiUnavailable e) {
            return false;
        }
    }

    private static @NonNull MileBanksIAPI getLoadBankAPI() throws ApiUnavailable {
        RegisteredServiceProvider<MileBanksIAPI> provider =
                Bukkit.getServicesManager().getRegistration(MileBanksIAPI.class);

        if (provider == null) throw new ApiUnavailable();

        return provider.getProvider();
    }
}