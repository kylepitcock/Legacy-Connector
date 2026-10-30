package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

/** 1.12.2 {@code Block}: a registry entry with a material, hardness/resistance, light and a creative tab. */
public class Block extends IForgeRegistryEntry.Impl<Block> {
    public static final legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespacedDefaultedByKey<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, Block> REGISTRY = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.view(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS, "air");
    protected final Material material;
    protected float blockHardness;
    protected float blockResistance;
    protected int lightValue;
    protected int lightOpacity = 255;
    private String unlocalizedName;
    private CreativeTabs displayOnCreativeTab;
    /** The modern block registered for this one (set by the bridge). */
    public Object mcc$modern;
    public static final legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB FULL_BLOCK_AABB = new legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    public static final legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB NULL_AABB = null;

    protected SoundType blockSoundType = SoundType.STONE;
    protected final legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer blockState;
    private legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState defaultBlockState;

    protected MapColor blockMapColor;

    public Block(Material material, MapColor color) {
        this(material);
        this.blockMapColor = color;
    }

    private static int MCC_STATE_FAILURES;

    public Block(Material material) {
        this.material = material;
        this.blockMapColor = material == null ? null : material.getMaterialMapColor();
        this.translucent = material != null && !material.blocksLight();
        legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer container = null;
        try {
            // overridden by mods before their own fields exist, as in 1.12; a state container that fails to build leaves the plain one
            container = createBlockState();
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Blocks").warn("The block state of {} could not be built: {}", getClass().getName(), e.toString(), MCC_STATE_FAILURES++ < 2 ? e : null);
        }
        this.blockState = container != null ? container : new legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer(this);
        setDefaultState(blockState.getBaseState());
        try {
            this.fullBlock = getDefaultState().isOpaqueCube();
        } catch (RuntimeException | LinkageError e) {
            // a mod's isOpaqueCube reading fields its constructor has not set yet: a full block, as Block's own answer
            this.fullBlock = true;
        }
        this.lightOpacity = fullBlock ? 255 : 0;
    }

    protected legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer createBlockState() {
        return new legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer(this);
    }

    public legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer getBlockState() {
        return blockState;
    }

    protected final void setDefaultState(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        this.defaultBlockState = state;
    }

    public final legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getDefaultState() {
        return defaultBlockState;
    }

    /** The state saved as these 4 metadata bits; mods override both directions. */
    @SuppressWarnings("deprecation")
    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    public int getMetaFromState(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return 0;
    }

    public Material getMaterialLegacy() {
        return material != null ? material : legacyconnector.v1_12_2.net.minecraft.block.material.Material.ROCK;
    }

    public Block setUnlocalizedName(String name) {
        this.unlocalizedName = name;
        return this;
    }

    public String getUnlocalizedName() {
        return "tile." + unlocalizedName;
    }

    public String getTranslationKey() {
        return getUnlocalizedName();
    }

    protected Block setSoundType(SoundType type) {
        this.blockSoundType = type;
        return this;
    }

    public SoundType getSoundType() {
        return blockSoundType;
    }

    public Block setHardness(float hardness) {
        this.blockHardness = hardness;
        if (this.blockResistance < hardness * 5.0F) this.blockResistance = hardness * 5.0F;
        return this;
    }

    public Block setBlockUnbreakable() {
        this.blockHardness = -1.0F;
        return this;
    }

    public Block setResistance(float resistance) {
        this.blockResistance = resistance * 3.0F;
        return this;
    }

    public Block setLightLevel(float level) {
        this.lightValue = (int) (15.0F * level);
        return this;
    }

    public Block setLightOpacity(int opacity) {
        this.lightOpacity = opacity;
        return this;
    }

    public Block setCreativeTab(CreativeTabs tab) {
        this.displayOnCreativeTab = tab;
        return this;
    }

    public CreativeTabs getCreativeTabToDisplayOn() {
        return displayOnCreativeTab;
    }

    /** Forge: whether the state has a tile entity (BlockContainer says yes). */
    public boolean hasTileEntity(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return false;
    }

    public legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity createTileEntity(legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return null;
    }

    public float mcc$hardness() {
        return blockHardness;
    }

    public float mcc$resistance() {
        return blockResistance;
    }

    public int mcc$light() {
        return lightValue;
    }

