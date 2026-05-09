# MileBanks - Minecraft Banking Plugin

MileBanks is a Minecraft plugin developed by Milekat, designed to bring a comprehensive banking system to your server.
It supports a flexible tag-based account system, multi-currency, and a full transaction history backed by Elasticsearch.

## Features

- **Command: /money** — requires `mile-banks.admin` permission
  - Add, remove, or reset money on a player's account
  - View all balances for a player
  - Full support for multi-currency when configured

- **Tag System**
  - Accounts are identified by tags (e.g. `player-uuid`, `player-name`)
  - Built-in tags are populated automatically on player join
  - Custom tags can be defined in `config.yml` and set by external plugins via the API

- **Multi-Currency**
  - Configure any number of currencies in `config.yml`
  - When multiple currencies are active, all money operations require an explicit currency
  - Single-currency setups have zero overhead — currency is transparent

- **Transaction History**
  - Every operation is stored as a transaction in Elasticsearch
  - Resets archive old transactions before writing the new balance
  - Compatible with Kibana for real-time graphs and analytics

- **API** (`mile-banks-api`)
  - Full programmatic control over accounts and tags
  - Currency-aware guards: `MissingCurrencyException` / `UnknownCurrencyException`
  - Expose currency config: `isMultiCurrency()`, `getCurrencies()`

## Requirements

- **Elasticsearch** ≥ 8 (client `9.1.4`)
- **Java** 21
- **Bukkit / Spigot / Paper** — tested on Paper 1.21.x

## Installation

1. Download the plugin jar from the [MileBanks GitHub repository](https://github.com/tutur1004/MileBanks).
2. Start your server once to generate `config.yml`, then stop it.
3. Fill in the Elasticsearch connection details in `config.yml`.
4. Restart the server.

## Configuration

```yaml
storage:
  type: ElasticSearch
  elasticsearch:
    prefix: "banks-"        # Index prefix (lowercase, letters, digits, dashes)
    hostname: "localhost"
    port: "9200"
    username: "user"
    password: "pass"
    replicas: 0
    save-interval-ticks: 20 # How often pending transactions are flushed (ticks)

tags:
  enable_builtin_tags: true   # Adds player-uuid and player-name automatically
  currencies:
    list: [ ]                 # Add 1 or more entry to enable named-currency (e.g. "gold", "gems")
  # Custom tags (only when enable_builtin_tags: false)
  custom:
    string: [ "player-name", "player-uuid" ]
    integer: []
```

## Commands

All subcommands require the `mile-banks.admin` permission.

> `<currency>` is only required when **multiple currencies** are configured.

### Player commands

| Command | Description |
|---|---|
| `/money get <player>` | Show all balances for a player |
| `/money add <player> [currency] <amount> [reason]` | Add money |
| `/money remove <player> [currency] <amount> [reason]` | Remove money |
| `/money reset <player> [currency] [amount]` | Reset balance (default: 0) |

### Tag commands

| Command | Description |
|---|---|
| `/money tags get <tag> <value>` | Show balances for a tag |
| `/money tags add <tag> <value> [currency] <amount> [reason]` | Add money |
| `/money tags remove <tag> <value> [currency] <amount> [reason]` | Remove money |
| `/money tags reset <tag> <value> [currency] [amount]` | Reset balance (default: 0) |

### Other

| Command | Description |
|---|---|
| `/money reload` | Reload config and reconnect storage |
| `/money help` | Show help |

## API

Add `mile-banks-api` to your project via the [GitHub Packages](https://github.com/tutur1004/MileBanks/packages).

```java
try {
    MileBanksIAPI api = MileBanksAPI.getApi();

    // --- Single currency ---
    Map<String, Object> tags = new HashMap<>();
    tags.put("player-uuid", player.getUniqueId().toString());
    api.setPlayerTags(player.getUniqueId(), tags);

    api.addMoneyByTags(tags, 1000, "Killed a dragon");
    int balance = api.getMoneyByTags(tags);
    api.resetMoneyByTags(tags, "Admin wipe");        // reset to 0
    api.resetMoneyByTags(tags, 500, "Starter pack"); // reset to 500

    // --- Multi-currency ---
    if (api.isMultiCurrency()) {
        Map<String, Object> goldTags = new HashMap<>(tags);
        goldTags.put("currency", "gold");

        api.addMoneyByTags(goldTags, 200, "Quest reward");
        int goldBalance = api.getMoneyByTags(goldTags);
        api.resetMoneyByTags(goldTags, 0, "Season reset");

        // Available currencies
        List<String> currencies = api.getCurrencies();
    }

} catch (MissingCurrencyException e) {
    // Multi-currency is on but no "currency" tag was provided
    // e.getAvailableCurrencies()
} catch (UnknownCurrencyException e) {
    // The provided currency is not in the config
    // e.getCurrency(), e.getAvailableCurrencies()
} catch (StorageException e) {
    // Other storage error
} catch (ApiUnavailable e) {
    // Plugin not loaded
}
```

### Key API methods

| Method | Description |
|---|---|
| `isMultiCurrency()` | True when more than one currency is configured |
| `getCurrencies()` | Immutable list of configured currency names |
| `getMoney(UUID)` | All tag balances for a player |
| `getMoneyByTags(Map)` | Balance for a specific tag set |
| `addMoneyByTags(Map, int, String)` | Add money |
| `removeMoneyByTags(Map, int, String)` | Remove money |
| `resetMoneyByTags(Map, int, String)` | Reset to amount (archives old transactions) |
| `resetMoneyByTags(Map, String)` | Reset to 0 |
| `setPlayerTags(UUID, Map)` | Register a player's tags |
| `getPlayerTags(UUID)` | Retrieve a player's registered tags |

> When multi-currency is enabled, all `getMoneyByTags` / `addMoneyByTags` / `removeMoneyByTags` / `resetMoneyByTags` calls must include `"currency"` in the tags map, otherwise a `MissingCurrencyException` is thrown.
> `resetMoneyByTag(tagName, tagValue, ...)` is blocked in multi-currency mode — use `resetMoneyByTags` with a full tags map instead.

## Credits

- **Developer:** Milekat — [GitHub](https://github.com/tutur1004)

## Support

Report issues at [MileBanks GitHub Issues](https://github.com/tutur1004/MileBanks/issues).
