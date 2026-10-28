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

    // ---- area F: more hooks over the modern events

    private static net.minecraft.world.entity.LivingEntity living(Entity e) {
        return e != null && e.mcc$modern() instanceof net.minecraft.world.entity.LivingEntity l ? l : null;
    }

    /** Fires the pickup check: -1 when a mod forbids the pickup, 1 when one forces it, 0 to pick up as usual. */
    public static int onItemPickup(legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem entityItem, EntityPlayer player) {
        var p = player(player);
        if (p == null || entityItem == null || !(entityItem.mcc$modern() instanceof net.minecraft.world.entity.item.ItemEntity item)) return 0;
        var result = EventHooks.fireItemPickupPre(item, p).canPickup();
        return result == net.neoforged.neoforge.common.util.TriState.FALSE ? -1 : result == net.neoforged.neoforge.common.util.TriState.TRUE ? 1 : 0;
    }

    /** The extra ticks a mod gives an expiring item entity (a negative number lets it despawn). */
    public static int onItemExpire(legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem entity, ItemStack item) {
        if (entity == null || !(entity.mcc$modern() instanceof net.minecraft.world.entity.item.ItemEntity modern)) return -1;
        return EventHooks.onItemExpire(modern);
    }

    /** True when a mod stops the taming. */
    public static boolean onAnimalTame(legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal animal, EntityPlayer tamer) {
        var p = player(tamer);
        if (p == null || animal == null || !(animal.mcc$modern() instanceof net.minecraft.world.entity.animal.Animal modern)) return false;
        return EventHooks.onAnimalTame(modern, p);
    }

    public static ItemStack onItemUseFinish(EntityLivingBase entity, ItemStack item, int duration, ItemStack result) {
        var l = living(entity);
        if (l == null) return result;
        return Stacks1122.fromModern(EventHooks.onItemUseFinish(l, Stacks1122.toModern(item), duration, Stacks1122.toModern(result)));
    }

    /** True when a mod canceled stopping the use. */
    public static boolean onUseItemStop(EntityLivingBase entity, ItemStack item, int duration) {
        var l = living(entity);
        return l != null && EventHooks.onUseItemStop(l, Stacks1122.toModern(item), duration);
    }

    /** Whether the player can harvest the state (the harvest check event, after the block's own rule). */
    public static boolean doPlayerHarvestCheck(EntityPlayer player, IBlockState state, boolean success) {
        var p = player(player);
        if (p == null || state == null) return success;
        var event = new net.neoforged.neoforge.event.entity.player.PlayerEvent.HarvestCheck(p, state(state), p.level(), p.blockPosition(), success);
        return net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).canHarvest();
    }

    /** The burn time mods give the stack (-1: the item's own). */
    public static int getItemBurnTime(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return 0;
        return EventHooks.getItemBurnTime(Stacks1122.toModern(itemStack), -1, null);
    }

    /** True when a mod stops the lightning bolt hitting the entity. */
    public static boolean onEntityStruckByLightning(Entity entity, legacyconnector.v1_12_2.net.minecraft.entity.effect.EntityLightningBolt bolt) {
        if (entity == null || entity.mcc$modern() == null || bolt == null || !(bolt.mcc$modern() instanceof net.minecraft.world.entity.LightningBolt modern)) return false;
        return EventHooks.onEntityStruckByLightning(entity.mcc$modern(), modern);
    }

    /** The neighbour notification event for a block change; returns the legacy event (canceled when a mod canceled the modern one). */
    public static legacyconnector.v1_12_2.net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent onNeighborNotify(World world, BlockPos pos,
            IBlockState state, java.util.EnumSet<legacyconnector.v1_12_2.net.minecraft.util.EnumFacing> notifiedSides, boolean forceRedstoneUpdate) {
        var event = new legacyconnector.v1_12_2.net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent(world, pos, state, notifiedSides, forceRedstoneUpdate);
        var level = world == null ? null : world.mcc$level();
        if (level != null) {
            java.util.EnumSet<net.minecraft.core.Direction> sides = java.util.EnumSet.noneOf(net.minecraft.core.Direction.class);
            if (notifiedSides != null) for (var f : notifiedSides) sides.add(f.mcc$modern());
            if (EventHooks.onNeighborNotify(level, pos(pos), state(state), sides, forceRedstoneUpdate).isCanceled()) event.setCanceled(true);
        }
        return event;
    }

    // ---- area F: placement and spawn events

    private static net.neoforged.neoforge.common.util.BlockSnapshot mcc$modernSnapshot(legacyconnector.v1_12_2.net.minecraftforge.common.util.BlockSnapshot snapshot) {
        if (snapshot == null) return null;
        if (snapshot.mcc$modern != null) return snapshot.mcc$modern;
        var world = snapshot.getWorld();
        var level = world == null ? null : world.mcc$level();
        return level == null ? null : net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(), level, pos(snapshot.getPos()), snapshot.getFlag());
    }

    /** The placement event for a block a player placed (canceled when a mod canceled the placement). */
    public static legacyconnector.v1_12_2.net.minecraftforge.event.world.BlockEvent.PlaceEvent onPlayerBlockPlace(EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraftforge.common.util.BlockSnapshot blockSnapshot, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing direction,
            EnumHand hand) {
        var world = blockSnapshot.getWorld();
        var placedAgainst = world == null ? null : world.getBlockState(blockSnapshot.getPos().offset(direction.getOpposite()));
        var event = new legacyconnector.v1_12_2.net.minecraftforge.event.world.BlockEvent.PlaceEvent(blockSnapshot, placedAgainst, player, hand);
        var modern = mcc$modernSnapshot(blockSnapshot);
        if (modern != null && EventHooks.onBlockPlace(player(player), modern, direction.mcc$modern())) event.setCanceled(true);
        return event;
    }

    /** Whether a mod canceled the placement of a block by an entity (a modern placement event is fired). */
    public static boolean onBlockPlace(Entity entity, legacyconnector.v1_12_2.net.minecraftforge.common.util.BlockSnapshot blockSnapshot,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing direction) {
        var modern = mcc$modernSnapshot(blockSnapshot);
        return modern != null && EventHooks.onBlockPlace(entity == null ? null : entity.mcc$modern(), modern, direction.mcc$modern());
    }

    private static net.minecraft.world.entity.Mob mcc$mob(Object entity) {
        return entity instanceof Entity e && e.mcc$modern() instanceof net.minecraft.world.entity.Mob m ? m : null;
    }

    private static legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result mcc$positionCheck(Object entity, World world, boolean spawner) {
        var mob = mcc$mob(entity);
        var level = world == null ? null : world.mcc$level();
        if (mob == null || !(level instanceof net.minecraft.world.level.ServerLevelAccessor accessor)) {
            return legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT;
        }
        var event = new net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck(mob, accessor,
                spawner ? net.minecraft.world.entity.MobSpawnType.SPAWNER : net.minecraft.world.entity.MobSpawnType.NATURAL, null);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        return switch (event.getResult()) {
            case SUCCEED -> legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW;
            case FAIL -> legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.DENY;
            default -> legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT;
        };
    }

    /** Whether mods allow (ALLOW), forbid (DENY) or leave to the mob's own rules (DEFAULT) a spawn at the entity's position. */
    public static legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result canEntitySpawn(
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity, World world, float x, float y, float z, boolean isSpawner) {
        return mcc$positionCheck(entity, world, isSpawner);
    }

    public static boolean canEntitySpawnSpawner(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity, World world, float x, float y, float z) {
        var result = mcc$positionCheck(entity, world, true);
        if (result == legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT) {
            var mob = mcc$mob(entity);
            var level = world == null ? null : world.mcc$level();
            return mob == null || !(level instanceof net.minecraft.world.level.ServerLevelAccessor a)
                    || mob.checkSpawnRules(a, net.minecraft.world.entity.MobSpawnType.SPAWNER) && mob.checkSpawnObstruction(a);
        }
        return result == legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW;
    }

    /**
     * Fires the spawn finalization event: true when a mod took over (canceled it), and the caller then skips the mob's own
     * {@code onInitialSpawn}.
     */
    public static boolean doSpecialSpawn(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity, World world, float x, float y, float z) {
        var mob = mcc$mob(entity);
        var level = world == null ? null : world.mcc$level();
        if (mob == null || !(level instanceof net.minecraft.world.level.ServerLevelAccessor accessor)) return false;
        var event = new net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent(mob, accessor, x, y, z,
                accessor.getCurrentDifficultyAt(mob.blockPosition()), net.minecraft.world.entity.MobSpawnType.NATURAL, null, null);
        return net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    /** Whether mods force (ALLOW), forbid (DENY) or leave to the mob (DEFAULT) its despawning. */
    public static legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result canEntityDespawn(
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity) {
        var mob = mcc$mob(entity);
        if (mob == null || !(mob.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT;
        }
        var event = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.MobDespawnEvent(mob, level));
        return switch (event.getResult()) {
            case ALLOW -> legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW;
            case DENY -> legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.DENY;
            default -> legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT;
        };
    }

    public static int getMaxSpawnPackSize(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity) {
        var mob = mcc$mob(entity);
        return mob == null ? 4 : EventHooks.getMaxSpawnClusterSize(mob);
    }
}
