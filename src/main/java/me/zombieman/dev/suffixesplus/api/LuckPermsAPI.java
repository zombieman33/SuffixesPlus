package me.zombieman.dev.suffixesplus.api;

import me.zombieman.dev.suffixesplus.SuffixesPlus;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;

import java.time.Duration;
import java.util.UUID;

public class LuckPermsAPI {

    private final LuckPerms luckPerms;
    private final SuffixesPlus plugin;

    public LuckPermsAPI(SuffixesPlus plugin) {
        this.plugin = plugin;
        this.luckPerms = LuckPermsProvider.get();
    }

    public void addTempPermission(UUID uuid, String permission) {
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user != null) {
            Node node = PermissionNode.builder(permission)
                    .value(true)
                    .build();

            user.data().add(node);
            luckPerms.getUserManager().saveUser(user);
        }
    }

    public void removeParent(UUID uuid, String parentGroup) {
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user != null) {

            String suffix = plugin.getConfig().getString("suffix.prefix", "suffix_");
            parentGroup = parentGroup.replace(suffix, "");

            parentGroup = suffix + parentGroup;

            Node node = InheritanceNode.builder(parentGroup).build();
            user.data().remove(node);
            luckPerms.getUserManager().saveUser(user);
        }
    }

    public void addParent(UUID uuid, String parentGroup) {
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user != null) {

            String suffix = plugin.getConfig().getString("suffix.prefix", "suffix_");
            parentGroup = parentGroup.replace(suffix, "");

            parentGroup = suffix + parentGroup;

            Node node = InheritanceNode.builder(parentGroup).build();
            user.data().add(node);
            luckPerms.getUserManager().saveUser(user);
        }
    }

    public void removePermission(UUID uuid, String permission) {
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user != null) {
            Node node = PermissionNode.builder(permission).value(true).build();
            user.data().remove(node);
            luckPerms.getUserManager().saveUser(user);
        }
    }
}
