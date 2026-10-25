package org.kylepitcock.legacyconnector.v1_7_10.adapter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockAccessView;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/**
 * A real 1.21.1 block standing in for a 1.7.10 mod block. Metadata becomes the {@link #META} block state property;
 * every gameplay hook is forwarded to the legacy block using legacy coordinates, sides and world views.
 *
 * <p>Gameplay semantics that changed and are translated here:</p>
 * <ul>
 *     <li>right click: legacy {@code onBlockActivated} ran before item use regardless of the held item; modern splits
 *     this into {@code useItemOn}/{@code useWithoutItem}, both routed to the one legacy method;</li>
 *     <li>drops: legacy blocks compute drops in code ({@code getDrops}), so loot tables are bypassed;
 *     silk touch and fortune are read from the tool's enchantment components;</li>
 *     <li>explosion resistance: legacy stored 3x the configured value and divided by 5 when used;</li>
 *     <li>random ticks: legacy random ticks called {@code updateTick}, same as scheduled ticks;</li>
 *     <li>shapes: legacy block bounds (possibly state dependent) become voxel shapes.</li>
 * </ul>
 */
public class LegacyBlockAdapter extends net.minecraft.world.level.block.Block {
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    private final Block legacy;
    /** The deepslate twin of an ore block (same legacy block, deepslate texture), or null; set by BlockBridge. */
    private LegacyBlockAdapter deepVariant;
    private LegacyBlockAdapter deepBase;
    private final boolean dynamicBounds;
    private final boolean customHardness;
    private final VoxelShape[] staticShapes = new VoxelShape[16];
    private final boolean collisionList;
    private final boolean ladder;
    private final boolean pickBlock;
    private final boolean fireSource;
    private final boolean entityDestroy;
    private final boolean customRemoval;
    private final boolean sustainsPlants;
    private final boolean customExplosion;
    private final boolean enchantBonus;

    protected LegacyBlockAdapter(Block legacy, BlockBehaviour.Properties properties) {
        super(properties);
        this.legacy = legacy;
        this.dynamicBounds = overrides(legacy, "setBlockBoundsBasedOnState", legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess.class, int.class, int.class, int.class);
        this.customHardness = overrides(legacy, "getPlayerRelativeBlockHardness", EntityPlayer.class, World.class, int.class, int.class, int.class)
                || overrides(legacy, "getBlockHardness", World.class, int.class, int.class, int.class);
        this.collisionList = overrides(legacy, "addCollisionBoxesToList", World.class, int.class, int.class, int.class, AxisAlignedBB.class,
                java.util.List.class, legacyconnector.v1_7_10.net.minecraft.entity.Entity.class);
        this.ladder = overrides(legacy, "isLadder", legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess.class, int.class, int.class, int.class,
                EntityLivingBase.class);
        this.pickBlock = overrides(legacy, "getPickBlock", legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition.class, World.class, int.class, int.class, int.class)
                || overrides(legacy, "getPickBlock", legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition.class, World.class, int.class, int.class, int.class,
                        EntityPlayer.class);
        this.fireSource = overrides(legacy, "isFireSource", World.class, int.class, int.class, int.class,
                legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection.class);
        this.entityDestroy = overrides(legacy, "canEntityDestroy", legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess.class, int.class, int.class,
                int.class, legacyconnector.v1_7_10.net.minecraft.entity.Entity.class);
        this.customRemoval = overrides(legacy, "removedByPlayer", World.class, EntityPlayer.class, int.class, int.class, int.class)
                || overrides(legacy, "removedByPlayer", World.class, EntityPlayer.class, int.class, int.class, int.class, boolean.class);
        this.sustainsPlants = overrides(legacy, "canSustainPlant", legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess.class, int.class, int.class,
                int.class, legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection.class,
                legacyconnector.v1_7_10.net.minecraftforge.common.IPlantable.class);
        this.customExplosion = overrides(legacy, "onBlockExploded", World.class, int.class, int.class, int.class,
                legacyconnector.v1_7_10.net.minecraft.world.Explosion.class);
        this.enchantBonus = overrides(legacy, "getEnchantPowerBonus", World.class, int.class, int.class, int.class);
        registerDefaultState(stateDefinition.any().setValue(META, 0));
    }

