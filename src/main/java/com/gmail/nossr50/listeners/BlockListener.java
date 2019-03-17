package com.gmail.nossr50.listeners;

import com.gmail.nossr50.config.MainConfig;
import com.gmail.nossr50.config.WorldBlacklist;
import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.datatypes.skills.ToolType;
import com.gmail.nossr50.events.fake.FakeBlockBreakEvent;
import com.gmail.nossr50.events.fake.FakeBlockDamageEvent;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.skills.alchemy.Alchemy;
import com.gmail.nossr50.skills.excavation.ExcavationManager;
import com.gmail.nossr50.skills.herbalism.Herbalism;
import com.gmail.nossr50.skills.herbalism.HerbalismManager;
import com.gmail.nossr50.skills.mining.MiningManager;
import com.gmail.nossr50.skills.repair.Repair;
import com.gmail.nossr50.skills.salvage.Salvage;
import com.gmail.nossr50.skills.woodcutting.WoodcuttingManager;
import com.gmail.nossr50.util.BlockUtils;
import com.gmail.nossr50.util.EventUtils;
import com.gmail.nossr50.util.ItemUtils;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.skills.SkillUtils;
import com.gmail.nossr50.util.sounds.SoundManager;
import com.gmail.nossr50.util.sounds.SoundType;
import com.gmail.nossr50.worldguard.WorldGuardManager;
import com.gmail.nossr50.worldguard.WorldGuardUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class BlockListener implements Listener {
    private final mcMMO plugin;

    public BlockListener(final mcMMO plugin) {
        this.plugin = plugin;
    }

    /**
     * Monitor BlockPistonExtend events.
     *
     * @param event The event to monitor
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        /* WORLD BLACKLIST CHECK */
        if(WorldBlacklist.isWorldBlacklisted(event.getBlock().getWorld()))
            return;

        BlockFace direction = event.getDirection();
        Block movedBlock = event.getBlock();
        movedBlock = movedBlock.getRelative(direction, 2);

        for (Block b : event.getBlocks()) {
            if (BlockUtils.shouldBeWatched(b.getState())) {
                movedBlock = b.getRelative(direction);

                if(mcMMO.getConfigManager().getConfigExploitPrevention().doPistonsMarkBlocksUnnatural())
                    mcMMO.getPlaceStore().setTrue(movedBlock);
            }
        }
    }

    /**
     * Monitor BlockPistonRetract events.
     *
     * @param event The event to watch
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        /* WORLD BLACKLIST CHECK */
        if(WorldBlacklist.isWorldBlacklisted(event.getBlock().getWorld()))
            return;

        // Get opposite direction so we get correct block
        BlockFace direction = event.getDirection();
        Block movedBlock = event.getBlock().getRelative(direction);
        mcMMO.getPlaceStore().setTrue(movedBlock);

        for (Block block : event.getBlocks()) {
            movedBlock = block.getRelative(direction);
            mcMMO.getPlaceStore().setTrue(movedBlock);
        }
    }

    /**
     * Monitor blocks formed by entities (snowmen)
     *
     * @param event The event to watch
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBlockFormEvent(EntityBlockFormEvent event)
    {
        /* WORLD BLACKLIST CHECK */
        if(WorldBlacklist.isWorldBlacklisted(event.getBlock().getWorld()))
            return;

        if(BlockUtils.shouldBeWatched(event.getBlock().getState()))
        {
            mcMMO.getPlaceStore().setTrue(event.getBlock());
        }
    }

    /**
     * Monitor falling blocks.
     *
     * @param event The event to watch
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallingBlock(EntityChangeBlockEvent event) {
        /* WORLD BLACKLIST CHECK */
        if(WorldBlacklist.isWorldBlacklisted(event.getBlock().getWorld()))
            return;

        if (BlockUtils.shouldBeWatched(event.getBlock().getState()) && event.getEntityType().equals(EntityType.FALLING_BLOCK)) {
            if (event.getTo().equals(Material.AIR) && mcMMO.getPlaceStore().isTrue(event.getBlock())) {
                event.getEntity().setMetadata("mcMMOBlockFall", new FixedMetadataValue( plugin, event.getBlock().getLocation()));
            } else {
                List<MetadataValue> values = event.getEntity().getMetadata( "mcMMOBlockFall" );

                if (!values.isEmpty()) {

                    if (values.get(0).value() == null) return;
                    Block spawn = ((org.bukkit.Location) values.get(0).value()).getBlock();


                    mcMMO.getPlaceStore().setTrue( event.getBlock() );
                    mcMMO.getPlaceStore().setFalse( spawn );

                }
            }
        }
    }

    /**
     * Monitor BlockPlace events.
     *
     * @param event The event to watch
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        /* WORLD BLACKLIST CHECK */
        if(WorldBlacklist.isWorldBlacklisted(event.getBlock().getWorld()))
            return;

        Player player = event.getPlayer();

        if (!UserManager.hasPlayerDataKey(player)) {
            return;
        }

        BlockState blockState = event.getBlock().getState();

        /* Check if the blocks placed should be monitored so they do not give out XP in the future */
        if (BlockUtils.shouldBeWatched(blockState)) {
            // Don't count de-barking wood
            if (!Tag.LOGS.isTagged(event.getBlockReplacedState().getType()) || !Tag.LOGS.isTagged(event.getBlockPlaced().getType()))
                mcMMO.getPlaceStore().setTrue(blockState);
        }

        McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);

        if (blockState.getType() == Repair.anvilMaterial && PrimarySkillType.REPAIR.getPermissions(player)) {
            mcMMOPlayer.getRepairManager().placedAnvilCheck();
        }
        else if (blockState.getType() == Salvage.anvilMaterial && PrimarySkillType.SALVAGE.getPermissions(player)) {
            mcMMOPlayer.getSalvageManager().placedAnvilCheck();
        }
    }

    /**
     * Monitor BlockMultiPlace events.
     *
     * @param event The event to watch
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        /* WORLD BLACKLIST CHECK */
        if(WorldBlacklist.isWorldBlacklisted(event.getBlock().getWorld()))
            return;

        Player player = event.getPlayer();

        if (!UserManager.hasPlayerDataKey(player)) {
            return;
        }

        for (BlockState replacedBlockState : event.getReplacedBlockStates())
        {
            BlockState blockState = replacedBlockState.getBlock().getState();

            /* Check if the blocks placed should be monitored so they do not give out XP in the future */
            if (BlockUtils.shouldBeWatched(blockState)) {
                mcMMO.getPlaceStore().setTrue(blockState);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event)
    {
        /* WORLD BLACKLIST CHECK */
        if(WorldBlacklist.isWorldBlacklisted(event.getBlock().getWorld()))
            return;

        BlockState blockState = event.getBlock().getState();

        if (!BlockUtils.shouldBeWatched(blockState)) {
            return;
        }

        mcMMO.getPlaceStore().setFalse(blockState);
    }

    /**
     * Monitor BlockBreak events.
     *
     * @param event The event to monitor
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        /* WORLD BLACKLIST CHECK */
        if(WorldBlacklist.isWorldBlacklisted(event.getBlock().getWorld()))
            return;

        /* WORLD GUARD MAIN FLAG CHECK */
        if(WorldGuardUtils.isWorldGuardLoaded())
        {
            if(!WorldGuardManager.getInstance().hasMainFlag(event.getPlayer()))
                return;
        }

        if (event instanceof FakeBlockBreakEvent) {
            return;
        }

        BlockState blockState = event.getBlock().getState();
        Location location = blockState.getLocation();

        if (!BlockUtils.shouldBeWatched(blockState)) {
            return;
        }

        /* ALCHEMY - Cancel any brew in progress for that BrewingStand */
        if (blockState instanceof BrewingStand && Alchemy.brewingStandMap.containsKey(location)) {
            Alchemy.brewingStandMap.get(location).cancelBrew();
        }

        Player player = event.getPlayer();

        if (!UserManager.hasPlayerDataKey(player) || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        /* HERBALISM */
        if (BlockUtils.affectedByGreenTerra(blockState)) {
            HerbalismManager herbalismManager = mcMMOPlayer.getHerbalismManager();

            /* Green Terra */
            if (herbalismManager.canActivateAbility()) {
                mcMMOPlayer.checkAbilityActivation(PrimarySkillType.HERBALISM);
            }

            /*
             * We don't check the block store here because herbalism has too many unusual edge cases.
             * Instead, we check it inside the drops handler.
             */
            if (PrimarySkillType.HERBALISM.getPermissions(player)) {
                herbalismManager.herbalismBlockCheck(blockState);
            }
        }

        /* MINING */
        else if (BlockUtils.affectedBySuperBreaker(blockState) && ItemUtils.isPickaxe(heldItem) && PrimarySkillType.MINING.getPermissions(player) && !mcMMO.getPlaceStore().isTrue(blockState)) {
            MiningManager miningManager = mcMMOPlayer.getMiningManager();
            miningManager.miningBlockCheck(blockState);
        }

        /* WOOD CUTTING */
        else if (BlockUtils.isLog(blockState) && ItemUtils.isAxe(heldItem) && PrimarySkillType.WOODCUTTING.getPermissions(player) && !mcMMO.getPlaceStore().isTrue(blockState)) {
            WoodcuttingManager woodcuttingManager = mcMMOPlayer.getWoodcuttingManager();
            if (woodcuttingManager.canUseTreeFeller(heldItem)) {
                woodcuttingManager.processTreeFeller(blockState);
            }
            else {
                woodcuttingManager.woodcuttingBlockCheck(blockState);
            }
        }

        /* EXCAVATION */
        else if (BlockUtils.affectedByGigaDrillBreaker(blockState) && ItemUtils.isShovel(heldItem) && PrimarySkillType.EXCAVATION.getPermissions(player) && !mcMMO.getPlaceStore().isTrue(blockState)) {
            ExcavationManager excavationManager = mcMMOPlayer.getExcavationManager();
            excavationManager.excavationBlockCheck(blockState);

            if (mcMMOPlayer.getAbilityMode(SuperAbilityType.GIGA_DRILL_BREAKER)) {
                excavationManager.gigaDrillBreaker(blockState);
            }
        }

        /* Remove metadata from placed watched blocks */
        mcMMO.getPlaceStore().setFalse(blockState);
    }

    /**
     * Handle BlockBreak events where the event is modified.
     *
     * @param event The event to modify
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakHigher(BlockBreakEvent event) {
        /* WORLD BLACKLIST CHECK */
        if(WorldBlacklist.isWorldBlacklisted(event.getBlock().getWorld()))
            return;

        /* WORLD GUARD MAIN FLAG CHECK */
        if(WorldGuardUtils.isWorldGuardLoaded())
        {
            if(!WorldGuardManager.getInstance().hasMainFlag(event.getPlayer()))
                return;
        }

        if (event instanceof FakeBlockBreakEvent) {
            return;
        }

        Player player = event.getPlayer();

        if (!UserManager.hasPlayerDataKey(player) || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        BlockState blockState = event.getBlock().getState();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (Herbalism.isRecentlyRegrown(blockState)) {
            event.setCancelled(true);
            return;
        }

        if (ItemUtils.isSword(heldItem)) {
            HerbalismManager herbalismManager = UserManager.getPlayer(player).getHerbalismManager();

            if (herbalismManager.canUseHylianLuck()) {
                if (herbalismManager.processHylianLuck(blockState)) {
                    blockState.update(true);
                    event.setCancelled(true);
                }
                else if (blockState.getType() == Material.FLOWER_POT) {
                    blockState.setType(Material.AIR);
                    blockState.update(true);
                    event.setCancelled(true);
                }
            }
        }
        /*else if (!heldItem.containsEnchantment(Enchantment.SILK_TOUCH)) {
            SmeltingManager smeltingManager = UserManager.getPlayer(player).getSmeltingManager();

            if (smeltingManager.canUseFluxMining(blockState)) {
                if (smeltingManager.processFluxMining(blockState)) {
                    blockState.update(true);
                    event.setCancelled(true);
                }
            }
        }*/
    }

    /**
     * Monitor BlockDamage events.
     *
     * @param event The event to watch
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        BlockState blockState = event.getBlock().getState();

        /* WORLD BLACKLIST CHECK */
        if(WorldBlacklist.isWorldBlacklisted(event.getBlock().getWorld()))
            return;

        /* WORLD GUARD MAIN FLAG CHECK */
        if(WorldGuardUtils.isWorldGuardLoaded())
        {
            if(!WorldGuardManager.getInstance().hasMainFlag(event.getPlayer()))
                return;
        }

        if (event instanceof FakeBlockDamageEvent) {
            return;
        }
        if (!UserManager.hasPlayerDataKey(player)) {
            return;
        }

        McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);

        /*
         * ABILITY PREPARATION CHECKS
         *
         * We check permissions here before processing activation.
         */
        if (BlockUtils.canActivateAbilities(blockState)) {
            ItemStack heldItem = player.getInventory().getItemInMainHand();

            if (mcMMOPlayer.getToolPreparationMode(ToolType.HOE) && ItemUtils.isHoe(heldItem) && (BlockUtils.affectedByGreenTerra(blockState) || BlockUtils.canMakeMossy(blockState)) && Permissions.greenTerra(player)) {
                mcMMOPlayer.checkAbilityActivation(PrimarySkillType.HERBALISM);
            }
            else if (mcMMOPlayer.getToolPreparationMode(ToolType.AXE) && ItemUtils.isAxe(heldItem) && BlockUtils.isLog(blockState) && Permissions.treeFeller(player)) {
                mcMMOPlayer.checkAbilityActivation(PrimarySkillType.WOODCUTTING);
            }
            else if (mcMMOPlayer.getToolPreparationMode(ToolType.PICKAXE) && ItemUtils.isPickaxe(heldItem) && BlockUtils.affectedBySuperBreaker(blockState) && Permissions.superBreaker(player)) {
                mcMMOPlayer.checkAbilityActivation(PrimarySkillType.MINING);
            }
            else if (mcMMOPlayer.getToolPreparationMode(ToolType.SHOVEL) && ItemUtils.isShovel(heldItem) && BlockUtils.affectedByGigaDrillBreaker(blockState) && Permissions.gigaDrillBreaker(player)) {
                mcMMOPlayer.checkAbilityActivation(PrimarySkillType.EXCAVATION);
            }
            else if (mcMMOPlayer.getToolPreparationMode(ToolType.FISTS) && heldItem.getType() == Material.AIR && (BlockUtils.affectedByGigaDrillBreaker(blockState) || blockState.getType() == Material.SNOW || BlockUtils.affectedByBlockCracker(blockState) && Permissions.berserk(player))) {
                mcMMOPlayer.checkAbilityActivation(PrimarySkillType.UNARMED);
            }
        }

        /*
         * TREE FELLER SOUNDS
         *
         * We don't need to check permissions here because they've already been checked for the ability to even activate.
         */
        if (mcMMOPlayer.getAbilityMode(SuperAbilityType.TREE_FELLER) && BlockUtils.isLog(blockState) && MainConfig.getInstance().getTreeFellerSoundsEnabled()) {
            SoundManager.sendSound(player, blockState.getLocation(), SoundType.FIZZ);
        }
    }

    private Player getPlayerFromFurnace(Block furnaceBlock) {
        List<MetadataValue> metadata = furnaceBlock.getMetadata(mcMMO.furnaceMetadataKey);

        if (metadata.isEmpty()) {
            return null;
        }

        return plugin.getServer().getPlayerExact(metadata.get(0).asString());
    }

    /**
     * Handle BlockDamage events where the event is modified.
     *
     * @param event The event to modify
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamageHigher(BlockDamageEvent event) {
        /* WORLD BLACKLIST CHECK */
        if(WorldBlacklist.isWorldBlacklisted(event.getBlock().getWorld()))
            return;

        /* WORLD GUARD MAIN FLAG CHECK */
        if(WorldGuardUtils.isWorldGuardLoaded())
        {
            if(!WorldGuardManager.getInstance().hasMainFlag(event.getPlayer()))
                return;
        }

        if (event instanceof FakeBlockDamageEvent) {
            return;
        }

        Player player = event.getPlayer();

        if (!UserManager.hasPlayerDataKey(player)) {
            return;
        }

        McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        Block block = event.getBlock();
        BlockState blockState = block.getState();

        /*
         * ABILITY TRIGGER CHECKS
         *
         * We don't need to check permissions here because they've already been checked for the ability to even activate.
         */
        if (mcMMOPlayer.getAbilityMode(SuperAbilityType.GREEN_TERRA) && BlockUtils.canMakeMossy(blockState)) {
            if (mcMMOPlayer.getHerbalismManager().processGreenTerra(blockState)) {
                blockState.update(true);
            }
        }
        else if (mcMMOPlayer.getAbilityMode(SuperAbilityType.BERSERK) && heldItem.getType() == Material.AIR) {
            if (SuperAbilityType.BERSERK.blockCheck(block.getState()) && EventUtils.simulateBlockBreak(block, player, true)) {
                event.setInstaBreak(true);
                SoundManager.sendSound(player, block.getLocation(), SoundType.POP);
            }
            else if (mcMMOPlayer.getUnarmedManager().canUseBlockCracker() && BlockUtils.affectedByBlockCracker(blockState) && EventUtils.simulateBlockBreak(block, player, true)) {
                if (mcMMOPlayer.getUnarmedManager().blockCrackerCheck(blockState)) {
                    blockState.update();
                }
            }
        }
        else if (mcMMOPlayer.getWoodcuttingManager().canUseLeafBlower(heldItem) && BlockUtils.isLeaves(blockState) && EventUtils.simulateBlockBreak(block, player, true)) {
            event.setInstaBreak(true);
            SoundManager.sendSound(player, block.getLocation(), SoundType.POP);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockDamageCleanup(BlockDamageEvent event) {
        Player player = event.getPlayer();
        McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);
        BlockState blockState = event.getBlock().getState();

        ItemStack heldItem = player.getInventory().getItemInMainHand();

        cleanupAbilityTools(player, mcMMOPlayer, blockState, heldItem);

        debugStickDump(player, blockState);
    }

    public void debugStickDump(Player player, BlockState blockState) {
        if(player.getInventory().getItemInMainHand().getType() == Material.DEBUG_STICK)
        {
            if(mcMMO.getPlaceStore().isTrue(blockState))
                player.sendMessage("[mcMMO DEBUG] This block is not natural and does not reward treasures/XP");
            else
            {
                player.sendMessage("[mcMMO DEBUG] This block is considered natural by mcMMO");
                UserManager.getPlayer(player).getExcavationManager().printExcavationDebug(player, blockState);
            }

            if(WorldGuardUtils.isWorldGuardLoaded())
            {
                if(WorldGuardManager.getInstance().hasMainFlag(player))
                    player.sendMessage("[mcMMO DEBUG] World Guard main flag is permitted for this player in this region");
                else
                    player.sendMessage("[mcMMO DEBUG] World Guard main flag is DENIED for this player in this region");

                if(WorldGuardManager.getInstance().hasXPFlag(player))
                    player.sendMessage("[mcMMO DEBUG] World Guard xp flag is permitted for this player in this region");
                else
                    player.sendMessage("[mcMMO DEBUG] World Guard xp flag is not permitted for this player in this region");
            }

            if(blockState instanceof Furnace)
            {
                Furnace furnace = (Furnace) blockState;
                if(furnace.hasMetadata(mcMMO.furnaceMetadataKey))
                {
                    player.sendMessage("[mcMMO DEBUG] This furnace has a registered owner");
                    Player furnacePlayer = getPlayerFromFurnace(furnace.getBlock());
                    if(furnacePlayer != null)
                    {
                        player.sendMessage("[mcMMO DEBUG] This furnace is owned by player "+furnacePlayer.getName());
                    }
                }
                else
                    player.sendMessage("[mcMMO DEBUG] This furnace does not have a registered owner");
            }

            if(ExperienceConfig.getInstance().isExperienceBarsEnabled())
                player.sendMessage("[mcMMO DEBUG] XP bars are enabled, however you should check per-skill settings to make sure those are enabled.");
        }
    }

    public void cleanupAbilityTools(Player player, McMMOPlayer mcMMOPlayer, BlockState blockState, ItemStack heldItem) {
        SkillUtils.removeAbilityBuff(heldItem);
        SkillUtils.handleAbilitySpeedDecrease(player);
    }

}
