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
        if (args.length >= 2) {
            try {
                MoneyAction moneyAction = MoneyAction.valueOf(args[0].toUpperCase(Locale.ROOT));
                Map<String, Object> tags = new HashMap<>();

                if (!moneyAction.equals(MoneyAction.TAGS)) {
                    // --- Player-based commands ---
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

                    if (moneyAction == MoneyAction.GET) {
                        Main.message(sender, "Account(s):");
                        for (Map.Entry<String, Object> tag : tags.entrySet()) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&r - &e" + tag.getKey() + "&f: &b" + tag.getValue()));
                            if (Main.isMultiCurrency()) {
                                for (String currency : Main.CURRENCIES) {
                                    Map<String, Object> currencyTags = new HashMap<>(
                                            Map.of(tag.getKey(), tag.getValue()));
                                    currencyTags.put("currency", currency);
                                    try {
                                        int bal = Main.getStorage().getCacheBalance(currencyTags);
                                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                "&r   [" + currency + "] &eBalance: &a" + bal));
                                    } catch (StorageExecuteException e) {
                                        Main.message(sender, "&cBalance not found for " + currency + ".");
                                    }
                                }
                            } else {
                                try {
                                    int bal = Main.getStorage().getCacheBalance(
                                            Map.of(tag.getKey(), tag.getValue()));
                                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                            "&r  >&eBalance: &a" + bal));
                                } catch (StorageExecuteException e) {
                                    Main.message(sender, "&cMoney not found for this tag.");
                                }
                            }
                        }
                        return true;
                    }

                    // For ADD / REMOVE / SET: currency is mandatory when multi-currency
                    String selectedCurrency = null;
                    int amountArgIndex = 2;
                    if (Main.isMultiCurrency()) {
                        if (args.length == 2 || !Main.CURRENCIES.contains(args[2])) {
                            Main.message(sender, "&cA currency must be specified. Available: &e"
                                    + String.join("&c, &e", Main.CURRENCIES));
                            return true;
                        }
                        selectedCurrency = args[2];
                        amountArgIndex = 3;
                    }

                    int amount = 0;
                    String reason = "Command";
                    if (args.length > amountArgIndex) {
                        amount = Integer.parseInt(args[amountArgIndex]);
                        if (args.length > amountArgIndex + 1) {
                            reason = String.join(" ", Arrays.copyOfRange(args, amountArgIndex + 1, args.length));
                        }
                    }

                    Map<String, Object> operationTags = new HashMap<>(tags);
                    if (Main.isMultiCurrency()) {
                        operationTags.put("currency", selectedCurrency);
                    }

                    switch (moneyAction) {
                        case ADD -> {
                            Main.getStorage().addMoneyToTags(operationTags, amount, reason);
                            Main.message(sender, "Added " + amount + " to balance"
                                    + (Main.isMultiCurrency() ? " [" + selectedCurrency + "]" : "") + ".");
                        }
                        case REMOVE -> {
                            Main.getStorage().removeMoneyToTags(operationTags, amount, reason);
                            Main.message(sender, "Removed " + amount + " from balance"
                                    + (Main.isMultiCurrency() ? " [" + selectedCurrency + "]" : "") + ".");
                        }
                        case SET -> {
                            if (tags.size() > 1) {
                                Main.message(sender, "&cYou can't set balance to multiple tags.");
                                return true;
                            }
                            Main.getStorage().resetMoneyToTags(operationTags, reason);
                            Main.getStorage().addMoneyToTags(operationTags, amount, reason);
                            Main.message(sender, "Set balance to " + amount
                                    + (Main.isMultiCurrency() ? " [" + selectedCurrency + "]" : "") + ".");
                        }
                        default -> {
                            return sendHelp(sender, label);
                        }
                    }

                } else {
                    // --- Tags subcommand ---
                    if (args.length < 4) return sendHelp(sender, label);

                    if (!Main.TAGS.containsKey(args[2])) {
                        Main.message(sender, "&cTag '" + args[2] + "' doesn't exist.");
                        return true;
                    }
                    tags.put(args[2], args[3]);
                    moneyAction = MoneyAction.valueOf(args[1].toUpperCase(Locale.ROOT));

                    if (moneyAction == MoneyAction.GET) {
                        if (Main.isMultiCurrency()) {
                            Main.message(sender, "Balance(s) for " + args[2] + "=" + args[3] + ":");
                            for (String currency : Main.CURRENCIES) {
                                Map<String, Object> currencyTags = new HashMap<>(tags);
                                currencyTags.put("currency", currency);
                                try {
                                    int bal = Main.getStorage().getCacheBalance(currencyTags);
                                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                            "&r [" + currency + "] &eBalance: &a" + bal));
                                } catch (StorageExecuteException e) {
                                    Main.message(sender, "&cBalance not found for " + currency + ".");
                                }
                            }
                        } else {
                            try {
                                int bal = Main.getStorage().getCacheBalance(tags);
                                Main.message(sender, "Balance for " + args[2] + "=" + args[3]
                                        + ": &a" + bal);
                            } catch (StorageExecuteException e) {
                                Main.message(sender, "&cMoney not found for this tag.");
                            }
                        }
                        return true;
                    }

                    // Tags ADD / REMOVE / SET: currency is mandatory when multi-currency
                    String selectedCurrency = null;
                    int amountArgIndex = 4;
                    if (Main.isMultiCurrency()) {
                        if (args.length == 4 || !Main.CURRENCIES.contains(args[4])) {
                            Main.message(sender, "&cA currency must be specified. Available: &e"
                                    + String.join("&c, &e", Main.CURRENCIES));
                            return true;
                        }
                        selectedCurrency = args[4];
                        amountArgIndex = 5;
                    }

                    int amount = 0;
                    String reason = "Command";
                    if (args.length > amountArgIndex) {
                        amount = Integer.parseInt(args[amountArgIndex]);
                        if (args.length > amountArgIndex + 1) {
                            reason = String.join(" ", Arrays.copyOfRange(args, amountArgIndex + 1, args.length));
                        }
                    }

                    Map<String, Object> operationTags = new HashMap<>(tags);
                    if (Main.isMultiCurrency()) {
                        operationTags.put("currency", selectedCurrency);
                    }

                    switch (moneyAction) {
                        case ADD -> {
                            Main.getStorage().addMoneyToTags(operationTags, amount, reason);
                            Main.message(sender, "Added " + amount + " to balance"
                                    + (Main.isMultiCurrency() ? " [" + selectedCurrency + "]" : "") + ".");
                        }
                        case REMOVE -> {
                            Main.getStorage().removeMoneyToTags(operationTags, amount, reason);
                            Main.message(sender, "Removed " + amount + " from balance"
                                    + (Main.isMultiCurrency() ? " [" + selectedCurrency + "]" : "") + ".");
                        }
                        case SET -> {
                            Main.getStorage().resetMoneyToTags(operationTags, reason);
                            Main.getStorage().addMoneyToTags(operationTags, amount, reason);
                            Main.message(sender, "Set balance to " + amount
                                    + (Main.isMultiCurrency() ? " [" + selectedCurrency + "]" : "") + ".");
                        }
                        default -> {
                            return sendHelp(sender, label);
                        }
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
        } else if (args.length == 1) {
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

    private boolean sendHelp(@NotNull CommandSender sender, String lbl) {
        boolean multi = Main.isMultiCurrency();
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.add",
                "add <player>" + (multi ? " [currency]" : "") + " <amount> [reason]&r: &eAdd money to a player's balance(s)"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.remove",
                "remove <player>" + (multi ? " [currency]" : "") + " <amount> [reason]&r: &eRemove money from player's balance(s)"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.get",
                "get <player>&r: &eGet all balances of a player and their values"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.tags.add",
                "tags add <tag-name> <tag-value>" + (multi ? " [currency]" : "") + " <amount> [reason]&r: &eAdd money to a tag balance"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.tags.remove",
                "tags remove <tag-name> <tag-value>" + (multi ? " [currency]" : "") + " <amount> [reason]&r: &eRemove money from a tag balance"
        ));
        Main.message(sender, "&6/" + lbl + " " + Main.getConfigs().getMessage(
                "messages.command.money.help.tags.set",
                "tags set <tag-name> <tag-value>" + (multi ? " [currency]" : "") + " <amount> [reason]&r: &eSet money to a tag balance"
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
                // Exclude "currency" from tag name suggestions (it's internal)
                List<String> tagNames = Main.TAGS.keySet().stream()
                        .filter(k -> !k.equals("currency"))
                        .collect(Collectors.toList());
                return McTools.getTabArgs(args[2], tagNames);
            } else if (Main.isMultiCurrency() &&
                    (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") ||
                            args[0].equalsIgnoreCase("set"))) {
                // Suggest currencies (or amount if starts with digit)
                return McTools.getTabArgs(args[2], Main.CURRENCIES);
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("tags") && Main.isMultiCurrency()) {
            // args[1]=action, args[2]=tag, args[3]=value, args[4]=currency or amount
            if (!args[1].equalsIgnoreCase("get")) {
                return McTools.getTabArgs(args[4], Main.CURRENCIES);
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
