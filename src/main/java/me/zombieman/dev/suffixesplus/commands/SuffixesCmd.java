package me.zombieman.dev.suffixesplus.commands;

import me.zombieman.dev.suffixesplus.SuffixesPlus;
import me.zombieman.dev.suffixesplus.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class SuffixesCmd implements CommandExecutor {

    private SuffixesPlus plugin;

    public SuffixesCmd(SuffixesPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;

        Player player = (Player) sender;

        if (!player.hasPermission("suffixsplus.command.suffix")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to run this command!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        if (plugin.getLuckPermsHook().getAllSuffixes().isEmpty()) {
            player.sendMessage(ChatColor.RED + "There aren't any suffixes.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        try {
            String configSuffix = plugin.getConfig().getString("suffix.prefix", "suffix_");

            if (!plugin.getSuffixDatabase().getAllSuffixes().isEmpty()) {
                player.sendMessage(ChatColor.AQUA.toString() + ChatColor.STRIKETHROUGH + " " + ChatColor.AQUA + " Non Purchasable Suffixes " + ChatColor.AQUA.toString() + ChatColor.STRIKETHROUGH + " ");

                for (String s : plugin.getSuffixDatabase().getAllSuffixes()) {
                    player.sendMessage(ChatUtil.parseLegacyColors("&7Suffix: " + ChatColor.translateAlternateColorCodes('&', plugin.getLuckPermsHook().getGroupSuffixColor(configSuffix + s.replace(configSuffix, "")))));
                }

            }

            player.sendMessage(ChatColor.AQUA.toString() + ChatColor.STRIKETHROUGH + "   " + ChatColor.AQUA + " Purchasable Suffixes " + ChatColor.AQUA.toString() + ChatColor.STRIKETHROUGH + "   ");

            for (String s : plugin.getLuckPermsHook().getAllSuffixes()) {
                s = configSuffix + s.replace(configSuffix, "");
                if (!plugin.getSuffixDatabase().getAllSuffixes().contains(s.replace(configSuffix, ""))) {
                    player.sendMessage(ChatUtil.parseLegacyColors("&7Suffix: " + ChatColor.translateAlternateColorCodes('&', plugin.getLuckPermsHook().getGroupSuffixColor(s))));
                }
            }

            player.sendMessage(ChatColor.AQUA.toString() + ChatColor.STRIKETHROUGH + "                                    ");

        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "There was an error trying to connect to the database, please try again later.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            throw new RuntimeException(e);
        }

        return true;
    }
}