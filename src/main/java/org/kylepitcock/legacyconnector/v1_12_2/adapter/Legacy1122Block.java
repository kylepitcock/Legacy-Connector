package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;

/**
 * The modern block standing for a 1.12.2 block. A 1.12 block saved its state as 4 metadata bits, so the modern block keeps exactly
 * that: one {@link #META} property, mapped to the legacy state through the block's own {@code getStateFromMeta}/{@code getMetaFromState}.
 *
 * <p>Gameplay hooks the mod's block overrides are forwarded with legacy worlds, positions, states and players (as the 1.7.10 adapter
 * does); hooks it does not override keep the modern behaviour. Translated semantics:</p>
 * <ul>
 *     <li>right click: 1.12 called {@code onBlockActivated} for each hand before the item was used; the modern {@code useItemOn} runs
 *     for each hand (also an empty one), so it is the one forwarded, and a false answer lets the item be used;</li>
 *     <li>drops: computed by the legacy code ({@code getItemDropped}, {@code quantityDropped}, {@code damageDropped}, silk touch),
 *     not by loot tables; a mod's own {@code harvestBlock} replaces the modern harvesting;</li>
 *     <li>random ticks: the legacy {@code randomTick} (an update tick by default) when the block asked for them.</li>
 * </ul>
 */
public class Legacy1122Block extends net.minecraft.world.level.block.Block implements net.minecraft.world.level.block.BonemealableBlock {
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    private final Block legacy;
    private final boolean activated;
    private final boolean clicked;
    private final boolean added;
    private final boolean broken;
    private final boolean neighbor;
    private final boolean updateTick;
    private final boolean randomTick;
    private final boolean displayTick;
    private final boolean placedBy;
    private final boolean harvested;
    private final boolean playerDestroy;
    private final boolean collision;
    private final boolean walk;
    private final boolean fallen;
    private final boolean event;
    private final boolean harvest;
    private final boolean removed;
    private final boolean comparator;
    private final boolean pickItem;
    private final boolean collisionList;
    private final boolean pushReaction;
    private final boolean relativeHardness;
    // hooks taking an IBlockAccess
    private final boolean bounds;
    private final boolean collisionBox;
    private final boolean replaceable;
    private final boolean lightValue;
    private final boolean lightOpacity;
    private final boolean slipperiness;
    private final boolean dropsList;
    private final boolean pickBlock;
    private final boolean explosionResistance;
    private final boolean powerSource;
    private final boolean expDrop;
    private final boolean weakPower;
    private final boolean strongPower;
    private int failures;
    /** Set while legacy code computes a shape or breaking speed: the shim's defaults ask the modern block again, which must not recurse. */
    private static final ThreadLocal<boolean[]> IN_LEGACY_SHAPE = ThreadLocal.withInitial(() -> new boolean[1]);
    private static final ThreadLocal<boolean[]> IN_LEGACY_HARDNESS = ThreadLocal.withInitial(() -> new boolean[1]);

