package me.zombieman.dev.suffixesplus.commands;

import me.zombieman.dev.suffixesplus.SuffixesPlus;
import me.zombieman.dev.suffixesplus.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SuffixCmd implements CommandExecutor, TabCompleter {

    private SuffixesPlus plugin;
    public SuffixCmd(SuffixesPlus plugin) {
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

        if (args.length >= 1 && player.hasPermission("suffixsplus.command.suffixadmin")) {

            String action = args[0];

            String configSuffix = plugin.getConfig().getString("suffix.prefix", "suffix_");
            String suffixPrefix = configSuffix;

            switch (action) {
                case "add":

                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /suffix add <suffix>");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return false;
                    }

                    String suffix = args[1];

                    boolean contains = plugin.getLuckPermsHook().getAllSuffixes().contains(suffixPrefix + suffix.replace(suffixPrefix, ""));

                    if (!contains) {
                        player.sendMessage(ChatColor.RED + "The " + suffix + " isn't a valid suffix.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return false;
                    }

                    try {

                        if (plugin.getSuffixDatabase().getAllSuffixes().contains(suffix)) {
                            player.sendMessage(ChatColor.RED + "This suffix is already a non purchasable suffix!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            return false;
                        }

                        plugin.getSuffixDatabase().addSuffix(suffix);
                        player.sendMessage(ChatColor.GREEN + "Successfully added the " + suffix + " to the list of non purchasable suffixes.");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                    } catch (SQLException e) {
                        player.sendMessage(ChatColor.RED + "There was an error trying to connect to the database, please try again later.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        throw new RuntimeException(e);
                    }
                    return true;

                case "remove":

                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /suffix remove <suffix>");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return false;
                    }

                    if (!plugin.getLuckPermsHook().getAllSuffixes().contains(suffixPrefix + args[1].replace(suffixPrefix, ""))) {
                        player.sendMessage(ChatColor.RED + "The " + args[1] + " isn't a valid suffix.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return false;
                    }

                    try {

                        if (!plugin.getSuffixDatabase().getAllSuffixes().contains(args[1])) {
                            player.sendMessage(ChatColor.RED + "This suffix is not in the list of non purchasable suffixes!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            return false;
                        }

                        plugin.getSuffixDatabase().removeSuffix(args[1]);
                        player.sendMessage(ChatColor.GREEN + "Successfully removed the " + args[1] + " from the list of non purchasable suffixes.");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                    } catch (SQLException e) {
                        player.sendMessage(ChatColor.RED + "There was an error trying to connect to the database, please try again later.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        throw new RuntimeException(e);
                    }
                    return true;

                case "clear":

                    try {
                        plugin.getSuffixDatabase().clearSuffixes();
                        player.sendMessage(ChatColor.GREEN + "Successfully cleared the list of non purchasable suffixes..");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                    } catch (SQLException e) {
                        player.sendMessage(ChatColor.RED + "There was an error trying to connect to the database, please try again later.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        throw new RuntimeException(e);
                    }
                    return true;
                case "info":

                    if (plugin.getLuckPermsHook().getAllSuffixes().isEmpty()) {
                        player.sendMessage(ChatColor.RED + "There aren't any suffixes in this list!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return false;
                    }

                    try {

                        if (plugin.getSuffixDatabase().getAllSuffixes().isEmpty()) {
                            player.sendMessage(ChatColor.RED.toString() + ChatColor.STRIKETHROUGH + "                                    ");
                            player.sendMessage(ChatColor.RED + "There aren't any suffixes in this list!");
                            player.sendMessage(ChatColor.RED.toString() + ChatColor.STRIKETHROUGH + "                                    ");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            return false;
                        }

                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

                        player.sendMessage(ChatColor.AQUA.toString() + ChatColor.STRIKETHROUGH + " " + ChatColor.AQUA + " Non Purchasable Suffixes " + ChatColor.AQUA.toString() + ChatColor.STRIKETHROUGH + " ");

                        for (String s : plugin.getSuffixDatabase().getAllSuffixes()) {
                            player.sendMessage(ChatUtil.parseLegacyColors("&7Suffix: " + ChatColor.translateAlternateColorCodes('&', plugin.getLuckPermsHook().getGroupSuffixColor(configSuffix + s.replace(configSuffix, "")))));
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

        plugin.guiManager.openSuffixGui(player, 0);

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        Player player = (Player) sender;

        if (player.hasPermission("suffixsplus.command.suffixadmin")) {
            if (args.length == 1) {
                completions.add("add");
                completions.add("remove");
                completions.add("clear");
                completions.add("info");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("add")) {
                    for (String suffix : plugin.getLuckPermsHook().getAllSuffixes()) {
                        try {
                            if (!plugin.getSuffixDatabase().getAllSuffixes().contains(suffix)) {
                                completions.add(suffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), ""));
                            }
                        } catch (SQLException e) {
                            completions.add("Error connecting to database!");
                            throw new RuntimeException(e);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("remove")) {
                    try {
                        completions.addAll(plugin.getSuffixDatabase().getAllSuffixes());
                    } catch (SQLException e) {
                        completions.add("Error connecting to database!");
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        String lastArg = args[args.length - 1];
        return completions.stream().filter(s -> s.startsWith(lastArg)).collect(Collectors.toList());
    }
}