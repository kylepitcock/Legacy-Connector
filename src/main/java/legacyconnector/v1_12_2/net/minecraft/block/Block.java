package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

/** 1.12.2 {@code Block}: a registry entry with a material, hardness/resistance, light and a creative tab. */
public class Block extends IForgeRegistryEntry.Impl<Block> {
    public static final legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespacedDefaultedByKey<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, Block> REGISTRY = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.view(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS, "air");
    protected final Material blockMaterial;
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
        this.blockMaterial = material;
        legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer container = null;
        try {
            // overridden by mods before their own fields exist, as in 1.12; a state container that fails to build leaves the plain one
            container = createBlockState();
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Blocks").warn("The block state of {} could not be built: {}", getClass().getName(), e.toString(), MCC_STATE_FAILURES++ < 2 ? e : null);
        }
        this.blockState = container != null ? container : new legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer(this);
        setDefaultState(blockState.getBaseState());
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
        return blockMaterial != null ? blockMaterial : legacyconnector.v1_12_2.net.minecraft.block.material.Material.ROCK;
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
        return this instanceof ITileEntityProvider;
    }

    public String getLocalizedName() {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(getTranslationKey() + ".name");
    }

    public Block disableStats() {
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
        return blockMaterial;
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

    public void getSubBlocks(CreativeTabs tab, legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> items) {
        items.add(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(this));
    }
}
