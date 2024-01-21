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

import java.util.*;
import java.util.stream.Collectors;

public class MoneyCmd implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length>=2) {
            try {
                MoneyAction moneyAction = MoneyAction.valueOf(args[0].toUpperCase(Locale.ROOT));
                Map<String, Object> tags = new HashMap<>();
                if (!moneyAction.equals(MoneyAction.TAGS)) {
                    Player player = Bukkit.getPlayerExact(args[1]);
                    if (player == null) {
                        Main.message(sender, "&cPlayer not found.");
                        return true;
                    }
                    if (!Main.PLAYER_TAGS.containsKey(player.getUniqueId())) {
                        Main.message(sender, "&cPlayer not found in database.");
                        return true;
                    }
                    tags = Main.PLAYER_TAGS.get(player.getUniqueId());
                } else if (args.length >= 4) {
                    if (Main.TAGS.containsKey(args[1])) {
                        tags.put(args[1], args[2]);
                    } else {
                        Main.message(sender, "&cThis tag doesn't exist.");
                        return true;
                    }
                    moneyAction = MoneyAction.valueOf(args[3].toUpperCase(Locale.ROOT));
                } else sendHelp(sender, label);
                if (moneyAction.equals(MoneyAction.GET)) {
                    Main.message(sender, "Player tags:");
                    tags.forEach((key, value) -> {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&r - &e" + key + "&f: &b" + value));
                        try {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&r  >&eBalance: &a" +
                                            Main.getStorage().getCacheBalance(key, value)));
                        } catch (StorageExecuteException e) {
                            Main.message(sender, "&cMoney not found for this tag.");
                        }
                    });
                } else if (args.length >= 3) {
                    int amount = 0;
                    String reason = "Command";
                    if (args.length >= 4 && !args[0].equalsIgnoreCase("tags")) {
                        reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                        amount = Integer.parseInt(args[2]);
                    } else if (args.length >= 5) {
                        reason = String.join(" ", Arrays.copyOfRange(args, 5, args.length));
                        amount = Integer.parseInt(args[4]);
                    }
                    switch (moneyAction) {
                        case ADD -> {
                            Main.getStorage().addMoneyToTags(tags, amount, reason);
                            Main.message(sender, "Added " + amount + " to balance.");
                        }
                        case REMOVE -> {
                            Main.getStorage().addMoneyToTags(tags, amount, reason);
                            Main.message(sender, "Removed " + amount + " from balance.");
                        }
                        case SET -> {
                            if (tags.size() > 1) {
                                Main.message(sender, "&cYou can't set balance to multiple tags.");
                                return true;
                            }
                            String tagName = tags.keySet().iterator().next();
                            Object tagValue = tags.values().iterator().next();
                            Main.getStorage().setMoneyToTag(tagName, tagValue, amount, reason);
                            Main.message(sender, "Set balance to " + amount + ".");
                        }
                    }
                } else sendHelp(sender, label);
            } catch (Exception exception) {
                Main.message(sender, "&cError: " + exception.getLocalizedMessage());
                Main.message(sender, "&cInvalid command usage, see /" + label + " help");
                Main.stack(exception.getStackTrace());
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
                "add <player> <amount> [reason]&r: &eAdd money to a player's balance"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.",
                "remove <player> <amount> [reason]&r: &eRemove money from player's balance"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.",
                "set <player> <amount> [reason]&r: &eSet player's money balance"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.",
                "get <player>&r: &eGet balance of a player"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.",
                "tags <player>&r: &eGet list of tags of a player"
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

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length <= 1) {
            return McTools.getTabArgs(args[0], Arrays.asList("add", "remove", "get", "set", "tags", "reload", "help"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") ||
                    args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("get")) {
                return McTools.getTabArgs(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("tags")) {
                return McTools.getTabArgs(args[1], new ArrayList<>(Main.TAGS.keySet()));
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("tags")) {
                return McTools.getTabArgs(args[3], Arrays.asList("add", "remove", "set"));
            }
        }
        return List.of("");
    }

    enum MoneyAction {
        ADD,
        REMOVE,
        GET,
        SET,
        TAGS
    }
}
