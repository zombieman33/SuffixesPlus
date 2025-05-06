package me.zombieman.dev.suffixesplus.managers;

import me.zombieman.dev.suffixesplus.SuffixesPlus;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class PurchasedManager {

    private final SuffixesPlus plugin;

    public PurchasedManager(SuffixesPlus plugin) {
        this.plugin = plugin;
    }

    public void givePurchasedRole(CommandSender player, String playerName, String all) {

        if (all == null) player.sendMessage("Invalid arguments!");

        if (!all.equalsIgnoreCase("purchasable") && !all.equalsIgnoreCase("all") && !all.equalsIgnoreCase("reset")) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {

                UUID uuid = null;
                if (plugin.getDatabase().getUuidByUsername(playerName) != null)
                    uuid = UUID.fromString(plugin.getDatabase().getUuidByUsername(playerName));

                Player target = Bukkit.getPlayer(playerName);
                if (target != null && uuid == null) uuid = target.getUniqueId();

                if (uuid == null) {
                    player.sendMessage("Player not found!");
                    return;
                }

                plugin.getDatabase().updatePurchasable(uuid, all.replace("reset", "null"));

                if (all.equalsIgnoreCase("reset")) {
                    player.sendMessage("Successfully reset " + playerName + " suffix access!");
                    return;
                }

                String message = all.equalsIgnoreCase("purchasable") ? " purchasable " : all.equalsIgnoreCase("all") ? " " : null;

                player.sendMessage("Successfully given " + playerName + " all" + message + "suffixes!");
            } catch (SQLException e) {
                player.sendMessage("Database error!");
                e.printStackTrace();
            }
        });
    }

}