    public static LegacyBlockAdapter create(Block legacy) {
        BlockBehaviour.Properties p = properties(legacy);
        boolean te = legacy instanceof legacyconnector.v1_7_10.net.minecraft.block.ITileEntityProvider
                || overrides(legacy, "hasTileEntity", int.class) || overrides(legacy, "createTileEntity", World.class, int.class);
        return te ? new LegacyEntityBlockAdapter(legacy, p) : new LegacyBlockAdapter(legacy, p);
    }

    public static boolean overrides(Object obj, String name, Class<?>... params) {
        for (Class<?> c = obj.getClass(); c != null && c != Block.class && c != Object.class; c = c.getSuperclass()) {
            if (c.getName().startsWith("legacyconnector.")) return false; // shim classes are not mod overrides
            try {
                Method m = c.getDeclaredMethod(name, params);
                return m != null;
            } catch (NoSuchMethodException ignored) {
            } catch (LinkageError e) {
                // A declared method mentions a class that does not exist (an API the modset never provided): look in the superclass.
            }
        }
        return false;
    }

    private static BlockBehaviour.Properties properties(Block legacy) {
        Material material = legacy.getMaterial();
        // Mod materials may be built from map colours the shim does not provide yet; fall back to stone.
        var legacyColor = material == null ? null : material.getMaterialMapColor();
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().mapColor(legacyColor == null
                ? net.minecraft.world.level.material.MapColor.STONE : legacyColor.mcc$modern());
        float hardness = legacy.mcc$hardness();
        float resistance = legacy.getExplosionResistance(null);
        if (hardness < 0) p.strength(-1.0F, 3600000.0F);
        else p.strength(hardness, resistance);
        p.sound(sound(legacy.stepSound));
        int light = legacy.getLightValue();
        if (light > 0) p.lightLevel(s -> light);
        if (!legacy.isOpaqueCube()) {
            p.noOcclusion();
            p.isViewBlocking((s, g, pos) -> false);
            p.isSuffocating((s, g, pos) -> false);
            p.isRedstoneConductor((s, g, pos) -> false);
        }
        // Material.air blocks (Railcraft's hidden residual-heat markers, force fields): not solid, not selectable,
        // not rendered - they are air that carries a tile entity.
        if (material == Material.air) p.air();
        if (!material.blocksMovement()) p.noCollission();
        if (legacy.getTickRandomly()) p.randomTicks();
        if (material.isReplaceable()) p.replaceable();
        if (!material.isToolNotRequired()) p.requiresCorrectToolForDrops();
        if (material.getCanBurn()) p.ignitedByLava();
        if (material.isLiquid()) p.liquid();
        if (material.getMaterialMobility() == 1) p.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY);
        else if (material.getMaterialMobility() == 2) p.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK);
        p.friction(legacy.slipperiness);
        p.noLootTable(); // drops are computed by the legacy code in getDrops
        return p;
    }

    private static SoundType sound(Block.SoundType legacy) {
        if (legacy == null) return SoundType.STONE;
        return switch (legacy.soundName) {
            case "wood" -> SoundType.WOOD;
            case "gravel" -> SoundType.GRAVEL;
            case "grass" -> SoundType.GRASS;
            case "cloth" -> SoundType.WOOL;
            case "sand" -> SoundType.SAND;
            case "snow" -> SoundType.SNOW;
            case "ladder" -> SoundType.LADDER;
            case "anvil" -> SoundType.ANVIL;
            default -> legacy == Block.soundTypeGlass ? SoundType.GLASS : legacy.frequency > 1.2F ? SoundType.METAL : SoundType.STONE;
        };
    }

    /** As BlockBreakable.shouldSideBeRendered: two neighbouring glass-like blocks of the same kind draw no faces between them. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) && legacy instanceof legacyconnector.v1_7_10.net.minecraft.block.BlockBreakable bb && bb.mcc$hidesSimilarNeighbours()
                && adjacent.getValue(META).equals(state.getValue(META));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(META);
    }

    public Object legacy() {
        return legacy;
    }

    public LegacyBlockAdapter deepVariant() {
        return deepVariant;
    }

    /** Pairs an ore block with its deepslate twin. */
    public void linkDeepVariant(LegacyBlockAdapter variant) {
        this.deepVariant = variant;
        variant.deepBase = this;
    }

    /** Whether this is the deepslate twin of another block. */
    public boolean isDeepVariant() {
        return deepBase != null;
    }

    /** The block a deepslate twin stands for (itself otherwise). */
    public LegacyBlockAdapter baseBlock() {
        return deepBase != null ? deepBase : this;
    }

    public BlockState stateForMeta(int meta) {
        return defaultBlockState().setValue(META, meta & 15);
    }

    // ------------------------------------------------------------------ helpers

    protected static World world(LevelAccessor level) {
        return World.mcc$wrap(level);
    }

    protected static int meta(BlockState state) {
        return state.getValue(META);
    }

    private static EntityPlayer player(Player p) {
        return p == null ? null : (EntityPlayer) EntityBridge.wrap(p);
    }

    @Override
    public String getDescriptionId() {
        return legacy.getUnlocalizedName() + ".name";
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return activate(level, pos, player, hit) ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player, hit) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    private boolean activate(Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Vec3 h = hit.getLocation();
        return legacy.onBlockActivated(world(level), pos.getX(), pos.getY(), pos.getZ(), player(player), hit.getDirection().get3DDataValue(),
                (float) (h.x - pos.getX()), (float) (h.y - pos.getY()), (float) (h.z - pos.getZ()));
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        legacy.onBlockClicked(world(level), pos.getX(), pos.getY(), pos.getZ(), player(player));
    }

    // ------------------------------------------------------------------ lifecycle in the world

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        if (!oldState.is(this)) legacy.onBlockAdded(world(level), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!newState.is(this)) legacy.breakBlock(world(level), pos.getX(), pos.getY(), pos.getZ(), legacy, meta(state));
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighbor, BlockPos from, boolean moving) {
        try {
            legacy.onNeighborBlockChange(world(level), pos.getX(), pos.getY(), pos.getZ(), BlockBridge.legacyBlock(neighbor.defaultBlockState()));
        } catch (RuntimeException | LinkageError e) {
            legacyFailure("onNeighborBlockChange", e);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        World w = world(level);
        try {
            legacy.updateTick(w, pos.getX(), pos.getY(), pos.getZ(), w.rand);
        } catch (RuntimeException | LinkageError e) {
            legacyFailure("updateTick", e);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        World w = world(level);
        try {
            legacy.updateTick(w, pos.getX(), pos.getY(), pos.getZ(), w.rand);
        } catch (RuntimeException | LinkageError e) {
            legacyFailure("updateTick", e);
        }
    }

    /** Legacy block code throwing in a world callback must not crash the server; log it, a few times per block. */
    private void legacyFailure(String callback, Throwable e) {
        if (failures++ < 3) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Blocks").error("{} of {} threw; the call is skipped{}", callback, legacy,
                    failures == 3 ? " (further failures of this block are not logged)" : "", e);
        }
    }

    private int failures;

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        World w = world(level);
        legacy.randomDisplayTick(w, pos.getX(), pos.getY(), pos.getZ(), new Random(random.nextLong()));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        legacy.onBlockPlacedBy(world(level), pos.getX(), pos.getY(), pos.getZ(),
                placer == null ? null : (EntityLivingBase) EntityBridge.wrap(placer), ItemStack.mcc$wrap(stack));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        legacy.onBlockHarvested(world(level), pos.getX(), pos.getY(), pos.getZ(), meta(state), player(player));
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        legacy.onBlockDestroyedByPlayer(world(level), pos.getX(), pos.getY(), pos.getZ(), meta(state));
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        return legacy.onBlockEventReceived(world(level), pos.getX(), pos.getY(), pos.getZ(), id, param);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !(level instanceof LevelAccessor acc) || legacy.canBlockStay(world(acc), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return legacy.getTickRandomly();
    }

    // ------------------------------------------------------------------ entities

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        legacy.onEntityCollidedWithBlock(world(level), pos.getX(), pos.getY(), pos.getZ(), EntityBridge.wrap(entity));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        legacy.onEntityWalking(world(level), pos.getX(), pos.getY(), pos.getZ(), EntityBridge.wrap(entity));
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        legacy.onFallenUpon(world(level), pos.getX(), pos.getY(), pos.getZ(), EntityBridge.wrap(entity), distance);
        super.fallOn(level, state, pos, entity, distance);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter getter, BlockPos pos) {
        if (customHardness && getter instanceof LevelAccessor level) {
            return legacy.getPlayerRelativeBlockHardness(player(player), world(level), pos.getX(), pos.getY(), pos.getZ());
        }
        return super.getDestroyProgress(state, player, getter, pos);
    }

    // ------------------------------------------------------------------ redstone

    @Override
    protected boolean isSignalSource(BlockState state) {
        return legacy.canProvidePower();
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return legacy.isProvidingWeakPower(BlockAccessView.of(level), pos.getX(), pos.getY(), pos.getZ(), direction.get3DDataValue());
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return legacy.isProvidingStrongPower(BlockAccessView.of(level), pos.getX(), pos.getY(), pos.getZ(), direction.get3DDataValue());
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return legacy.hasComparatorInputOverride();
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return legacy.getComparatorInputOverride(world(level), pos.getX(), pos.getY(), pos.getZ(), 0);
    }

    // ------------------------------------------------------------------ shape and rendering

    /**
     * A render id above the vanilla ones with no ISimpleBlockRenderingHandler behind it draws nothing in 1.7.10 (BiblioCraft
     * gives its tile-entity-rendered blocks such ids): the block is invisible, not a cube.
     */
    public static boolean drawnByNobody(int type) {
        if (type <= 41) return false;
        try {
            return legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.mcc$blockHandler(type) == null;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        int type;
        try {
            type = legacy.getRenderType();
        } catch (RuntimeException | LinkageError e) {
            // Called by resource reloading and rendering; a throwing legacy block must not abort either.
            return RenderShape.MODEL;
        }
        if (type == -1 || drawnByNobody(type)) return RenderShape.INVISIBLE;
        // 22 = chest: drawn entirely by the tile entity renderer.
        return type == 22 ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (legacy.getMaterial() == Material.air) return Shapes.empty();
        if (!dynamicBounds) {
            int m = meta(state);
            VoxelShape s = staticShapes[m];
            if (s == null) staticShapes[m] = s = boundsShape();
            return s;
        }
        synchronized (legacy) {
            try {
                legacy.setBlockBoundsBasedOnState(BlockAccessView.of(level), pos.getX(), pos.getY(), pos.getZ());
                return boundsShape();
            } catch (RuntimeException | LinkageError e) {
                // Shapes are also computed without a world (state cache init, worldgen) where legacy code expects its
                // tile entity; a full block is the safe answer there.
                shapeFailure(e);
                return Shapes.block();
            }
        }
    }

    private boolean reportedShapeFailure;

    private void shapeFailure(Throwable e) {
        if (reportedShapeFailure) return;
        reportedShapeFailure = true;
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/Blocks").debug("Legacy bounds of {} failed outside a world context: {}", legacy, e.toString());
    }

    private VoxelShape boundsShape() {
        double x1 = legacy.getBlockBoundsMinX(), y1 = legacy.getBlockBoundsMinY(), z1 = legacy.getBlockBoundsMinZ();
        double x2 = legacy.getBlockBoundsMaxX(), y2 = legacy.getBlockBoundsMaxY(), z2 = legacy.getBlockBoundsMaxZ();
        if (x1 <= 0 && y1 <= 0 && z1 <= 0 && x2 >= 1 && y2 >= 1 && z2 >= 1) return Shapes.block();
        if (x2 <= x1 || y2 <= y1 || z2 <= z1) return Shapes.empty();
        return Shapes.box(Math.max(0, x1), Math.max(0, y1), Math.max(0, z1), Math.min(1, x2), Math.min(1, y2), Math.min(1, z2));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!hasCollision) return Shapes.empty();
        if (collisionList && level instanceof LevelAccessor acc) {
            // Several boxes (machines, frames): collect what the legacy block adds for a mask around the block.
            java.util.List<Object> boxes = new java.util.ArrayList<>();
            AxisAlignedBB mask = AxisAlignedBB.getBoundingBox(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
            synchronized (legacy) {
                try {
                    legacy.addCollisionBoxesToList(world(acc), pos.getX(), pos.getY(), pos.getZ(), mask, boxes, null);
                } catch (RuntimeException | LinkageError e) {
                    shapeFailure(e);
                    return Shapes.block();
                }
            }
            VoxelShape shape = Shapes.empty();
            for (Object o : boxes) {
                if (!(o instanceof AxisAlignedBB b)) continue;
                shape = Shapes.or(shape, Shapes.create(b.minX - pos.getX(), b.minY - pos.getY(), b.minZ - pos.getZ(),
                        b.maxX - pos.getX(), b.maxY - pos.getY(), b.maxZ - pos.getZ()));
            }
            return shape;
        }
        if (level instanceof LevelAccessor acc) {
            AxisAlignedBB box;
            synchronized (legacy) {
                try {
                    box = legacy.getCollisionBoundingBoxFromPool(world(acc), pos.getX(), pos.getY(), pos.getZ());
                } catch (RuntimeException | LinkageError e) {
                    shapeFailure(e);
                    return Shapes.block();
                }
            }
            if (box == null) return Shapes.empty();
            return Shapes.create(box.minX - pos.getX(), box.minY - pos.getY(), box.minZ - pos.getZ(),
                    box.maxX - pos.getX(), box.maxY - pos.getY(), box.maxZ - pos.getZ());
        }
        return getShape(state, level, pos, context);
    }

    // ------------------------------------------------------------------ Forge hooks

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        try {
            return legacy.getFlammability(BlockAccessView.of(level), pos.getX(), pos.getY(), pos.getZ(),
                    legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection.getOrientation(direction.get3DDataValue()));
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        try {
            return legacy.getFireSpreadSpeed(BlockAccessView.of(level), pos.getX(), pos.getY(), pos.getZ(),
                    legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection.getOrientation(direction.get3DDataValue()));
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getFlammability(state, level, pos, direction) > 0;
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        if (!ladder) return super.isLadder(state, level, pos, entity);
        try {
            return legacy.isLadder(BlockAccessView.of(level), pos.getX(), pos.getY(), pos.getZ(), (EntityLivingBase) EntityBridge.wrap(entity));
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
            net.minecraft.world.level.material.FluidState fluid) {
        if (!customRemoval) return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        try {
            return legacy.removedByPlayer(World.mcc$wrap(level), (EntityPlayer) EntityBridge.wrap(player), pos.getX(), pos.getY(), pos.getZ(), willHarvest);
        } catch (RuntimeException | LinkageError e) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }
    }

    @Override
    public net.neoforged.neoforge.common.util.TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos pos, Direction facing,
            BlockState plant) {
        if (!sustainsPlants || !(plant.getBlock() instanceof LegacyBlockAdapter adapter)
                || !(adapter.legacy() instanceof legacyconnector.v1_7_10.net.minecraftforge.common.IPlantable plantable)) {
            return super.canSustainPlant(state, level, pos, facing, plant);
        }
        try {
            boolean ok = legacy.canSustainPlant(BlockAccessView.of(level), pos.getX(), pos.getY(), pos.getZ(),
                    legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection.getOrientation(facing.get3DDataValue()), plantable);
            return ok ? net.neoforged.neoforge.common.util.TriState.TRUE : net.neoforged.neoforge.common.util.TriState.FALSE;
        } catch (RuntimeException | LinkageError e) {
            return net.neoforged.neoforge.common.util.TriState.DEFAULT;
        }
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        if (!customExplosion) {
            super.onBlockExploded(state, level, pos, explosion);
            return;
        }
        try {
            World world = World.mcc$wrap(level);
            var center = explosion.center();
            legacy.onBlockExploded(world, pos.getX(), pos.getY(), pos.getZ(),
                    new legacyconnector.v1_7_10.net.minecraft.world.Explosion(world, null, center.x, center.y, center.z, 4.0F));
        } catch (RuntimeException | LinkageError e) {
            super.onBlockExploded(state, level, pos, explosion);
        }
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        if (!enchantBonus || !(level instanceof LevelAccessor acc)) return super.getEnchantPowerBonus(state, level, pos);
        try {
            return legacy.getEnchantPowerBonus(world(acc), pos.getX(), pos.getY(), pos.getZ());
        } catch (RuntimeException | LinkageError e) {
            return 0.0F;
        }
    }

    // ------------------------------------------------------------------ drops

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
        int meta = meta(state);
        Entity breaker = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
        List<ItemStack> legacyDrops;
        if (silk && breaker instanceof Player p && legacy.canSilkHarvest(w, player(p), pos.getX(), pos.getY(), pos.getZ(), meta)) {
            legacyDrops = new ArrayList<>();
            ItemStack s = legacy.mcc$createStackedBlock(meta);
            if (s != null) legacyDrops.add(s);
        } else {
            legacyDrops = legacy.getDrops(w, pos.getX(), pos.getY(), pos.getZ(), meta, fortune);
        }
        List<net.minecraft.world.item.ItemStack> out = new ArrayList<>();
        if (legacyDrops != null) {
            for (ItemStack s : legacyDrops) if (s != null && s.mcc$count() > 0) out.add(s.mcc$modern());
        }
        return out;
    }

    @Override
    public net.minecraft.world.item.ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, LevelReader level, BlockPos pos,
            Player player) {
        if (pickBlock && level instanceof LevelAccessor acc && target instanceof net.minecraft.world.phys.BlockHitResult hit) {
            try {
                World w = world(acc);
                var loc = hit.getLocation();
                var mop = new legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition(pos.getX(), pos.getY(), pos.getZ(), hit.getDirection().get3DDataValue(),
                        legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(loc.x, loc.y, loc.z));
                ItemStack picked = legacy.getPickBlock(mop, w, pos.getX(), pos.getY(), pos.getZ(), (EntityPlayer) EntityBridge.wrap(player));
                return picked == null ? net.minecraft.world.item.ItemStack.EMPTY : picked.mcc$modern();
            } catch (RuntimeException | LinkageError e) {
                // fall through to the plain clone stack
            }
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    public boolean isFireSource(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos, net.minecraft.core.Direction side) {
        if (!fireSource || !(level instanceof LevelAccessor acc)) return super.isFireSource(state, level, pos, side);
        try {
            return legacy.isFireSource(world(acc), pos.getX(), pos.getY(), pos.getZ(), legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection.mcc$of(side));
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    public boolean canEntityDestroy(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (!entityDestroy) return super.canEntityDestroy(state, level, pos, entity);
        try {
            return legacy.canEntityDestroy(BlockAccessView.of(level), pos.getX(), pos.getY(), pos.getZ(), EntityBridge.wrap(entity));
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }

    @Override
    public net.minecraft.world.item.ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level instanceof LevelAccessor acc) {
            World w = world(acc);
            var item = legacy.getItem(w, pos.getX(), pos.getY(), pos.getZ());
            if (item != null) return new ItemStack(item, 1, legacy.getDamageValue(w, pos.getX(), pos.getY(), pos.getZ())).mcc$modern();
        }
        return super.getCloneItemStack(level, pos, state);
    }
}
