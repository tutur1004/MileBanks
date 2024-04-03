package fr.milekat.banks.commands;

import fr.milekat.banks.Main;
import fr.milekat.utils.McTools;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import fr.milekat.utils.storage.exceptions.StorageLoadException;
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
                        Main.message(sender, "&cPlayer '" + args[1] + "' not found.");
                        return true;
                    }
                    if (!Main.PLAYER_TAGS.containsKey(player.getUniqueId())) {
                        Main.message(sender, "&cPlayer not found in database.");
                        return true;
                    }
                    tags = Main.PLAYER_TAGS.get(player.getUniqueId());
                } else if (args.length >= 4) {
                    if (Main.TAGS.containsKey(args[2])) {
                        tags.put(args[2], args[3]);
                    } else {
                        Main.message(sender, "&cTag '" + args[2] + "' doesn't exist.");
                        return true;
                    }
                    moneyAction = MoneyAction.valueOf(args[1].toUpperCase(Locale.ROOT));
                } else return sendHelp(sender, label);
                int amount = 0;
                String reason = "Command";
                if (args.length >= 3 && !args[0].equalsIgnoreCase("tags")) {
                    amount = Integer.parseInt(args[2]);
                    if (args.length >= 4) {
                        reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    }
                } else if (args.length >= 5) {
                    amount = Integer.parseInt(args[4]);
                    if (args.length >= 6) {
                        reason = String.join(" ", Arrays.copyOfRange(args, 5, args.length));
                    }
                }
                switch (moneyAction) {
                    case GET -> {
                        Main.message(sender, "Account(s):");
                        tags.forEach((key, value) -> {
                            sender.sendMessage(
                                    ChatColor.translateAlternateColorCodes('&',
                                            "&r - &e" + key + "&f: &b" + value));
                            try {
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                        "&r  >&eBalance: &a" +
                                                Main.getStorage().getCacheBalance(key, value)));
                            } catch (StorageExecuteException e) {
                                Main.message(sender, "&cMoney not found for this tag.");
                            }
                        });
                    }
                    case ADD -> {
                        Main.getStorage().addMoneyToTags(tags, amount, reason);
                        Main.message(sender, "Added " + amount + " to balance.");
                    }
                    case REMOVE -> {
                        Main.getStorage().removeMoneyToTags(tags, amount, reason);
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
            } catch (IllegalArgumentException exception) {
                Main.message(sender, "&cInvalid action, see /" + label + " help for more info.");
                return true;
            } catch (Exception exception) {
                Main.message(sender, "&cError: " + exception.getLocalizedMessage());
                Main.message(sender, "&cInvalid command usage, see /" + label + " help for more info.");
                Main.getMileLogger().stack(exception.getStackTrace());
            }
        } else if (args.length==1) {
            if (args[0].equalsIgnoreCase("reload")) {
                Main.message(sender, "Reloading plugin..");
                Main.reloadConfigs();
                try {
                    Main.reloadStorage();
                    Main.message(sender, "Plugin reloaded!");
                } catch (StorageLoadException e) {
                    Main.message(sender, "&cFatal storage error: " + e.getLocalizedMessage());
                    Main.getMileLogger().stack(e.getStackTrace());
                    Main.getInstance().onDisable();
                }
            } else sendHelp(sender, label);
        } else sendHelp(sender, label);
        return true;
    }

    private boolean sendHelp(@NotNull CommandSender sender, String lbl){
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.add",
                "add <player> <amount> [reason]&r: &eAdd money to a player's balance(s)"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.remove",
                "remove <player> <amount> [reason]&r: &eRemove money from player's balance(s)"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.get",
                "get <player>&r: &eGet all balances of a player and their values"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.tags.add",
                "tags add <tag-name> <tag-value> <amount> [reason]&r: &eAdd money to a tag balance"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.tags.remove",
                "tags remove <tag-name> <tag-value> <amount> [reason]&r: &eRemove money from a tag balance"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.tags.set",
                "tags set <tag-name> <tag-value> <amount> [reason]&r: &eSet money to a tag balance"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.tags.get",
                "tags get <tag-name> <tag-value>&r: &eGet a tag balance"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.reload",
                "reload&r: &eReload the plugin"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.help",
                "help&r: &eShow this help message"
        ));
        return true;
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
                return McTools.getTabArgs(args[1], Arrays.asList("add", "remove", "set", "get"));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("tags")) {
                return McTools.getTabArgs(args[2], new ArrayList<>(Main.TAGS.keySet()));
            }
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
