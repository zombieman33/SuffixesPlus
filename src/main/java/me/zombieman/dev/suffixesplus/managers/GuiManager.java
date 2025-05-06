package me.zombieman.dev.suffixesplus.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import me.zombieman.dev.suffixesplus.SuffixesPlus;
import me.zombieman.dev.suffixesplus.hooks.LuckPermsHook;
import me.zombieman.dev.suffixesplus.utils.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GuiManager {

    private final SuffixesPlus plugin;
    private final LuckPermsHook luckPermsHook;

    public GuiManager(SuffixesPlus plugin, LuckPermsHook luckPermsHook) {
        this.plugin = plugin;
        this.luckPermsHook = luckPermsHook;
    }

    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public void openSuffixGui(Player player, int page) throws SQLException {
        List<String> allSuffixes = luckPermsHook.getAllSuffixes();

        if (allSuffixes.isEmpty()) {
            player.sendMessage(ChatColor.RED + "There aren't any suffixes in this list!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int suffixesPerPage = 36;
        int maxPage = (int) Math.ceil((double) allSuffixes.size() / suffixesPerPage);

        Inventory gui = Bukkit.createInventory(null, 5 * 9, Component.text("Choose Your Suffix (Page " + (page + 1) + "/" + maxPage + ")"));

        // Fill row 5 with black panes
        ItemStack blackPane = createPane();
        for (int i = 36; i <= 44; i++) {
            gui.setItem(i, blackPane);
        }

        // Async fetch all suffix items
        List<CompletableFuture<ItemStack>> futures = new ArrayList<>();
        for (String suffix : allSuffixes) {
            futures.add(createSuffixItemAsync(player, suffix));
        }

        // When all items are ready, place them in GUI
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            List<ItemStack> owned = new ArrayList<>();
            List<ItemStack> notOwned = new ArrayList<>();

            for (CompletableFuture<ItemStack> future : futures) {
                try {
                    ItemStack item = future.get();
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "ownsSuffix"), PersistentDataType.BOOLEAN)) {
                        owned.add(item);
                    } else {
                        notOwned.add(item);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            List<ItemStack> allItems = new ArrayList<>();
            allItems.addAll(owned);
            allItems.addAll(notOwned);

            int startIndex = page * suffixesPerPage;
            int endIndex = Math.min(startIndex + suffixesPerPage, allItems.size());

            for (int i = startIndex, slot = 0; i < endIndex && slot < 36; i++, slot++) {
                gui.setItem(slot, allItems.get(i));
            }

            // Player head
            try {
                ItemStack head = createPlayerHead(player, luckPermsHook.getPlayerSuffixes(player.getUniqueId()).size());
                gui.setItem(43, head);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            gui.setItem(40, createClearAllButton());
            gui.setItem(41, createRandomSuffixButton());
            gui.setItem(39, createInfoButton());

            // Navigation
            if (page < maxPage - 1) gui.setItem(44, createNavigationButton("Next", Material.ARROW));
            if (page > 0) gui.setItem(36, createNavigationButton("Previous", Material.ARROW));

            try {
                if (player.hasPermission("suffixsplus.gui.preview")) {
                    ItemStack preview = createPreview(player);
                    gui.setItem(page > 0 ? 37 : 36, preview);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                setCurrentPage(player, page);
                player.openInventory(gui);
            });
        });
    }


    // Helper method to create navigation buttons
    private ItemStack createNavigationButton(String label, Material material) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<gold><bold>" + label));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "guiItem"), PersistentDataType.STRING, label.toLowerCase());
        button.setItemMeta(meta);
        return button;
    }

    private ItemStack createPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty());
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "guiItem"), PersistentDataType.STRING, "true");
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack createPlayerHead(Player player, int ownedSuffixes) throws SQLException {
        // Create the player's head item
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();

        Component currentSuffix = ChatUtil.parseLegacyColors("&b&l | &bCurrent Suffix: &an/a");
        String currentSuffixFromDB = plugin.getDatabase().getPlayer(player.getUniqueId(), player.getName()).getCurrentSuffix();

        currentSuffixFromDB = plugin.getConfig().getString("suffix.prefix", "suffix_") + currentSuffixFromDB.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "");

        if (currentSuffixFromDB != null && luckPermsHook.getGroupSuffixColor(currentSuffixFromDB) != null) {
            currentSuffix = ChatUtil.parseLegacyColors("&b&l | &bCurrent Suffix:" + ChatColor.translateAlternateColorCodes('&', luckPermsHook.getGroupSuffixColor(currentSuffixFromDB)));
        }

        // Set player-specific properties
        meta.setOwningPlayer(player);
        meta.displayName(ChatUtil.parseLegacyColors(PlaceholderAPI.setPlaceholders(player, plugin.getConfig().getString("player.rankPlaceholder", "%vault_prefix%")).replace(plugin.getConfig().getString("player.rankPlaceholder", "%vault_prefix%"), "&7") + player.getName()));
        meta.lore(List.of(
                Component.empty(),
                currentSuffix,
                MiniMessage.miniMessage().deserialize("<aqua><bold> | </bold>Owned Suffixes: <bold>" + ownedSuffixes + "</bold>"),
                Component.empty()
        ));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "guiItem"), PersistentDataType.STRING, "playerHead");
        playerHead.setItemMeta(meta);
        return playerHead;
    }

    public CompletableFuture<ItemStack> createSuffixItemAsync(Player player, String suffix) {
        String configSuffix = plugin.getConfig().getString("suffix.prefix", "suffix_");
        String formattedSuffix = suffix.replace(configSuffix, "");

        return plugin.getLuckPermsHook()
                .checkAccessAsync(player.getUniqueId(), player.getName(), configSuffix, formattedSuffix)
                .thenApply(hasAccess -> {
                    String permissionNode = "suffixsplus.suffix." + formattedSuffix.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
                    if (!hasAccess && player.hasPermission(permissionNode)) {
                        hasAccess = true;
                    }

                    ItemStack item = createSuffixItem(suffix, hasAccess, player);

                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "ownsSuffix"), PersistentDataType.BOOLEAN, hasAccess);
                        item.setItemMeta(meta);
                    }

                    return item;
                });
    }


    private ItemStack createPreview(Player player) throws SQLException {
        // Create the player's head item
        ItemStack anvil = new ItemStack(Material.ANVIL);
        ItemMeta meta = anvil.getItemMeta();

        Component currentSuffix = ChatUtil.parseLegacyColors("&b&l | &bCurrent Suffix: &an/a");
        String currentSuffixFromDB = plugin.getDatabase().getPlayer(player.getUniqueId(), player.getName()).getCurrentSuffix();

        currentSuffixFromDB = plugin.getConfig().getString("suffix.prefix", "suffix_") + currentSuffixFromDB.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "");

        if (currentSuffixFromDB != null && luckPermsHook.getGroupSuffixColor(currentSuffixFromDB) != null) {
            currentSuffix = ChatUtil.parseLegacyColors("&b&l | &bCurrent Suffix:" + ChatColor.translateAlternateColorCodes('&', luckPermsHook.getGroupSuffixColor(currentSuffixFromDB)));
        }

        meta.displayName(ChatUtil.parseLegacyColors(PlaceholderAPI.setPlaceholders(player, plugin.getConfig().getString("player.rankPlaceholder", "%vault_prefix%")).replace(plugin.getConfig().getString("player.rankPlaceholder", "%vault_prefix%"), "&7") + player.getName()));
        meta.lore(List.of(
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<aqua><bold> | </bold>Click to preview your suffix!"),
                currentSuffix,
                Component.empty()
        ));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "guiItem"), PersistentDataType.STRING, "preview_anvil");
        anvil.setItemMeta(meta);
        return anvil;
    }

    private ItemStack createClearAllButton() {
        ItemStack clearItem = new ItemStack(Material.BARRIER);
        ItemMeta meta = clearItem.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<red><bold>Clear All Suffixes"));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "guiItem"), PersistentDataType.STRING, "clearAll");
        clearItem.setItemMeta(meta);
        return clearItem;
    }

    private ItemStack createRandomSuffixButton() {
        ItemStack randomItem = new ItemStack(Material.DIAMOND);
        ItemMeta meta = randomItem.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<green><bold>Random Suffix"));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "guiItem"), PersistentDataType.STRING, "randomSuffix");
        randomItem.setItemMeta(meta);
        return randomItem;
    }

    private ItemStack createInfoButton() {
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta meta = infoItem.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<blue><bold>Info"));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "guiItem"), PersistentDataType.STRING, "infoButton");
        infoItem.setItemMeta(meta);
        return infoItem;
    }


    private ItemStack createSuffixItem(String suffix, boolean ownsSuffix, Player player) {
        ItemStack suffixItem = new ItemStack(Material.PAPER);
        ItemMeta meta = suffixItem.getItemMeta();
        String formattedSuffix = suffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "");
        if (!formattedSuffix.isEmpty()) {
            formattedSuffix = Character.toUpperCase(formattedSuffix.charAt(0)) + formattedSuffix.substring(1);
        }

        formattedSuffix = formattedSuffix + " Suffix";

        // Set item name and enchant if owned
        Component displayName = MiniMessage.miniMessage().deserialize(ownsSuffix ? "<green><bold>" + formattedSuffix : "<red>" + formattedSuffix);
        meta.displayName(displayName);

        if (ownsSuffix) {
            suffixItem = new ItemStack(Material.NAME_TAG);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        String rank = PlaceholderAPI.setPlaceholders(player, plugin.getConfig().getString("player.rankPlaceholder", "%vault_prefix%"));
        rank = rank.replace(plugin.getConfig().getString("player.rankPlaceholder", "%vault_prefix%"), "&7");

        String groupSuffixColor = luckPermsHook.getGroupSuffixColor(suffix);

        boolean isPurchasable = true;

        try {
            List<String> allSuffixes = plugin.getSuffixDatabase().getAllSuffixes();
            if (allSuffixes.contains(suffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "").toLowerCase())) isPurchasable = false;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (groupSuffixColor == null) groupSuffixColor = "&7";

        // Set the lore using parsed legacy color codes
        meta.lore(List.of(
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<aqua><bold> | </bold>Suffix: "),
                ChatUtil.parseLegacyColors(ChatColor.translateAlternateColorCodes('&', groupSuffixColor)),
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<aqua><bold> | </bold>Preview: "),
                ChatUtil.parseLegacyColors(rank + player.getName() + ChatColor.translateAlternateColorCodes('&', groupSuffixColor)), // Convert legacy codes for rank and suffix
                Component.empty(),
                MiniMessage.miniMessage().deserialize("<aqua><bold> | </bold>Active: <bold>" + luckPermsHook.hasSuffix(player.getUniqueId(), suffix)),
                MiniMessage.miniMessage().deserialize("<aqua><bold> | </bold>Purchasable: <bold>" + isPurchasable)

        ));

        // Add NBT tag
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "suffix"), PersistentDataType.STRING, suffix);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "guiItem"), PersistentDataType.STRING, "suffixItem");
        suffixItem.setItemMeta(meta);
        return suffixItem;
    }

    public int getCurrentPage(Player player) {
        return playerPages.getOrDefault(player.getUniqueId(), 0);
    }

    public void setCurrentPage(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);
    }
}
