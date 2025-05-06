package me.zombieman.dev.suffixesplus.databases;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.List;

import java.util.ArrayList;

public class SuffixDatabase {
    private final Dao<SuffixData, String> suffixDao;
    private final ConnectionSource connectionSource;

    public SuffixDatabase(String jdbcUrl, String username, String password) throws SQLException {
        connectionSource = new JdbcConnectionSource(jdbcUrl, username, password);
        TableUtils.createTableIfNotExists(connectionSource, SuffixData.class);
        suffixDao = DaoManager.createDao(connectionSource, SuffixData.class);
        System.out.println("Database connection established and suffix table checked.");
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
