package me.zombieman.dev.suffixesplus.commands;

import me.zombieman.dev.suffixesplus.SuffixesPlus;
import me.zombieman.dev.suffixesplus.utils.ChatUtil;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import java.util.UUID;
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
        MiniMessage miniMessage = MiniMessage.miniMessage();

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
                case "help":

                    String suffixArg = "<suffix>";

                    if (args.length >= 2) {
                        suffixArg = args[1];
                    }

                    player.sendMessage(ChatColor.GREEN.toString() + ChatColor.STRIKETHROUGH + "                                         ");
                    player.sendMessage(ChatColor.GREEN + "Creating the suffix:");
                    player.sendMessage(miniMessage.deserialize("<yellow>1. /lp creategroup suffix_" + suffixArg)
                            .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<green>Click here")))
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/lp creategroup suffix_" + suffixArg)));
                    player.sendMessage(miniMessage.deserialize("<yellow>2. /lp editor")
                            .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<green>Click here")))
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/lp editor")));
                    player.sendMessage(miniMessage.deserialize("<yellow>3. Add `suffix.0.<suffix>` to the suffix group."));
                    player.sendMessage(ChatColor.GREEN.toString() + ChatColor.STRIKETHROUGH + "                                         ");
                    player.sendMessage(ChatColor.GREEN + "Making the suffix non purchasable:");
                    player.sendMessage(miniMessage.deserialize("<yellow>1. /suffix add <bold>" + suffixArg + "</bold>")
                            .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<green>Click here")))
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/suffix add " + suffixArg)));
                    player.sendMessage(ChatColor.GREEN.toString() + ChatColor.STRIKETHROUGH + "                                         ");
                    player.sendMessage(ChatColor.GREEN + "Checking for the suffixes:");
                    player.sendMessage(miniMessage.deserialize("<yellow>1. /suffixes")
                            .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<green>Click here")))
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/suffixes")));
                    player.sendMessage(ChatColor.GREEN.toString() + ChatColor.STRIKETHROUGH + "                                         ");
                    player.sendMessage(ChatColor.GREEN + "Adding the permission:");
                    player.sendMessage(miniMessage.deserialize("<yellow>1. /lp user <player> permission")
                            .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<green>Click here")))
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/lp user <player> permission setsuffixsplus.suffix." + suffixArg)));
                    player.sendMessage(miniMessage.deserialize("<yellow>   set suffixsplus.suffix." + suffixArg)
                            .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<green>Click here")))
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/lp user <player> permission setsuffixsplus.suffix." + suffixArg)));
                    player.sendMessage(miniMessage.deserialize("<#7289da><strikethrough>                                         </strikethrough>"));
                    player.sendMessage(miniMessage.deserialize("<#7289da><bold>Click Here To Get Support!</bold>")
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/SuypvRBa4c"))
                            .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<#7289da>Click Here To Get Support!"))));
                    player.sendMessage(miniMessage.deserialize("<#7289da><strikethrough>                                         </strikethrough>"));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

                    return true;
                case "support":
                    player.sendMessage(miniMessage.deserialize("<#7289da><strikethrough>                                         </strikethrough>"));
                    player.sendMessage(miniMessage.deserialize("<#7289da><bold>Click Here To Get Support!</bold>")
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/SuypvRBa4c"))
                            .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<#7289da>Click Here To Get Support!"))));
                    player.sendMessage(miniMessage.deserialize("<#7289da><strikethrough>                                         </strikethrough>"));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                    return true;

                case "addtempsuffix":
                    if (args.length < 4) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /suffix addtempsuffix <player> <suffix> <time>");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return false;
                    }

                    String targetPlayerName = args[1];
                    String tempSuffix = args[2];
                    String time = args[3];

                    UUID uuid;
                    try {
                        if (!plugin.getDatabase().getAllUsernames().contains(targetPlayerName)) {
                            player.sendMessage(ChatColor.RED + "Player not found!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            return false;
                        } else {
                            uuid = UUID.fromString(plugin.getDatabase().getUuidByUsername(targetPlayerName));
                        }
                    } catch (SQLException e) {
                        System.err.println("Error connecting to database: " + e.getMessage());
                        player.sendMessage(ChatColor.RED + "There was an error trying to connect to the database, please try again later.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return false;
                    }

                    try {


                        if (plugin.getLuckPermsHook().getPlayerSuffixes(uuid).contains(configSuffix + tempSuffix.replace(configSuffix, ""))) {
                            player.sendMessage(ChatColor.RED + targetPlayerName + " already this suffix!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            return false;
                        }


                        if (!plugin.getTempSuffixDatabase().getActiveSuffixes(uuid).isEmpty()) {
                            player.sendMessage(ChatColor.RED + targetPlayerName + " already has a temp suffix!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            return false;
                        }

                        if (parseTime(time, player) == -1) {
                            return false;
                        }

                        plugin.getTempSuffixDatabase().addTempSuffix(uuid, tempSuffix, parseTime(time, player));
                        player.sendMessage(ChatColor.GREEN + "Successfully added temporary suffix " + tempSuffix + " to " + targetPlayerName + ".");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                    } catch (SQLException e) {
                        player.sendMessage(ChatColor.RED + "Error connecting to the database. Please try again later.");
                        throw new RuntimeException(e);
                    }
                    return true;

                case "removetempsuffix":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /suffix removetempsuffix <player>");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return false;
                    }

                    targetPlayerName = args[1];

                    try {
                        if (!plugin.getDatabase().getAllUsernames().contains(targetPlayerName)) {
                            player.sendMessage(ChatColor.RED + "Player not found!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            return false;
                        } else {
                            uuid = UUID.fromString(plugin.getDatabase().getUuidByUsername(targetPlayerName));
                        }
                    } catch (SQLException e) {
                        System.err.println("Error connecting to database: " + e.getMessage());
                        player.sendMessage(ChatColor.RED + "There was an error trying to connect to the database, please try again later.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return false;
                    }



                    try {
                        if (plugin.getTempSuffixDatabase().getActiveSuffixes(uuid).isEmpty()) {
                            player.sendMessage(ChatColor.RED + "Temporary suffix not found for this player.");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            return false;
                        }

                        plugin.getTempSuffixDatabase().removeTempSuffix(uuid);
                        player.sendMessage(ChatColor.GREEN + "Successfully removed temporary suffix " + plugin.getTempSuffixDatabase().getActiveSuffixes(uuid).get(0) + " from " + targetPlayerName + ".");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

                    } catch (SQLException e) {
                        player.sendMessage(ChatColor.RED + "Error connecting to the database. Please try again later.");
                        throw new RuntimeException(e);
                    }
                    return true;

                case "checktempsuffix":

                    targetPlayerName = args[1];

                    List<String> activeSuffixes = null;
                    try {
                        if (!plugin.getDatabase().getAllUsernames().contains(targetPlayerName)) {
                            player.sendMessage(ChatColor.RED + "Player not found!");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            return false;
                        } else {
                            uuid = UUID.fromString(plugin.getDatabase().getUuidByUsername(targetPlayerName));
                            activeSuffixes = plugin.getTempSuffixDatabase().getActiveSuffixes(uuid);
                        }
                    } catch (SQLException e) {
                        System.err.println("Error connecting to database: " + e.getMessage());
                        player.sendMessage(ChatColor.RED + "There was an error trying to connect to the database, please try again later.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return false;
                    }

                    if (activeSuffixes.isEmpty()) {
                        player.sendMessage(ChatColor.RED + targetPlayerName + " doesn't have any active temporary suffixes.");
                    } else {
                        player.sendMessage(ChatColor.GREEN + targetPlayerName + "'s active temporary suffix: " + String.join(", ", activeSuffixes));
                    }
                    return true;

                default:
                    player.sendMessage("Unknown subcommand. Usage: /tempsuffix <add|remove|check> <suffix> [duration (in seconds)]");
                    return false;
            }
        }

        try {
            plugin.guiManager.openSuffixGui(player, 0);
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "There was an error while connecting to database. Please try again later.");
            System.err.println("Error connecting to database: " + e.getMessage());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f ,1.0f);
            return false;
        }
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
                completions.add("help");
                completions.add("info");
                completions.add("support");
                completions.add("addtempsuffix");
                completions.add("checktempsuffix");
                completions.add("removetempsuffix");
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
                } else if (args[0].equalsIgnoreCase("help")) {
                    for (String suffix : plugin.getLuckPermsHook().getAllSuffixes()) {
                        completions.add(suffix.replace("suffix_", ""));
                    }
                } else if (args[0].equalsIgnoreCase("addtempsuffix") || args[0].equalsIgnoreCase("removetempsuffix") || args[0].equalsIgnoreCase("checktempsuffix")) {
                    try {
                        completions.addAll(plugin.getDatabase().getAllUsernames());
                    } catch (SQLException e) {
                        completions.add("Error connecting to database!");
                        throw new RuntimeException(e);
                    }
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("addtempsuffix")) {
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
                }
            } else if (args.length == 4) {
                if (args[0].equalsIgnoreCase("addtempsuffix")) {
                    completions.add("<time>");
                }
            }
        }

        String lastArg = args[args.length - 1];
        return completions.stream().filter(s -> s.startsWith(lastArg)).collect(Collectors.toList());
    }

    public static long parseTime(String timeStr, Player player) {
        long timeInMillis = 0;
        timeStr = timeStr.toLowerCase();

        // Check if the time string contains 's' or any seconds-related word
        if (timeStr.endsWith("s") || timeStr.endsWith("sec") || timeStr.endsWith("second") || timeStr.endsWith("seconds")) {
            player.sendMessage(ChatColor.RED + "You cannot use seconds. Please use a larger time unit (e.g., minutes, hours).");
            return -1;
        }

        // Check for minutes
        if (timeStr.endsWith("m") || timeStr.endsWith("min") || timeStr.endsWith("minute") || timeStr.endsWith("minutes")) {
            String num = timeStr.replaceAll("[^0-9]", "");
            timeInMillis = Integer.parseInt(num) * 60 * 1000L;
        }
        // Check for hours
        else if (timeStr.endsWith("h") || timeStr.endsWith("hour") || timeStr.endsWith("hours")) {
            String num = timeStr.replaceAll("[^0-9]", "");
            timeInMillis = Integer.parseInt(num) * 60 * 60 * 1000L;
        }
        // Check for days
        else if (timeStr.endsWith("d") || timeStr.endsWith("day") || timeStr.endsWith("days")) {
            String num = timeStr.replaceAll("[^0-9]", "");
            timeInMillis = Integer.parseInt(num) * 24 * 60 * 60 * 1000L;
        }
        // Check for weeks
        else if (timeStr.endsWith("w") || timeStr.endsWith("week") || timeStr.endsWith("weeks")) {
            String num = timeStr.replaceAll("[^0-9]", "");
            timeInMillis = Integer.parseInt(num) * 7 * 24 * 60 * 60 * 1000L;
        }
        // Check for months (approx 30 days)
        else if (timeStr.endsWith("mo") || timeStr.endsWith("month") || timeStr.endsWith("months")) {
            String num = timeStr.replaceAll("[^0-9]", "");
            timeInMillis = Integer.parseInt(num) * 30 * 24 * 60 * 60 * 1000L;
        }
        // Check for years
        else if (timeStr.endsWith("y") || timeStr.endsWith("year") || timeStr.endsWith("years")) {
            String num = timeStr.replaceAll("[^0-9]", "");
            timeInMillis = Integer.parseInt(num) * 365 * 24 * 60 * 60 * 1000L;
        } else {
            player.sendMessage(ChatColor.RED + "Invalid time format. Please use minutes (m), hours (h), days (d), weeks (w), months (mo), or years (y).");
            return -1;
        }

        return timeInMillis;
    }

}