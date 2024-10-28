package me.zombieman.dev.suffixesplus.databases;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "nonPurchasableSuffixes")
public class SuffixData {

    @DatabaseField(id = true)
    private String suffix;

    public SuffixData() {}

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
