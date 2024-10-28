package me.zombieman.dev.suffixesplus.listeners;

import me.zombieman.dev.suffixesplus.SuffixesPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;

public class PlayerListener implements Listener {

    private SuffixesPlus plugin;

    public PlayerListener(SuffixesPlus plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        try {
            plugin.getDatabase().getPlayer(player.getUniqueId(), player.getName());
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}