    public static Block getBlockFromName(String name) {
        if (name == null) return null;
        Block b = REGISTRY.getObject(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(name));
        if (b != null) return b;
        try {
            return getBlockById(Integer.parseInt(name));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Block getBlockById(int id) {
        return REGISTRY.getObjectById(id);
    }

    public static int getIdFromBlock(Block block) {
        return REGISTRY.getIDForObject(block);
    }

    public static Block getBlockFromItem(legacyconnector.v1_12_2.net.minecraft.item.Item item) {
        return item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemBlock ib ? ib.getBlock() : null;
    }

    /**
     * 1.12 packs a state as {@code blockId + (meta << 12)}, which holds 4096 blocks; with more registered (big packs plus the vanilla
     * wrappers) the higher ids use a range of their own above 65535, so every state survives the round trip (BuildCraft's facades
     * check it).
     */
    private static final int MCC$WIDE_STATE_IDS = 65536;

    public static legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getStateById(int id) {
        int blockId = id >= MCC$WIDE_STATE_IDS ? (id - MCC$WIDE_STATE_IDS) >> 4 : id & 4095;
        int meta = id >= MCC$WIDE_STATE_IDS ? (id - MCC$WIDE_STATE_IDS) & 15 : id >> 12 & 15;
        Block block = getBlockById(blockId);
        return block == null ? null : block.getStateFromMeta(meta);
    }

    public static int getStateId(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        Block block = state.getBlock();
        int blockId = getIdFromBlock(block);
        int meta = block.getMetaFromState(state) & 15;
        return blockId < 4096 ? blockId + (meta << 12) : MCC$WIDE_STATE_IDS + (blockId << 4 | meta);
    }

    public Block setTranslationKey(String name) {
        return setUnlocalizedName(name);
    }

    /** Whether the block wants random ticks (recorded; mods read it back through {@link #getTickRandomly}). */
    protected boolean needsRandomTick;
    /** Friction of the block's top (0.6 normal, 0.98 ice). */
    public float slipperiness = 0.6F;

    public Block setTickRandomly(boolean shouldTick) {
        this.needsRandomTick = shouldTick;
        return this;
    }

    public boolean getTickRandomly() {
        return needsRandomTick;
    }

    /** Forge: sets {@link #slipperiness}. */
    public void setDefaultSlipperiness(float slipperiness) {
        this.slipperiness = slipperiness;
    }

    /** Vanilla's form: whether the block is a tile entity provider (Forge asks {@link #hasTileEntity(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState)}). */
    @Deprecated
    public boolean hasTileEntity() {
        return hasTileEntity || this instanceof ITileEntityProvider;
    }

    /** Set by BlockContainer (vanilla's tile entity blocks). */
    protected boolean hasTileEntity;
    /** Whether the default state is an opaque cube (lighting, culling). */
    protected boolean fullBlock;
    /** Whether light passes through (glass, leaves): not opaque. */
    protected boolean translucent;
    /** How fast the block's break particles fall. */
    public float blockParticleGravity = 1.0F;
    /** Forge: the player harvesting this block in harvestBlock (read by getDrops). */
    protected static final ThreadLocal<legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer> harvesters = new ThreadLocal<>();

    public String getLocalizedName() {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(getTranslationKey() + ".name");
    }

    public Block disableStats() {
        enableStats = false;
        return this;
    }

    private final java.util.Map<String, Integer> harvest = new java.util.HashMap<>();

    public void setHarvestLevel(String toolClass, int level) {
        harvest.put(toolClass, level);
    }

    public void setHarvestLevel(String toolClass, int level, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        harvest.put(toolClass, level);
    }

    public String getHarvestTool(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return harvest.isEmpty() ? null : harvest.keySet().iterator().next();
    }

    public int getHarvestLevel(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        String tool = getHarvestTool(state);
        return tool == null ? -1 : harvest.get(tool);
    }

    public CreativeTabs getCreativeTab() {
        return displayOnCreativeTab;
    }

    public Material getMaterial(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return material;
    }

    public int damageDropped(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return 0;
    }

    public float getBlockHardness(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return blockHardness;
    }

    public int getLightValue(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return lightValue;
    }

    public boolean isOpaqueCube(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return true;
    }

    public boolean isFullCube(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return true;
    }

    /** Vanilla: a block whose default state is an opaque cube is a full block. */
    public boolean isFullBlock(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return isOpaqueCube(getDefaultState());
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType getRenderType(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType.MODEL;
    }

    /** The resistance to explosions (a fifth of the value set with setResistance, which multiplied it by three). */
    public float getExplosionResistance(legacyconnector.v1_12_2.net.minecraft.entity.Entity exploder) {
        return blockResistance / 5.0F;
    }

    public void getSubBlocks(CreativeTabs tab, legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> items) {
        items.add(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(this));
    }

    // ------------------------------------------------------------------ gameplay hooks (1.12.2 defaults; mods override; the modern
    // block forwards the ones a mod overrides, see Legacy1122Block). Hooks taking an IBlockAccess wait for that interface (world area).

    /** The random blocks roll (drops). */
    protected static final java.util.Random RANDOM = new java.util.Random();
    /** Whether the block takes its light from its brightest neighbour (slabs, stairs, farmland). */
    protected boolean useNeighborBrightness;

    public boolean onBlockActivated(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ) {
        return false;
    }

    public void onBlockClicked(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
    }

    public void onBlockAdded(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
    }

    /** Called when the block is replaced; the modern block entity is removed by the game itself. */
    public void breakBlock(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
    }

    public void neighborChanged(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, Block block, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos fromPos) {
    }

    public void updateTick(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, java.util.Random rand) {
    }

    /** A random tick: an update tick unless the block says otherwise. */
    public void randomTick(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, java.util.Random random) {
        updateTick(world, pos, state, random);
    }

    public void randomDisplayTick(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, java.util.Random rand) {
    }

    public int tickRate(legacyconnector.v1_12_2.net.minecraft.world.World world) {
        return 10;
    }

    public void onBlockPlacedBy(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase placer,
            legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
    }

    public void onBlockHarvested(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
    }

    public void onPlayerDestroy(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        onBlockDestroyedByPlayer(world, pos, state);
    }

    /** The older MCP name of {@link #onPlayerDestroy} (mods built against those mappings override this one). */
    public void onBlockDestroyedByPlayer(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
    }

    public void onEntityCollision(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        onEntityCollidedWithBlock(world, pos, state, entity);
    }

    /** The older MCP name of {@link #onEntityCollision}. */
    public void onEntityCollidedWithBlock(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn) {
    }

    /** The older MCP name of {@link #onExplosionDestroy}. */
    public void onBlockDestroyedByExplosion(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.world.Explosion explosionIn) {
    }

    /** The older MCP name of {@link #isExceptBlockForAttachWithPiston}. */
    protected static boolean isExcepBlockForAttachWithPiston(Block attachBlock) {
        return isExceptBlockForAttachWithPiston(attachBlock);
    }

    protected static boolean isExceptionBlockForAttaching(Block attachBlock) {
        return attachBlock instanceof BlockShulkerBox || attachBlock instanceof BlockLeaves || mcc$named(attachBlock, "beacon", "cauldron", "glass",
                "glowstone", "ice", "sea_lantern", "stained_glass");
    }

    /** Whether a player may spawn inside the block (not solid, not liquid: signs, flowers). */
    public boolean canSpawnInBlock() {
        Material m = getMaterialLegacy();
        return !m.isSolid() && !m.isLiquid();
    }

    /** Whether the block draws its own breaking cracks (beds, chests in a TESR). */
    public boolean hasCustomBreakingProgress(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return false;
    }

    /** Forge: drops spawned while capturing are collected instead (leaves gather their drops for shears this way). */
    protected static final ThreadLocal<Boolean> captureDrops = ThreadLocal.withInitial(() -> false);
    protected static final ThreadLocal<legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack>> capturedDrops = ThreadLocal.withInitial(legacyconnector.v1_12_2.net.minecraft.util.NonNullList::create);

    /** Starts capturing drops (returns an empty list) or stops and returns what was captured. */
    protected legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> captureDrops(boolean start) {
        if (start) {
            captureDrops.set(true);
            capturedDrops.get().clear();
            return legacyconnector.v1_12_2.net.minecraft.util.NonNullList.create();
        }
        captureDrops.set(false);
        legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> out = legacyconnector.v1_12_2.net.minecraft.util.NonNullList.create();
        out.addAll(capturedDrops.get());
        capturedDrops.get().clear();
        return out;
    }

    /** Forge: the colour a beacon beam takes through this block (null: unchanged). */
    public float[] getBeaconColorMultiplier(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos beaconPos) {
        return null;
    }

    /** Forge: whether the bed block at a position is the foot half. */
    public boolean isBedFoot(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof BlockBed && state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT;
    }

    /** How a block's model is shifted per position (flowers and grass sit off-centre). */
    public enum EnumOffsetType {
        NONE,
        XZ,
        XYZ;
    }

    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.NONE;
    }

    /** Whether the stats count this block (set by disableStats). */
    protected boolean enableStats = true;

    /** Every block state by its numeric id ({@link #getStateId}), as 1.12's identity map answered it. */
    public static final legacyconnector.v1_12_2.net.minecraft.util.ObjectIntIdentityMap<legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState> BLOCK_STATE_IDS =
            new legacyconnector.v1_12_2.net.minecraft.util.ObjectIntIdentityMap<>() {
                @Override
                public int get(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState key) {
                    return key == null ? -1 : getStateId(key);
                }

                @Override
                public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getByValue(int value) {
                    return getStateById(value);
                }
            };

    /** Forge: whether the block sets entities in it on fire (fire and lava). */
    public boolean isBurning(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return this == legacyconnector.v1_12_2.net.minecraft.init.Blocks.FIRE || getMaterialLegacy() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.LAVA
                || getMaterialLegacy() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.FIRE;
    }

    /** Forge: dyes the block (a block with a "color" property takes the colour), true when it changed. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean recolorBlock(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side, legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor color) {
        legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        for (legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty prop : state.getPropertyKeys()) {
            if (prop.getName().equals("color") && prop.getValueClass() == legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.class) {
                if (state.getValue(prop) != color) {
                    world.setBlockState(pos, state.withProperty(prop, color));
                    return true;
                }
            }
        }
        return false;
    }

    /** Forge: the path node type of the block for mobs' path finding (null: decided from the block as vanilla does). */
    public legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNodeType getAiPathNodeType(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return isBurning(world, pos) ? legacyconnector.v1_12_2.net.minecraft.pathfinding.PathNodeType.DAMAGE_FIRE : null;
    }

    /** Forge (client): the block's own particles when hit; false lets the game spawn the default ones. */
    public boolean addHitEffects(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World worldObj,
            legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult target, legacyconnector.v1_12_2.net.minecraft.client.particle.ParticleManager manager) {
        return false;
    }

    /** Forge (client): the block's own particles when broken; false lets the game spawn the default ones. */
    public boolean addDestroyEffects(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.client.particle.ParticleManager manager) {
        return false;
    }

    public void onEntityWalk(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
    }

    public void onFallenUpon(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, float fallDistance) {
    }

    public void onLanded(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
    }

    public boolean eventReceived(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, int id, int param) {
        return false;
    }

    /**
     * Whether the block may be placed here: the block there now can be replaced. Read through the world view (its generation region
     * while decorating: a read of the real level from a generation worker waited on the server thread for good).
     */
    public boolean canPlaceBlockAt(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return world == null || world.mcc$state(pos.mcc$modern()).canBeReplaced();
    }

    public boolean canPlaceBlockOnSide(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return canPlaceBlockAt(world, pos);
    }

    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getStateForPlacement(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY,
            float hitZ, int meta, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase placer) {
        return getStateFromMeta(meta);
    }

    /** Forge's hand-aware form. */
    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getStateForPlacement(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY,
            float hitZ, int meta, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase placer, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer);
    }

    // ------------------------------------------------------------------ drops

    public legacyconnector.v1_12_2.net.minecraft.item.Item getItemDropped(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, java.util.Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.item.Item.getItemFromBlock(this);
    }

    public int quantityDropped(java.util.Random random) {
        return 1;
    }

    public int quantityDroppedWithBonus(int fortune, java.util.Random random) {
        return quantityDropped(random);
    }

    public int quantityDropped(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, int fortune, java.util.Random random) {
        return quantityDroppedWithBonus(fortune, random);
    }

    /** The drops 1.12's default getDrops computes: getItemDropped x quantityDropped, with damageDropped as metadata. */
    public java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> mcc$defaultDrops(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, int fortune) {
        java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> out = new java.util.ArrayList<>();
        java.util.Random rand = RANDOM;
        int count = quantityDropped(state, fortune, rand);
        for (int i = 0; i < count; i++) {
            legacyconnector.v1_12_2.net.minecraft.item.Item item = getItemDropped(state, rand, fortune);
            if (item != null && item != legacyconnector.v1_12_2.net.minecraft.init.Items.AIR) out.add(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, 1, damageDropped(state)));
        }
        return out;
    }

