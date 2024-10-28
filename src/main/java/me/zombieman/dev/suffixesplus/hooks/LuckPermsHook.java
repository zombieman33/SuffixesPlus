package me.zombieman.dev.suffixesplus.hooks;

import me.zombieman.dev.suffixesplus.SuffixesPlus;
import me.zombieman.dev.suffixesplus.utils.ChatUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class LuckPermsHook {
    private final LuckPerms luckPerms;
    private final SuffixesPlus plugin;

    public LuckPermsHook(SuffixesPlus plugin, LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
        this.plugin = plugin;
    }

    // Method to get all suffix groups (that end with "_suffix")
    public List<String> getAllSuffixes() {
        return luckPerms.getGroupManager().getLoadedGroups().stream()
                .filter(group -> group.getName().startsWith(plugin.getConfig().getString("suffix.prefix", "suffix_")))
                .map(Group::getName)
                .collect(Collectors.toList());
    }

    public List<String> getPlayerSuffixes(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return List.of();

        // Get all suffixes from the configuration
        List<String> allSuffixes = getAllSuffixes();

        List<String> playerSuffixes = new ArrayList<>();

        for (String suffix : allSuffixes) {

            suffix = suffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "");

            String permission = "suffixsplus.suffix." + suffix;

            // Log the permission being checked
//            Bukkit.getLogger().info("Checking permission: " + permission);

            // Check if the player has the permission for this suffix
            if (player.hasPermission(permission)) {
                playerSuffixes.add(suffix);
            }
        }

        return playerSuffixes;
    }


    // New method to get the highest-priority suffix with colors for a player
    public Optional<String> getFormattedSuffix(UUID playerUUID) {
        User user = luckPerms.getUserManager().getUser(playerUUID);
        if (user == null) return Optional.empty();

        // Find the suffix node with the highest priority
        return user.getNodes(NodeType.SUFFIX).stream()
                .map(SuffixNode.class::cast)
                .max((a, b) -> Integer.compare(a.getPriority(), b.getPriority())) // Highest priority
                .map(SuffixNode::getMetaValue); // Return the suffix value
    }

    // Method to get all suffixes with colors and formatting intact
    public List<String> getAllFormattedSuffixes(UUID playerUUID) {
        User user = luckPerms.getUserManager().getUser(playerUUID);
        if (user == null) return List.of();

        // Get all suffixes with their formatting intact
        return user.getNodes(NodeType.SUFFIX).stream()
                .map(SuffixNode.class::cast)
                .map(SuffixNode::getMetaValue) // Get the raw suffix value
                .collect(Collectors.toList());
    }

    public String getGroupSuffixColor(String groupName) {
        // Get the LuckPerms group by name
        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            return null; // No such group found
        }

        // Fetch the highest-priority suffix node assigned to the group
        Optional<SuffixNode> suffixNode = group.getNodes(NodeType.SUFFIX)
                .stream()
                .max(Comparator.comparingInt(SuffixNode::getPriority));

        // Return the suffix with colors, if present
        return suffixNode.map(SuffixNode::getMetaValue).orElse(null);
    }

    public void clearAllSuffixes(UUID uuid, boolean message) {
        // Fetch the LuckPerms user object for the player
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) {
//            plugin.getLogger().warning("User not found for UUID: " + uuid);
            return;
        }

        boolean suffixRemoved = false;
        boolean groupRemoved = false;

        // Prefix for suffix groups from config
        String suffixPrefix = plugin.getConfig().getString("suffix.prefix", "suffix_");
