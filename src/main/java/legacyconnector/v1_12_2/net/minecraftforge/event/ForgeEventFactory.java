package legacyconnector.v1_12_2.net.minecraftforge.event;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.neoforged.neoforge.event.EventHooks;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * The hooks mods call to let other mods react to what their own blocks, items and entities do. Each fires the modern event the
 * game fires for the same thing (so modern mods, and legacy handlers through the layer's event bridges, see it) and returns
 * its answer.
 */
public class ForgeEventFactory {
    private ForgeEventFactory() {}

    private static net.minecraft.core.BlockPos pos(BlockPos p) {
        return new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ());
    }

    private static net.minecraft.world.level.block.state.BlockState state(IBlockState s) {
        var m = s == null ? null : StatesBridge1122.toModern(s);
        return m == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : m;
    }

    private static net.minecraft.world.entity.player.Player player(EntityPlayer p) {
        return p == null ? null : p.mcc$player();
    }

    /** The chance each drop of a harvested block is kept (the drop list itself is the mod's). */
    public static float fireBlockHarvesting(List<ItemStack> drops, World world, BlockPos pos, IBlockState state, int fortune, float dropChance,
            boolean silkTouch, EntityPlayer player) {
        return dropChance;
    }

    /** Whether an entity may change blocks (the mobGriefing rule, as other mods decide). */
    public static boolean getMobGriefingEvent(World world, Entity entity) {
        var level = world == null ? null : world.mcc$level();
        if (level == null) return true;
        return EventHooks.canEntityGrief(level, entity == null ? null : entity.mcc$modern());
    }

    public static int onArrowLoose(ItemStack stack, World world, EntityPlayer player, int charge, boolean hasAmmo) {
        var p = player(player);
        if (p == null || world == null) return charge;
        return EventHooks.onArrowLoose(Stacks1122.toModern(stack), world.mcc$level(), p, charge, hasAmmo);
    }

    /** Null when nobody took over nocking the arrow, else the result to return from the bow's use. */
    public static ActionResult<ItemStack> onArrowNock(ItemStack item, World world, EntityPlayer player, EnumHand hand, boolean hasAmmo) {
        var p = player(player);
        if (p == null || world == null) return null;
        var result = EventHooks.onArrowNock(Stacks1122.toModern(item), world.mcc$level(), p, hand.mcc$modern(), hasAmmo);
        if (result == null) return null;
        return new ActionResult<>(EnumActionResult.mcc$of(result.getResult()), item);
    }

    public static void onPlayerDestroyItem(EntityPlayer player, ItemStack stack, EnumHand hand) {
        var p = player(player);
        if (p != null) EventHooks.onPlayerDestroyItem(p, Stacks1122.toModern(stack), hand == null ? null : hand.mcc$modern());
    }

    /** Whether an entity may break the block (withers, dragons and mods' mobs ask). */
    public static boolean onEntityDestroyBlock(EntityLivingBase entity, BlockPos pos, IBlockState state) {
        if (entity == null || !(entity.mcc$modern() instanceof net.minecraft.world.entity.LivingEntity living)) return true;
        return EventHooks.onEntityDestroyBlock(living, pos(pos), state(state));
    }

    /** True when the impact was canceled. */
    public static boolean onProjectileImpact(Entity entity, legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult ray) {
        if (entity == null || !(entity.mcc$modern() instanceof net.minecraft.world.entity.projectile.Projectile projectile) || ray == null) return false;
        return EventHooks.onProjectileImpact(projectile, ray.mcc$modern());
    }

    public static int getExperienceDrop(EntityLivingBase entity, EntityPlayer attackingPlayer, int originalExperience) {
        if (entity == null || !(entity.mcc$modern() instanceof net.minecraft.world.entity.LivingEntity living)) return originalExperience;
        return EventHooks.getExperienceDrop(living, player(attackingPlayer), originalExperience);
    }

    /** -1 when bone meal use was canceled, 1 when another mod applied it, 0 to let the block grow as usual. */
    public static int onApplyBonemeal(EntityPlayer player, World world, BlockPos pos, IBlockState state, ItemStack stack, EnumHand hand) {
        if (world == null) return 0;
        var event = EventHooks.fireBonemealEvent(player(player), world.mcc$level(), pos(pos), state(state), Stacks1122.toModern(stack));
        if (event.isCanceled()) return -1;
        return event.isSuccessful() ? 1 : 0;
    }

    public static float getBreakSpeed(EntityPlayer player, IBlockState state, float original, BlockPos pos) {
        var p = player(player);
        return p == null ? original : EventHooks.getBreakSpeed(p, state(state), original, pos(pos));
    }

    public static int onItemUseTick(EntityLivingBase entity, ItemStack item, int duration) {
        if (entity == null || !(entity.mcc$modern() instanceof net.minecraft.world.entity.LivingEntity living)) return duration;
        return EventHooks.onItemUseTick(living, Stacks1122.toModern(item), duration);
    }

    /** The state a fluid places when it flows into another (lava and water making stone, as mods change it). */
    public static IBlockState fireFluidPlaceBlockEvent(World world, BlockPos pos, BlockPos liquidPos, IBlockState state) {
        if (world == null) return state;
        var result = EventHooks.fireFluidPlaceBlockEvent(world.mcc$level(), pos(pos), pos(liquidPos), state(state));
        var legacy = StatesBridge1122.toLegacy(result);
        return legacy == null ? state : legacy;
    }

    /** Whether a mod wants the fluid to make a source block here (the fluid's own rule otherwise). */
    public static boolean canCreateFluidSource(World world, BlockPos pos, IBlockState state, boolean def) {
        return def;
    }

    /** 1.12's hoe event: 0 when nobody handled the hoe (it then tills as usual). */
    public static int onHoeUse(ItemStack stack, EntityPlayer player, World worldIn, BlockPos pos) {
        return 0;
    }
}
