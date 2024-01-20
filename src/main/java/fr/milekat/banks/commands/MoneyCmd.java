package fr.milekat.banks.commands;

import fr.milekat.banks.Main;
import fr.milekat.banks.storage.exceptions.StorageExecuteException;
import fr.milekat.banks.storage.exceptions.StorageLoaderException;
import fr.milekat.utils.McTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoneyCmd implements TabExecutor {
    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the playerUuid.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length>=2) {
            try {
                MoneyAction moneyAction = MoneyAction.valueOf(args[0].toUpperCase(Locale.ROOT));
                Player player = Bukkit.getPlayerExact(args[1]);
                if (player==null) {
                    Main.message(sender, "&cPlayer not found.");
                    return true;
                }
                if (!Main.playerTags.containsKey(player.getUniqueId())) {
                    Main.message(sender, "&cPlayer not found in database.");
                    return true;
                }
                Map<String, Object> tags = Main.playerTags.get(player.getUniqueId());
                if (moneyAction.equals(MoneyAction.GET)) {
                    try {
                        Main.message(sender, "Player money: " + Main.getStorage().getTagsMoney(tags));
                    } catch (StorageExecuteException e) {
                        throw new RuntimeException(e);
                    }
                } else if (args.length >= 3) {
                    int amount = Integer.parseInt(args[2]);
                    String reason = "Command";
                    if (args.length == 4) {
                        reason = args[3];
                    }
                    switch (moneyAction) {
                        case ADD -> Main.getStorage().addMoneyToTags(player.getUniqueId(), tags, amount, reason);
                        case REMOVE -> Main.getStorage().removeMoneyToTags(player.getUniqueId(), tags, amount, reason);
                        case SET -> Main.getStorage().setMoneyToTags(player.getUniqueId(), tags, amount, reason);
                    }
                } else if (moneyAction.equals(MoneyAction.TAGS)) {
                    Main.message(sender, "Player tags:");
                    tags.forEach((key, value) -> sender.sendMessage(ChatColor.translateAlternateColorCodes(
                            '&', "&r - &e" + key + "&f: &b" + value)));
                } else sendHelp(sender, label);
            } catch (Exception exception) {
                Main.message(sender, exception.getLocalizedMessage());
                Main.message(sender, "&cInvalid command usage, see /" + label + " help");
            }
        } else if (args.length==1) {
            if (args[0].equalsIgnoreCase("reload")) {
                Main.message(sender, "Reloading plugin..");
                Main.reloadConfigs();
                try {
                    Main.reloadStorage();
                    Main.message(sender, "Plugin reloaded!");
                } catch (StorageLoaderException e) {
                    Main.message(sender, "&cFatal storage error: " + e.getLocalizedMessage());
                    Main.stack(e.getStackTrace());
                    Main.getInstance().onDisable();
                }
            } else sendHelp(sender, label);
        } else sendHelp(sender, label);
        return true;
    }

    private void sendHelp(@NotNull CommandSender sender, String lbl){
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.",
                "add <playerUuid> <amount> [reason]&r: &eAdd money to a playerUuid's balance"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.",
                "remove <playerUuid> <amount> [reason]&r: &eRemove money from playerUuid's balance"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.",
                "set <playerUuid> <amount> [reason]&r: &eSet playerUuid's money balance"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.",
                "get <playerUuid>&r: &eGet balance of a playerUuid"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.",
                "tags <playerUuid>&r: &eGet list of tags of a playerUuid"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.",
                "reload&r: &eReload the plugin"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.",
                "help&r: &eShow this help message"
        ));
    }

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender  Source of the command.  For players tab-completing a
     *                command inside a command block, this will be the playerUuid, not
     *                the command block.
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    The arguments passed to the command, including final
     *                partial argument to be completed
     * @return A List of possible completions for the final argument, or null
     * to default to the command executor
     */
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length<=1) {
            return McTools.getTabArgs(args[0], Arrays.asList("add", "remove", "get", "set", "tags", "reload", "help"));
        }
        return null;
    }

    enum MoneyAction {
        ADD,
        REMOVE,
        GET,
        SET,
        TAGS
    }
}
