package me.zombieman.dev.suffixesplus.databases;


import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "playerData")
public class PlayerData {

    @DatabaseField(id = true)
    private String uuid;
    @DatabaseField(canBeNull = false)
    private String username;
    @DatabaseField(defaultValue = "null")
    private String suffix;
    @DatabaseField(defaultValue = "true")
    private boolean notifications;

    public PlayerData() {}

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCurrentSuffix() {
        return suffix;
    }

    public void setCurrentSuffix(String suffix) {
        this.suffix = suffix;
    }

    public boolean getNotifications() {
        return notifications;
    }
    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }
}