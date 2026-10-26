package legacyconnector.v1_7_10.net.minecraftforge.common;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandom;

/**
 * Forge 1.7.10's static hook collection. Only the state mods reach into directly lives here (the seed list tall grass
 * drops from); behaviour hooks the engine drives are not part of the shim.
 */
public class ForgeHooks {
    /** The armour points a player wears (the value the armour bar shows). */
    public static int getTotalArmorValue(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        return player.getTotalArmorValue();
    }

    /** Seeds tall grass can drop (mods add their own through {@code MinecraftForge.addGrassSeed}). */
    static final List<SeedEntry> seedList = new ArrayList<>();

    public static class SeedEntry extends WeightedRandom.Item {
        public final ItemStack seed;

        public SeedEntry(ItemStack seed, int weight) {
            super(weight);
            this.seed = seed;
        }

        public ItemStack getStack(java.util.Random rand) {
            return seed.copy();
        }
    }

    /** A seed tall grass drops: wheat seeds (weight 10, Forge's default entry) or one a mod added, picked by weight. */
    public static ItemStack getGrassSeed(legacyconnector.v1_7_10.net.minecraft.world.World world) {
        List<SeedEntry> entries = new ArrayList<>(seedList.size() + 1);
        entries.add(new SeedEntry(new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.wheat_seeds), 10));
        entries.addAll(seedList);
        java.util.Random random = world == null ? new java.util.Random() : world.rand;
        SeedEntry entry = (SeedEntry) WeightedRandom.getRandomItem(random, entries);
        return entry == null || entry.seed == null ? null : entry.getStack(random);
    }

    /** Whether the tool in {@code stack} is the kind that breaks {@code block} (with this metadata) quickly. */
    public static boolean isToolEffective(ItemStack stack, legacyconnector.v1_7_10.net.minecraft.block.Block block, int metadata) {
        if (stack == null || stack.getItem() == null) return false;
        for (String toolClass : stack.getItem().getToolClasses(stack)) {
            if (block.isToolEffective(toolClass, metadata)) return true;
        }
        return false;
    }

    /** As Forge: a block that needs no tool is harvestable by hand; otherwise the held tool's harvest level must reach the block's. */
    public static boolean canHarvestBlock(legacyconnector.v1_7_10.net.minecraft.block.Block block,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, int metadata) {
        if (block.getMaterial().isToolNotRequired()) return true;
        ItemStack stack = player.inventory.getCurrentItem();
        String tool = block.getHarvestTool(metadata);
        if (stack == null || tool == null) return player.canHarvestBlock(block);
        int toolLevel = stack.getItem().getHarvestLevel(stack, tool);
        if (toolLevel < 0) return player.canHarvestBlock(block);
        return toolLevel >= block.getHarvestLevel(metadata);
    }

    /** How fast the player breaks the block: 0 for unbreakable blocks, slower by 3x when the tool cannot harvest it. */
    public static float blockStrength(legacyconnector.v1_7_10.net.minecraft.block.Block block,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        float hardness = block.getBlockHardness(world, x, y, z);
        if (hardness < 0.0F) return 0.0F;
        if (!canHarvestBlock(block, player, metadata)) return player.getBreakSpeed(block, true, metadata, x, y, z) / hardness / 100.0F;
        return player.getBreakSpeed(block, false, metadata, x, y, z) / hardness / 30.0F;
    }

    public static float onLivingFall(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, float distance) {
        return distance;
    }

    public static void onLivingJump(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity) {
    }

    public static float getEnchantPower(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return world.getBlock(x, y, z).getEnchantPowerBonus(world, x, y, z);
    }

    public static boolean canToolHarvestBlock(legacyconnector.v1_7_10.net.minecraft.block.Block block, int metadata, ItemStack stack) {
        if (block.getMaterial().isToolNotRequired()) return true;
        return stack != null && stack.getItem() != null && stack.getItem().canHarvestBlock(block, stack);
    }

    /**
     * The legacy break event {@link #onBlockBreakEvent} is firing; the bridge's listener for the modern break event posts
     * this object to the legacy bus instead of a new one, so the caller gets back what the legacy handlers changed.
     */
    private static final ThreadLocal<legacyconnector.v1_7_10.net.minecraftforge.event.world.BlockEvent.BreakEvent> mcc$pendingBreak = new ThreadLocal<>();

    /** The legacy event to post for the modern break event being fired by {@link #onBlockBreakEvent}, or null. */
    public static legacyconnector.v1_7_10.net.minecraftforge.event.world.BlockEvent.BreakEvent mcc$takePendingBreak() {
        var e = mcc$pendingBreak.get();
        mcc$pendingBreak.remove();
        return e;
    }

    /**
     * Fires the break event for a block a player is about to break (tools that break several blocks call it for each
     * extra block): the modern event, so protection mods on either side see it, and through it the legacy one. As
     * Forge, the event starts cancelled when the player may not break blocks here (adventure mode, a held item that
     * cannot break blocks) and the client is told the block is still there when it ends cancelled.
     */
    public static legacyconnector.v1_7_10.net.minecraftforge.event.world.BlockEvent.BreakEvent onBlockBreakEvent(
            legacyconnector.v1_7_10.net.minecraft.world.World world, legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType gameType,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP player, int x, int y, int z) {
        legacyconnector.v1_7_10.net.minecraft.block.Block block = world.getBlock(x, y, z);
        legacyconnector.v1_7_10.net.minecraftforge.event.world.BlockEvent.BreakEvent event = new legacyconnector.v1_7_10.net.minecraftforge.event.world.BlockEvent.BreakEvent(
                x, y, z, world, block, world.getBlockMetadata(x, y, z), player);
        net.minecraft.world.level.Level level = world.mcc$fullLevel();
        if (level == null || player == null || !(player.mcc$entity() instanceof net.minecraft.server.level.ServerPlayer sp)) {
            MinecraftForge.EVENT_BUS.post(event);
            return event;
        }
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
        net.minecraft.world.level.GameType type = gameType == null || gameType.getID() < 0 ? sp.gameMode.getGameModeForPlayer()
                : net.minecraft.world.level.GameType.byId(gameType.getID());
        mcc$pendingBreak.set(event);
        try {
            var modern = net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(level, type, sp, pos, level.getBlockState(pos));
            if (mcc$pendingBreak.get() == event) {
                // no legacy listener took it: report the modern outcome
                if (modern.isCanceled()) event.setCanceled(true);
            } else if (modern.isCanceled() && !event.isCanceled()) {
                event.setCanceled(true);
            }
        } finally {
            mcc$pendingBreak.remove();
        }
        return event;
    }

    /** Posts the attack event; false when a handler cancelled the attack. */
    public static boolean onLivingAttack(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity,
            legacyconnector.v1_7_10.net.minecraft.util.DamageSource source, float amount) {
        return !MinecraftForge.EVENT_BUS.post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingAttackEvent(entity, source, amount));
    }

    /** Posts the hurt event; the damage to apply (0 when cancelled). */
    public static float onLivingHurt(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity,
            legacyconnector.v1_7_10.net.minecraft.util.DamageSource source, float amount) {
        legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingHurtEvent event =
                new legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingHurtEvent(entity, source, amount);
        return MinecraftForge.EVENT_BUS.post(event) ? 0.0F : event.ammount;
    }
}
