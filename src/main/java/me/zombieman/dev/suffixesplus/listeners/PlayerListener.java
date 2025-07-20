package me.zombieman.dev.suffixesplus.listeners;

import me.zombieman.dev.suffixesplus.SuffixesPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class PlayerListener implements Listener {

    private SuffixesPlus plugin;

    public PlayerListener(SuffixesPlus plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            try {
                plugin.getDatabase().getPlayer(player.getUniqueId(), player.getName());
                if (!plugin.getDatabase().getPlayer(player.getUniqueId(), player.getName()).getUsername().equalsIgnoreCase(player.getName())) {
                    plugin.getDatabase().updateUsername(player, player.getName());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

    }

    @EventHandler
    public void onJoinPermissionCheck(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        CompletableFuture.runAsync(() -> {
            try {
                String currentSuffix = plugin.getDatabase().getPlayer(player.getUniqueId(), player.getName()).getCurrentSuffix();

                if (currentSuffix.equalsIgnoreCase("n/a")) return;

                String configSuffix = plugin.getConfig().getString("suffix.prefix", "suffix_");
                String suffix = currentSuffix.replace(configSuffix, "");

                plugin.getLuckPermsHook().checkAccessAsync(player.getUniqueId(), player.getName(), configSuffix, currentSuffix).thenAccept(hasAccess -> {
                    if (hasAccess || player.hasPermission("suffixsplus.suffix." + suffix)) return;

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getLuckPermsHook().removeSuffix(player, suffix);
                    });

                });

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
