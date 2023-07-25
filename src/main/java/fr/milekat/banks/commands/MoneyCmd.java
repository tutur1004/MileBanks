package fr.milekat.banks.commands;

import fr.milekat.banks.Main;
import fr.milekat.banks.listeners.DefaultTags;
import fr.milekat.banks.storage.exceptions.StorageExecuteException;
import fr.milekat.utils.McTools;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MoneyCmd implements TabExecutor {
    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
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
            Action action;
            Player player;
            Map<String, Object> tags;
            int amount;
            try {
                action = Action.valueOf(args[0].toUpperCase(Locale.ROOT));
                player = Bukkit.getPlayer(args[1]);
                assert player != null;
                assert DefaultTags.playerTags.containsKey(player.getUniqueId());
                tags = DefaultTags.playerTags.get(player.getUniqueId());
                if (action.equals(Action.GET)) {
                    try {
                        Main.message(sender, "Player money: " + Main.getStorage().getTagsMoney(tags));
                    } catch (StorageExecuteException e) {
                        throw new RuntimeException(e);
                    }
                } else if (args.length>=3) {
                    amount = Integer.parseInt(args[2]);
                    switch (action) {
                        // TODO: 18/07/2023 Set reason ?
                        case ADD -> Main.getStorage().addMoneyToTags(player.getUniqueId(), tags, amount, "");
                        case REMOVE -> Main.getStorage().removeMoneyToTags(player.getUniqueId(), tags, amount, "");
                        case SET -> Main.getStorage().setMoneyToTags(player.getUniqueId(), tags, amount, "");
                    }
                }
            } catch (Exception exception) {
                Main.message(sender, "&c/money <add/remove/set> <player> <amount>");
                Main.message(sender, exception.getLocalizedMessage());
                return true;
            }
        } else {
            Main.message(sender, "&c/money <add/remove/set> <player> <amount>");
        }
        return true;
    }

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender  Source of the command.  For players tab-completing a
     *                command inside a command block, this will be the player, not
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
            return McTools.getTabArgs(args[0], Arrays.asList("add", "remove", "get", "set"));
        } else if (args.length > 2) {
            return Collections.singletonList("");
        }
        return null;
    }

    enum Action {
        ADD,
        REMOVE,
        GET,
        SET
    }
}
