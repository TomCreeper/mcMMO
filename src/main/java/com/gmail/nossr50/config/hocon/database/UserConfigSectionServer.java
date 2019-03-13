package com.gmail.nossr50.config.hocon.database;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class UserConfigSectionServer {

    /* DEFAULT VALUES */
    private static final boolean USE_SSL_DEFAULT = true;
    private static final int SERVER_PORT_DEFAULT = 3306;
    private static final String SERVER_ADDRESS_DEFAULT = "localhost";

    /*
     * CONFIG NODES
     */

    @Setting(value = "Use_SSL", comment =   "Enables SSL for MySQL/MariaDB connections." +
                                            "\nIf your SQL server supports SSL, it is recommended to have it on but not necessary." +
                                            "\nIf you run into any issues involving SSL, its best to just turn this off." +
            "\nDefault value: "+USE_SSL_DEFAULT)
    private boolean useSSL = USE_SSL_DEFAULT;

    @Setting(value = "Server_Port", comment = "Your MySQL/MariaDB server port" +
            "\nThe default port is typically 3306 for MySQL, but every server configuration is different!" +
            "\nDefault value: "+SERVER_PORT_DEFAULT)
    private int serverPort = SERVER_PORT_DEFAULT;

    @Setting(value = "Server_Address", comment = "The address for your MySQL/MariaDB server" +
            "If the MySQL server is hosted on the same machine, you can use the localhost alias" +
            "\nDefault value: "+SERVER_ADDRESS_DEFAULT)
    private String serverAddress = SERVER_ADDRESS_DEFAULT;

    @Setting(value = "Max_Connections", comment = "This setting is the max simultaneous MySQL/MariaDB connections allowed at a time." +
            "\nThis needs to be high enough to support multiple player logins in quick succession, it is recommended that you do not lower these values")
    private ConfigSectionMaxConnections configSectionMaxConnections = new ConfigSectionMaxConnections();

    @Setting(value = "Max_Pool_Size", comment = "This setting is the max size of the pool of cached connections that we hold at any given time.")
    private ConfigSectionMaxPoolSize configSectionMaxPoolSize = new ConfigSectionMaxPoolSize();

    /*
     * GETTER BOILERPLATE
     */

    public boolean isUseSSL() {
        return useSSL;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public ConfigSectionMaxConnections getConfigSectionMaxConnections() {
        return configSectionMaxConnections;
    }

    public ConfigSectionMaxPoolSize getConfigSectionMaxPoolSize() {
        return configSectionMaxPoolSize;
    }


}
