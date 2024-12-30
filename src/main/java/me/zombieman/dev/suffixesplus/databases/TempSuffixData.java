package me.zombieman.dev.suffixesplus.databases;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "temp_suffix_data")
public class TempSuffixData {

    @DatabaseField(id = true)
    private String uuid;

    @DatabaseField(canBeNull = false)
    private String tempSuffix;

    @DatabaseField(canBeNull = false)
    private long expirationTime;

    public TempSuffixData() {}

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTempSuffix() {
        return tempSuffix;
    }

    public void setTempSuffix(String tempSuffix) {
        this.tempSuffix = tempSuffix;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }
}
