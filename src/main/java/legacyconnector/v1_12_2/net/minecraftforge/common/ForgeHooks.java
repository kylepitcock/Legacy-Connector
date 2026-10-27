package legacyconnector.v1_12_2.net.minecraftforge.common;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.neoforged.neoforge.common.CommonHooks;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;

/** Forge's helpers mods call from their own blocks, items and recipes; each answers through the modern game's hooks. */
public class ForgeHooks {
    private static final ThreadLocal<EntityPlayer> CRAFTING_PLAYER = new ThreadLocal<>();
    private static final Pattern URL_PATTERN = Pattern.compile("((?:[a-z0-9]{2,}:\\/\\/)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_\\.]{1,}\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"§ \n]|$))",
            Pattern.CASE_INSENSITIVE);

    private ForgeHooks() {}

    private static net.minecraft.core.BlockPos pos(BlockPos p) {
        return new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ());
    }

    /** What is left in the crafting grid of an item used up in a recipe (a bucket of a milk bucket, nothing for most). */
    public static ItemStack getContainerItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.getItem().hasContainerItem(stack)) return ItemStack.EMPTY;
        ItemStack container = stack.getItem().getContainerItem(stack);
        if (container != null && !container.isEmpty() && container.isItemStackDamageable() && container.getMetadata() > container.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        return container == null ? ItemStack.EMPTY : container;
    }

    /** The items a recipe leaves in each slot of the grid. */
    public static NonNullList<ItemStack> defaultRecipeGetRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> ret = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < ret.size(); i++) ret.set(i, getContainerItem(inv.getStackInSlot(i)));
        return ret;
    }

    public static void setCraftingPlayer(EntityPlayer player) {
        CRAFTING_PLAYER.set(player);
        CommonHooks.setCraftingPlayer(player == null ? null : player.mcc$player());
    }

    public static EntityPlayer getCraftingPlayer() {
        return CRAFTING_PLAYER.get();
    }

    /** The enchanting power the block at {@code pos} gives a nearby enchanting table (1 for a bookshelf). */
    public static float getEnchantPower(World world, BlockPos pos) {
        if (world == null || world.mcc$level() == null) return 0.0F;
        var p = pos(pos);
        return world.mcc$level().getBlockState(p).getEnchantPowerBonus(world.mcc$level(), p);
    }

    /** A seed broken grass drops (wheat seeds or one a mod added through {@code MinecraftForge.addGrassSeed}). */
    public static ItemStack getGrassSeed(Random rand, int fortune) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.WHEAT_SEEDS);
    }

    public static void onLivingJump(EntityLivingBase entity) {
        if (entity != null && entity.mcc$modern() instanceof net.minecraft.world.entity.LivingEntity living) CommonHooks.onLivingJump(living);
    }

    /** False when a mod stops the entity from changing dimension. */
    public static boolean onTravelToDimension(Entity entity, int dimension) {
        if (entity == null || entity.mcc$modern() == null) return true;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        var level = server == null ? null : legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.mcc$level(dimension);
        return level == null || CommonHooks.onTravelToDimension(entity.mcc$modern(), level.dimension());
    }

    public static boolean onCropsGrowPre(World worldIn, BlockPos pos, IBlockState state, boolean def) {
        if (worldIn == null || worldIn.mcc$level() == null) return def;
        var modern = StatesBridge1122.toModern(state);
        return modern == null ? def : CommonHooks.canCropGrow(worldIn.mcc$level(), pos(pos), modern, def);
    }

    public static void onCropsGrowPost(World worldIn, BlockPos pos, IBlockState state, IBlockState blockState) {
        if (worldIn == null || worldIn.mcc$level() == null) return;
        var modern = StatesBridge1122.toModern(blockState != null ? blockState : state);
        if (modern != null) CommonHooks.fireCropGrowPost(worldIn.mcc$level(), pos(pos), modern);
    }

    /** The chance the anvil is damaged by this repair. */
    public static float onAnvilRepair(EntityPlayer player, ItemStack output, ItemStack left, ItemStack right) {
        if (player == null || player.mcc$player() == null) return 0.12F;
        return CommonHooks.onAnvilRepair(player.mcc$player(), Stacks1122.toModern(output), Stacks1122.toModern(left), Stacks1122.toModern(right));
    }

    /** How much of the block the player breaks per tick (0 when the player cannot harvest it at all). */
    public static float blockStrength(IBlockState state, EntityPlayer player, World world, BlockPos pos) {
        var modern = StatesBridge1122.toModern(state);
        if (modern == null || player == null || player.mcc$player() == null || world == null) return 0.0F;
        return modern.getDestroyProgress(player.mcc$player(), world.mcc$level(), pos(pos));
    }

    /** The point the entity looks at within {@code length} blocks (blocks only), or null. */
    public static RayTraceResult rayTraceEyes(EntityLivingBase entity, double length) {
        if (entity == null || entity.mcc$modern() == null) return null;
        var e = entity.mcc$modern();
        var start = e.getEyePosition();
        var end = start.add(e.getLookAngle().scale(length));
        var hit = e.level().clip(new net.minecraft.world.level.ClipContext(start, end, net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, e));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? null : RayTraceResult.mcc$of(hit);
    }

    public static Vec3d rayTraceEyeHitVec(EntityLivingBase entity, double length) {
        RayTraceResult r = rayTraceEyes(entity, length);
        return r == null ? null : r.hitVec;
    }

    /** A chat text with the links in it clickable. */
    public static ITextComponent newChatWithLinks(String string) {
        return newChatWithLinks(string, true);
    }

    public static ITextComponent newChatWithLinks(String string, boolean allowMissingHeader) {
        ITextComponent ichat = null;
        Matcher matcher = URL_PATTERN.matcher(string);
        int lastEnd = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String part = string.substring(lastEnd, start);
            if (!part.isEmpty()) {
                if (ichat == null) ichat = new TextComponentString(part);
                else ichat.appendText(part);
            }
            lastEnd = end;
            String url = string.substring(start, end);
            ITextComponent link = new TextComponentString(url);
            try {
                if (new java.net.URI(url).getScheme() == null) {
                    if (!allowMissingHeader) {
                        if (ichat == null) ichat = new TextComponentString(url);
                        else ichat.appendText(url);
                        continue;
                    }
                    url = "http://" + url;
                }
            } catch (java.net.URISyntaxException e) {
                if (ichat == null) ichat = new TextComponentString(url);
                else ichat.appendText(url);
                continue;
            }
            link.getStyle().setClickEvent(new legacyconnector.v1_12_2.net.minecraft.util.text.event.ClickEvent(
                    legacyconnector.v1_12_2.net.minecraft.util.text.event.ClickEvent.Action.OPEN_URL, url));
            link.getStyle().setUnderlined(true);
            link.getStyle().setColor(legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting.BLUE);
            if (ichat == null) ichat = link;
            else ichat.appendSibling(link);
        }
        String end = string.substring(lastEnd);
        if (ichat == null) ichat = new TextComponentString(end);
        else if (!end.isEmpty()) ichat.appendText(end);
        return ichat;
    }

    // ---- area F: block breaking and harvesting, attacks, falls

    private static net.minecraft.world.level.Level mcc$level(Object world) {
        return world instanceof World w ? w.mcc$level() : null;
    }

    /**
     * Fires the block break event for a player breaking the block: -1 when a mod canceled it, else the experience the block drops
     * (the modern event no longer carries one; the block drops its own when broken).
     */
    public static int onBlockBreakEvent(World world, legacyconnector.v1_12_2.net.minecraft.world.GameType gameType,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP entityPlayer, BlockPos pos) {
        var level = world == null ? null : world.mcc$level();
        var sp = entityPlayer == null ? null : entityPlayer.mcc$serverPlayer();
        if (level == null || sp == null || pos == null) return 0;
        var p = pos(pos);
        var event = CommonHooks.fireBlockBreak(level, gameType == null ? sp.gameMode.getGameModeForPlayer() : gameType.mcc$modern(), sp, p, level.getBlockState(p));
        return event.isCanceled() ? -1 : 0;
    }

    /** Whether the player can harvest the block at {@code pos} (right tool, or no tool needed; mods' harvest check events decide too). */
    public static boolean canHarvestBlock(legacyconnector.v1_12_2.net.minecraft.block.Block block, EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, BlockPos pos) {
        var level = mcc$level(world);
        var p = player == null ? null : player.mcc$player();
        if (level == null || p == null || pos == null) return true;
        var mp = pos(pos);
        var state = level.getBlockState(mp);
        return net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(p, state, level, mp);
    }

    /** Whether the stack is a tool that breaks the block at {@code pos} faster (its tool type matches). */
    public static boolean isToolEffective(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, BlockPos pos, ItemStack stack) {
        var level = mcc$level(world);
        if (level == null || pos == null || stack == null || stack.isEmpty()) return false;
        var state = level.getBlockState(pos(pos));
        var modern = Stacks1122.toModern(stack);
        return modern.isCorrectToolForDrops(state) || modern.getDestroySpeed(state) > 1.0F;
    }

    /** Whether the stack's tool can harvest the block at {@code pos} (blocks that need no tool always can). */
    public static boolean canToolHarvestBlock(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, BlockPos pos, ItemStack stack) {
        var level = mcc$level(world);
        if (level == null || pos == null) return false;
        var state = level.getBlockState(pos(pos));
        if (!state.requiresCorrectToolForDrops()) return true;
        return stack != null && !stack.isEmpty() && Stacks1122.toModern(stack).isCorrectToolForDrops(state);
    }

    /** False when a mod canceled the player's attack on the target. */
    public static boolean onPlayerAttackTarget(EntityPlayer player, Entity target) {
        var p = player == null ? null : player.mcc$player();
        if (p == null || target == null || target.mcc$modern() == null) return true;
        return CommonHooks.onPlayerAttackTarget(p, target.mcc$modern());
    }

    /** The fall distance and damage multiplier after mods' fall handlers, or null when the fall was canceled. */
    public static float[] onLivingFall(EntityLivingBase entity, float distance, float damageMultiplier) {
        if (entity == null || !(entity.mcc$modern() instanceof net.minecraft.world.entity.LivingEntity living)) return new float[] {distance, damageMultiplier};
        return CommonHooks.onLivingFall(living, distance, damageMultiplier);
    }

    /** How far away a mob notices the player: the distance scaled by the player's visibility (sneaking, invisibility, mob heads). */
    public static double getPlayerVisibilityDistance(EntityPlayer player, double xzDistance, double maxXZDistance) {
        var p = player == null ? null : player.mcc$player();
        if (p == null) return xzDistance;
        double visibility = p.getVisibilityPercent(null);
        double max = maxXZDistance * visibility;
        return xzDistance >= max ? max : xzDistance;
    }
}
