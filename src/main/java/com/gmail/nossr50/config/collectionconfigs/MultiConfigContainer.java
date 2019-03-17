package com.gmail.nossr50.config.collectionconfigs;

import com.gmail.nossr50.config.ConfigCollection;
import com.gmail.nossr50.config.Unload;
import com.gmail.nossr50.mcMMO;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Represents a type of config collection, these config collections are spread across multiple config files potentially
 * @param <T>
 */
public class MultiConfigContainer<T> implements Unload {

    /* CONSTANTS */
    public static final String DEFAULT_MULTICONFIG_FILENAME_SUFFIX = ".vanilla.yml";

    /* VARS */
    private final String configPrefix;
    private Collection<T> collection;
    public final CollectionClassType collectionClassType;
    private ConfigCollection vanillaConfig;

    @Override
    public void unload() {
        collection.clear();
        vanillaConfig.unload();
    }

    public MultiConfigContainer(String configPrefix, CollectionClassType collectionClassType)
    {
        //Define Config Class
        this.collectionClassType = collectionClassType;

        //Define Config Filename Prefix
        this.configPrefix = configPrefix;

        //Init Collection
        collection = new ArrayList<T>();

        //Load Configs

        //Vanilla Config
        initConfigAndAddCollection(mcMMO.p.getDataFolder().getAbsolutePath(),getVanillaConfigName(configPrefix), false, true, false);

        //Custom Configs
        loadCustomCollections(configPrefix);
    }

    /**
     * Add another collection to this collection
     * @param otherCollection
     */
    private void addCollection(Collection<T> otherCollection)
    {
        collection.addAll(otherCollection);
    }

    /**
     * Grabs the Class to instance for this config collection
     * @param collectionClassType the type of class
     * @return the class to instance for this config collection
     */
    private Class getConfigClass(CollectionClassType collectionClassType)
    {
        switch(collectionClassType) {
            case REPAIR:
                return RepairConfig.class;
            case SALVAGE:
                return SalvageConfig.class;
            default:
                mcMMO.p.getLogger().severe("[DEBUG] Config Class type is undefined!");
                return null;
        }
    }

    /**
     * Gets the name of the vanilla config which is always present
     * @param configPrefix the prefix to the filename, for example "repair" or "salvage"
     * @return the name of the vanilla config file for this collection
     */
    private String getVanillaConfigName(String configPrefix)
    {
        return configPrefix+DEFAULT_MULTICONFIG_FILENAME_SUFFIX;
    }

    /**
     * Initializes a config and attempts to load add its collection
     * @param parentFolderPath Path to the "parent" folder on disk
     * @param relativePath Path to the config relative to the "parent" folder, this should mirror internal structure of resource files
     * @param mergeNewKeys if true, the users config will add keys found in the internal file that are missing from the users file during load
     * @param copyDefaults if true, the users config file when it is first made will be a copy of an internal resource file of the same name and path
     * @param removeOldKeys if true, the users config file will have keys not found in the internal default resource file of the same name and path removed
     */
    private void initConfigAndAddCollection(String parentFolderPath, String relativePath, boolean mergeNewKeys, boolean copyDefaults, boolean removeOldKeys)
    {
        mcMMO.p.getLogger().info("Reading from collection config - "+relativePath);
        ConfigCollection configCollection = null;

        try {
            //String parentFolderPath, String relativePath, boolean mergeNewKeys, boolean copyDefaults, boolean removeOldKeys
            configCollection = (ConfigCollection) getConfigClass(collectionClassType).getConstructor(String.class, String.class, Boolean.class, Boolean.class, Boolean.class).newInstance(parentFolderPath, relativePath, mergeNewKeys, copyDefaults, removeOldKeys);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        //Add the collection loaded from this config
        addCollection(configCollection.getLoadedCollection());
    }

    /**
     * mcMMO allows collection config files to be named things like repair.whatevernameyouwanthere.yml and so on,
     *  these files are treated in the same way as the vanilla file. They serve the purpose of organization
     * @param configPrefix the prefix of the file name, for example "repair", "salvage", etc
     */
    public void loadCustomCollections(String configPrefix)
    {
        String vanillaConfigFileName = getVanillaConfigName(configPrefix);

        //Find other files
        Pattern pattern = Pattern.compile(configPrefix+"\\.(?:.+)\\.yml");
        //File dataFolder = McmmoCore.getDataFolderPath();
        File dataFolder = mcMMO.p.getDataFolder();

        for (String fileName : dataFolder.list()) {
            //Vanilla Config is already loaded
            if(fileName.equalsIgnoreCase(vanillaConfigFileName))
                continue;

            //Find files that match the pattern
            if (!pattern.matcher(fileName).matches()) {
                continue;
            }

            //Init file
            File currentFile = new File(dataFolder, fileName);

            //Make sure its not a directory (needed?)
            if(currentFile.isDirectory())
                continue;

            //Load and add the collections
            initConfigAndAddCollection(dataFolder.getAbsolutePath(), fileName, false, false, false);
        }
    }

    public Collection<T> getCollection() {
        return collection;
    }
}
