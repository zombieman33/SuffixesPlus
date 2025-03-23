package me.zombieman.dev.suffixesplus;

import me.zombieman.dev.suffixesplus.commands.SuffixCmd;
import me.zombieman.dev.suffixesplus.commands.SuffixesCmd;
import me.zombieman.dev.suffixesplus.databases.PlayerDatabase;
import me.zombieman.dev.suffixesplus.databases.SuffixDatabase;
import me.zombieman.dev.suffixesplus.managers.TempSuffixManager;
import me.zombieman.dev.suffixesplus.hooks.LuckPermsHook;
import me.zombieman.dev.suffixesplus.listeners.InventoryClickListener;
import me.zombieman.dev.suffixesplus.listeners.PlayerListener;
import me.zombieman.dev.suffixesplus.managers.GuiManager;
import me.zombieman.dev.suffixesplus.api.LuckPermsAPI;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class SuffixesPlus extends JavaPlugin {

    private LuckPermsHook luckPermsHook;
    private PlayerDatabase database;
    private SuffixDatabase suffixDatabase;
    private TempSuffixManager tempSuffixManager;
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
        tempSuffixManager = new TempSuffixManager(this);

        new PlayerListener(this);
        new InventoryClickListener(this, luckPermsHook);

        getCommand("suffix").setExecutor(new SuffixCmd(this));
        getCommand("suffixes").setExecutor(new SuffixesCmd(this));

        checkIfPlayerHasSuffix();
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

    public void checkIfPlayerHasSuffix() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                int onlinePlayers = Bukkit.getOnlinePlayers().size();

                if (onlinePlayers == 0) return;

                for (Player player : Bukkit.getOnlinePlayers()) {

                    String currentSuffix = getDatabase().getPlayer(player.getUniqueId(), player.getName()).getCurrentSuffix();

                    if (!currentSuffix.equalsIgnoreCase("n/a")) continue;

                    int ownedSuffixes = 0;

                    for (String suffix : getLuckPermsHook().getAllSuffixes()) {
                        suffix = suffix.replace(getConfig().getString("suffix.prefix", "suffix_"), "");
                        boolean hasPermission = player.hasPermission("suffixsplus.suffix." + suffix);

                        if (hasPermission) ownedSuffixes++;
                    }

                    if (ownedSuffixes == 0) continue;

                    if (!getDatabase().getPlayer(player.getUniqueId(), player.getName()).getNotifications() && getConfig().getBoolean("suffix.notification", true)) continue;

                    player.sendMessage(MiniMessage.miniMessage().deserialize("""
                                <green><strikethrough>                                             </strikethrough>
                                <green>🌟 Remember to use your suffixes! 🌟
                                <green>You can choose from %ownedSuffixes% suffixes.
                                <green>/suffix (CLICK ME)
                                <green><strikethrough>                                             </strikethrough>"""
                                    .replace("%ownedSuffixes%", String.valueOf(ownedSuffixes)))
                            .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<green>Click to view suffixes")))
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/suffix")));

                    if (getConfig().getBoolean("suffix.notification", true)) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("""
                                        <gray>/suffix notification (to turn off notifications)""")
                                .hoverEvent(HoverEvent.showText(MiniMessage.miniMessage().deserialize("<green>Click to turn of suffix notifications")))
                                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/suffix notification")));
                    }

                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                }
            } catch (SQLException e) {
                System.err.println("There was an error connecting to the database: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0L, 20L * 60 * 20);
    }


}