    public Legacy1122Block(Block legacy, Properties properties) {
        super(properties);
        this.legacy = legacy;
        Class<?> W = World.class, P = legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.class, S = IBlockState.class;
        this.activated = overrides(legacy, "onBlockActivated", W, P, S, EntityPlayer.class, EnumHand.class, EnumFacing.class, float.class, float.class, float.class);
        this.clicked = overrides(legacy, "onBlockClicked", W, P, EntityPlayer.class);
        this.added = overrides(legacy, "onBlockAdded", W, P, S);
        this.broken = overrides(legacy, "breakBlock", W, P, S);
        this.neighbor = overrides(legacy, "neighborChanged", S, W, P, Block.class, P);
        this.updateTick = overrides(legacy, "updateTick", W, P, S, Random.class);
        this.randomTick = updateTick || overrides(legacy, "randomTick", W, P, S, Random.class);
        this.displayTick = overrides(legacy, "randomDisplayTick", S, W, P, Random.class);
        this.placedBy = overrides(legacy, "onBlockPlacedBy", W, P, S, EntityLivingBase.class, legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class);
        this.harvested = overrides(legacy, "onBlockHarvested", W, P, S, EntityPlayer.class);
        this.playerDestroy = overrides(legacy, "onPlayerDestroy", W, P, S);
        this.collision = overrides(legacy, "onEntityCollision", W, P, S, legacyconnector.v1_12_2.net.minecraft.entity.Entity.class);
        this.walk = overrides(legacy, "onEntityWalk", W, P, legacyconnector.v1_12_2.net.minecraft.entity.Entity.class);
        this.fallen = overrides(legacy, "onFallenUpon", W, P, legacyconnector.v1_12_2.net.minecraft.entity.Entity.class, float.class);
        this.event = overrides(legacy, "eventReceived", S, W, P, int.class, int.class);
        this.harvest = overrides(legacy, "harvestBlock", W, EntityPlayer.class, P, S, legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity.class,
                legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class);
        this.removed = overrides(legacy, "removedByPlayer", S, W, P, EntityPlayer.class, boolean.class);
        this.comparator = overrides(legacy, "hasComparatorInputOverride", S);
        this.pickItem = overrides(legacy, "getItem", W, P, S);
        Class<?> BB = legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB.class;
        this.collisionList = overrides(legacy, "addCollisionBoxToList", S, W, P, BB, java.util.List.class, legacyconnector.v1_12_2.net.minecraft.entity.Entity.class,
                boolean.class);
        this.pushReaction = overrides(legacy, "getPushReaction", S);
        this.relativeHardness = overrides(legacy, "getPlayerRelativeBlockHardness", S, EntityPlayer.class, W, P);
        Class<?> A = legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess.class;
        this.bounds = overrides(legacy, "getBoundingBox", S, A, P);
        this.collisionBox = overrides(legacy, "getCollisionBoundingBox", S, A, P);
        this.replaceable = overrides(legacy, "isReplaceable", A, P);
        this.lightValue = overrides(legacy, "getLightValue", S, A, P);
        this.lightOpacity = overrides(legacy, "getLightOpacity", S, A, P);
        this.slipperiness = overrides(legacy, "getSlipperiness", S, A, P, legacyconnector.v1_12_2.net.minecraft.entity.Entity.class);
        this.dropsList = overrides(legacy, "getDrops", legacyconnector.v1_12_2.net.minecraft.util.NonNullList.class, A, P, S, int.class)
                || overrides(legacy, "getDrops", A, P, S, int.class);
        this.pickBlock = overrides(legacy, "getPickBlock", S, legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult.class, W, P, EntityPlayer.class);
        this.powerSource = overrides(legacy, "canProvidePower", S);
        this.expDrop = overrides(legacy, "getExpDrop", S, A, P, int.class);
        this.weakPower = overrides(legacy, "getWeakPower", S, A, P, EnumFacing.class);
        this.strongPower = overrides(legacy, "getStrongPower", S, A, P, EnumFacing.class);
        this.explosionResistance = overrides(legacy, "getExplosionResistance", W, P, legacyconnector.v1_12_2.net.minecraft.entity.Entity.class,
                legacyconnector.v1_12_2.net.minecraft.world.Explosion.class);
        registerDefaultState(stateDefinition.any().setValue(META, 0));
    }

    /**
     * Whether the block's or item's class declares the method below the base class: the mod's own classes and the shim's vanilla
     * subclasses (BlockFalling, BlockRotatedPillar, ...) count; Block, Item and ItemBlock themselves do not (their defaults are what
     * the modern game already does); synthesized vanilla classes only pass calls up, so they are looked through.
     */
    public static boolean overrides(Object obj, String name, Class<?>... params) {
        for (Class<?> c = obj.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            if (c == Block.class || c == legacyconnector.v1_12_2.net.minecraft.item.Item.class || c == legacyconnector.v1_12_2.net.minecraft.item.ItemBlock.class) {
                return false;
            }
            if (c.getName().startsWith("legacyconnector_synth.")) continue;
            try {
                c.getDeclaredMethod(name, params);
                return true;
            } catch (NoSuchMethodException ignored) {
            } catch (LinkageError e) {
                // a declared method names a class that does not exist: look further up
            }
        }
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(META);
    }

    public Block legacy() {
        return legacy;
    }

    @Override
    public String getDescriptionId() {
        return legacy.getUnlocalizedName() + ".name";
    }

    // ------------------------------------------------------------------ helpers

    private static World world(Level level) {
        return WorldBridge1122.wrap(level);
    }

    private static legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos(BlockPos pos) {
        return legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(pos);
    }

