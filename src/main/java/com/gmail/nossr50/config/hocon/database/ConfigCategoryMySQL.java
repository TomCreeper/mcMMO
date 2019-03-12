package com.gmail.nossr50.config.hocon.database;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ConfigCategoryMySQL {

    @Setting(value = "Enabled", comment = "If set to true, mcMMO will use MySQL/MariaDB instead of FlatFile storage")
    private boolean enabled = true;

    @Setting(value = "User", comment = "Your MySQL User Settings")
    private ConfigCategoryUser configCategoryUser;

    @Setting(value = "Database", comment = "Database settings for MySQL/MariaDB")
    private ConfigCategoryDatabase configCategoryDatabase;

    @Setting(value = "Server", comment = "Your MySQL/MariaDB server settings.")
    private ConfigCategoryServer configCategoryServer;

    /*
     * GETTER BOILERPLATE
     */

    public boolean isMySQLEnabled() {
        return enabled;
    }

    public ConfigCategoryDatabase getConfigCategoryDatabase() {
        return configCategoryDatabase;
    }

    public ConfigCategoryServer getConfigCategoryServer() {
        return configCategoryServer;
    }
}