    public final void dropBlockAsItem(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, int fortune) {
        dropBlockAsItemWithChance(world, pos, state, 1.0F, fortune);
    }

    public void dropBlockAsItemWithChance(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, float chance, int fortune) {
        if (world == null || world.isRemote) return;
        for (legacyconnector.v1_12_2.net.minecraft.item.ItemStack drop : mcc$drops(world, pos, state, fortune)) {
            if (world.rand.nextFloat() <= chance) spawnAsEntity(world, pos, drop);
        }
    }

    /** The block's drops through the modern block (which asks the mod's getDrops when it overrides it). */
    protected java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> mcc$drops(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, int fortune) {
        if (mcc$modern instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block b) return b.legacyDrops(world, pos, state, fortune);
        return mcc$defaultDrops(state, fortune);
    }

    /** Drops a stack as an item entity with 1.12's random offset in the block. */
    public static void spawnAsEntity(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        if (world == null || world.isRemote || stack == null || stack.isEmpty()) return;
        if (captureDrops.get()) {
            capturedDrops.get().add(stack);
            return;
        }
        var level = world.mcc$level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel)) return;
        double dx = level.random.nextFloat() * 0.5F + 0.25D;
        double dy = level.random.nextFloat() * 0.5F + 0.25D;
        double dz = level.random.nextFloat() * 0.5F + 0.25D;
        var entity = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz,
                org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack));
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    /** What a player who breaks the block gets: the silk touch stack or the drops (the modern block handles harvesting itself). */
    public void harvestBlock(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity te, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        int fortune = 0;
        boolean silk = false;
        if (stack != null && !stack.isEmpty()) {
            fortune = legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment.getEnchantmentByLocation("fortune"), stack);
            silk = legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment.getEnchantmentByLocation("silk_touch"), stack) > 0;
        }
        if (player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) p.causeFoodExhaustion(0.005F);
        harvesters.set(player);
        try {
            if (silk && canSilkHarvest(world, pos, state, player)) {
                legacyconnector.v1_12_2.net.minecraft.item.ItemStack drop = getSilkTouchDrop(state);
                if (!drop.isEmpty()) spawnAsEntity(world, pos, drop);
            } else {
                dropBlockAsItem(world, pos, state, fortune);
            }
        } finally {
            harvesters.remove();
        }
    }

    public boolean canSilkHarvest(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return canSilkHarvest() && !hasTileEntity(state);
    }

    @Deprecated
    protected boolean canSilkHarvest() {
        return getDefaultState().isFullCube() && !hasTileEntity(getDefaultState());
    }

    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack mcc$silkTouchDrop(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return getSilkTouchDrop(state);
    }

    protected legacyconnector.v1_12_2.net.minecraft.item.ItemStack getSilkTouchDrop(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        legacyconnector.v1_12_2.net.minecraft.item.Item item = legacyconnector.v1_12_2.net.minecraft.item.Item.getItemFromBlock(this);
        if (item == null || item == legacyconnector.v1_12_2.net.minecraft.init.Items.AIR) return legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY;
        int meta = item.getHasSubtypes() ? getMetaFromState(state) : 0;
        return new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, 1, meta);
    }

    /** The stack the pick-block key gives (no IBlockAccess form here: world blocks only). */
    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack getItem(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        legacyconnector.v1_12_2.net.minecraft.item.Item item = legacyconnector.v1_12_2.net.minecraft.item.Item.getItemFromBlock(this);
        return item == null || item == legacyconnector.v1_12_2.net.minecraft.init.Items.AIR ? legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY : new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, 1, damageDropped(state));
    }

    /** Forge: breaking by a player; true when the block was removed. */
    public boolean removedByPlayer(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            boolean willHarvest) {
        onBlockHarvested(world, pos, state, player);
        return world.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR.getDefaultState(), world.isRemote ? 11 : 3);
    }

    public boolean rotateBlock(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing axis) {
        var level = world == null ? null : world.mcc$level();
        if (level == null) return false;
        var mp = pos.mcc$modern();
        var state = level.getBlockState(mp);
        var rotated = state.rotate(level, mp, net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
        if (rotated == state) return false;
        level.setBlockAndUpdate(mp, rotated);
        return true;
    }

    public void onPlantGrow(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos source) {
    }

    public boolean getUseNeighborBrightness(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return useNeighborBrightness;
    }

    public SoundType getSoundType(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return getSoundType();
    }

    public boolean canRenderInLayer(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer layer) {
        return getRenderLayer() == layer;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer getRenderLayer() {
        return legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer.SOLID;
    }

    public boolean isCollidable() {
        return true;
    }

    public boolean canCollideCheck(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, boolean hitIfLiquid) {
        return isCollidable();
    }

    public boolean canProvidePower(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return false;
    }

    public boolean hasComparatorInputOverride(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return false;
    }

    public int getComparatorInputOverride(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState blockState, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return 0;
    }

    public boolean requiresUpdates() {
        return true;
    }

    public boolean isAssociatedBlock(Block other) {
        return this == other;
    }

    public static boolean isEqualTo(Block a, Block b) {
        return a != null && b != null && (a == b || a.isAssociatedBlock(b));
    }

    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState withRotation(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.util.Rotation rot) {
        return state;
    }

    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState withMirror(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.util.Mirror mirror) {
        return state;
    }

    public boolean isToolEffective(String type, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return type != null && type.equals(getHarvestTool(state));
    }

    public float getEnchantPowerBonus(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return 0.0F;
    }

    // ------------------------------------------------------------------ more 1.12.2 block rules

    /** The player harvesting the block on this thread (Forge's harvesters), for mods' getDrops. */
    public static void mcc$harvester(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        if (player == null) harvesters.remove();
        else harvesters.set(player);
    }

    /** How much of the block a player breaks per tick: the modern rule (tool speed, effects, water, air) on the modern state. */
    public float getPlayerRelativeBlockHardness(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        if (player == null || !(player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) || world == null) return 0.0F;
        var level = world.mcc$level();
        var mp = pos.mcc$modern();
        return level.getBlockState(mp).getDestroyProgress(p, level, mp);
    }

    /** Drops experience orbs worth the amount (ores). */
    public void dropXpOnBlockBreak(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, int amount) {
        if (world == null || world.isRemote || amount <= 0) return;
        if (world.mcc$level() instanceof net.minecraft.server.level.ServerLevel level) {
            net.minecraft.world.entity.ExperienceOrb.award(level, net.minecraft.world.phys.Vec3.atCenterOf(pos.mcc$modern()), amount);
        }
    }

    /** Blocks pistons and attached blocks never connect to (shulker boxes, leaves, glass, ...), as 1.12 listed them. */
    public static boolean isExceptBlockForAttachWithPiston(Block block) {
        if (block == null || block.getRegistryName() == null) return false;
        String name = block.getRegistryName().getPath();
        return name.endsWith("shulker_box") || name.equals("leaves") || name.equals("leaves2") || name.equals("barrier") || name.equals("cauldron")
                || name.equals("glass") || name.equals("beacon") || name.equals("sea_lantern") || name.equals("stained_glass") || name.equals("piston")
                || name.equals("sticky_piston") || name.equals("piston_head") || name.equals("melon_block") || name.equals("pumpkin") || name.equals("lit_pumpkin");
    }

    public boolean canEntitySpawn(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return true;
    }

    /** Whether the top face is a full solid face (mobs stand on it). */
    public boolean isTopSolid(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return state.getMaterial().isOpaque() && state.isFullCube();
    }

    public boolean isTranslucent(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return translucent;
    }

    public int getLightOpacity(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return lightOpacity;
    }

    public boolean isBlockNormalCube(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return state.getMaterial().blocksMovement() && state.isFullCube();
    }

    public boolean isNormalCube(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return state.getMaterial().isOpaque() && state.isFullCube() && !state.canProvidePower();
    }

    public boolean causesSuffocation(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return material.blocksMovement() && getDefaultState().isFullCube();
    }

    /** The box an entity collides with, offset to the position, added to the list when it meets the entity's box. */
    protected static void addCollisionBoxToList(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB entityBox,
            java.util.List<legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB> collidingBoxes, legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB blockBox) {
        if (blockBox == null) return;
        legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB box = blockBox.offset(pos);
        if (entityBox.intersects(box)) collidingBoxes.add(box);
    }

    /**
     * The collision boxes of the block at a position (the modern shape of the block there: the legacy collision box once the world
     * area forwards getCollisionBoundingBox), for the entity box given.
     */
    public void addCollisionBoxToList(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB entityBox,
            java.util.List<legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB> collidingBoxes, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, boolean isActualState) {
        if (world == null) return;
        var level = world.mcc$level();
        var mp = pos.mcc$modern();
        for (var box : level.getBlockState(mp).getCollisionShape(level, mp).toAabbs()) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB.mcc$of(box));
        }
    }

    /**
     * The block's bounds when they depend on the state alone (slabs, torches, ladders), or null for a full or world-dependent
     * shape: the modern block's shape until the world area forwards getBoundingBox(IBlockAccess).
     */
    public legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB mcc$staticBounds(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return null;
    }

    /** Leaves near a removed log start to decay (the modern leaves do it by distance on their own). */
    public void beginLeavesDecay(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
    }

    public boolean canBeReplacedByLeaves(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return !state.isFullCube();
    }

    public float getAmbientOcclusionLightValue(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return state.isBlockNormalCube() ? 0.2F : 1.0F;
    }

    public boolean isFullyOpaque(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return state.getMaterial().isOpaque() && state.isFullCube();
    }

    public legacyconnector.v1_12_2.net.minecraft.block.material.MapColor getMapColor(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return blockMapColor != null ? blockMapColor : material.getMaterialMapColor();
    }

    public legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction getPushReaction(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return material.getPushReaction();
    }

    // ------------------------------------------------------------------ hooks taking an IBlockAccess (1.12.2 vanilla and Forge defaults)
    // The modern block calls the ones a mod overrides (bounds, collision box, light, replaceability, drops, friction, pick block,
    // explosion resistance; see Legacy1122Block); the rest are what mods call on their own and on vanilla blocks.

    /** Forge: whether the state counts as air (material AIR). */
    public boolean isAir(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return state.getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.AIR;
    }

    /** Whether placing a block here replaces this one (by material: air, water, tall grass, snow layers...). */
    public boolean isReplaceable(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return world.getBlockState(pos).getMaterial().isReplaceable();
    }

    /** The state with the properties that depend on the neighbours (connections, ...) filled in; the stored state by default. */
    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getActualState(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return state;
    }

    /** Forge: the state with its unlisted (render) properties filled in; the state itself by default. */
    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getExtendedState(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return state;
    }

    /** The outline box, relative to the block (a full cube, or the state-only bounds the block declares). */
    public legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB getBoundingBox(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess source, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        var b = mcc$staticBounds(state);
        return b != null ? b : FULL_BLOCK_AABB;
    }

    /** The collision box, relative to the block (the outline box by default; null for none). */
    public legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB getCollisionBoundingBox(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBoundingBox(state, world, pos);
    }

    /** The highlighted box in world coordinates. */
    public legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB getSelectedBoundingBox(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return state.getBoundingBox(world, pos).offset(pos);
    }

    /** How the face supports attachments (SOLID by default, as in 1.12.2). */
    public legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape getBlockFaceShape(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing face) {
        Boolean vanilla = org.kylepitcock.legacyconnector.v1_12_2.bridge.BlockFaces1122.vanillaSturdy(state, world, pos, face);
        return vanilla == null || vanilla ? legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape.SOLID
                : legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape.UNDEFINED;
    }

    /** Forge: whether the side is solid (torches, levers attach): a vanilla block's modern face, else a normal cube or a solid top. */
    public boolean isSideSolid(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        Boolean vanilla = org.kylepitcock.legacyconnector.v1_12_2.bridge.BlockFaces1122.vanillaSturdy(state, world, pos, side);
        if (vanilla != null) return vanilla;
        if (side == legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP && state.isTopSolid()) return true;
        return isNormalCube(state, world, pos);
    }

    /** Forge: a normal cube at a position (the state's own answer by default). */
    public boolean isNormalCube(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return state.isNormalCube();
    }

    public boolean isLeaves(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return state.getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.LEAVES || mcc$vanillaTagged(net.minecraft.tags.BlockTags.LEAVES);
    }

    /** Whether this is a vanilla block wrapper whose modern block is in the tag (vanilla logs and leaves; read at runtime only). */
    private boolean mcc$vanillaTagged(net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag) {
        return mcc$modern instanceof net.minecraft.world.level.block.Block b && !(b instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block)
                && b.defaultBlockState().is(tag);
    }

    /** Forge: whether the block is a log (trees grow around it, leaves stay); false unless overridden. */
    public boolean isWood(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return mcc$vanillaTagged(net.minecraft.tags.BlockTags.LOGS);
    }

    public boolean isFoliage(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return false;
    }

    /** Whether the side is drawn: not when the box reaches that side and the neighbour hides it. */
    public boolean shouldSideBeRendered(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        var bb = state.getBoundingBox(world, pos);
        switch (side) {
            case DOWN -> {
                if (bb.minY > 0.0D) return true;
            }
            case UP -> {
                if (bb.maxY < 1.0D) return true;
            }
            case NORTH -> {
                if (bb.minZ > 0.0D) return true;
            }
            case SOUTH -> {
                if (bb.maxZ < 1.0D) return true;
            }
            case WEST -> {
                if (bb.minX > 0.0D) return true;
            }
            case EAST -> {
                if (bb.maxX < 1.0D) return true;
            }
            default -> {
            }
        }
        var other = pos.offset(side);
        return !world.getBlockState(other).doesSideBlockRendering(world, other, side.getOpposite());
    }

    /** Forge: whether this block hides the neighbour's face against it (opaque cubes do). */
    public boolean doesSideBlockRendering(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing face) {
        return state.isOpaqueCube();
    }

    /** Forge: the drops of the block at a position, added to the list (getItemDropped x quantityDropped with damageDropped). */
    public void getDrops(legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> drops,
            legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, int fortune) {
        drops.addAll(mcc$defaultDrops(state, fortune));
    }

    /** Forge's older form: the drops as a new list. */
    @Deprecated
    public java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getDrops(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, int fortune) {
        legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> ret = legacyconnector.v1_12_2.net.minecraft.util.NonNullList.create();
        getDrops(ret, world, pos, state, fortune);
        return ret;
    }

    /** Forge: the light the block gives at a position (its state's light by default). */
    public int getLightValue(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return state.getLightValue();
    }

    public int getLightOpacity(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return state.getLightOpacity();
    }

    /** Forge: the friction of the top for an entity (the block's slipperiness by default). */
    public float getSlipperiness(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return slipperiness;
    }

    /** Forge: the resistance against an explosion at a position (the block-wide resistance by default). */
    public float getExplosionResistance(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.entity.Entity exploder, legacyconnector.v1_12_2.net.minecraft.world.Explosion explosion) {
        return getExplosionResistance(exploder);
    }

    /** Forge: whether fences, walls and panes beside it connect on that side (decided by the face shape unless overridden). */
    public boolean canBeConnectedTo(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        return false;
    }

    /** Forge: whether the player's held tool harvests the block (drops it): no tool needed, or the right tool of a high enough level. */
    public boolean canHarvestBlock(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        var state = world.getBlockState(pos);
        state = state.getBlock().getActualState(state, world, pos);
        if (state.getMaterial().isToolNotRequired()) return true;
        var stack = player.getHeldItemMainhand();
        String tool = getHarvestTool(state);
        if (stack.isEmpty() || tool == null) return player.canHarvestBlock(state);
        int toolLevel = stack.getItem().getHarvestLevel(stack, tool, player, state);
        if (toolLevel < 0) return player.canHarvestBlock(state);
        return toolLevel >= getHarvestLevel(state);
    }

    /** Whether entities walk through the block (materials that do not block movement). */
    public boolean isPassable(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return !getMaterialLegacy().blocksMovement();
    }

    /** Forge: whether an entity (the dragon, the wither) may destroy the block; everything but the end's own blocks may. */
    public boolean canEntityDestroy(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return this != legacyconnector.v1_12_2.net.minecraft.init.Blocks.BEDROCK && this != legacyconnector.v1_12_2.net.minecraft.init.Blocks.END_PORTAL
                && this != legacyconnector.v1_12_2.net.minecraft.init.Blocks.END_PORTAL_FRAME && this != legacyconnector.v1_12_2.net.minecraft.init.Blocks.COMMAND_BLOCK;
    }

    /** Forge: the stack the pick-block key gives (the block's own item by default). */
    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack getPickBlock(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult target, legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return getItem(world, pos, state);
    }

    /** Forge: weak (indirect) redstone power out of a side; none by default. */
    public int getWeakPower(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return 0;
    }

    public int getStrongPower(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return 0;
    }

    /** Forge: whether redstone dust connects to the block from that side (power sources do). */
    public boolean canConnectRedstone(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return state.canProvidePower() && side != null;
    }


    /** Forge: whether the block burns (by its material). */
    public boolean isFlammable(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing face) {
        return getFlammability(world, pos, face) > 0;
    }

    /** Forge: how easily fire burns the block away (vanilla's fire table through the modern fire block). */
    public int getFlammability(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing face) {
        return mcc$modern instanceof net.minecraft.world.level.block.Block b
                ? b.defaultBlockState().getFlammability(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.UP) : 0;
    }

    /** Forge: how easily fire spreads to the block. */
    public int getFireSpreadSpeed(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing face) {
        return mcc$modern instanceof net.minecraft.world.level.block.Block b
                ? b.defaultBlockState().getFireSpreadSpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.UP) : 0;
    }

    /** Forge: whether fire on top burns forever (netherrack, magma). */
    public boolean isFireSource(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return side == legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP && (this == legacyconnector.v1_12_2.net.minecraft.init.Blocks.NETHERRACK
                || this == legacyconnector.v1_12_2.net.minecraft.init.Blocks.MAGMA);
    }

    /** Forge: whether the block is a ladder for an entity (BlockLadder and vines are). */
    public boolean isLadder(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return this instanceof BlockLadder || this == legacyconnector.v1_12_2.net.minecraft.init.Blocks.LADDER || this == legacyconnector.v1_12_2.net.minecraft.init.Blocks.VINE;
    }

    /** Forge: whether a beacon pyramid may be built of it (iron, gold, diamond and emerald blocks). */
    public boolean isBeaconBase(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos beacon) {
        return this == legacyconnector.v1_12_2.net.minecraft.init.Blocks.EMERALD_BLOCK || this == legacyconnector.v1_12_2.net.minecraft.init.Blocks.GOLD_BLOCK
                || this == legacyconnector.v1_12_2.net.minecraft.init.Blocks.DIAMOND_BLOCK || this == legacyconnector.v1_12_2.net.minecraft.init.Blocks.IRON_BLOCK;
    }

    /** Whether the block is the vanilla block of one of these 1.12 registry paths (vanilla blocks are wrappers, compared by name). */
    public static boolean mcc$named(Block block, String... paths) {
        if (block == null) return false;
        var name = block.getRegistryName();
        String modern = block.mcc$modern instanceof net.minecraft.world.level.block.Block b && !(b instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block)
                ? net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath() : null;
        for (String p : paths) {
            if (name != null && "minecraft".equals(name.getNamespace()) && p.equals(name.getPath())) return true;
            if (modern != null && MCC$MODERN_NAMES.getOrDefault(p, java.util.List.of(p)).stream().anyMatch(m -> m.equals(modern)
                    || m.equals("*_terracotta") && modern.endsWith("_terracotta") && !modern.endsWith("glazed_terracotta"))) {
                return true;
            }
        }
        return false;
    }

    /** The modern blocks a 1.12 vanilla name became (a wrapper made from the modern block is named after it). */
    private static final java.util.Map<String, java.util.List<String>> MCC$MODERN_NAMES = java.util.Map.of("grass", java.util.List.of("grass_block"),
            "dirt", java.util.List.of("dirt", "coarse_dirt", "podzol"), "sand", java.util.List.of("sand", "red_sand"), "hardened_clay",
            java.util.List.of("terracotta"), "stained_hardened_clay", java.util.List.of("*_terracotta"), "reeds", java.util.List.of("sugar_cane"));

    /** Forge: whether the plant may grow on this block (cactus on sand, crops on farmland, plains plants on grass and dirt...). */
    public boolean canSustainPlant(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing direction,
            legacyconnector.v1_12_2.net.minecraftforge.common.IPlantable plantable) {
        var plantPos = pos.offset(direction);
        var plant = plantable.getPlant(world, plantPos);
        var plantType = plantable.getPlantType(world, plantPos);
        Block plantBlock = plant == null ? null : plant.getBlock();
        if (mcc$named(plantBlock, "cactus")) return mcc$named(this, "cactus", "sand");
        if (mcc$named(plantBlock, "reeds") && mcc$named(this, "reeds")) return true;
        if (plantable instanceof BlockBush bush && bush.canSustainBush(state)) return true;
        if (plantType == null) return false;
        switch (plantType) {
            case Desert:
                return mcc$named(this, "sand", "hardened_clay", "stained_hardened_clay");
            case Nether:
                return mcc$named(this, "soul_sand");
            case Crop:
                return mcc$named(this, "farmland");
            case Cave:
                return state.isSideSolid(world, pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP);
            case Plains:
                return mcc$named(this, "grass", "dirt", "farmland");
            case Water:
                return state.getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.WATER;
            case Beach: {
                boolean isBeach = mcc$named(this, "grass", "dirt", "sand");
                if (!isBeach) return false;
                for (var side : legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.Plane.HORIZONTAL) {
                    if (world.getBlockState(pos.offset(side)).getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.WATER) return true;
                }
                return false;
            }
            default:
                return false;
        }
    }

    /** Forge: whether the soil hydrates crops (wet farmland). */
    public boolean isFertile(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        if (!mcc$named(this, "farmland")) return false;
        var modern = world.mcc$state(org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos));
        return modern.hasProperty(net.minecraft.world.level.block.FarmBlock.MOISTURE) && modern.getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) > 0;
    }

    /** Forge: the block is blown up: it is removed and told (vanilla's onBlockDestroyedByExplosion). */
    public void onBlockExploded(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.world.Explosion explosion) {
        world.setBlockToAir(pos);
        onExplosionDestroy(world, pos, explosion);
    }

    /** Called after an explosion removed the block (TNT primes itself here). */
    public void onExplosionDestroy(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.world.Explosion explosionIn) {
        onBlockDestroyedByExplosion(worldIn, pos, explosionIn);
    }

    /** Whether an explosion drops the block's items (by chance) rather than destroying them. */
    public boolean canDropFromExplosion(legacyconnector.v1_12_2.net.minecraft.world.Explosion explosionIn) {
        return true;
    }

    /** Forge: whether a torch may stand on top (solid tops, fences, walls and glass). */
    public boolean canPlaceTorchOnTop(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        if (state.isTopSolid() || state.getBlockFaceShape(world, pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP)
                == legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape.SOLID) {
            return this != legacyconnector.v1_12_2.net.minecraft.init.Blocks.END_GATEWAY && this != legacyconnector.v1_12_2.net.minecraft.init.Blocks.LIT_PUMPKIN;
        }
        return mcc$named(this, "glass", "stained_glass", "cobblestone_wall", "fence", "nether_brick_fence", "spruce_fence", "birch_fence", "jungle_fence",
                "dark_oak_fence", "acacia_fence");
    }

    /** Forge: whether leaves next to the block stay (logs sustain them). */
    public boolean canSustainLeaves(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return mcc$vanillaTagged(net.minecraft.tags.BlockTags.LOGS);
    }

    /** Forge: whether ore generation may replace the block (stone for the overworld's ores). */
    public boolean isReplaceableOreGen(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            com.google.common.base.Predicate<legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState> target) {
        return target.apply(state);
    }

    /** Forge: a neighbour's tile entity changed (comparators read inventories through walls). */
    public void onNeighborChange(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos neighbor) {
    }

    /** Forge: whether the block wants {@link #onNeighborChange} when a neighbour's tile entity changes. */
    public boolean getWeakChanges(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return false;
    }

    /** The hit of the ray from start to end on the block's box at the position (null for a miss). */
    public legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult collisionRayTrace(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState blockState,
            legacyconnector.v1_12_2.net.minecraft.world.World worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d start, legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d end) {
        return rayTrace(pos, start, end, blockState.getBoundingBox(worldIn, pos));
    }

    /** The hit of a ray on a block-relative box placed at the position, or null. */
    protected legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult rayTrace(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d start, legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d end,
            legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB boundingBox) {
        if (boundingBox == null) return null;
        var box = boundingBox.offset(pos).mcc$modern();
        var from = start.mcc$modern();
        var to = end.mcc$modern();
        var hit = net.minecraft.world.phys.AABB.clip(java.util.List.of(box), from, to, net.minecraft.core.BlockPos.ZERO);
        if (hit == null) return null;
        return new legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult(legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d.mcc$of(hit.getLocation()),
                legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.byIndex(hit.getDirection().get3DDataValue()), pos);
    }

    /** The light of the position packed for rendering (sky << 20 | block << 4), with the block's own light as the minimum. */
    public int getPackedLightmapCoords(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess source,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return source.getCombinedLight(pos, state.getLightValue(source, pos));
    }

    /** Where the model is drawn off-centre (flowers, tall grass are offset at random); no offset by default. */
    public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getOffset(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess worldIn, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d.ZERO;
    }

    /** Forge: whether the block on top of a chest keeps it shut (opaque solid faces do). */
    public boolean doesSideBlockChestOpening(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState blockState,
            legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return isSideSolid(blockState, world, pos, side);
    }

    /** Tooltip lines of the block's item (client; a server never asks). */
    public void addInformation(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World player,
            java.util.List<String> tooltip, legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag advanced) {
    }

    /** Forge: whether pistons drag neighbours along with this block (slime). */
    public boolean isStickyBlock(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return mcc$named(this, "slime");
    }

    /** Forge: the experience the block drops when broken (ores override it); none by default. */
    public int getExpDrop(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, int fortune) {
        return 0;
    }

    /** Forge: the facings rotateBlock accepts (those of a FACING property, if any). */
    public legacyconnector.v1_12_2.net.minecraft.util.EnumFacing[] getValidRotations(legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        var state = world.getBlockState(pos);
        for (var prop : state.getPropertyKeys()) {
            if (prop.getName().equals("facing") && prop.getValueClass() == legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.class) {
                @SuppressWarnings("unchecked")
                java.util.Collection<legacyconnector.v1_12_2.net.minecraft.util.EnumFacing> values =
                        (java.util.Collection<legacyconnector.v1_12_2.net.minecraft.util.EnumFacing>) prop.getAllowedValues();
                return values.toArray(new legacyconnector.v1_12_2.net.minecraft.util.EnumFacing[0]);
            }
        }
        return new legacyconnector.v1_12_2.net.minecraft.util.EnumFacing[0];
    }

    /** Forge: whether the block is a bed for the entity (vanilla beds). */
    public boolean isBed(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.Entity player) {
        return mcc$vanillaTagged(net.minecraft.tags.BlockTags.BEDS);
    }

    /** Forge: the direction a bed's head points (its FACING, else north). */
    public legacyconnector.v1_12_2.net.minecraft.util.EnumFacing getBedDirection(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        for (var prop : state.getPropertyKeys()) {
            if (prop.getName().equals("facing") && state.getValue(prop) instanceof legacyconnector.v1_12_2.net.minecraft.util.EnumFacing f) return f;
        }
        return legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.NORTH;
    }

    /** Forge: whether an entity counts as inside a material here (null: decide by the block's material). */
    public Boolean isEntityInsideMaterial(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos blockpos,
            legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState iblockstate, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, double yToTest,
            legacyconnector.v1_12_2.net.minecraft.block.material.Material materialIn, boolean testingHead) {
        return null;
    }

    /** Forge: whether an AABB counts as inside a material here (null: decide by the block's material). */
    public Boolean isAABBInsideMaterial(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB boundingBox, legacyconnector.v1_12_2.net.minecraft.block.material.Material materialIn) {
        return null;
    }

    /** How the block pushes an entity moving through it (flowing liquids push); unchanged by default. */
    public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d modifyAcceleration(legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn,
            legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d motion) {
        return motion;
    }

    /** Forge: whether the block passes weak power through (normal cubes do). */
    public boolean shouldCheckWeakPower(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return state.isNormalCube();
    }

    /** Forge: spawns the block's own particles where an entity lands on it; true replaces the default dust (none by default). */
    public boolean addLandingEffects(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.WorldServer worldObj,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos blockPosition, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState iblockstate,
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity, int numberOfParticles) {
        return false;
    }

    /** Forge: a block next to an observer-like block changed (comparators and observers hear it); nothing by default. */
    public void observedNeighborChange(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState observerState, legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos observerPos, Block changedBlock, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos changedBlockPos) {
    }

    /**
     * Forge: the fog colour while the camera is inside this block: water's blue (lighter with respiration), lava's red, otherwise
     * the colour the game computed.
     */
    public legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getFogColor(legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d originalColor, float partialTicks) {
        var material = state.getMaterial();
        if (material == legacyconnector.v1_12_2.net.minecraft.block.material.Material.WATER) {
            float f = 0.0F;
            if (entity instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase living) {
                f = legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.getRespirationModifier(living) * 0.2F;
            }
            return new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(0.02F + f, 0.02F + f, 0.2F + f);
        }
        if (material == legacyconnector.v1_12_2.net.minecraft.block.material.Material.LAVA) {
            return new legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d(0.6F, 0.1F, 0.0F);
        }
        return originalColor;
    }
}
