package me.zombieman.dev.suffixesplus.databases;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SuffixDatabase {
    private final Dao<SuffixData, String> suffixDao;
    private final HikariDataSource hikari;
    private final ConnectionSource connectionSource;

    public SuffixDatabase(String jdbcUrl, String username, String password) throws SQLException {
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
        config.setPoolName("SuffixesPlusHikariPool-SuffixDatabase");

        hikari = new HikariDataSource(config);

        connectionSource = new DataSourceConnectionSource(hikari, jdbcUrl);

        // Create table if not exists
        TableUtils.createTableIfNotExists(connectionSource, SuffixData.class);

        suffixDao = DaoManager.createDao(connectionSource, SuffixData.class);
        System.out.println("Database connection established with HikariCP and suffix table checked.");
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

    public void addSuffix(String suffix) throws SQLException {
        SuffixData suffixData = new SuffixData();
        suffixData.setSuffix(suffix);
        suffixDao.create(suffixData);
    }

    public void removeSuffix(String suffix) throws SQLException {
        suffixDao.deleteById(suffix);
    }

    public void clearSuffixes() throws SQLException {
        TableUtils.clearTable(connectionSource, SuffixData.class);
    }

    public boolean suffixDoesNotExists(String suffix) throws SQLException {
        return suffixDao.queryForId(suffix) == null;
    }

    public List<String> getAllSuffixes() throws SQLException {
        List<SuffixData> allSuffixes = suffixDao.queryForAll();
        List<String> suffixList = new ArrayList<>();
        for (SuffixData suffixData : allSuffixes) {
            suffixList.add(suffixData.getSuffix());
        }
        return suffixList;
    }
}
