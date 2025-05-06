package me.zombieman.dev.suffixesplus.commands;

import me.zombieman.dev.suffixesplus.SuffixesPlus;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SuffixAdminCmd implements CommandExecutor, TabCompleter {

    private final SuffixesPlus plugin;

    public SuffixAdminCmd(SuffixesPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("suffixsplus.command.suffixadmin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to run this command!");
            return false;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("set")) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /suffixadmin set <player> <all, purchasable, reset>");
            return false;
        }

        String playerName = args[1];
        String type = args[2].toLowerCase();

        if (!type.equals("all") && !type.equals("purchasable") && !type.equals("reset")) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /suffixadmin set <player> <all, purchasable, reset>");
            return false;
        }

        AtomicBoolean playerExists = new AtomicBoolean(true);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (plugin.getDatabase().getUuidByUsername(playerName) == null && Bukkit.getPlayer(playerName) == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown player " + playerName);
                    playerExists.set(false);
                } else {
                    plugin.getPurchasedManager().givePurchasedRole(sender, playerName, type);
                }
            } catch (SQLException e) {
                sender.sendMessage(ChatColor.RED + "There was an error while accessing the database.");
                e.printStackTrace();
            }
        });

        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        Player player = (Player) sender;

        if (!player.hasPermission("suffixsplus.command.suffixadmin")) return null;

        if (args.length == 1) {
            completions.add("set");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set")) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    completions.add(target.getName());
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set")) {
                completions.add("all");
                completions.add("purchasable");
                completions.add("reset");
            }
        }

        String lastArg = args[args.length - 1];
        return completions.stream().filter(s -> s.startsWith(lastArg)).collect(Collectors.toList());
    }
}
