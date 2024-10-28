package me.zombieman.dev.suffixesplus.databases;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDatabase {
    private final Dao<PlayerData, String> suffixDataStringDao;
    private final ConnectionSource connectionSource;

    public PlayerDatabase(String jdbcUrl, String username, String password) throws SQLException {
        connectionSource = new JdbcConnectionSource(jdbcUrl, username, password);
        TableUtils.createTableIfNotExists(connectionSource, PlayerData.class);
        suffixDataStringDao = DaoManager.createDao(connectionSource, PlayerData.class);
        System.out.println("Database connection established and tables checked.");
    }

    public void close() {
        try {
            if (connectionSource != null && !connectionSource.isOpen("default")) {
                connectionSource.close();
                System.out.println("Database connection closed.");
            }
        } catch (Exception e) {
            System.out.println("Failed to close database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void updateUsername(OfflinePlayer offlinePlayer, String username) throws SQLException {
        String uuid = offlinePlayer.getUniqueId().toString();
        PlayerData playerData = suffixDataStringDao.queryForId(uuid);
        if (playerData != null) {
            playerData.setUsername(username);
            suffixDataStringDao.update(playerData);
        }
    }
    public void updateSuffixes(OfflinePlayer offlinePlayer, String suffixes) throws SQLException {
        String uuid = offlinePlayer.getUniqueId().toString();
        PlayerData playerData = suffixDataStringDao.queryForId(uuid);
        if (playerData != null) {
            playerData.setCurrentSuffix(suffixes);
            suffixDataStringDao.update(playerData);
        }
    }
    public void updateSuffixes(UUID uuid, String suffixes) throws SQLException {
        PlayerData playerData = suffixDataStringDao.queryForId(uuid.toString());
        if (playerData != null) {
            playerData.setCurrentSuffix(suffixes);
            suffixDataStringDao.update(playerData);
        }
    }

    // Get the linked account for the player, create a new one if it doesn't exist
    public PlayerData getPlayer(UUID uuid, String name) throws SQLException {
        PlayerData playerData = suffixDataStringDao.queryForId(uuid.toString());

        if (playerData == null) {
            playerData = new PlayerData();
            playerData.setUuid(uuid.toString());
            playerData.setUsername(name);
            if (playerData.getCurrentSuffix() == null) playerData.setCurrentSuffix("N/A");
            suffixDataStringDao.create(playerData);
        }

        return playerData;
    }
    public String getUuidByUsername(String username) throws SQLException {
        // Query for a player by username
        PlayerData playerData = suffixDataStringDao.queryBuilder()
                .where()
                .eq("username", username)
                .queryForFirst();

        // If suffixData is null, that means no player was found with the given username
        if (playerData != null) {
            return playerData.getUuid();
        } else {
            return null;
        }
    }
    public List<String> getAllUsernames() throws SQLException {
        List<String> usernames = new ArrayList<>();

        List<PlayerData> allsuffixData = suffixDataStringDao.queryForAll();

        for (PlayerData playerData : allsuffixData) {
            usernames.add(playerData.getUsername());
        }

        return usernames;
    }
    public String getUsernameByUUID(UUID uuid) throws SQLException {
        PlayerData playerData = suffixDataStringDao.queryForId(uuid.toString());
        if (playerData != null) {
            return playerData.getUsername();
        } else {
            return null;
        }
    }
}