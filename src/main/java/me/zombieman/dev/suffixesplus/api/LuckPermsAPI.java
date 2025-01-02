package me.zombieman.dev.suffixesplus.api;

import me.zombieman.dev.suffixesplus.SuffixesPlus;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import java.util.UUID;

public class LuckPermsAPI {

    private final SuffixesPlus plugin;

    public LuckPermsAPI(SuffixesPlus plugin) {
        this.plugin = plugin;
    }

    public void addTempPermission(UUID uuid, String permission) {
        String command = String.format("lp user %s permission set %s true", uuid, permission);
        executeCommand(command);
    }

    public void removeParent(UUID uuid, String parentGroup) {
        String suffix = plugin.getConfig().getString("suffix.prefix", "suffix_");
        parentGroup = parentGroup.replace(suffix, "");
        parentGroup = suffix + parentGroup;

        String command = String.format("lp user %s parent remove %s", uuid, parentGroup);
        executeCommand(command);
    }

    public void addParent(UUID uuid, String parentGroup) {
        String suffix = plugin.getConfig().getString("suffix.prefix", "suffix_");
        parentGroup = parentGroup.replace(suffix, "");
        parentGroup = suffix + parentGroup;

        String command = String.format("lp user %s parent add %s", uuid, parentGroup);
        executeCommand(command);
    }

    public void removePermission(UUID uuid, String permission) {
        String command = String.format("lp user %s permission unset %s", uuid, permission);
        executeCommand(command);
    }

    private void executeCommand(String command) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(console, command));
    }
}
