# MileBanks - Minecraft Banking Plugin

MileBanks is a powerful Minecraft plugin developed by Milekat, 
designed to bring a comprehensive banking system to your Minecraft server. 
The plugin includes an API and various commands for managing player accounts and their associated tags.

## Features

- **Command: /money**
  - Admin command requiring 'mile-banks.admin' permission.
  - Perform actions such as adding, withdrawing, and setting money in a player's account.
  - View tags associated with a player.

- **Tag System**
  - Central to the plugin, the tag system allows custom parameters to be associated with players.
  - Players can be grouped into different categories using tags.
  - Enables players to have accounts in multiple groups.

- **Complete Transaction History and Player Account Storage**

  - MileBanks ensures a comprehensive record of all transactions and player accounts. 
  - Leveraging Elasticsearch, the plugin provides the foundation for powerful analytics and data visualization. 
  - Explore possibilities with Elastic + Kibana for creating real-time in-game graphs, enriching the server experience.

- **API**
  - The plugin includes a robust API ('banks-api') that provides complete control over player accounts.
  - Developers can leverage the API to manage accounts programmatically.
  - Offers flexibility for creating custom features and integrations with other plugins.

## Requirements

- **Elasticsearch Server**
    - MileBanks requires an Elasticsearch server with a minimum version of 7.
    - Connection information should be configured in the `config.yml` file.
- **Bukkit/Spigot/Paper Server**
    - MileBanks is a Bukkit plugin and requires a Bukkit/Spigot/Paper server to run.
    - The plugin is compiled with Java 17, to support minor version, please rebuild by yourself.
    - The plugin has been tested on Paper 1.20.4.

## Installation

1. Download the MileBanks plugin from [MileBanks GitHub Repository](https://github.com/tutur1004/MileBanks).

2. Ensure that Elasticsearch server is set up with version 7 or higher.

3. Fill the Elasticsearch connection details in the `config.yml` file.

4. Add the "banks-api" to your project:
    - Find the API in the GitHub packages of the project [here](https://github.com/tutur1004/MileBanks/packages).
    - Include the API in your plugin by calling `MileBanksAPI#getApi`.
    - Handle the potential exception "ApiUnavailable."

### Example Code (Loading API and adding money to a player's account)

```java
try {
    // Get the API
    MileBanksAPI mileBanksAPI = MileBanksAPI.getApi();
    // Use the API for further functionality ...
    ...
    // Example: Deposit 1000 to the player's account with tags
    Map<String, String> tags = new HashMap<>();
    tags.put("uuid", player.getUniqueId().toString());
    tags.put("squad", "eagle");
    tags.put("faction", "north");
    // Update the player's tags
    mileBanksAPI.setPlayerTags(playerUuid, tags);
    // Add money to the player's account (It will affect all the tags also)
    mileBanksAPI.addMoneyByTags(player.getUniqueId(), tags, 1000, "He killed a dragon!");
} catch (ApiUnavailable e) {
    // Handle API unavailability
    e.printStackTrace();
}
```

## Credits

- **Developer:** Milekat
    - GitHub: [Milekat GitHub](https://github.com/tutur1004)

- **README generated by**: ChatGPT 
    - Site: [ChatGPT](https://chat.openai.com/)

## Support and Issues

For support and reporting issues, please visit the 
[MileBanks GitHub Issues](https://github.com/tutur1004/MileBanks/issues) page.

Feel free to contribute to the development of MileBanks by submitting pull requests or suggesting improvements. 

Happy gaming!