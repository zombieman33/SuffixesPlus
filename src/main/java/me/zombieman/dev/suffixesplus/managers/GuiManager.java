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

        // Create a chest inventory with 5 rows (9 slots per row)
        Inventory gui = Bukkit.createInventory(null, 5 * 9, Component.text("Choose Your Suffix (Page " + (page + 1) + "/" + maxPage + ")"));

        // Fill row 5 (slots 36-44) with black stained glass pane
        ItemStack blackPane = createPane();
        for (int i = 36; i <= 44; i++) {
            gui.setItem(i, blackPane);
        }

        // Add player head in the right corner (slot 44)
        ItemStack playerHead;
        try {
            playerHead = createPlayerHead(player, luckPermsHook.getPlayerSuffixes(player.getUniqueId()).size());
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage("An error occurred while fetching your data.");
            return;
        }
        gui.setItem(44, playerHead);

        // Add the "Clear All Suffixes" button in the middle (slot 40)
        ItemStack clearAllItem = createClearAllButton();
        gui.setItem(40, clearAllItem);

        // Add the "Random Suffix" button (slot 41)
        ItemStack randomSuffixItem = createRandomSuffixButton();
        gui.setItem(41, randomSuffixItem);

        // Add the "Info" button
        ItemStack infoButton = createInfoButton();
        gui.setItem(39, infoButton);


        // Add suffix items for the current page
        List<ItemStack> ownedSuffixes = new ArrayList<>();
        List<ItemStack> notOwnedSuffixes = new ArrayList<>();

        for (String suffix : allSuffixes) {
            String formattedSuffix = suffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "");
            String permissionNode = "suffixsplus.suffix." + formattedSuffix.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");

            boolean hasPermission = player.hasPermission(permissionNode);
            ItemStack suffixItem = createSuffixItem(suffix, hasPermission, player);

            if (hasPermission) {
                ownedSuffixes.add(suffixItem);
            } else {
                notOwnedSuffixes.add(suffixItem);
            }
        }

        List<ItemStack> allSuffixItems = new ArrayList<>();
        allSuffixItems.addAll(ownedSuffixes);
        allSuffixItems.addAll(notOwnedSuffixes);

        // Display suffixes for the current page
        int startIndex = page * suffixesPerPage;
        int endIndex = Math.min(startIndex + suffixesPerPage, allSuffixItems.size());

        if (startIndex < 0 || startIndex >= allSuffixItems.size()) {
            return; // Prevents IndexOutOfBoundsException
        }

        int guiSize = gui.getSize(); // Assumes gui.getSize() gives the total number of slots
        for (int i = startIndex, slot = 0; i < endIndex && slot < guiSize; i++, slot++) {
            gui.setItem(slot, allSuffixItems.get(i));
        }


        // Add "Next" and "Previous" buttons
        if (page < maxPage - 1) {
            ItemStack nextButton = createNavigationButton("Next", Material.ARROW);
            gui.setItem(44, nextButton);
            gui.setItem(43, playerHead);
        }

        int slot = 36;

        if (page > 0) {
            slot = 37;
        }

        if (player.hasPermission("suffixsplus.gui.preview")) {
            ItemStack preview = createPreview(player);
            gui.setItem(slot, preview);
        }

        if (page > 0) {
            ItemStack prevButton = createNavigationButton("Previous", Material.ARROW);
            gui.setItem(36, prevButton);
        }

        // Open the GUI for the player
        setCurrentPage(player, page);
        player.openInventory(gui);
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
