package me.zombieman.dev.suffixesplus.databases;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import me.zombieman.dev.suffixesplus.SuffixesPlus;
import me.zombieman.dev.suffixesplus.api.LuckPermsAPI;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TempSuffixDatabase {

    private final Dao<TempSuffixData, String> tempSuffixDao;
    private final ConnectionSource connectionSource;
    private final ScheduledExecutorService scheduler;
    private final LuckPermsAPI luckPermsAPI;
    private final SuffixesPlus plugin;

    public TempSuffixDatabase(SuffixesPlus plugin, String jdbcUrl, String username, String password, LuckPermsAPI luckPermsAPI) throws SQLException {
        this.plugin = plugin;
        this.luckPermsAPI = luckPermsAPI;
        connectionSource = new JdbcConnectionSource(jdbcUrl, username, password);
        TableUtils.createTableIfNotExists(connectionSource, TempSuffixData.class);
        tempSuffixDao = DaoManager.createDao(connectionSource, TempSuffixData.class);
        scheduler = Executors.newScheduledThreadPool(1);
        startExpirationCheck();
        System.out.println("TempSuffixDatabase initialized.");
    }

    public void close() {
        try {
            if (!scheduler.isShutdown()) {
                scheduler.shutdown();
            }
            if (connectionSource.isOpen("default")) {
                connectionSource.close();
            }
            System.out.println("TempSuffixDatabase connection closed.");
        } catch (Exception e) {
            System.err.println("Failed to close TempSuffixDatabase connection: " + e.getMessage());
        }
    }

    public void addTempSuffix(UUID uuid, String suffix, long durationMillis) throws SQLException {
        long expirationTime = System.currentTimeMillis() + durationMillis;

        TempSuffixData tempSuffixData = tempSuffixDao.queryForId(uuid.toString());
        if (tempSuffixData == null) {
            tempSuffixData = new TempSuffixData();
            tempSuffixData.setUuid(uuid.toString());
        }

        tempSuffixData.setTempSuffix(suffix);
        tempSuffixData.setExpirationTime(expirationTime);
        tempSuffixDao.createOrUpdate(tempSuffixData);

        plugin.getLuckPermsHook().clearAllSuffixes(uuid, false);

        String configPrefix = plugin.getConfig().getString("suffix.prefix", "suffix_");

        plugin.getDatabase().updateSuffixes(uuid, configPrefix + suffix.replace(configPrefix, ""));

        luckPermsAPI.addTempPermission(uuid, "suffixsplus.suffix." + suffix);
        luckPermsAPI.addParent(uuid, suffix);
    }

    public void removeTempSuffix(UUID uuid) throws SQLException {
        TempSuffixData tempSuffixData = tempSuffixDao.queryForId(uuid.toString());
        if (tempSuffixData != null) {
            String suffix = tempSuffixData.getTempSuffix();
            tempSuffixDao.delete(tempSuffixData);

            luckPermsAPI.removePermission(uuid, "suffixsplus.suffix." + suffix);
            luckPermsAPI.removeParent(uuid, suffix);

            String configPrefix = plugin.getConfig().getString("suffix.prefix", "suffix_");

            if (plugin.getDatabase().getPlayer(uuid, plugin.getDatabase().getUsernameByUUID(uuid)).getCurrentSuffix().equalsIgnoreCase(configPrefix + suffix.replace(configPrefix, ""))) {
                plugin.getDatabase().updateSuffixes(uuid, null);
            }
        }
    }

    private void startExpirationCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<TempSuffixData> allData = tempSuffixDao.queryForAll();
                long currentTime = System.currentTimeMillis();
                for (TempSuffixData data : allData) {
                    if (data.getExpirationTime() <= currentTime) {
                        removeTempSuffix(UUID.fromString(data.getUuid()));
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error during expiration check: " + e.getMessage());
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public List<String> getActiveSuffixes(UUID uuid) throws SQLException {
        List<String> activeSuffixes = new ArrayList<>();
        TempSuffixData tempSuffixData = tempSuffixDao.queryForId(uuid.toString());
        if (tempSuffixData != null && tempSuffixData.getExpirationTime() > System.currentTimeMillis()) {
            activeSuffixes.add(tempSuffixData.getTempSuffix());
        }
        return activeSuffixes;
    }
}
