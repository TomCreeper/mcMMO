package com.gmail.nossr50.config.hocon.database;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ConfigCategoryMaxPoolSize {
    @Setting(value = "Misc")
    private int misc = 10;

    @Setting(value = "Load")
    private int load = 20;

    @Setting(value = "Save")
    private int save = 20;

    /*
     * GETTER BOILERPLATE
     */

    public int getMisc() {
        return misc;
    }

    public int getLoad() {
        return load;
    }

    public int getSave() {
        return save;
    }
}
