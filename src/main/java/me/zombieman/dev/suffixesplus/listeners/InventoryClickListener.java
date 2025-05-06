package me.zombieman.dev.suffixesplus.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.zombieman.dev.suffixesplus.SuffixesPlus;
import me.zombieman.dev.suffixesplus.hooks.LuckPermsHook;
import me.zombieman.dev.suffixesplus.utils.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.List;

public class InventoryClickListener implements Listener {

    private final SuffixesPlus plugin;
    private final LuckPermsHook luckPermsHook;

    public InventoryClickListener(SuffixesPlus plugin, LuckPermsHook luckPermsHook) {
        this.plugin = plugin;
        this.luckPermsHook = luckPermsHook;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Check if the event involves a player and an item
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        ItemMeta itemMeta = clickedItem.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "guiItem");

        if (!itemMeta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        String guiAction = itemMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        event.setCancelled(true);

        int currentPage = plugin.guiManager.getCurrentPage(player);

        System.out.println("Current page: " + currentPage);
        System.out.println("Gui action: " + guiAction);

        switch (guiAction) {
            case "playerHead":
                break;

            case "clearAll":
                player.closeInventory();
                clearAllSuffixes(player);
                break;

            case "randomSuffix":
                player.closeInventory();
                try {
                    assignRandomSuffix(player);
                } catch (SQLException e) {
                    player.sendMessage(ChatColor.RED + "There was an error connecting to the database, please try again later.");
                    plugin.getLogger().warning("Database Warning: " + e.getMessage());
                    return;
                }
                break;

            case "infoButton":
                player.closeInventory();
                player.sendMessage(ChatUtil.parseLegacyColors("&6Suffix Info: &fSelect a suffix to customize your player tag."));
                break;

            case "suffixItem":
                player.closeInventory();
                handleSuffixSelection(player, itemMeta);
                break;
            case "next":
                try {
                    plugin.guiManager.openSuffixGui(player, currentPage + 1);
                    break;
                } catch (SQLException e) {
                    player.sendMessage(ChatColor.RED + "There was an error while connecting to database. Please try again later.");
                    System.err.println("Error connecting to database: " + e.getMessage());
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f ,1.0f);
                    return;
                }
            case "previous":
                try {
                    plugin.guiManager.openSuffixGui(player, currentPage - 1);
                    break;
                } catch (SQLException e) {
                    player.sendMessage(ChatColor.RED + "There was an error while connecting to database. Please try again later.");
                    System.err.println("Error connecting to database: " + e.getMessage());
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
            case "preview_anvil":
                try {

                    String currentSuffixFromDB = plugin.getDatabase().getPlayer(player.getUniqueId(), player.getName()).getCurrentSuffix();

                    if (currentSuffixFromDB == null || currentSuffixFromDB.equalsIgnoreCase("n/a")) {
                        player.sendMessage(ChatColor.RED + "You need to equip a suffix first!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO ,1.0f, 1.0f);
                        return;
                    }

                    player.sendMessage(MiniMessage.miniMessage().deserialize("<aqua>Preview:"));
                    player.sendMessage("");


                    currentSuffixFromDB = plugin.getConfig().getString("suffix.prefix", "suffix_") + currentSuffixFromDB.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "");

                    String currentSuffix = ChatColor.translateAlternateColorCodes('&', luckPermsHook.getGroupSuffixColor(currentSuffixFromDB));

                    player.sendMessage(ChatUtil.parseLegacyColors(PlaceholderAPI.setPlaceholders(player, plugin.getConfig().getString("player.rankPlaceholder", "%vault_prefix%")).replace(plugin.getConfig().getString("player.rankPlaceholder", "%vault_prefix%"), "&7") + player.getName() + currentSuffix));
                    player.sendMessage("");
                    player.sendMessage(ChatUtil.parseLegacyColors(currentSuffix));

                    player.sendMessage("");

                    player.closeInventory();
                    break;

                } catch (SQLException e) {
                    player.sendMessage(ChatColor.RED + "There was an error while connecting to database. Please try again later.");
                    System.err.println("Error connecting to database: " + e.getMessage());
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
            default:
                break;
        }
    }

    private void clearAllSuffixes(Player player) {
        luckPermsHook.clearAllSuffixes(player.getUniqueId(), true);
    }

    private void assignRandomSuffix(Player player) throws SQLException{
        // Get all suffixes the player owns
        List<String> ownedSuffixes = luckPermsHook.getPlayerSuffixes(player.getUniqueId());

        // Debug: Log owned suffixes
//        player.sendMessage(ChatUtil.parseLegacyColors("&eDebug: Your owned suffixes: " + ownedSuffixes));

        if (ownedSuffixes.isEmpty()) {
            // If the player doesn't have any suffixes, notify them
            player.sendMessage(ChatUtil.parseLegacyColors("&cYou don't have any suffixes to choose from!"));
            return;
        }

        // Pick a random suffix from the owned suffixes list
        String randomSuffix = ownedSuffixes.get((int) (Math.random() * ownedSuffixes.size()));

        // Debug: Log the random suffix chosen
//        player.sendMessage(ChatUtil.parseLegacyColors("&eDebug: Chosen random suffix: " + randomSuffix));

        randomSuffix = plugin.getConfig().getString("suffix.prefix", "suffix_") + randomSuffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "");
        String currentSuffix = plugin.getDatabase().getPlayer(player.getUniqueId(), player.getName()).getCurrentSuffix();
        if (randomSuffix.equalsIgnoreCase(currentSuffix)) {
            assignRandomSuffix(player);
            return;
        }

        // Clear all other suffixes and set the random one
        luckPermsHook.clearAllSuffixes(player.getUniqueId(), false);
        luckPermsHook.addGroup(player.getUniqueId(), plugin.getConfig().getString("suffix.prefix", "suffix_") + randomSuffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), ""));

        plugin.getDatabase().updateSuffixes(player.getUniqueId(), randomSuffix);

        // Format the message to display the suffix with its colors
        String formattedSuffix = plugin.getConfig().getString("suffix.prefix", "suffix_") + randomSuffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "");

        // Send the player a message with the random suffix they got
        player.sendMessage(ChatUtil.parseLegacyColors("&a&m                                                                     "));
        player.sendMessage(ChatUtil.parseLegacyColors("&aYou have been assigned a random suffix:" + ChatColor.translateAlternateColorCodes('&', luckPermsHook.getGroupSuffixColor(formattedSuffix))));
        player.sendMessage(ChatUtil.parseLegacyColors("&a&m                                                                     "));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
    }

    private void handleSuffixSelection(Player player, ItemMeta itemMeta) {
        NamespacedKey suffixKey = new NamespacedKey(plugin, "suffix");
        if (!itemMeta.getPersistentDataContainer().has(suffixKey, PersistentDataType.STRING)) return;

        // Retrieve the suffix from the item meta
        String suffix = itemMeta.getPersistentDataContainer().get(suffixKey, PersistentDataType.STRING);
        if (suffix == null) {
            player.sendMessage(ChatColor.RED + "This isn't a valid suffix!");
            return;
        }

        String cleanedSuffix = suffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "");
        String permission = "suffixsplus.suffix." + cleanedSuffix;

        // Check if the player has access to the suffix
        checkSuffixAccess(player, cleanedSuffix, permission);
    }

    private void checkSuffixAccess(Player player, String cleanedSuffix, String permission) {
        // First, try to check if the player has custom access to the suffix
        luckPermsHook.checkAccess(player.getUniqueId(), player.getName(),
                plugin.getConfig().getString("suffix.prefix", "suffix_"), cleanedSuffix, hasAccess -> {
                    if (hasAccess) {
                        handleSuffixAssignment(player, cleanedSuffix);
                    } else {
                        // If the player doesn't have access, check their permissions
                        if (player.hasPermission(permission)) {
                            handleSuffixAssignment(player, cleanedSuffix);
                        } else {
                            handleSuffixPurchase(player, cleanedSuffix);
                        }
                    }
                });
    }

    private void handleSuffixAssignment(Player player, String cleanedSuffix) {
        System.out.println(luckPermsHook.hasSuffix(player.getUniqueId(), cleanedSuffix));
        if (luckPermsHook.hasSuffix(player.getUniqueId(), cleanedSuffix)) {
            luckPermsHook.removeSuffix(player, cleanedSuffix);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            player.sendMessage(ChatUtil.parseLegacyColors("&c&m                                               "));
            player.sendMessage(ChatUtil.parseLegacyColors("&cSuffix removed:" + ChatColor.translateAlternateColorCodes('&', luckPermsHook.getGroupSuffixColor(cleanedSuffix))));
            player.sendMessage(ChatUtil.parseLegacyColors("&c&m                                               "));
            return;
        }
        luckPermsHook.clearAllSuffixes(player.getUniqueId(), false);
        luckPermsHook.addGroup(player.getUniqueId(), cleanedSuffix);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendMessage(ChatUtil.parseLegacyColors("&a&m                                               "));
        player.sendMessage(ChatUtil.parseLegacyColors("&a&lSuffix added:"));
        player.sendMessage(ChatUtil.parseLegacyColors(PlaceholderAPI.setPlaceholders(player, plugin.getConfig().getString("player.rankPlaceholder", "%vault_prefix%")).replace(plugin.getConfig().getString("player.rankPlaceholder", "%vault_prefix%"), "&7") + player.getName() + ChatColor.translateAlternateColorCodes('&', luckPermsHook.getGroupSuffixColor(cleanedSuffix))));
        player.sendMessage(ChatUtil.parseLegacyColors("&a&m                                               "));
        try {
            plugin.getDatabase().updateSuffixes(player, cleanedSuffix);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleSuffixPurchase(Player player, String cleanedSuffix) {
        // If the player doesn't have access or permission, check if the suffix is purchasable
        try {
            if (!luckPermsHook.isPurchasable(cleanedSuffix)) {
                player.sendMessage(ChatColor.RED + "The " + cleanedSuffix + " suffix is not purchasable!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "There was an error checking if the suffix is purchasable.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            e.printStackTrace();
            return;
        }


        for (String message : plugin.getConfig().getStringList("suffix.purchaseMessage")) {
            Component deserialize = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("suffix.purchaseHoverMessage", "<aqua>Click here to purchase the %suffix% <aqua>suffix!")
                    .replace("%suffix%", cleanedSuffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "")));
            ClickEvent event = ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, plugin.getConfig().getString("suffix.link", "https://store.fewer.live/"));

            player.sendMessage(MiniMessage.miniMessage().deserialize(message.replace("%suffix%", cleanedSuffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "")))
                    .hoverEvent(deserialize)
                    .clickEvent(event));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

}
