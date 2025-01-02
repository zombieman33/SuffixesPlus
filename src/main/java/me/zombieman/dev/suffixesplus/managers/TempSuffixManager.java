package me.zombieman.dev.suffixesplus.managers;


import me.zombieman.dev.suffixesplus.SuffixesPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;

public class TempSuffixManager {

    private final SuffixesPlus plugin;

    public TempSuffixManager(SuffixesPlus plugin) {
        this.plugin = plugin;
        start();
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {

                try {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        String currentSuffix = plugin.getDatabase().getPlayer(player.getUniqueId(), player.getName()).getCurrentSuffix();

                        if (!player.hasPermission("suffixsplus.suffix." + currentSuffix.replace(plugin.getConfig().getString("suffix.prefix", "suffix_"), ""))) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getUniqueId() + " parent remove " + currentSuffix);
                            });
                            plugin.getDatabase().updateSuffixes(player, "n/a");
                        }

                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0L, 20 * 60L);
    }


}