//        plugin.getLogger().info("Suffix prefix is: " + suffixPrefix);

        // Remove all groups that start with the suffix prefix
        List<InheritanceNode> groupNodes = user.getNodes(NodeType.INHERITANCE).stream()
                .map(InheritanceNode.class::cast)
                .filter(inheritanceNode -> inheritanceNode.getGroupName().startsWith(suffixPrefix))
                .collect(Collectors.toList());

        for (InheritanceNode groupNode : groupNodes) {
            // Remove the group node
            user.data().remove(groupNode);
            groupRemoved = true;
//            plugin.getLogger().info("Removed group '" + groupNode.getGroupName() + "' from player: " + uuid);
        }

        // Now remove the corresponding suffix nodes for the removed groups
        List<SuffixNode> suffixNodes = user.getNodes(NodeType.SUFFIX).stream()
                .map(SuffixNode.class::cast)
                .collect(Collectors.toList());

        for (SuffixNode suffixNode : suffixNodes) {
            if (suffixNode.getMetaValue().startsWith(suffixPrefix)) {
                // Remove the suffix node
                user.data().remove(suffixNode);
                suffixRemoved = true;
                plugin.getLogger().info("Removed suffix '" + suffixNode.getMetaValue() + "' from player: " + uuid);
            }
        }

        // Notify the player if no suffixes or groups were removed
        if (message) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (!suffixRemoved && !groupRemoved) {
                    player.sendMessage(ChatUtil.parseLegacyColors("&c&m                                                                     "));
                    player.sendMessage(ChatUtil.parseLegacyColors("&cNo suffixes or groups were found to clear."));
                    player.sendMessage(ChatUtil.parseLegacyColors("&c&m                                                                     "));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                } else {
                    player.sendMessage(ChatUtil.parseLegacyColors("&a&m                                                                     "));
                    player.sendMessage(ChatUtil.parseLegacyColors("&aAll suffixes and related groups have been cleared."));
                    player.sendMessage(ChatUtil.parseLegacyColors("&a&m                                                                     "));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                }
            }
        }

        // Save the changes back to LuckPerms
        luckPerms.getUserManager().saveUser(user);

        try {
            plugin.getDatabase().updateSuffixes(uuid, "n/a");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void removeSuffix(Player player, String suffix) {
        // Fetch the LuckPerms user object for the player
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            plugin.getLogger().warning("User not found for UUID: " + player.getUniqueId());
            return;
        }

        boolean suffixRemoved = false;
        boolean groupRemoved = false;

        // Remove the suffix node if the player has it
        Optional<SuffixNode> suffixNodeOptional = user.getNodes(NodeType.SUFFIX).stream()
                .map(SuffixNode.class::cast)
                .filter(suffixNode -> suffixNode.getMetaValue().equalsIgnoreCase(suffix))
                .findFirst();

        if (suffixNodeOptional.isPresent()) {
            user.data().remove(suffixNodeOptional.get());
            suffixRemoved = true;
            plugin.getLogger().info("Removed suffix '" + suffix + "' from player: " + player.getName());
        }

        // Now remove the group corresponding to the suffix
        String suffixPrefix = plugin.getConfig().getString("suffix.prefix", "suffix_");
        String groupName = suffixPrefix + suffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "");

        Optional<InheritanceNode> groupNodeOptional = user.getNodes(NodeType.INHERITANCE).stream()
                .map(InheritanceNode.class::cast)
                .filter(inheritanceNode -> inheritanceNode.getGroupName().equalsIgnoreCase(groupName))
                .findFirst();

        if (groupNodeOptional.isPresent()) {
            user.data().remove(groupNodeOptional.get());
            groupRemoved = true;
            plugin.getLogger().info("Removed group '" + groupName + "' from player: " + player.getName());
        }

        // Notify if nothing was removed
        if (!suffixRemoved && !groupRemoved) {
            player.sendMessage(ChatUtil.parseLegacyColors("&cNo suffix or group was removed."));
        }

        // Save the changes back to LuckPerms
        luckPerms.getUserManager().saveUser(user);

        try {
            plugin.getDatabase().updateSuffixes(player.getUniqueId(), "n/a");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to check if a player has a group corresponding to the specified suffix
    public boolean hasSuffix(UUID playerUUID, String suffix) {
        User user = luckPerms.getUserManager().getUser(playerUUID);
        if (user == null) {
            return false; // User not found
        }

        // Get the group prefix from the config (default "suffix_")
        String suffixPrefix = plugin.getConfig().getString("suffix.prefix", "suffix_");

        // Build the expected group name using the prefix and suffix
        String expectedGroupName = suffixPrefix + suffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), "");

        // Check if the user has an InheritanceNode corresponding to the group
        return user.getNodes(NodeType.INHERITANCE).stream()
                .map(InheritanceNode.class::cast)
                .anyMatch(inheritanceNode -> inheritanceNode.getGroupName().equalsIgnoreCase(expectedGroupName));
    }


    // Method to add a group to a player
    public void addGroup(UUID playerUUID, String groupName) {
        User user = luckPerms.getUserManager().getUser(playerUUID);
        if (user == null) {
            plugin.getLogger().warning("User not found for UUID: " + playerUUID);
            return;
        }

        // Create an inheritance node to represent the group
        InheritanceNode groupNode = InheritanceNode.builder(groupName).build();

        // Add the group node to the user's data
        user.data().add(groupNode);

        // Save the changes back to LuckPerms
        luckPerms.getUserManager().saveUser(user);

        // Optional: Log the action or notify the player
        Bukkit.getLogger().info("Added group '" + groupName + "' to player: " + user.getUniqueId());
    }

    public boolean isPurchasable(String groupName) throws SQLException {
        return plugin.getSuffixDatabase().suffixDoesNotExists(groupName);
    }
}