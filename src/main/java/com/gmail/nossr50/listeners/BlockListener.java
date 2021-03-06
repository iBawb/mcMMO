package com.gmail.nossr50.listeners;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.config.AdvancedConfig;
import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.config.HiddenConfig;
import com.gmail.nossr50.datatypes.McMMOPlayer;
import com.gmail.nossr50.datatypes.PlayerProfile;
import com.gmail.nossr50.events.fake.FakeBlockBreakEvent;
import com.gmail.nossr50.events.fake.FakeBlockDamageEvent;
import com.gmail.nossr50.events.fake.FakePlayerAnimationEvent;
import com.gmail.nossr50.runnables.StickyPistonTracker;
import com.gmail.nossr50.skills.excavation.Excavation;
import com.gmail.nossr50.skills.herbalism.Herbalism;
import com.gmail.nossr50.skills.mining.MiningManager;
import com.gmail.nossr50.skills.repair.Repair;
import com.gmail.nossr50.skills.repair.Salvage;
import com.gmail.nossr50.skills.smelting.SmeltingManager;
import com.gmail.nossr50.skills.unarmed.Unarmed;
import com.gmail.nossr50.skills.utilities.AbilityType;
import com.gmail.nossr50.skills.utilities.SkillTools;
import com.gmail.nossr50.skills.utilities.SkillType;
import com.gmail.nossr50.skills.utilities.ToolType;
import com.gmail.nossr50.skills.woodcutting.Woodcutting;
import com.gmail.nossr50.util.BlockChecks;
import com.gmail.nossr50.util.ItemChecks;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.Users;

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
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        List<Block> blocks = event.getBlocks();
        BlockFace direction = event.getDirection();
        Block futureEmptyBlock = event.getBlock().getRelative(direction); // Block that would be air after piston is finished

        for (Block b : blocks) {
            if (mcMMO.placeStore.isTrue(b)) {
                b.getRelative(direction).setMetadata("pistonTrack", new FixedMetadataValue(plugin, true));
                if (b.equals(futureEmptyBlock)) {
                    mcMMO.placeStore.setFalse(b);
                }
            }
        }

        for (Block b : blocks) {
            if (b.getRelative(direction).hasMetadata("pistonTrack")) {
                mcMMO.placeStore.setTrue(b.getRelative(direction));
                b.getRelative(direction).removeMetadata("pistonTrack", plugin);
            }
        }
    }

    /**
     * Monitor BlockPistonRetract events.
     *
     * @param event The event to watch
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (event.isSticky()) {
            //Needed only because under some circumstances Minecraft doesn't move the block
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new StickyPistonTracker(event), 2);
        }
    }

    /**
     * Monitor BlockPlace events.
     *
     * @param event The event to watch
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (Misc.isNPCPlayer(player)) {
            return;
        }

        Block block = event.getBlock();

        /* Check if the blocks placed should be monitored so they do not give out XP in the future */
        if (BlockChecks.shouldBeWatched(block)) {
            mcMMO.placeStore.setTrue(block);
        }

        if (Repair.anvilMessagesEnabled) {
            int blockID = block.getTypeId();

            if (blockID == Repair.anvilID) {
                Repair.placedAnvilCheck(player, blockID);
            }
            else if (blockID == Salvage.anvilID) {
                Salvage.placedAnvilCheck(player, blockID);
            }
        }
    }

    /**
     * Monitor BlockBreak events.
     *
     * @param event The event to monitor
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event instanceof FakeBlockBreakEvent) {
            return;
        }

        Player player = event.getPlayer();

        if (Misc.isNPCPlayer(player)) {
            return;
        }

        McMMOPlayer mcMMOPlayer = Users.getPlayer(player);
        PlayerProfile profile = mcMMOPlayer.getProfile();
        Block block = event.getBlock();
        ItemStack heldItem = player.getItemInHand();

        /* HERBALISM */
        if (BlockChecks.canBeGreenTerra(block)) {
            /* Green Terra */
            if (profile.getToolPreparationMode(ToolType.HOE) && player.hasPermission("mcmmo.ability.herbalism.greenterra")) {
                SkillTools.abilityCheck(player, SkillType.HERBALISM);
            }

            /*
             * We don't check the block store here because herbalism has too many unusual edge cases.
             * Instead, we check it inside the drops handler.
             */
            if (player.hasPermission("mcmmo.skills.herbalism")) {
                Herbalism.herbalismProcCheck(block, mcMMOPlayer, plugin); //Double drops

                if (profile.getAbilityMode(AbilityType.GREEN_TERRA)) {
                    Herbalism.herbalismProcCheck(block, mcMMOPlayer, plugin); //Triple drops
                }
            }
        }

        /* MINING */
        else if (BlockChecks.canBeSuperBroken(block) && ItemChecks.isPickaxe(heldItem) && player.hasPermission("mcmmo.skills.mining") && !mcMMO.placeStore.isTrue(block)) {
            MiningManager miningManager = new MiningManager(mcMMOPlayer);
            miningManager.miningBlockCheck(block);

            if (profile.getAbilityMode(AbilityType.SUPER_BREAKER)) {
                miningManager.miningBlockCheck(block);
            }
        }

        /* WOOD CUTTING */
        else if (BlockChecks.isLog(block) && player.hasPermission("mcmmo.skills.woodcutting") && !mcMMO.placeStore.isTrue(block)) {
            if (profile.getAbilityMode(AbilityType.TREE_FELLER) && player.hasPermission("mcmmo.ability.woodcutting.treefeller") && ItemChecks.isAxe(heldItem)) {
                Woodcutting.beginTreeFeller(mcMMOPlayer, block);
            }
            else {
                if (Config.getInstance().getWoodcuttingRequiresTool()) {
                    if (ItemChecks.isAxe(heldItem)) {
                        Woodcutting.beginWoodcutting(mcMMOPlayer, block);
                    }
                }
                else {
                    Woodcutting.beginWoodcutting(mcMMOPlayer, block);
                }
            }
        }

        /* EXCAVATION */
        else if (BlockChecks.canBeGigaDrillBroken(block) && ItemChecks.isShovel(heldItem) && player.hasPermission("mcmmo.skills.excavation") && !mcMMO.placeStore.isTrue(block)) {
            Excavation.excavationProcCheck(block, mcMMOPlayer);

            if (profile.getAbilityMode(AbilityType.GIGA_DRILL_BREAKER)) {
                Excavation.gigaDrillBreaker(mcMMOPlayer, block);
            }
        }

        /* Remove metadata from placed watched blocks */
        if (BlockChecks.shouldBeWatched(block) && mcMMO.placeStore.isTrue(block)) {
            mcMMO.placeStore.setFalse(block);
        }
    }

    /**
     * Handle BlockBreak events where the event is modified.
     *
     * @param event The event to modify
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakHigher(BlockBreakEvent event) {
        if (event instanceof FakeBlockBreakEvent) {
            return;
        }

        Player player = event.getPlayer();

        if (Misc.isNPCPlayer(player)) {
            return;
        }

        Block block = event.getBlock();
        ItemStack heldItem = player.getItemInHand();

        if (player.hasPermission("mcmmo.ability.herbalism.hylianluck") && ItemChecks.isSword(heldItem)) {
            Herbalism.hylianLuck(block, player, event);
        }
        else if (BlockChecks.canBeFluxMined(block) && ItemChecks.isPickaxe(heldItem) && !heldItem.containsEnchantment(Enchantment.SILK_TOUCH) && player.hasPermission("mcmmo.ability.smelting.fluxmining") && !mcMMO.placeStore.isTrue(block)) {
            SmeltingManager smeltingManager = new SmeltingManager(Users.getPlayer(player));
            smeltingManager.fluxMining(event);
        }
    }

    /**
     * Monitor BlockDamage events.
     *
     * @param event The event to watch
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (event instanceof FakeBlockDamageEvent) {
            return;
        }

        Player player = event.getPlayer();

        if (Misc.isNPCPlayer(player)) {
            return;
        }

        PlayerProfile profile = Users.getPlayer(player).getProfile();
        Block block = event.getBlock();

        /*
         * ABILITY PREPARATION CHECKS
         *
         * We check permissions here before processing activation.
         */
        if (BlockChecks.canActivateAbilities(block)) {
            ItemStack heldItem = player.getItemInHand();

            if (HiddenConfig.getInstance().useEnchantmentBuffs()) {
                if ((ItemChecks.isPickaxe(heldItem) && !profile.getAbilityMode(AbilityType.SUPER_BREAKER)) || (ItemChecks.isShovel(heldItem) && !profile.getAbilityMode(AbilityType.GIGA_DRILL_BREAKER))) {
                    SkillTools.removeAbilityBuff(heldItem);
                }
            }
            else {
                if ((profile.getAbilityMode(AbilityType.SUPER_BREAKER) && !BlockChecks.canBeSuperBroken(block)) || (profile.getAbilityMode(AbilityType.GIGA_DRILL_BREAKER) && !BlockChecks.canBeGigaDrillBroken(block))) {
                    SkillTools.handleAbilitySpeedDecrease(player);
                }
            }

            if (profile.getToolPreparationMode(ToolType.HOE) && ItemChecks.isHoe(heldItem) && (BlockChecks.canBeGreenTerra(block) || BlockChecks.canMakeMossy(block)) && player.hasPermission("mcmmo.ability.herbalism.greenterra")) {
                SkillTools.abilityCheck(player, SkillType.HERBALISM);
            }
            else if (profile.getToolPreparationMode(ToolType.AXE) && ItemChecks.isAxe(heldItem) && BlockChecks.isLog(block) && player.hasPermission("mcmmo.ability.woodcutting.treefeller")) {
                SkillTools.abilityCheck(player, SkillType.WOODCUTTING);
            }
            else if (profile.getToolPreparationMode(ToolType.PICKAXE) && ItemChecks.isPickaxe(heldItem) && BlockChecks.canBeSuperBroken(block) && player.hasPermission("mcmmo.ability.mining.superbreaker")) {
                SkillTools.abilityCheck(player, SkillType.MINING);
            }
            else if (profile.getToolPreparationMode(ToolType.SHOVEL) && ItemChecks.isShovel(heldItem) && BlockChecks.canBeGigaDrillBroken(block) && player.hasPermission("mcmmo.ability.excavation.gigadrillbreaker")) {
                SkillTools.abilityCheck(player, SkillType.EXCAVATION);
            }
            else if (profile.getToolPreparationMode(ToolType.FISTS) && heldItem.getType() == Material.AIR && (BlockChecks.canBeGigaDrillBroken(block) || block.getType() == Material.SNOW || (block.getType() == Material.SMOOTH_BRICK && block.getData() == 0x0)) && player.hasPermission("mcmmo.ability.unarmed.berserk")) {
                SkillTools.abilityCheck(player, SkillType.UNARMED);
            }
        }

        /*
         * TREE FELLER SOUNDS
         *
         * We don't need to check permissions here because they've already been checked for the ability to even activate.
         */
        if (profile.getAbilityMode(AbilityType.TREE_FELLER) && BlockChecks.isLog(block)) {
            player.playSound(block.getLocation(), Sound.FIZZ, Misc.FIZZ_VOLUME, Misc.FIZZ_PITCH);
        }
    }

    /**
     * Handle BlockDamage events where the event is modified.
     *
     * @param event The event to modify
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamageHigher(BlockDamageEvent event) {
        if (event instanceof FakeBlockDamageEvent) {
            return;
        }

        Player player = event.getPlayer();

        if (Misc.isNPCPlayer(player)) {
            return;
        }

        McMMOPlayer mcMMOPlayer = Users.getPlayer(player);
        PlayerProfile profile = mcMMOPlayer.getProfile();
        ItemStack heldItem = player.getItemInHand();
        Block block = event.getBlock();

        /*
         * ABILITY TRIGGER CHECKS
         *
         * We don't need to check permissions here because they've already been checked for the ability to even activate.
         */
        // Except right here, which is for the Green Thumb activation, not the normal effect of Green Terra
        if (profile.getAbilityMode(AbilityType.GREEN_TERRA) && BlockChecks.canMakeMossy(block) && player.hasPermission("mcmmo.ability.herbalism.greenthumbblocks")) {
            Herbalism.greenTerra(player, block);
        }
        else if (profile.getAbilityMode(AbilityType.BERSERK)) {
            if (SkillTools.triggerCheck(player, block, AbilityType.BERSERK)) {
                if (heldItem.getType() == Material.AIR) {
                    FakePlayerAnimationEvent armswing = new FakePlayerAnimationEvent(player);
                    plugin.getServer().getPluginManager().callEvent(armswing);

                    event.setInstaBreak(true);
                    player.playSound(block.getLocation(), Sound.ITEM_PICKUP, Misc.POP_VOLUME, Misc.POP_PITCH);
                }
            }
            // Another perm check for the cracked blocks activation
            else if (BlockChecks.canBeCracked(block) && player.hasPermission("mcmmo.ability.unarmed.blockcracker")) {
                Unarmed.blockCracker(player, block);
            }
        }
        else if ((profile.getSkillLevel(SkillType.WOODCUTTING) >= AdvancedConfig.getInstance().getLeafBlowUnlockLevel()) && BlockChecks.isLeaves(block)) {
            if (SkillTools.triggerCheck(player, block, AbilityType.LEAF_BLOWER)) {
                if (Config.getInstance().getWoodcuttingRequiresTool()) {
                    if (ItemChecks.isAxe(heldItem)) {
                        event.setInstaBreak(true);
                        Woodcutting.beginLeafBlower(player, block);
                    }
                }
                else if (!(heldItem.getType() == Material.SHEARS)) {
                    event.setInstaBreak(true);
                    Woodcutting.beginLeafBlower(player, block);
                }
            }
        }
    }
}