    private static IBlockState state(BlockState state) {
        return StatesBridge1122.toLegacy(state);
    }

    private static EntityPlayer player(Player p) {
        return p != null && EntityBridge1122.wrap(p) instanceof EntityPlayer e ? e : null;
    }

    private static legacyconnector.v1_12_2.net.minecraft.entity.Entity entity(Entity e) {
        return EntityBridge1122.wrap(e);
    }

    /** Legacy code throwing in a world callback must not crash the server: logged a few times per block, then the call is skipped. */
    private void failure(String callback, Throwable e) {
        if (failures++ < 3) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Blocks").error("{} of {} threw; the call is skipped{}", callback, legacy.getClass().getName(),
                    failures == 3 ? " (further failures of this block are not logged)" : "", e);
        }
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!activated) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        EntityPlayer p = player(player);
        if (p == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        Vec3 h = hit.getLocation();
        boolean done;
        try {
            done = legacy.onBlockActivated(world(level), pos(pos), state(state), p, hand == InteractionHand.OFF_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND,
                    EnumFacing.mcc$of(hit.getDirection()), (float) (h.x - pos.getX()), (float) (h.y - pos.getY()), (float) (h.z - pos.getZ()));
        } catch (RuntimeException | LinkageError e) {
            failure("onBlockActivated", e);
            done = false;
        } finally {
            Stacks1122.flushViews();
        }
        return done ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!clicked) return;
        try {
            legacy.onBlockClicked(world(level), pos(pos), player(player));
        } catch (RuntimeException | LinkageError e) {
            failure("onBlockClicked", e);
        } finally {
            Stacks1122.flushViews();
        }
    }

    // ------------------------------------------------------------------ lifecycle in the world

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!added || oldState.is(this)) return;
        try {
            legacy.onBlockAdded(world(level), pos(pos), state(state));
        } catch (RuntimeException | LinkageError e) {
            failure("onBlockAdded", e);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        // A tile entity block removed as "moving" is carried away whole (Create contraptions, Sable sub-levels copy its NBT); 1.12.2's
        // pistons never moved tile entities, so breakBlock meant destruction and would drop the inventory that also travels (a duplicate).
        boolean carried = moving && state.hasBlockEntity() && level.getBlockEntity(pos) instanceof Legacy1122BlockEntity;
        if (broken && !newState.is(this) && !carried) {
            try {
                legacy.breakBlock(world(level), pos(pos), state(state));
            } catch (RuntimeException | LinkageError e) {
                failure("breakBlock", e);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, BlockPos from,
            boolean moving) {
        super.neighborChanged(state, level, pos, neighborBlock, from, moving);
        // what a legacy tile offers per side can depend on its neighbours: let cached capabilities (Create, pipes) re-ask
        if (!level.isClientSide && state.hasBlockEntity()) level.invalidateCapabilities(pos);
        if (!neighbor) return;
        try {
            legacy.neighborChanged(state(state), world(level), pos(pos), StatesBridge1122.legacyBlock(neighborBlock.defaultBlockState()), pos(from));
        } catch (RuntimeException | LinkageError e) {
            failure("neighborChanged", e);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!updateTick) return;
        World w = world(level);
        try {
            legacy.updateTick(w, pos(pos), state(state), w.rand);
        } catch (RuntimeException | LinkageError e) {
            failure("updateTick", e);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return legacy.getTickRandomly() && randomTick;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        World w = world(level);
        try {
            legacy.randomTick(w, pos(pos), state(state), w.rand);
        } catch (RuntimeException | LinkageError e) {
            failure("randomTick", e);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!displayTick) return;
        try {
            legacy.randomDisplayTick(state(state), world(level), pos(pos), new Random(random.nextLong()));
        } catch (RuntimeException | LinkageError e) {
            failure("randomDisplayTick", e);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!placedBy) return;
        try {
            var p = placer == null ? null : entity(placer);
            legacy.onBlockPlacedBy(world(level), pos(pos), state(state), p instanceof EntityLivingBase l ? l : null, Stacks1122.view(stack));
        } catch (RuntimeException | LinkageError e) {
            failure("onBlockPlacedBy", e);
        } finally {
            Stacks1122.flushViews();
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (harvested && !removed) {
            try {
                legacy.onBlockHarvested(world(level), pos(pos), state(state), player(player));
            } catch (RuntimeException | LinkageError e) {
                failure("onBlockHarvested", e);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
            net.minecraft.world.level.material.FluidState fluid) {
        if (!removed) return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        // the mod's removedByPlayer calls onBlockHarvested itself, as Forge's did
        try {
            return legacy.removedByPlayer(state(state), world(level), pos(pos), player(player), willHarvest);
        } catch (RuntimeException | LinkageError e) {
            failure("removedByPlayer", e);
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level, pos, state);
        if (!playerDestroy || !(level instanceof Level l)) return;
        try {
            legacy.onPlayerDestroy(world(l), pos(pos), state(state));
        } catch (RuntimeException | LinkageError e) {
            failure("onPlayerDestroy", e);
        }
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        boolean handled = super.triggerEvent(state, level, pos, id, param);
        if (!event) {
            // BlockContainer.eventReceived passed the event to the tile entity
            if (legacy instanceof legacyconnector.v1_12_2.net.minecraft.block.BlockContainer) {
                BlockEntity be = level.getBlockEntity(pos);
                return be != null && be.triggerEvent(id, param) || handled;
            }
            return handled;
        }
        try {
            return legacy.eventReceived(state(state), world(level), pos(pos), id, param) || handled;
        } catch (RuntimeException | LinkageError e) {
            failure("eventReceived", e);
            return handled;
        }
    }

    // ------------------------------------------------------------------ entities

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!collision) return;
        try {
            legacy.onEntityCollision(world(level), pos(pos), state(state), entity(entity));
        } catch (RuntimeException | LinkageError e) {
            failure("onEntityCollision", e);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!walk) {
            super.stepOn(level, pos, state, entity);
            return;
        }
        try {
            legacy.onEntityWalk(world(level), pos(pos), entity(entity));
        } catch (RuntimeException | LinkageError e) {
            failure("onEntityWalk", e);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        if (!fallen) {
            super.fallOn(level, state, pos, entity, distance);
            return;
        }
        try {
            legacy.onFallenUpon(world(level), pos(pos), entity(entity), distance);
        } catch (RuntimeException | LinkageError e) {
            failure("onFallenUpon", e);
            super.fallOn(level, state, pos, entity, distance);
        }
    }

    // ------------------------------------------------------------------ vanilla base class rules: glass, ladders, bone meal

    /** BlockBreakable (glass, ice): the faces between two of the same block in the same state are not drawn. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction direction) {
        if (legacy instanceof legacyconnector.v1_12_2.net.minecraft.block.BlockBreakable b && b.mcc$hidesSimilarNeighbours() && adjacent.is(this)
                && adjacent.getValue(META).equals(state.getValue(META))) {
            return true;
        }
        return super.skipRendering(state, adjacent, direction);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return legacy instanceof legacyconnector.v1_12_2.net.minecraft.block.BlockLadder || super.isLadder(state, level, pos, entity);
    }

    /** IGrowable blocks (crops, saplings): bone meal asks and grows them through the legacy code. */
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        if (!(legacy instanceof legacyconnector.v1_12_2.net.minecraft.block.IGrowable g) || !(level instanceof Level l)) return false;
        try {
            return g.canGrow(world(l), pos(pos), state(state), l.isClientSide);
        } catch (RuntimeException | LinkageError e) {
            failure("canGrow", e);
            return false;
        }
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        if (!(legacy instanceof legacyconnector.v1_12_2.net.minecraft.block.IGrowable g)) return false;
        try {
            World w = world(level);
            return g.canUseBonemeal(w, w.rand, pos(pos), state(state));
        } catch (RuntimeException | LinkageError e) {
            failure("canUseBonemeal", e);
            return false;
        }
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (!(legacy instanceof legacyconnector.v1_12_2.net.minecraft.block.IGrowable g)) return;
        try {
            World w = world(level);
            g.grow(w, w.rand, pos(pos), state(state));
        } catch (RuntimeException | LinkageError e) {
            failure("grow", e);
        }
    }

    // ------------------------------------------------------------------ shape, pistons, breaking speed

    private final net.minecraft.world.phys.shapes.VoxelShape[] staticShapes = new net.minecraft.world.phys.shapes.VoxelShape[16];
    private final boolean[] staticKnown = new boolean[16];

    /** The legacy block's state-only bounds as a shape (null: none, keep the modern one). */
    private net.minecraft.world.phys.shapes.VoxelShape staticShape(BlockState state) {
        int m = state.getValue(META);
        if (staticKnown[m]) return staticShapes[m];
        net.minecraft.world.phys.shapes.VoxelShape shape = null;
        try {
            var b = legacy.mcc$staticBounds(state(state));
            if (b != null) {
                shape = b.minX <= 0 && b.minY <= 0 && b.minZ <= 0 && b.maxX >= 1 && b.maxY >= 1 && b.maxZ >= 1 ? net.minecraft.world.phys.shapes.Shapes.block()
                        : net.minecraft.world.phys.shapes.Shapes.box(Math.max(0, b.minX), Math.max(0, b.minY), Math.max(0, b.minZ), Math.min(1, b.maxX),
                                Math.min(1, b.maxY), Math.min(1, b.maxZ));
            }
        } catch (RuntimeException | LinkageError e) {
            shape = null;
        }
        staticShapes[m] = shape;
        staticKnown[m] = true;
        return shape;
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter getter, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        if (bounds && !IN_LEGACY_SHAPE.get()[0]) {
            var b = legacyBox(state, getter, pos, false);
            if (b != null) return b;
        }
        var s = staticShape(state);
        return s != null ? s : super.getShape(state, getter, pos, context);
    }

    /** A legacy block-relative box (getBoundingBox or getCollisionBoundingBox at the position) as a shape; null when it cannot be had. */
    private net.minecraft.world.phys.shapes.VoxelShape legacyBox(BlockState state, net.minecraft.world.level.BlockGetter getter, BlockPos pos, boolean collision) {
        boolean[] guard = IN_LEGACY_SHAPE.get();
        guard[0] = true;
        try {
            var access = WorldBridge1122.access(getter);
            var ls = state(state);
            var b = collision ? legacy.getCollisionBoundingBox(ls, access, pos(pos)) : legacy.getBoundingBox(ls, access, pos(pos));
            if (b == null) return net.minecraft.world.phys.shapes.Shapes.empty();
            if (b.minX <= 0 && b.minY <= 0 && b.minZ <= 0 && b.maxX >= 1 && b.maxY >= 1 && b.maxZ >= 1 && b.maxX <= 1 && b.maxY <= 1 && b.maxZ <= 1) {
                return net.minecraft.world.phys.shapes.Shapes.block();
            }
            return net.minecraft.world.phys.shapes.Shapes.create(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
        } catch (RuntimeException | LinkageError e) {
            failure(collision ? "getCollisionBoundingBox" : "getBoundingBox", e);
            return null;
        } finally {
            guard[0] = false;
        }
    }

    /** Several collision boxes (machines, pipes, frames): what the mod's addCollisionBoxToList adds for a mask around the block. */
    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter getter, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        if (collisionBox && !collisionList && !IN_LEGACY_SHAPE.get()[0]) {
            var b = legacyBox(state, getter, pos, true);
            if (b != null) return b;
        }
        if (!collisionList || !(getter instanceof Level level) || IN_LEGACY_SHAPE.get()[0]) return super.getCollisionShape(state, getter, pos, context);
        List<legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB> boxes = new ArrayList<>();
        var mask = new legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 2,
                pos.getY() + 2, pos.getZ() + 2);
        var entity = context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext ec && ec.getEntity() != null ? entity(ec.getEntity()) : null;
        boolean[] guard = IN_LEGACY_SHAPE.get();
        guard[0] = true;
        try {
            legacy.addCollisionBoxToList(state(state), world(level), pos(pos), mask, boxes, entity, true);
        } catch (RuntimeException | LinkageError e) {
            failure("addCollisionBoxToList", e);
            return super.getCollisionShape(state, getter, pos, context);
        } finally {
            guard[0] = false;
        }
        var shape = net.minecraft.world.phys.shapes.Shapes.empty();
        for (var b : boxes) {
            if (b == null) continue;
            shape = net.minecraft.world.phys.shapes.Shapes.or(shape, net.minecraft.world.phys.shapes.Shapes.create(b.minX - pos.getX(), b.minY - pos.getY(),
                    b.minZ - pos.getZ(), b.maxX - pos.getX(), b.maxY - pos.getY(), b.maxZ - pos.getZ()));
        }
        return shape;
    }

    @Override
    public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) {
        if (!pushReaction) return super.getPistonPushReaction(state);
        try {
            var r = legacy.getPushReaction(state(state));
            return r == null ? super.getPistonPushReaction(state) : r.mcc$modern();
        } catch (RuntimeException | LinkageError e) {
            return super.getPistonPushReaction(state);
        }
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, net.minecraft.world.level.BlockGetter getter, BlockPos pos) {
        boolean[] guard = IN_LEGACY_HARDNESS.get();
        if (relativeHardness && getter instanceof Level level && !guard[0]) {
            guard[0] = true;
            try {
                return legacy.getPlayerRelativeBlockHardness(state(state), player(player), world(level), pos(pos));
            } catch (RuntimeException | LinkageError e) {
                failure("getPlayerRelativeBlockHardness", e);
            } finally {
                guard[0] = false;
            }
        }
        return super.getDestroyProgress(state, player, getter, pos);
    }

    // ------------------------------------------------------------------ redstone

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        if (!comparator) return false;
        try {
            return legacy.hasComparatorInputOverride(state(state));
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        try {
            return legacy.getComparatorInputOverride(state(state), world(level), pos(pos));
        } catch (RuntimeException | LinkageError e) {
            failure("getComparatorInputOverride", e);
            return 0;
        }
    }

    // ------------------------------------------------------------------ drops

    /** The drops the legacy block gives at a position (its default rule unless the mod changed the parts it is made of). */
    public List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> legacyDrops(World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            IBlockState state, int fortune) {
        try {
            if (dropsList) {
                legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> drops = legacyconnector.v1_12_2.net.minecraft.util.NonNullList.create();
                legacy.getDrops(drops, world, pos, state, fortune);
                return drops;
            }
            return legacy.mcc$defaultDrops(state, fortune);
        } catch (RuntimeException | LinkageError e) {
            failure("getDrops", e);
            return List.of();
        }
    }

    @Override
    protected List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ServerLevel level = params.getLevel();
        Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin == null) return List.of();
        BlockPos pos = BlockPos.containing(origin);
        net.minecraft.world.item.ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        int fortune = 0;
        boolean silk = false;
        if (tool != null && !tool.isEmpty()) {
            var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            fortune = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.FORTUNE), tool);
            silk = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.SILK_TOUCH), tool) > 0;
        }
        World w = world(level);
        IBlockState legacyState = state(state);
        Entity breaker = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
        List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> drops;
        try {
            if (silk && legacy.canSilkHarvest(w, pos(pos), legacyState, breaker instanceof Player p ? player(p) : null)) {
                drops = new ArrayList<>();
                drops.add(legacy.mcc$silkTouchDrop(legacyState));
            } else {
                drops = legacyDrops(w, pos(pos), legacyState, fortune);
            }
        } catch (RuntimeException | LinkageError e) {
            failure("getDrops", e);
            return List.of();
        }
        List<net.minecraft.world.item.ItemStack> out = new ArrayList<>();
        for (var s : drops) if (s != null && !s.isEmpty()) out.add(Stacks1122.toModern(s));
        return out;
    }

    /** A mod's own harvestBlock replaces the modern harvesting (exhaustion and drops), as in 1.12. */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, net.minecraft.world.item.ItemStack tool) {
        if (!harvest) {
            super.playerDestroy(level, player, pos, state, blockEntity, tool);
            return;
        }
        try {
            var te = blockEntity instanceof Legacy1122BlockEntity host ? host.legacy() : null;
            legacy.harvestBlock(world(level), player(player), pos(pos), state(state), te, Stacks1122.view(tool));
        } catch (RuntimeException | LinkageError e) {
            failure("harvestBlock", e);
        } finally {
            Stacks1122.flushViews();
        }
    }

    @Override
    public net.minecraft.world.item.ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (pickBlock && level instanceof Level l) {
            try {
                var hit = new legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult(
                        new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                        legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP, pos(pos));
                var s = legacy.getPickBlock(state(state), hit, world(l), pos(pos), null);
                if (s != null && !s.isEmpty()) return Stacks1122.toModern(s);
            } catch (RuntimeException | LinkageError e) {
                failure("getPickBlock", e);
            }
        }
        if (pickItem && level instanceof Level l) {
            try {
                var s = legacy.getItem(world(l), pos(pos), state(state));
                if (s != null && !s.isEmpty()) return Stacks1122.toModern(s);
            } catch (RuntimeException | LinkageError e) {
                failure("getItem", e);
            }
        }
        return super.getCloneItemStack(level, pos, state);
    }

    // ------------------------------------------------------------------ hooks taking an IBlockAccess

    @Override
    protected boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        if (!replaceable) return super.canBeReplaced(state, context);
        try {
            return legacy.isReplaceable(world(context.getLevel()), pos(context.getClickedPos()));
        } catch (RuntimeException | LinkageError e) {
            failure("isReplaceable", e);
            return super.canBeReplaced(state, context);
        }
    }

    @Override
    public int getLightEmission(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (!lightValue) return super.getLightEmission(state, level, pos);
        try {
            return Math.max(0, Math.min(15, legacy.getLightValue(state(state), WorldBridge1122.access(level), pos(pos))));
        } catch (RuntimeException | LinkageError e) {
            failure("getLightValue", e);
            return super.getLightEmission(state, level, pos);
        }
    }

    @Override
    protected int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (!lightOpacity) return super.getLightBlock(state, level, pos);
        try {
            return Math.max(0, Math.min(15, legacy.getLightOpacity(state(state), WorldBridge1122.access(level), pos(pos))));
        } catch (RuntimeException | LinkageError e) {
            failure("getLightOpacity", e);
            return super.getLightBlock(state, level, pos);
        }
    }

    @Override
    public float getFriction(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
        if (!slipperiness) return super.getFriction(state, level, pos, entity);
        try {
            return legacy.getSlipperiness(state(state), WorldBridge1122.access(level), pos(pos), entity == null ? null : entity(entity));
        } catch (RuntimeException | LinkageError e) {
            failure("getSlipperiness", e);
            return super.getFriction(state, level, pos, entity);
        }
    }

    @Override
    public float getExplosionResistance(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        if (!explosionResistance || !(level instanceof Level l)) return super.getExplosionResistance(state, level, pos, explosion);
        try {
            var source = explosion == null ? null : explosion.getDirectSourceEntity();
            // the explosion math is the same in both versions: (resistance + 0.3) * 0.3 per step
            return legacy.getExplosionResistance(world(l), pos(pos), source == null ? null : entity(source), null);
        } catch (RuntimeException | LinkageError e) {
            failure("getExplosionResistance", e);
            return super.getExplosionResistance(state, level, pos, explosion);
        }
    }

    // ------------------------------------------------------------------ redstone output (buttons, levers, mod emitters)

    @Override
    protected boolean isSignalSource(BlockState state) {
        if (!powerSource) return super.isSignalSource(state);
        try {
            return legacy.canProvidePower(state(state));
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        if (!weakPower) return super.getSignal(state, level, pos, direction);
        try {
            return Math.max(0, Math.min(15, legacy.getWeakPower(state(state), WorldBridge1122.access(level), pos(pos), EnumFacing.mcc$of(direction))));
        } catch (RuntimeException | LinkageError e) {
            failure("getWeakPower", e);
            return 0;
        }
    }

    @Override
    protected int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        if (!strongPower) return super.getDirectSignal(state, level, pos, direction);
        try {
            return Math.max(0, Math.min(15, legacy.getStrongPower(state(state), WorldBridge1122.access(level), pos(pos), EnumFacing.mcc$of(direction))));
        } catch (RuntimeException | LinkageError e) {
            failure("getStrongPower", e);
            return 0;
        }
    }

    /** The experience a mod's block drops when broken (Forge's getExpDrop, with the tool's fortune). */
    @Override
    public int getExpDrop(BlockState state, LevelAccessor level, BlockPos pos, BlockEntity blockEntity, Entity breaker, net.minecraft.world.item.ItemStack tool) {
        if (!expDrop) return super.getExpDrop(state, level, pos, blockEntity, breaker, tool);
        try {
            int fortune = 0;
            if (tool != null && !tool.isEmpty() && level instanceof Level l) {
                var enchants = l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                fortune = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.FORTUNE), tool);
            }
            return Math.max(0, legacy.getExpDrop(state(state), WorldBridge1122.access(level), pos(pos), fortune));
        } catch (RuntimeException | LinkageError e) {
            failure("getExpDrop", e);
            return 0;
        }
    }
}
