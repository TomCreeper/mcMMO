package com.gmail.nossr50.config.treasure;

import com.gmail.nossr50.config.ConfigCollection;
import com.gmail.nossr50.config.UnsafeValueValidation;
import com.gmail.nossr50.datatypes.treasure.*;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.EnchantmentUtils;
import com.gmail.nossr50.util.StringUtils;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TreasureConfig extends ConfigCollection implements UnsafeValueValidation {

    public HashMap<String, List<ExcavationTreasure>> excavationMap = new HashMap<String, List<ExcavationTreasure>>();

    public HashMap<EntityType, List<ShakeTreasure>> shakeMap = new HashMap<EntityType, List<ShakeTreasure>>();
    public HashMap<String, List<HylianTreasure>> hylianMap = new HashMap<String, List<HylianTreasure>>();

    public HashMap<Rarity, List<FishingTreasure>> fishingRewards = new HashMap<Rarity, List<FishingTreasure>>();
    public HashMap<Rarity, List<EnchantmentTreasure>> fishingEnchantments = new HashMap<Rarity, List<EnchantmentTreasure>>();

    public TreasureConfig() {
        //super(McmmoCore.getDataFolderPath().getAbsoluteFile(),"treasures.yml");
        super(mcMMO.p.getDataFolder().getAbsoluteFile(), "treasures.yml", false, true, false);
        validateEntries();
    }

    /**
     * This grabs an instance of this config class from the Config Manager
     * This method is deprecated and will be removed in the future
     * @see mcMMO#getConfigManager()
     * @return the instance of this config
     * @deprecated Please use mcMMO.getConfigManager() to grab a specific config instead
     */
    @Deprecated
    public static TreasureConfig getInstance() {
        return mcMMO.getConfigManager().getTreasureConfig();
    }

    /**
     * The version of this config
     *
     * @return
     */
    @Override
    public double getConfigVersion() {
        return 1;
    }

    @Override
    public List<String> validateKeys() {
        // Validate all the settings!
        List<String> errorMessages = new ArrayList<String>();
        try {
            for (String tier : getUserRootNode().getNode("Enchantment_Drop_Rates").getList(TypeToken.of(String.class))) {
                double totalEnchantDropRate = 0;
                double totalItemDropRate = 0;

                for (Rarity rarity : Rarity.values()) {
                    double enchantDropRate = getDoubleValue("Enchantment_Drop_Rates." + tier + "." + rarity.toString());
                    double itemDropRate = getDoubleValue("Item_Drop_Rates." + tier + "." + rarity.toString());

                    if ((enchantDropRate < 0.0 || enchantDropRate > 100.0) && rarity != Rarity.RECORD) {
                        errorMessages.add("The enchant drop rate for " + tier + " items that are " + rarity.toString() + "should be between 0.0 and 100.0!");
                    }

                    if (itemDropRate < 0.0 || itemDropRate > 100.0) {
                        errorMessages.add("The item drop rate for " + tier + " items that are " + rarity.toString() + "should be between 0.0 and 100.0!");
                    }

                    totalEnchantDropRate += enchantDropRate;
                    totalItemDropRate += itemDropRate;
                }

                if (totalEnchantDropRate < 0 || totalEnchantDropRate > 100.0) {
                    errorMessages.add("The total enchant drop rate for " + tier + " should be between 0.0 and 100.0!");
                }

                if (totalItemDropRate < 0 || totalItemDropRate > 100.0) {
                    errorMessages.add("The total item drop rate for " + tier + " should be between 0.0 and 100.0!");
                }
            }
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }

        return errorMessages;
    }

    @Override
    protected void register() {
        if (config.getConfigurationSection("Treasures") != null) {
            backup();
            return;
        }

        loadTreasures("Fishing");
        loadTreasures("Excavation");
        loadTreasures("Hylian_Luck");
        loadEnchantments();

        for (EntityType entity : EntityType.values()) {
            if (entity.isAlive()) {
                loadTreasures("Shake." + entity.toString());
            }
        }
    }

    private void loadTreasures(String type) {
        boolean isFishing = type.equals("Fishing");
        boolean isShake = type.contains("Shake");
        boolean isExcavation = type.equals("Excavation");
        boolean isHylian = type.equals("Hylian_Luck");

        ConfigurationSection treasureSection = config.getConfigurationSection(type);

        if (treasureSection == null) {
            return;
        }

        // Initialize fishing HashMap
        for (Rarity rarity : Rarity.values()) {
            if (!fishingRewards.containsKey(rarity)) {
                fishingRewards.put(rarity, (new ArrayList<FishingTreasure>()));
            }
        }

        for (String treasureName : treasureSection.getKeys(false)) {
            // Validate all the things!
            List<String> reason = new ArrayList<String>();

            String[] treasureInfo = treasureName.split("[|]");
            String materialName = treasureInfo[0];

            /*
             * Material, Amount, and Data
             */
            Material material;

            if (materialName.contains("INVENTORY")) {
                // Use magic material BEDROCK to know that we're grabbing something from the inventory and not a normal treasure
                if (!shakeMap.containsKey(EntityType.PLAYER))
                    shakeMap.put(EntityType.PLAYER, new ArrayList<ShakeTreasure>());
                shakeMap.get(EntityType.PLAYER).add(new ShakeTreasure(new ItemStack(Material.BEDROCK, 1, (byte) 0), 1, getInventoryStealDropChance(), getInventoryStealDropLevel()));
                continue;
            } else {
                material = Material.matchMaterial(materialName);
            }

            int amount = getIntValue(type + "." + treasureName + ".Amount");
            short data = (treasureInfo.length == 2) ? Short.parseShort(treasureInfo[1]) : (short) getIntValue(type + "." + treasureName + ".Data");

            if (material == null) {
                reason.add("Invalid material: " + materialName);
            }

            if (amount <= 0) {
                reason.add("Amount of " + treasureName + " must be greater than 0! " + amount);
            }

            if (material != null && material.isBlock() && (data > 127 || data < -128)) {
                reason.add("Data of " + treasureName + " is invalid! " + data);
            }

            /*
             * XP, Drop Chance, and Drop Level
             */

            int xp = getIntValue(type + "." + treasureName + ".XP");
            double dropChance = getDoubleValue(type + "." + treasureName + ".Drop_Chance");
            int dropLevel = getIntValue(type + "." + treasureName + ".Drop_Level");

            if (xp < 0) {
                reason.add(treasureName + " has an invalid XP value: " + xp);
            }

            if (dropChance < 0.0D) {
                reason.add(treasureName + " has an invalid Drop_Chance: " + dropChance);
            }

            if (dropLevel < 0) {
                reason.add(treasureName + " has an invalid Drop_Level: " + dropLevel);
            }

            /*
             * Specific Types
             */
            Rarity rarity = null;

            if (isFishing) {
                rarity = Rarity.getRarity(getStringValue(type + "." + treasureName + ".Rarity"));

                if (rarity == null) {
                    reason.add("Invalid Rarity for item: " + treasureName);
                }
            }

            /*
             * Itemstack
             */
            ItemStack item = null;

            if (materialName.contains("POTION")) {
                Material mat = Material.matchMaterial(materialName);
                if (mat == null) {
                    reason.add("Potion format for Treasures.yml has changed");
                } else {
                    item = new ItemStack(mat, amount, data);
                    PotionMeta itemMeta = (PotionMeta) item.getItemMeta();

                    PotionType potionType = null;
                    try {
                        potionType = PotionType.valueOf(getStringValue(type + "." + treasureName + ".PotionData.PotionType", "WATER"));
                    } catch (IllegalArgumentException ex) {
                        reason.add("Invalid Potion_Type: " + getStringValue(type + "." + treasureName + ".PotionData.PotionType", "WATER"));
                    }
                    boolean extended = getBooleanValue(type + "." + treasureName + ".PotionData.Extended", false);
                    boolean upgraded = getBooleanValue(type + "." + treasureName + ".PotionData.Upgraded", false);
                    itemMeta.setBasePotionData(new PotionData(potionType, extended, upgraded));

                    if (config.contains(type + "." + treasureName + ".Custom_Name")) {
                        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getStringValue(type + "." + treasureName + ".Custom_Name")));
                    }

                    if (config.contains(type + "." + treasureName + ".Lore")) {
                        List<String> lore = new ArrayList<String>();
                        for (String s : getStringValueList(type + "." + treasureName + ".Lore")) {
                            lore.add(ChatColor.translateAlternateColorCodes('&', s));
                        }
                        itemMeta.setLore(lore);
                    }
                    item.setItemMeta(itemMeta);
                }
            } else if (material != null) {
                item = new ItemStack(material, amount, data);

                if (config.contains(type + "." + treasureName + ".Custom_Name")) {
                    ItemMeta itemMeta = item.getItemMeta();
                    itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getStringValue(type + "." + treasureName + ".Custom_Name")));
                    item.setItemMeta(itemMeta);
                }

                if (config.contains(type + "." + treasureName + ".Lore")) {
                    ItemMeta itemMeta = item.getItemMeta();
                    List<String> lore = new ArrayList<String>();
                    for (String s : getStringValueList(type + "." + treasureName + ".Lore")) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', s));
                    }
                    itemMeta.setLore(lore);
                    item.setItemMeta(itemMeta);
                }
            }

            if (noErrorsInConfig(reason)) {
                if (isFishing) {
                    fishingRewards.get(rarity).add(new FishingTreasure(item, xp));
                } else if (isShake) {
                    ShakeTreasure shakeTreasure = new ShakeTreasure(item, xp, dropChance, dropLevel);

                    EntityType entityType = EntityType.valueOf(type.substring(6));
                    if (!shakeMap.containsKey(entityType))
                        shakeMap.put(entityType, new ArrayList<ShakeTreasure>());
                    shakeMap.get(entityType).add(shakeTreasure);
                } else if (isExcavation) {
                    ExcavationTreasure excavationTreasure = new ExcavationTreasure(item, xp, dropChance, dropLevel);
                    List<String> dropList = getStringValueList(type + "." + treasureName + ".Drops_From");

                    for (String blockType : dropList) {
                        if (!excavationMap.containsKey(blockType))
                            excavationMap.put(blockType, new ArrayList<ExcavationTreasure>());
                        excavationMap.get(blockType).add(excavationTreasure);
                    }
                } else if (isHylian) {
                    HylianTreasure hylianTreasure = new HylianTreasure(item, xp, dropChance, dropLevel);
                    List<String> dropList = getStringValueList(type + "." + treasureName + ".Drops_From");

                    for (String dropper : dropList) {
                        if (dropper.equals("Bushes")) {
                            AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(Material.FERN), hylianTreasure);
                            AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(Material.TALL_GRASS), hylianTreasure);
                            for (Material species : Tag.SAPLINGS.getValues())
                                AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(species), hylianTreasure);

                            AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(Material.DEAD_BUSH), hylianTreasure);
                            continue;
                        }
                        if (dropper.equals("Flowers")) {
                            AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(Material.POPPY), hylianTreasure);
                            AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(Material.DANDELION), hylianTreasure);
                            AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(Material.BLUE_ORCHID), hylianTreasure);
                            AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(Material.ALLIUM), hylianTreasure);
                            AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(Material.AZURE_BLUET), hylianTreasure);
                            AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(Material.ORANGE_TULIP), hylianTreasure);
                            AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(Material.PINK_TULIP), hylianTreasure);
                            AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(Material.RED_TULIP), hylianTreasure);
                            AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(Material.WHITE_TULIP), hylianTreasure);
                            continue;
                        }
                        if (dropper.equals("Pots")) {
                            for (Material species : Tag.FLOWER_POTS.getValues())
                                AddHylianTreasure(StringUtils.getFriendlyConfigMaterialString(species), hylianTreasure);
                            continue;
                        }
                        AddHylianTreasure(dropper, hylianTreasure);
                    }
                }
            }
        }
    }

    private void AddHylianTreasure(String dropper, HylianTreasure treasure) {
        if (!hylianMap.containsKey(dropper))
            hylianMap.put(dropper, new ArrayList<HylianTreasure>());
        hylianMap.get(dropper).add(treasure);
    }

    private void loadEnchantments() {
        for (Rarity rarity : Rarity.values()) {
            if (rarity == Rarity.RECORD) {
                continue;
            }

            if (!fishingEnchantments.containsKey(rarity)) {
                fishingEnchantments.put(rarity, (new ArrayList<EnchantmentTreasure>()));
            }

            ConfigurationSection enchantmentSection = config.getConfigurationSection("Enchantments_Rarity." + rarity.toString());

            if (enchantmentSection == null) {
                return;
            }

            for (String enchantmentName : enchantmentSection.getKeys(false)) {
                int level = getIntValue("Enchantments_Rarity." + rarity.toString() + "." + enchantmentName);
                Enchantment enchantment = EnchantmentUtils.getByName(enchantmentName);

                if (enchantment == null) {
                    plugin.getLogger().warning("Skipping invalid enchantment in treasures.yml: " + enchantmentName);
                    continue;
                }

                fishingEnchantments.get(rarity).add(new EnchantmentTreasure(enchantment, level));
            }
        }
    }

    public boolean getInventoryStealEnabled() {
        return config.contains("Shake.PLAYER.INVENTORY");
    }

    public boolean getInventoryStealStacks() {
        return getBooleanValue("Shake.PLAYER.INVENTORY.Whole_Stacks");
    }

    public double getInventoryStealDropChance() {
        return getDoubleValue("Shake.PLAYER.INVENTORY.Drop_Chance");
    }

    public int getInventoryStealDropLevel() {
        return getIntValue("Shake.PLAYER.INVENTORY.Drop_Level");
    }

    public double getItemDropRate(int tier, Rarity rarity) {
        return getDoubleValue("Item_Drop_Rates.Tier_" + tier + "." + rarity.toString());
    }

    public double getEnchantmentDropRate(int tier, Rarity rarity) {
        return getDoubleValue("Enchantment_Drop_Rates.Tier_" + tier + "." + rarity.toString());
    }
}
