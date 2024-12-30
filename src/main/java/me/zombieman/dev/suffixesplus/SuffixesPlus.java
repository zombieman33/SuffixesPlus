package me.zombieman.dev.suffixesplus;

import me.zombieman.dev.suffixesplus.commands.SuffixCmd;
import me.zombieman.dev.suffixesplus.commands.SuffixesCmd;
import me.zombieman.dev.suffixesplus.databases.PlayerDatabase;
import me.zombieman.dev.suffixesplus.databases.SuffixDatabase;
import me.zombieman.dev.suffixesplus.databases.TempSuffixDatabase;
import me.zombieman.dev.suffixesplus.hooks.LuckPermsHook;
import me.zombieman.dev.suffixesplus.listeners.InventoryClickListener;
import me.zombieman.dev.suffixesplus.listeners.PlayerListener;
import me.zombieman.dev.suffixesplus.managers.GuiManager;
import me.zombieman.dev.suffixesplus.api.LuckPermsAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class SuffixesPlus extends JavaPlugin {

    private LuckPermsHook luckPermsHook;
    private PlayerDatabase database;
    private SuffixDatabase suffixDatabase;
    private TempSuffixDatabase tempSuffixDatabase;
    public GuiManager guiManager;
    public LuckPermsAPI luckPermsAPI;
    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            getLogger().warning("-----------------------------------------");
            getLogger().warning("WARNING");
            getLogger().warning("LuckPerms plugin is not installed!");
            getLogger().warning(this.getPluginMeta().getName() + " is now being disabled!");
            getLogger().warning("-----------------------------------------");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("-----------------------------------------");
            getLogger().warning("WARNING");
            getLogger().warning("PlaceholderAPI plugin is not installed!");
            getLogger().warning(this.getPluginMeta().getName() + " is now being disabled!");
            getLogger().warning("-----------------------------------------");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        luckPermsAPI = new LuckPermsAPI(this);

        String username = this.getConfig().getString("database.username");
        String password = this.getConfig().getString("database.password");
        String url = this.getConfig().getString("database.url");

        try {
            database = new PlayerDatabase(url, username, password);
            suffixDatabase = new SuffixDatabase(url, username, password);
            tempSuffixDatabase = new TempSuffixDatabase(this, url, username, password, luckPermsAPI);
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to connect to database! " + e.getMessage());
            getLogger().severe("-----------------------------------------");
            getLogger().severe("ERROR: Failed to connect to database");
            getLogger().severe(this.getPluginMeta().getName() + " is now being disabled!");
            getLogger().severe("-----------------------------------------");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        LuckPerms luckPerms = LuckPermsProvider.get();
        luckPermsHook = new LuckPermsHook(this, luckPerms);

        guiManager = new GuiManager(this, luckPermsHook);

        new PlayerListener(this);
        new InventoryClickListener(this, luckPermsHook);

        getCommand("suffix").setExecutor(new SuffixCmd(this));
        getCommand("suffixes").setExecutor(new SuffixesCmd(this));
    }

    @Override
    public void onDisable() {
        getDatabase().close();
        getSuffixDatabase().close();
    }


    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }

    public PlayerDatabase getDatabase() {
        return database;
    }
    public SuffixDatabase getSuffixDatabase() {
        return suffixDatabase;
    }
    public TempSuffixDatabase getTempSuffixDatabase() {
        return tempSuffixDatabase;
    }
}
