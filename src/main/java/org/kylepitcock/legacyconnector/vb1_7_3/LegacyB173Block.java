package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The modern block of a b1.7.3 mod block. The legacy 4-bit metadata is one property {@link #META} (as for 1.12.2); hardness, resistance,
 * light, sound, opacity and collision come from the legacy block when it is registered, its bounds (set in its constructor) are the shape,
 * and its drops are asked from the legacy block ({@code getDroppedItemId/Count/Meta}), as b1.7.3's {@code dropStacks} did.
 */
public class LegacyB173Block extends net.minecraft.world.level.block.Block implements net.minecraft.world.level.block.BucketPickup {
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);
    private static final Random RANDOM = new Random();

    private final Block legacy;
    private final VoxelShape shape;
    private int failures;

    public LegacyB173Block(Block legacy, Properties properties) {
        super(nativeProperties(legacy,properties));
        this.legacy = legacy;
        if(legacy.getRenderType()==14)BedsB173.install();
        if(legacy instanceof legacyconnector.vb1_7_3.net.minecraft.block.LiquidBlock&&legacy.material==legacyconnector.vb1_7_3.net.minecraft.block.material.Material.WATER)LegacyWaterB173.install();
        this.shape = shapeOf(legacy);
        this.ownHarvest = overrides(legacy, "afterBreak", World.class, legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity.class,
                int.class, int.class, int.class, int.class);
        registerDefaultState(stateDefinition.any().setValue(META, 0));
    }

    private static Properties nativeProperties(Block legacy,Properties properties){
        properties.dynamicShape();
        // Dynamic states have no native collision cache, so calculateSolid otherwise returns false.
        // Beta's material movement flag is independent of opacity, metadata and collision shape.
        // Keep native heightmaps/fluid solidity in that material lane without changing entity collisions.
        if(legacy.material!=null&&legacy.material.blocksMovement())properties.forceSolidOn();
        return properties;
    }

    private static VoxelShape shapeOf(Block legacy) {
        double x0 = legacy.minX, y0 = legacy.minY, z0 = legacy.minZ, x1 = legacy.maxX, y1 = legacy.maxY, z1 = legacy.maxZ;
        if (x0 <= 0 && y0 <= 0 && z0 <= 0 && x1 >= 1 && y1 >= 1 && z1 >= 1) return Shapes.block();
        // A box with no extent on any axis has no outline and cannot be targeted in Beta (a mod sets it to hide a block such as a portal).
        if (x1 <= x0 && y1 <= y0 && z1 <= z0) return Shapes.empty();
        if (x1 <= x0 || y1 <= y0 || z1 <= z0) return Shapes.block();
        return Shapes.box(Math.max(0, x0), Math.max(0, y0), Math.max(0, z0), Math.min(1, x1), Math.min(1, y1), Math.min(1, z1));
    }

    public Block legacy() {
        return legacy;
    }

    @Override public boolean isBed(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.entity.LivingEntity entity){return BedsB173.isBed(state);}
    @Override public net.minecraft.core.Direction getBedDirection(BlockState state,net.minecraft.world.level.LevelReader level,BlockPos pos){return BedsB173.direction(state.getValue(META));}
    @Override public void setBedOccupied(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.LivingEntity entity,boolean occupied){
        if(BedsB173.isBed(state))level.setBlock(pos,state.setValue(META,BedsB173.occupied(state.getValue(META),occupied)),3);
    }
    @Override public java.util.Optional<net.minecraft.server.level.ServerPlayer.RespawnPosAngle> getRespawnPosition(BlockState state,net.minecraft.world.entity.EntityType<?> type,net.minecraft.world.level.LevelReader level,BlockPos pos,float yaw){
        if(!BedsB173.isBed(state))return java.util.Optional.empty();
        return net.minecraft.world.level.block.BedBlock.findStandUpPosition(type,level,pos,BedsB173.direction(state.getValue(META)),yaw)
                .map(p->net.minecraft.server.level.ServerPlayer.RespawnPosAngle.of(p,pos));
    }

    /** Native empty buckets (including stacked/creative buckets) execute the original Forge handler. */
    @Override public ItemStack pickupBlock(net.minecraft.world.entity.player.Player player,net.minecraft.world.level.LevelAccessor level,BlockPos pos,BlockState state){
        if(!(level instanceof net.minecraft.world.level.Level real))return ItemStack.EMPTY;
        return StacksB173.toModern(legacyconnector.vb1_7_3.net.minecraft.src.forge.MinecraftForge.fillCustomBucket(
                World.mcc$wrap(real),pos.getX(),pos.getY(),pos.getZ()));
    }
    @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound(){
        return java.util.Optional.of(legacy.material==legacyconnector.vb1_7_3.net.minecraft.block.material.Material.LAVA
                ? net.minecraft.sounds.SoundEvents.BUCKET_FILL_LAVA:net.minecraft.sounds.SoundEvents.BUCKET_FILL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(META);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return queryShape(state,level,pos,false);
    }
    @Override protected int getLightBlock(BlockState state,BlockGetter level,BlockPos pos) {
        // Beta's opacity table is independent of the opaque-cube flag (glass and leaves may absorb one level).
        return legacy==null?super.getLightBlock(state,level,pos):Math.clamp(Block.BLOCKS_LIGHT_OPACITY[legacy.id],0,15);
    }
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) {
        if(legacy==null)return super.getPistonPushReaction(state);
        return switch(legacy.getPistonBehavior()) {
            case 1 -> net.minecraft.world.level.material.PushReaction.DESTROY;
            case 2 -> net.minecraft.world.level.material.PushReaction.BLOCK;
            default -> net.minecraft.world.level.material.PushReaction.NORMAL;
        };
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return queryShape(state,level,pos,true);
    }
    private VoxelShape queryShape(BlockState state,BlockGetter level,BlockPos pos,boolean collision) {
        synchronized(legacy) {
            double[] bounds={legacy.minX,legacy.minY,legacy.minZ,legacy.maxX,legacy.maxY,legacy.maxZ};
            try {
                var world=new BlockQueryB173(level,pos,state);
                legacy.updateBoundingBox(world,pos.getX(),pos.getY(),pos.getZ());
                if(!collision && legacy.getRenderType()==2) {
                    return switch(state.getValue(META)) {
                        case 1 -> Shapes.box(0,0.2,0.35,0.3,0.8,0.65);
                        case 2 -> Shapes.box(0.7,0.2,0.35,1,0.8,0.65);
                        case 3 -> Shapes.box(0.35,0.2,0,0.65,0.8,0.3);
                        case 4 -> Shapes.box(0.35,0.2,0.7,0.65,0.8,1);
                        default -> Shapes.box(0.4,0,0.4,0.6,0.6,0.6);
                    };
                }
                // Beta targets (and so lets a player break) a block only when canCollideCheck(meta, false) holds: fluids and the like are not selectable.
                if(!collision && !legacy.hasCollision(state.getValue(META),false))return Shapes.empty();
                if(!collision && !(legacy instanceof legacyconnector.vb1_7_3.net.minecraft.block.StairsBlock)) {
                    VoxelShape outline=shapeOf(legacy);
                    // Beta outlines and targets a block by getBoundingBox (getSelectedBoundingBoxFromPool): none, or a box with no extent, hides it.
                    var selected=legacy.getBoundingBox(world,pos.getX(),pos.getY(),pos.getZ());
                    if(selected==null||selected.maxX<=selected.minX&&selected.maxY<=selected.minY&&selected.maxZ<=selected.minZ)return Shapes.empty();
                    return outline;
                }
                var boxes=new ArrayList<legacyconnector.vb1_7_3.net.minecraft.util.math.Box>();
                legacy.addIntersectingBoundingBox(world,pos.getX(),pos.getY(),pos.getZ(),
                        legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(pos.getX()-2,pos.getY()-2,pos.getZ()-2,pos.getX()+3,pos.getY()+3,pos.getZ()+3),boxes);
                VoxelShape result=Shapes.empty();
                for(var b:boxes)result=Shapes.or(result,Shapes.create(b.mcc$box().move(-pos.getX(),-pos.getY(),-pos.getZ())));
                return result;
            } catch(RuntimeException | LinkageError e) { call("shape",()->{throw e;});return shape; }
            finally { legacy.minX=bounds[0];legacy.minY=bounds[1];legacy.minZ=bounds[2];legacy.maxX=bounds[3];legacy.maxY=bounds[4];legacy.maxZ=bounds[5]; }
        }
    }

    /** b1.7.3 glass ({@code BlockBreakable} built with {@code transparent} false) draws no faces against the same block. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction direction) {
        if (adjacent.is(this) && legacy instanceof legacyconnector.vb1_7_3.net.minecraft.block.TranslucentBlock t && !t.mcc$drawsFacesBetweenSame()) {
            return true;
        }
        return super.skipRendering(state, adjacent, direction);
    }

    // ------------------------------------------------------------------------------------------------- legacy callbacks

    private static final ThreadLocal<int[]> PLACED_SIDE = new ThreadLocal<>();
    private final boolean ownHarvest;

    private boolean ownHarvest() {
        return ownHarvest;
    }

    private static boolean overrides(Block legacy, String name, Class<?>... params) {
        try {
            return legacy.getClass().getMethod(name, params).getDeclaringClass() != Block.class;
        } catch (NoSuchMethodException | RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** The legacy view of the level, also made the legacy client's current world (mods read {@code mc.theWorld}). */
    private static World view(net.minecraft.world.level.Level level) {
        World w = World.mcc$wrap(level);
        legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$use(w);
        return w;
    }

    /** Runs a legacy callback; failures are logged for the first few times per block and otherwise ignored, as a broken hook would be. */
    private void call(String what, Runnable action) {
        try (var views=EntityViewsB173.callbackScope()) {
            action.run();
        } catch (RuntimeException | LinkageError e) {
            CALLBACK_FAILURES.incrementAndGet();
            int n = failures++;
            if (n == 0) org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("{} of {} failed", what, legacy, e);
            else if (n < 3) org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("{} of {} failed: {}", what, legacy, e.toString());
        }
    }

    /** Failed legacy callbacks of all b1.7.3 blocks (the self test reports it). */
    public static final java.util.concurrent.atomic.AtomicInteger CALLBACK_FAILURES = new java.util.concurrent.atomic.AtomicInteger();

    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level.isClientSide || oldState.is(this)) return;
        if (addedEarly(level, pos)) return; // setPlacedBy already ran onBlockAdded, in Beta's order
        ADDED_BY_PLACE.put(level.dimension().location() + "@" + pos.asLong(), level.getGameTime());
        // NeoForge runs onPlace after the item's setPlacedBy (CommonHooks.onPlaceItemIntoWorld), where Beta ran onBlockAdded first: the
        // tile entity onBlockPlaced already configured (BuildCraft's engine orientation) must survive onBlockAdded making a fresh one.
        var host = level.getBlockEntity(pos) instanceof LegacyB173BlockEntity h ? h : null;
        var existing = host == null ? null : host.legacyIfPresent();
        call("onBlockAdded", () -> legacy.onPlaced(view(level), pos.getX(), pos.getY(), pos.getZ()));
        if (existing != null && host.legacyIfPresent() != existing && host.legacyIfPresent() != null
                && host.legacyIfPresent().getClass() == existing.getClass()) host.setLegacy(existing);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && !newState.is(this)) {
            call("onBlockRemoval", () -> legacy.onBreak(view(level), pos.getX(), pos.getY(), pos.getZ()));
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected void neighborChanged(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.level.block.Block neighbor,
            BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved);
        if (level.isClientSide) return;
        int neighborId = neighborId(neighbor,level.getBlockState(neighborPos));
        call("onNeighborBlockChange", () -> legacy.neighborUpdate(view(level), pos.getX(), pos.getY(), pos.getZ(), neighborId));
        if(legacy instanceof legacyconnector.vb1_7_3.net.minecraft.block.DoorBlock door && level.getBlockState(pos).is(this)
                && neighbor.defaultBlockState().isSignalSource()) {
            int y=pos.getY()-((level.getBlockState(pos).getValue(META)&8)!=0?1:0);
            call("door power",()->door.setOpen(view(level),pos.getX(),y,pos.getZ(),level.hasNeighborSignal(new BlockPos(pos.getX(),y,pos.getZ())) || level.hasNeighborSignal(new BlockPos(pos.getX(),y+1,pos.getZ()))));
        }
    }

    static int neighborId(net.minecraft.world.level.block.Block reportedSource,BlockState current) {
        // Modern setBlock reports the OLD block to its neighbours; Beta reported the newly placed id.
        // Keep explicit removal-source notifications (notably power sources) when the cell is now empty.
        return BlocksB173.legacyId(current.isAir()?reportedSource.defaultBlockState():current);
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        World w = view(level);
        try { call("updateTick", () -> legacy.onTick(w, pos.getX(), pos.getY(), pos.getZ(), w.random)); }
        finally { EntityViewsB173.flush(); }
    }

    @Override
    protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        tick(state,level,pos,random);
    }

    @Override
    public void animateTick(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        World w = view(level);
        call("randomDisplayTick", () -> legacy.randomDisplayTick(w, pos.getX(), pos.getY(), pos.getZ(), w.random));
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        PlayerViewB173 view = PlayerViewB173.of(player);
        boolean[] used = new boolean[1];
        // The clicking player is the legacy client's player for this callback (mods read mc.thePlayer), even when not in the level's list yet.
        // A GUI the block opens (displayGuiScreen) must reach the server's menu path, as it does for an item use.
        Runnable activate = () -> { World w = view(level); try (var ctx = legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(w, view)) {
            used[0] = legacy.onUse(w, pos.getX(), pos.getY(), pos.getZ(), view); } };
        call("blockActivated", () -> {
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) MenusB173.withActor(serverPlayer, activate);
            else activate.run();
        });
        if (legacy instanceof legacyconnector.vb1_7_3.net.minecraft.block.BlockWithEntity) {
            MenusB173.trace("right click on {} ({}) on the {} side: {}", legacy.getClass().getName(), state.getValue(META),
                    level.isClientSide ? "client" : "server", used[0] ? "used" : "not used");
        }
        if (!level.isClientSide) view.writeBack();
        return used[0] ? net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide) : net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    protected void attack(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        super.attack(state, level, pos, player);
        if (level.isClientSide) return;
        PlayerViewB173 view = PlayerViewB173.of(player);
        World world=view(level);
        try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(world,view)){
            call("onBlockClicked", () -> legacy.onBlockBreakStart(world, pos.getX(), pos.getY(), pos.getZ(), view));
        }
        view.writeBack();
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        int side = context.getClickedFace().get3DDataValue();
        boolean[] ok = {true};
        call("canPlaceBlockOnSide", () -> ok[0] = legacy.canPlaceAt(view(context.getLevel()), pos.getX(), pos.getY(), pos.getZ(), side));
        if (!ok[0]) return null;
        PLACED_SIDE.set(new int[] {side});
        BlockState state = super.getStateForPlacement(context);
        // b1.7.3's ItemBlock set block and metadata in one go (getPlacedBlockMetadata of the stack's damage), so onBlockAdded and the tile
        // entity it makes see the subtype (Minefactory's translocator and crafter are metadata 7 and 8 of one block)
        if (state != null && context.getItemInHand().getItem() instanceof LegacyB173BlockItem) {
            int damage = LegacyB173BlockItem.meta(context.getItemInHand());
            int[] meta = {damage};
            var item = legacy.id >= 0 && legacy.id < legacyconnector.vb1_7_3.net.minecraft.item.Item.ITEMS.length
                    ? legacyconnector.vb1_7_3.net.minecraft.item.Item.ITEMS[legacy.id] : null;
            if (item != null) call("getPlacedBlockMetadata", () -> meta[0] = item.getPlacementMetadata(damage));
            state = state.setValue(META, meta[0] & 15);
        }
        return state;
    }

    private static final java.util.Map<String, Long> ADDED_BY_PLACE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, Long> ADDED_EARLY = new java.util.concurrent.ConcurrentHashMap<>();

    /** True once, for the onPlace that follows a setPlacedBy of the same position within the same tick or the next. */
    private static boolean addedEarly(net.minecraft.world.level.Level level, BlockPos pos) {
        Long at = ADDED_EARLY.remove(level.dimension().location() + "@" + pos.asLong());
        return at != null && level.getGameTime() - at <= 1;
    }

    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        int[] side = PLACED_SIDE.get();
        PLACED_SIDE.remove();
        if (level.isClientSide) return;
        World w = view(level);
        // Beta ran onBlockAdded (which makes the tile entity of many blocks) before onBlockPlaced and onBlockPlacedBy; NeoForge runs onPlace after setPlacedBy
        Long placed = ADDED_BY_PLACE.remove(level.dimension().location() + "@" + pos.asLong());
        if ((placed == null || level.getGameTime() - placed > 1) && level.getBlockState(pos).is(this)) {
            ADDED_EARLY.put(level.dimension().location() + "@" + pos.asLong(), level.getGameTime());
            call("onBlockAdded", () -> legacy.onPlaced(view(level), pos.getX(), pos.getY(), pos.getZ()));
        }
        if (side != null) call("onBlockPlaced", () -> legacy.onPlaced(w, pos.getX(), pos.getY(), pos.getZ(), side[0]));
        if (placer instanceof net.minecraft.world.entity.player.Player player) {
            PlayerViewB173 view = PlayerViewB173.of(player);
            call("onBlockPlacedBy", () -> legacy.onPlaced(w, pos.getX(), pos.getY(), pos.getZ(), view));
            view.writeBack();
        }
    }

    @Override
    public void stepOn(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide) return;
        var view=EntityViewsB173.wrap(entity);
        World world=view(level);
        try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(world,view)) { call("onEntityWalking", () -> legacy.onSteppedOn(world, pos.getX(), pos.getY(), pos.getZ(), view)); }
        finally { EntityViewsB173.flush(); }
    }

    @Override
    protected void entityInside(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide) {
            if (legacy instanceof legacyconnector.vb1_7_3.net.minecraft.block.PortalBlock portal
                    && !entity.isPassenger() && !entity.isVehicle()
                    && net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
                call("portalPresentation", () -> org.kylepitcock.legacyconnector.vb1_7_3.client.DimensionClientB173.portalContact(entity,portal.getDimNumber()));
            return;
        }
        var view=EntityViewsB173.wrap(entity);
        World world=view(level);
        try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(world,view)) { call("onEntityCollidedWithBlock", () -> legacy.onEntityCollision(world, pos.getX(), pos.getY(), pos.getZ(), view)); }
        finally { EntityViewsB173.flush(); }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        try {
            return legacy.canEmitRedstonePower();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        if (!(level instanceof net.minecraft.world.level.Level l)) return 0;
        boolean[] on = new boolean[1];
        call("isPoweringTo", () -> on[0] = legacy.isEmittingRedstonePowerInDirection(view(l), pos.getX(), pos.getY(), pos.getZ(), direction.get3DDataValue()));
        return on[0] ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        if (!(level instanceof net.minecraft.world.level.Level l)) return 0;
        boolean[] on = new boolean[1];
        call("isIndirectlyPoweringTo", () -> on[0] = legacy.canTransferPowerInDirection(view(l), pos.getX(), pos.getY(), pos.getZ(), direction.get3DDataValue()));
        return on[0] ? 15 : 0;
    }

    /**
     * A player broke the block: b1.7.3 then called {@code onBlockDestroyedByPlayer} and {@code harvestBlock}. A mod's own harvestBlock does
     * its drops itself (they replace the modern ones); otherwise the modern drops are the legacy block's ({@link #getDrops}).
     */
    @Override
    public void playerDestroy(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, BlockPos pos, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity blockEntity, ItemStack tool) {
        int meta = state.getValue(META);
        World w = view(level);
        PlayerViewB173 view = PlayerViewB173.of(player);
        try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(w,view)){
            call("onBlockDestroyedByPlayer", () -> legacy.onMetadataChange(w, pos.getX(), pos.getY(), pos.getZ(), meta));
            if (ownHarvest()) {
                try(var award=StatisticsB173.nativeAward(player,net.minecraft.stats.Stats.BLOCK_MINED.get(this))){
                    call("harvestBlock", () -> legacy.afterBreak(w, view, pos.getX(), pos.getY(), pos.getZ(), meta));
                }
                view.writeBack();
                player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
                player.causeFoodExhaustion(0.005F);
                return;
            }
            super.playerDestroy(level, player, pos, state, blockEntity, tool);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int meta = state.getValue(META);
        List<ItemStack> out = new ArrayList<>(1);
        try {
            int count = legacy.getDroppedItemCount(RANDOM);
            for (int i = 0; i < count; i++) {
                int id = legacy.getDroppedItemId(meta, RANDOM);
                if (id <= 0) continue;
                ItemStack stack = StacksB173.toModern(id, 1, legacy.getDroppedItemMeta(meta));
                if (stack.isEmpty()) continue;
                boolean merged = false;
                for (ItemStack s : out) {
                    if (ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize()) {
                        s.grow(1);
                        merged = true;
                        break;
                    }
                }
                if (!merged) out.add(stack);
            }
        } catch (RuntimeException | LinkageError e) {
            if (failures++ < 3) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Drops of {} failed: {}", legacy, e.toString());
            }
        }
        return out;
    }
}
