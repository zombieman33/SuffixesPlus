package me.zombieman.dev.suffixesplus.managers;

import me.zombieman.dev.suffixesplus.SuffixesPlus;
import me.zombieman.dev.suffixesplus.hooks.LuckPermsHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class TempSuffixManager {

    private final SuffixesPlus plugin;
    private final LuckPermsHook luckPermsHook;

    public TempSuffixManager(SuffixesPlus plugin) {
        this.plugin = plugin;
        this.luckPermsHook = plugin.getLuckPermsHook();
        start();
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        String currentSuffix = plugin.getDatabase().getPlayer(player.getUniqueId(), player.getName()).getCurrentSuffix();

                        if (currentSuffix.equalsIgnoreCase("n/a")) return;

                        String configSuffix = plugin.getConfig().getString("suffix.prefix", "suffix_");
                        String suffix = currentSuffix.replace(configSuffix, "");

                        luckPermsHook.checkAccessAsync(player.getUniqueId(), player.getName(), configSuffix, currentSuffix).thenAccept(hasAccess -> {
                            if (hasAccess || player.hasPermission("suffixsplus.suffix." + suffix)) return;

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                luckPermsHook.removeSuffix(player, suffix);
                            });

                        });

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
            }

        }, 0L, 20 * 60L);
    }
}
