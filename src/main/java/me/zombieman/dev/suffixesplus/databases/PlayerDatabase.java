package me.zombieman.dev.suffixesplus.databases;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.zombieman.dev.suffixesplus.SuffixesPlus;
import me.zombieman.dev.suffixesplus.commands.SuffixesCmd;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDatabase {
    private final Dao<PlayerData, String> suffixDataStringDao;
    private final HikariDataSource hikari;
    private final ConnectionSource connectionSource;

    public PlayerDatabase(String jdbcUrl, String username, String password) throws SQLException {
        // HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setIdleTimeout(600_000); // 10 minutes
        config.setMaxLifetime(1_800_000); // 30 minutes
        config.setConnectionTimeout(30_000); // 30 seconds
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("SuffixesPlusHikariPool-PlayerDatabase");

        hikari = new HikariDataSource(config);

        connectionSource = new DataSourceConnectionSource(hikari, jdbcUrl);

        // Create table if not exists
        TableUtils.createTableIfNotExists(connectionSource, PlayerData.class);

        // Run schema updates safely
        try (Connection conn = hikari.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE playerData ADD COLUMN purchased VARCHAR(255) DEFAULT NULL;");
            stmt.executeUpdate("ALTER TABLE playerData ADD COLUMN notifications BOOLEAN NOT NULL DEFAULT FALSE;");
        } catch (SQLException ignored) {
            // Ignore if columns already exist
        }

        suffixDataStringDao = DaoManager.createDao(connectionSource, PlayerData.class);
        System.out.println("Database connection established with HikariCP and tables checked.");
    }

    public void close() {
        try {
            if (connectionSource != null) {
                connectionSource.close();
            }
            if (hikari != null) {
                hikari.close();
            }
            System.out.println("Database connection closed.");
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
    public void updatePurchasable(UUID uuid, String purchasable) throws SQLException {
        PlayerData playerData = suffixDataStringDao.queryForId(uuid.toString());
        if (playerData != null) {
            playerData.setPurchased(purchasable);
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
    public void updateNotification(UUID uuid, boolean notification) throws SQLException {
        PlayerData playerData = suffixDataStringDao.queryForId(uuid.toString());
        if (playerData != null) {
            playerData.setNotifications(notification);
            suffixDataStringDao.update(playerData);
        }
    }

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
        PlayerData playerData = suffixDataStringDao.queryBuilder()
                .where()
                .eq("username", username)
                .queryForFirst();

        return playerData != null ? playerData.getUuid() : null;
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
        return playerData != null ? playerData.getUsername() : null;
    }

    public void enableNotificationsForAll(SuffixesPlus plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<PlayerData> allPlayers = suffixDataStringDao.queryForAll();

                for (PlayerData playerData : allPlayers) {
                    if (!playerData.getNotifications()) {
                        playerData.setNotifications(true);
                        suffixDataStringDao.update(playerData);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
