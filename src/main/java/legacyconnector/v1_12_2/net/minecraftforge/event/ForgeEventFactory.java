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

    /** The placement event of a block placed by an entity (canceled when a mod canceled the placement; a modern placement event is fired). */
    public static legacyconnector.v1_12_2.net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent onBlockPlace(Entity entity,
            legacyconnector.v1_12_2.net.minecraftforge.common.util.BlockSnapshot blockSnapshot, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing direction) {
        var world = blockSnapshot.getWorld();
        var placedAgainst = world == null || direction == null ? null : world.getBlockState(blockSnapshot.getPos().offset(direction.getOpposite()));
        var event = new legacyconnector.v1_12_2.net.minecraftforge.event.world.BlockEvent.EntityPlaceEvent(blockSnapshot, placedAgainst, entity);
        var modern = mcc$modernSnapshot(blockSnapshot);
        if (modern != null && EventHooks.onBlockPlace(entity == null ? null : entity.mcc$modern(), modern,
                direction == null ? net.minecraft.core.Direction.UP : direction.mcc$modern())) {
            event.setCanceled(true);
        }
        return event;
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

    /** The older form without the spawner flag: a natural spawn. */
    @Deprecated
    public static legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result canEntitySpawn(
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity, World world, float x, float y, float z) {
        return mcc$positionCheck(entity, world, false);
    }

    /**
     * The mobs that may spawn naturally here: mods' {@code WorldEvent.PotentialSpawns} listeners edit the list; null when one cancels
     * (nothing spawns).
     */
    public static List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry> getPotentialSpawns(
            legacyconnector.v1_12_2.net.minecraft.world.WorldServer world, legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType type, BlockPos pos,
            List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry> oldList) {
        var event = new legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent.PotentialSpawns(world, type, pos, oldList);
        if (legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) return null;
        return event.getList();
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

    // ---- area F: terrain, explosion, projectile, bucket, tooltip and sound hooks

    private static legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.EventBus mcc$bus() {
        return legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS;
    }

    /** False when a mod replaced the chunk's biome blocks itself (DENY): the generator then skips its own surface pass. */
    public static boolean onReplaceBiomeBlocks(legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator gen, int x, int z,
            legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer primer, World world) {
        var event = new legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.ChunkGeneratorEvent.ReplaceBiomeBlocks(gen, x, z, primer, world);
        mcc$bus().post(event);
        return event.getResult() != legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.DENY;
    }

    /** Posts the population event before ({@code pre}) or after a mod generator populates a chunk. */
    public static void onChunkPopulate(boolean pre, legacyconnector.v1_12_2.net.minecraft.world.gen.IChunkGenerator gen, World world, java.util.Random rand,
            int x, int z, boolean hasVillageGenerated) {
        mcc$bus().post(pre ? new legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.PopulateChunkEvent.Pre(gen, world, rand, x, z, hasVillageGenerated)
                : new legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.PopulateChunkEvent.Post(gen, world, rand, x, z, hasVillageGenerated));
    }

    /**
     * True when a mod canceled a mod's explosion before it happened. Mods' handlers see the mod's own explosion object (as in 1.12);
     * explosions the modern game carries out reach them through the event bridge instead.
     */
    public static boolean onExplosionStart(World world, legacyconnector.v1_12_2.net.minecraft.world.Explosion explosion) {
        return mcc$bus().post(new legacyconnector.v1_12_2.net.minecraftforge.event.world.ExplosionEvent.Start(world, explosion));
    }

    /** Lets mods' handlers remove blocks (from the explosion's list) and entities (from {@code list}) a mod's explosion affects. */
    public static void onExplosionDetonate(World world, legacyconnector.v1_12_2.net.minecraft.world.Explosion explosion, List<Entity> list, double diameter) {
        mcc$bus().post(new legacyconnector.v1_12_2.net.minecraftforge.event.world.ExplosionEvent.Detonate(world, explosion, list));
    }

    /** True when a mod canceled the arrow's impact. */
    public static boolean onProjectileImpact(legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityArrow arrow,
            legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult ray) {
        return mcc$bus().post(new legacyconnector.v1_12_2.net.minecraftforge.event.entity.ProjectileImpactEvent.Arrow(arrow, ray));
    }

    /** True when a mod canceled the fireball's impact. */
    public static boolean onProjectileImpact(legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityFireball fireball,
            legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult ray) {
        return mcc$bus().post(new legacyconnector.v1_12_2.net.minecraftforge.event.entity.ProjectileImpactEvent.Fireball(fireball, ray));
    }

    /** True when a mod canceled the thrown entity's impact. */
    public static boolean onProjectileImpact(legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityThrowable throwable,
            legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult ray) {
        return mcc$bus().post(new legacyconnector.v1_12_2.net.minecraftforge.event.entity.ProjectileImpactEvent.Throwable(throwable, ray));
    }

    /** Spawn check of a mob at its position (a spawner's when {@code spawner} is not null). */
    public static legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result canEntitySpawn(
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity, World world, float x, float y, float z,
            legacyconnector.v1_12_2.net.minecraft.tileentity.MobSpawnerBaseLogic spawner) {
        return mcc$positionCheck(entity, world, spawner != null);
    }

    public static boolean canEntitySpawnSpawner(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity, World world, float x, float y, float z,
            legacyconnector.v1_12_2.net.minecraft.tileentity.MobSpawnerBaseLogic spawner) {
        return canEntitySpawnSpawner(entity, world, x, y, z);
    }

    /** The spawn finalization of a mob a spawner made: true when a mod took over. */
    public static boolean doSpecialSpawn(legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving entity, World world, float x, float y, float z,
            legacyconnector.v1_12_2.net.minecraft.tileentity.MobSpawnerBaseLogic spawner) {
        var mob = mcc$mob(entity);
        var level = world == null ? null : world.mcc$level();
        if (mob == null || !(level instanceof net.minecraft.world.level.ServerLevelAccessor accessor)) return false;
        var event = new net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent(mob, accessor, x, y, z,
                accessor.getCurrentDifficultyAt(mob.blockPosition()),
                spawner != null ? net.minecraft.world.entity.MobSpawnType.SPAWNER : net.minecraft.world.entity.MobSpawnType.NATURAL, null, null);
        return net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    /**
     * A bucket used on the world: null when no mod handled it (the bucket then does its own thing), else the stack to hold
     * (FAIL when a mod canceled the use; SUCCESS with a mod's filled bucket, given to the player).
     */
    public static ActionResult<ItemStack> onBucketUse(EntityPlayer player, World world, ItemStack stack,
            legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult target) {
        var event = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.FillBucketEvent(player, stack, world, target);
        if (mcc$bus().post(event)) return new ActionResult<>(EnumActionResult.FAIL, stack);
        if (event.getResult() != legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW) return null;
        var p = player(player);
        if (p != null && p.getAbilities().instabuild) return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        stack.shrink(1);
        ItemStack filled = event.getFilledBucket();
        if (stack.isEmpty()) return new ActionResult<>(EnumActionResult.SUCCESS, filled);
        if (p != null && filled != null && !filled.isEmpty()) {
            var modern = Stacks1122.toModern(filled);
            if (!p.getInventory().add(modern)) p.drop(modern, false);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /** Lets mods add lines to an item's tooltip (the list is the tooltip being built). */
    public static legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.ItemTooltipEvent onItemTooltip(ItemStack itemStack, EntityPlayer entityPlayer,
            List<String> toolTip, legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag flags) {
        var event = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.ItemTooltipEvent(itemStack, entityPlayer, toolTip,
                flags != null && flags.isAdvanced());
        mcc$bus().post(event);
        return event;
    }

    /**
     * An entity plays a sound: 1.12 handlers, then modern ones (sound mufflers and the like) may change or cancel it. The returned
     * event carries the sound to play.
     */
    public static legacyconnector.v1_12_2.net.minecraftforge.event.entity.PlaySoundAtEntityEvent onPlaySoundAtEntity(Entity entity,
            legacyconnector.v1_12_2.net.minecraft.util.SoundEvent name, legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category,
            float volume, float pitch) {
        var event = new legacyconnector.v1_12_2.net.minecraftforge.event.entity.PlaySoundAtEntityEvent(entity, name, category, volume, pitch);
        if (mcc$bus().post(event)) return event;
        var modern = entity == null ? null : entity.mcc$modern();
        var sound = event.getSound() == null ? null : event.getSound().mcc$event();
        if (modern == null || sound == null || event.getCategory() == null) return event;
        var holder = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        var result = EventHooks.onPlaySoundAtEntity(modern, holder, event.getCategory().mcc$modern(), event.getVolume(), event.getPitch());
        if (result.isCanceled() || result.getSound() == null) {
            event.setCanceled(true);
            return event;
        }
        if (result.getSound() != holder) {
            var id = result.getSound().unwrapKey().map(k -> k.location()).orElse(null);
            var legacy = id == null ? null : legacyconnector.v1_12_2.net.minecraft.util.SoundEvent.REGISTRY.getObject(
                    new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(id.getNamespace(), id.getPath()));
            if (legacy != null) event.setSound(legacy);
        }
        event.setCategory(legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.mcc$of(result.getSource()));
        event.setVolume(result.getNewVolume());
        event.setPitch(result.getNewPitch());
        return event;
    }

    /** The offers a merchant shows the player, after mods' {@code MerchantTradeOffersEvent} handlers (a copy; the merchant's own is kept). */
    public static legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipeList listTradeOffers(legacyconnector.v1_12_2.net.minecraft.entity.IMerchant merchant,
            EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipeList list) {
        legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipeList copy = null;
        if (list != null) {
            copy = new legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipeList();
            copy.addAll(list);
        }
        var event = new legacyconnector.v1_12_2.net.minecraftforge.event.village.MerchantTradeOffersEvent(merchant, player, copy);
        mcc$bus().post(event);
        return event.getList();
    }
}
